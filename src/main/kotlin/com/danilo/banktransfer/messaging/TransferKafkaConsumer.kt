package com.danilo.banktransfer.messaging

import com.danilo.banktransfer.application.TransferService
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.infrastructure.service.DeadLetterService
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.handler.annotation.Header
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Service

@Service
class TransferKafkaConsumer(
    private val transferService: TransferService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val sqsPublisher: TransferSqsPublisher,
    private val deadLetterService: DeadLetterService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["transfer-requested"],
        groupId = "bank-transfer-api",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeTransferRequest(
        @Payload message: String,
        @Header(KafkaHeaders.RECEIVED_TOPIC) topic: String,
        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) partition: Int,
        @Header(KafkaHeaders.OFFSET) offset: Long,
        acknowledgment: Acknowledgment
    ) {
        logger.info("Received transfer request from topic=$topic, partition=$partition, offset=$offset")

        try {
            // 1. DESERIALIZE: Parse JSON to TransferRequestedEvent
            val event = try {
                objectMapper.readValue(message, TransferRequestedEvent::class.java)
            } catch (e: JsonParseException) {
                logger.error("Failed to parse JSON message at offset=$offset: ${e.message}")
                // Poison message - cannot recover, send to DLQ
                deadLetterService.sendKafkaFailureToDLQ(
                    topic = topic,
                    partition = partition,
                    offset = offset,
                    messageBody = message,
                    exception = e
                )
                throw e  // Don't acknowledge - will be retried then sent to DLQ
            } catch (e: JsonMappingException) {
                logger.error("Failed to map JSON to TransferRequestedEvent at offset=$offset: ${e.message}")
                // Schema mismatch - poison message
                deadLetterService.sendKafkaFailureToDLQ(
                    topic = topic,
                    partition = partition,
                    offset = offset,
                    messageBody = message,
                    exception = e
                )
                throw e
            }

            logger.debug("Parsed event: transferId=${event.transferId}")

            // 2. PROCESS: Call business logic
            val result = transferService.processTransfer(event)

            // 3. PUBLISH RESULT: Send to appropriate topic/queue based on result
            when (result) {
                is TransferService.Result.Success -> {
                    logger.info("Transfer ${event.transferId} completed successfully, publishing to kafka")
                    publishTransferCompleted(result.event)
                }
                is TransferService.Result.Failure -> {
                    logger.warn("Transfer ${event.transferId} failed, sending to SQS")
                    publishTransferFailed(result.event)
                }
            }

            // 4. ACKNOWLEDGE: Only if everything succeeded
            // This advances the offset in Kafka, marking this message as processed
            acknowledgment.acknowledge()
            logger.info("Transfer ${event.transferId} fully processed, offset committed")

        } catch (e: Exception) {
            logger.error(
                "Critical error processing transfer message at offset=$offset: ${e.message}",
                e
            )
            // ❌ DO NOT ACKNOWLEDGE
            // Kafka will retry this message (up to max retries)
            // After max retries, the message goes to DLQ via KafkaRetryConfig
            throw e
        }
    }

    /**
     * Publish transfer completed event to Kafka topic
     * Uses .get() to ensure message was sent before returning
     */
    private fun publishTransferCompleted(event: com.danilo.banktransfer.domain.model.TransferCompletedEvent) {
        try {
            val messageJson = objectMapper.writeValueAsString(event)
            
            // Use .get() to wait for confirmation (blocks until broker confirms)
            val sendResult = kafkaTemplate.send("transfer-completed", event.transferId, messageJson)
            sendResult.get()  // Blocks and throws if failed
            
            logger.info("Transfer completed event published for transferId=${event.transferId}")
        } catch (e: Exception) {
            logger.error("Failed to publish transfer completed event: ${e.message}", e)
            // Even if Kafka send fails, we already acknowledged the incoming message
            // This means the transfer is committed to DB but completion event failed to publish
            // TODO: Add retry logic or DLQ for missed completion events
            throw e
        }
    }

    /**
     * Publish transfer failed event to SQS
     */
    private fun publishTransferFailed(event: com.danilo.banktransfer.domain.model.TransferFailedEvent) {
        try {
            sqsPublisher.publishTransferFailed(event)
            logger.info("Transfer failed event published to SQS for transferId=${event.transferId}")
        } catch (e: Exception) {
            logger.error("Failed to publish transfer failed event to SQS: ${e.message}", e)
            // Even if SQS send fails, we already acknowledged the incoming message
            // This means the transfer is committed to DB but failure event failed to publish
            // TODO: Add retry logic or DLQ for missed failure events
            throw e
        }
    }
}
