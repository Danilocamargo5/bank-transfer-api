package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TransferControllerTest {
    
    private lateinit var transferRepository: TransferRepository
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
        transferRepository = mockk()
        transferController = TransferController(transferRepository)
    }
    
    @Test
    fun `should accept valid transfer request`() {
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
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
    
    @Test
    fun `should reject zero amount`() {
        // Given
        val invalid = validRequest.copy(amount = BigDecimal("0.00"))
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
    
    @Test
    fun `should reject negative amount`() {
        // Given
        val invalid = validRequest.copy(amount = BigDecimal("-100.00"))
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
    
    @Test
    fun `should reject non-BRL currency`() {
        // Given
        val invalid = validRequest.copy(currency = "USD")
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
}
