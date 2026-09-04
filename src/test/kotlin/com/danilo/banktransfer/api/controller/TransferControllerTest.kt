package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.domain.model.Transfer
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import io.mockk.every
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
    
    private val completedTransfer = Transfer(
        transferId = "tf-test-001",
        sourceAccountId = "acc-001",
        destinationAccountId = "acc-002",
        amount = BigDecimal("100.00"),
        currency = Currency.BRL,
        status = TransferStatus.COMPLETED,
        requestedAt = Instant.now(),
        completedAt = Instant.now()
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
    
    @Test
    fun `should reject same source and destination account`() {
        // Given
        val invalid = validRequest.copy(
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-001"
        )
        
        // When
        val response = transferController.createTransfer(invalid)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
    
    @Test
    fun `should handle unexpected exception during transfer creation`() {
        // When - no mock setup means repository will throw when accessed
        val invalidRequest = validRequest.copy(sourceAccountId = "")
        val response = transferController.createTransfer(invalidRequest)
        
        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }
    
    @Test
    fun `should get all transfers successfully`() {
        // Given
        every { transferRepository.findAll() } returns listOf(completedTransfer)
        
        // When
        val response = transferController.getAllTransfers()
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(1, response.body?.size)
        assertEquals("COMPLETED", response.body?.get(0)?.status)
    }
    
    @Test
    fun `should return empty list when no transfers exist`() {
        // Given
        every { transferRepository.findAll() } returns emptyList()
        
        // When
        val response = transferController.getAllTransfers()
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(0, response.body?.size)
    }
    
    @Test
    fun `should get transfer status by id successfully`() {
        // Given
        every { transferRepository.findByTransferId("tf-test-001") } returns listOf(completedTransfer)
        
        // When
        val response = transferController.getTransferStatus("tf-test-001")
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("tf-test-001", response.body?.transferId)
        assertEquals("COMPLETED", response.body?.status)
    }
    
    @Test
    fun `should return 404 when transfer not found`() {
        // Given
        every { transferRepository.findByTransferId("non-existent") } returns emptyList()
        
        // When
        val response = transferController.getTransferStatus("non-existent")
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertNotNull(response.body)
        assertEquals("NOT_FOUND", response.body?.status)
        assertEquals("Transfer not found", response.body?.message)
    }
    
    @Test
    fun `should include failure reason in transfer response when present`() {
        // Given
        val failedTransfer = completedTransfer.copy(
            status = TransferStatus.FAILED,
            failureReason = "Insufficient balance"
        )
        every { transferRepository.findByTransferId("tf-failed-001") } returns listOf(failedTransfer)
        
        // When
        val response = transferController.getTransferStatus("tf-failed-001")
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Insufficient balance", response.body?.message)
    }
}
