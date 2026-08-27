package com.danilo.banktransfer.messaging

import com.danilo.banktransfer.domain.model.TransferCompletedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service

@Service
class TransferKafkaProducer(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun publishTransferCompleted(event: TransferCompletedEvent) {
        try {
            val message = objectMapper.writeValueAsString(event)
            kafkaTemplate.send("transfer-completed", event.transferId, message)
            logger.info("Published transfer-completed event for transferId: ${event.transferId}")
        } catch (e: Exception) {
            logger.error("Error publishing transfer-completed event: ${e.message}", e)
            throw e
        }
    }
}
