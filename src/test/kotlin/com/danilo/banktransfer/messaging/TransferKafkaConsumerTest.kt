package com.danilo.banktransfer.messaging

import com.danilo.banktransfer.application.TransferService
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.domain.model.TransferCompletedEvent
import com.danilo.banktransfer.domain.model.TransferFailedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.time.Instant

class TransferKafkaConsumerTest {
    
    private lateinit var transferService: TransferService
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    private lateinit var sqsPublisher: TransferSqsPublisher
    private lateinit var objectMapper: ObjectMapper
    private lateinit var consumer: TransferKafkaConsumer
    
    private val validMessage = """{
        "transferId":"tf-kafka-001",
        "sourceAccountId":"acc-001",
        "destinationAccountId":"acc-002",
        "amount":100.00,
        "currency":"BRL",
        "requestedAt":"2026-08-31T20:00:00Z"
    }"""
    
    @BeforeEach
    fun setup() {
        transferService = mockk()
        kafkaTemplate = mockk()
        sqsPublisher = mockk()
        objectMapper = ObjectMapper().also { it.registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule()) }
        consumer = TransferKafkaConsumer(transferService, kafkaTemplate, sqsPublisher, objectMapper)
    }
    
    @Test
    fun `should consume and process valid transfer`() {
        // Given
        val successEvent = TransferCompletedEvent(
            transferId = "tf-kafka-001",
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            completedAt = Instant.now()
        )
        
        every { transferService.processTransfer(any()) } returns TransferService.Result.Success(successEvent)
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk(relaxed = true)
        
        // When
        consumer.consumeTransferRequest(validMessage)
        
        // Then
        verify(atLeast = 1) { kafkaTemplate.send(any(), any(), any()) }
    }
    
    @Test
    fun `should handle transfer failure and publish to DLQ`() {
        // Given
        val failureEvent = TransferFailedEvent(
            transferId = "tf-kafka-001",
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            failureReason = "Account not found",
            failedAt = Instant.now()
        )
        
        every { transferService.processTransfer(any()) } returns TransferService.Result.Failure(failureEvent)
        every { sqsPublisher.publishTransferFailed(any()) } just runs
        
        // When
        consumer.consumeTransferRequest(validMessage)
        
        // Then
        verify(atLeast = 1) { sqsPublisher.publishTransferFailed(any()) }
    }
    
    @Test
    fun `should handle malformed message gracefully`() {
        // Given
        val malformedMessage = "{invalid json"
        every { sqsPublisher.publishTransferFailed(any()) } just runs
        
        // When & Then
        try {
            consumer.consumeTransferRequest(malformedMessage)
        } catch (e: Exception) {
            // Expected
        }
    }
}
