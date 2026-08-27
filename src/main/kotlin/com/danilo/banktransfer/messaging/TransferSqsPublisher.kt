package com.danilo.banktransfer.messaging

import com.danilo.banktransfer.domain.model.TransferFailedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest

@Service
class TransferSqsPublisher(
    private val sqsClient: SqsClient,
    private val objectMapper: ObjectMapper,
    @Value("\${aws.sqs.queue.transfer-failed}")
    private val transferFailedQueue: String,
    @Value("\${aws.sqs.endpoint}")
    private val sqsEndpoint: String,
    @Value("\${aws.region}")
    private val region: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun publishTransferFailed(event: TransferFailedEvent) {
        try {
            val message = objectMapper.writeValueAsString(event)
            val queueUrl = getQueueUrl(transferFailedQueue)
            
            val request = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(message)
                .build()

            sqsClient.sendMessage(request)
            logger.info("Published transfer-failed event to SQS for transferId: ${event.transferId}")
        } catch (e: Exception) {
            logger.error("Error publishing transfer-failed event to SQS: ${e.message}", e)
            throw e
        }
    }

    private fun getQueueUrl(queueName: String): String {
        // LocalStack SQS URL format
        return "$sqsEndpoint/000000000000/$queueName"
    }
}
