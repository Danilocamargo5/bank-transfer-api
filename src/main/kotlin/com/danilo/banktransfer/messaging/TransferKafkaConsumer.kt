package com.danilo.banktransfer.messaging

import com.danilo.banktransfer.application.TransferService
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class TransferKafkaConsumer(
    private val transferService: TransferService,
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val sqsPublisher: TransferSqsPublisher,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(
        topics = ["transfer-requested"],
        groupId = "bank-transfer-api",
        containerFactory = "kafkaListenerContainerFactory"
    )
    fun consumeTransferRequest(message: String) {
        logger.info("Received transfer request: $message")

        try {
            val event = objectMapper.readValue(message, TransferRequestedEvent::class.java)
            
            val result = transferService.processTransfer(event)

            when (result) {
                is TransferService.Result.Success -> {
                    logger.info("Transfer ${event.transferId} completed, publishing to kafka")
                    val completedMessage = objectMapper.writeValueAsString(result.event)
                    kafkaTemplate.send("transfer-completed", result.event.transferId, completedMessage)
                }
                is TransferService.Result.Failure -> {
                    logger.warn("Transfer ${event.transferId} failed, sending to SQS")
                    sqsPublisher.publishTransferFailed(result.event)
                }
            }
        } catch (e: Exception) {
            logger.error("Error processing transfer message: ${e.message}", e)
            // Could implement DLQ logic here for poison messages
        }
    }
}
