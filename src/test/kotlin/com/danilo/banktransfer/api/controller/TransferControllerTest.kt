package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import com.danilo.banktransfer.infrastructure.metrics.TransferMetrics
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.justRuns
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.HttpStatus
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TransferControllerTest {
    
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    private lateinit var transferRepository: TransferRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var transferMetrics: TransferMetrics
    private lateinit var transferController: TransferController
    
    private val validRequest = TransferRequestDTO(
        transferId = "tf-test-001",
        sourceAccountId = "acc-001",
        destinationAccountId = "acc-002",
        amount = BigDecimal("100.00"),
        currency = "BRL",
        requestedAt = Instant.now()
    )
    
    @BeforeEach
    fun setup() {
        kafkaTemplate = mockk()
        transferRepository = mockk()
        objectMapper = ObjectMapper()
        transferMetrics = mockk()
        transferController = TransferController(
            kafkaTemplate, 
            transferRepository, 
            objectMapper,
            transferMetrics
        )
    }
    
    @Test
    fun `should accept valid transfer request`() {
        // Given
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk()
        every { transferMetrics.recordKafkaPublish(any(), any()) } justRuns
        
        // When
        val response = transferController.createTransfer(validRequest)
        
        // Then
        assertEquals(HttpStatus.ACCEPTED, response.statusCode)
        assertNotNull(response.body)
        assertEquals("PENDING", response.body?.status)
    }
    
    @Test
    fun `should reject empty transferId`() {
        // Given
        val invalid = validRequest.copy(transferId = "")
        every { transferMetrics.recordKafkaPublish(any(), any()) } justRuns
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("VALIDATION_ERROR", response.body?.status)
    }
    
    @Test
    fun `should reject zero amount`() {
        // Given
        val invalid = validRequest.copy(amount = BigDecimal.ZERO)
        every { transferMetrics.recordKafkaPublish(any(), any()) } justRuns
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("VALIDATION_ERROR", response.body?.status)
    }
    
    @Test
    fun `should reject same source and destination`() {
        // Given
        val invalid = validRequest.copy(
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-001"
        )
        every { transferMetrics.recordKafkaPublish(any(), any()) } justRuns
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertNotNull(response.body)
        assertEquals("VALIDATION_ERROR", response.body?.status)
    }
    
    @Test
    fun `should handle kafka publishing error`() {
        // Given
        every { kafkaTemplate.send(any(), any(), any()) } throws RuntimeException("Kafka error")
        every { transferMetrics.recordKafkaPublish(any(), any()) } justRuns
        
        // When
        val response = transferController.createTransfer(validRequest)
        
        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
        assertNotNull(response.body)
        assertEquals("ERROR", response.body?.status)
    }
}
