package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.domain.dto.TransferResponseDTO
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/transfers")
class TransferController(
    private val kafkaTemplate: KafkaTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun createTransfer(@RequestBody request: TransferRequestDTO): ResponseEntity<TransferResponseDTO> {
        logger.info("Received transfer request: ${request.transferId}")

        return try {
            // Publish to Kafka for async processing
            val message = objectMapper.writeValueAsString(request)
            kafkaTemplate.send("transfer-requested", request.transferId, message)

            logger.info("Transfer request published to Kafka: ${request.transferId}")

            ResponseEntity.status(HttpStatus.ACCEPTED).body(
                TransferResponseDTO(
                    transferId = request.transferId,
                    status = "PENDING",
                    message = "Transfer request received and queued for processing"
                )
            )
        } catch (e: Exception) {
            logger.error("Error publishing transfer request: ${e.message}", e)
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                TransferResponseDTO(
                    transferId = request.transferId,
                    status = "ERROR",
                    message = "Failed to process transfer request: ${e.message}"
                )
            )
        }
    }
}
