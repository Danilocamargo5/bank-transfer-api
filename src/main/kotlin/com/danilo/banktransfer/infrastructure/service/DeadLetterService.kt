package com.danilo.banktransfer.infrastructure.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import java.time.Instant

@Service
class DeadLetterService(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.dlq.url}")
    private val dlqUrl: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Send critical failure to Dead Letter Queue for manual intervention
     * 
     * Used when:
     * - Both save and rollback fail (data inconsistency)
     * - Unrecoverable errors occur
     * - Manual investigation is needed
     */
    fun sendCriticalFailureToDLQ(
        transferId: String,
        sourceAccountId: String,
        destinationAccountId: String,
        amount: String,
        currency: String,
        failureReason: String,
        severity: String = "CRITICAL"
    ) {
        try {
            val dlqMessage = mapOf(
                "transferId" to transferId,
                "sourceAccountId" to sourceAccountId,
                "destinationAccountId" to destinationAccountId,
                "amount" to amount,
                "currency" to currency,
                "failureReason" to failureReason,
                "severity" to severity,
                "timestamp" to Instant.now().toString(),
                "requiresManualIntervention" to true
            )

            val messageBody = objectMapper.writeValueAsString(dlqMessage)

            val request = SendMessageRequest.builder()
                .queueUrl(dlqUrl)
                .messageBody(messageBody)
                .messageGroupId(transferId)  // For FIFO queues, keeps messages ordered
                .messageDeduplicationId("${transferId}-${System.currentTimeMillis()}")  // Prevent duplicates
                .build()

            sqsClient.sendMessage(request)

            logger.error(
                "CRITICAL FAILURE sent to DLQ for manual review: " +
                "transferId=$transferId, severity=$severity, reason=$failureReason"
            )
        } catch (e: Exception) {
            logger.error(
                "CATASTROPHIC: Failed to send critical failure to DLQ! " +
                "Transfer $transferId may be in inconsistent state. " +
                "Manual DBA intervention REQUIRED IMMEDIATELY!",
                e
            )
            
            // Even if DLQ send fails, we log it - ops team must see this error
            throw IllegalStateException(
                "CATASTROPHIC: Could not send critical failure to DLQ for transfer $transferId. " +
                "System needs immediate manual intervention.",
                e
            )
        }
    }

    /**
     * Send failed message from Kafka to DLQ
     * Used by KafkaRetryConfig when message fails after all retries
     */
    fun sendKafkaFailureToDLQ(
        topic: String,
        partition: Int,
        offset: Long,
        messageBody: String,
        exception: Exception
    ) {
        try {
            val dlqMessage = mapOf(
                "originalTopic" to topic,
                "partition" to partition,
                "offset" to offset,
                "messageBody" to messageBody,
                "exceptionType" to (exception::class.simpleName ?: "UnknownException"),
                "exceptionMessage" to (exception.message ?: "No message"),
                "timestamp" to Instant.now().toString(),
                "severity" to "HIGH"
            )

            val messageBodyJson = objectMapper.writeValueAsString(dlqMessage)

            val request = SendMessageRequest.builder()
                .queueUrl(dlqUrl)
                .messageBody(messageBodyJson)
                .build()

            sqsClient.sendMessage(request)

            logger.error(
                "Kafka failure sent to DLQ: topic=$topic, partition=$partition, offset=$offset, " +
                "exception=${exception::class.simpleName}"
            )
        } catch (e: Exception) {
            logger.error(
                "Failed to send Kafka failure to DLQ! " +
                "Original message: topic=$topic, partition=$partition, offset=$offset",
                e
            )
            throw e
        }
    }
}
