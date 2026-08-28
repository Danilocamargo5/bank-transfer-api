package com.danilo.banktransfer.domain.validator

import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.application.exception.InvalidTransferException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.Instant

class TransferValidatorTest {
    
    private val validRequest = TransferRequestDTO(
        transferId = "tf-123",
        sourceAccountId = "acc-001",
        destinationAccountId = "acc-002",
        amount = BigDecimal("100.00"),
        currency = "BRL",
        requestedAt = Instant.now()
    )
    
    @Test
    fun `should validate valid transfer request`() {
        // Should not throw any exception
        TransferValidator.validate(validRequest)
    }
    
    @Test
    fun `should reject empty transferId`() {
        val invalid = validRequest.copy(transferId = "")
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject transferId exceeding 100 characters`() {
        val invalid = validRequest.copy(transferId = "x".repeat(101))
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject empty sourceAccountId`() {
        val invalid = validRequest.copy(sourceAccountId = "")
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject empty destinationAccountId`() {
        val invalid = validRequest.copy(destinationAccountId = "")
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject same source and destination accounts`() {
        val invalid = validRequest.copy(
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-001"
        )
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject zero amount`() {
        val invalid = validRequest.copy(amount = BigDecimal.ZERO)
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject negative amount`() {
        val invalid = validRequest.copy(amount = BigDecimal("-100.00"))
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject amount with more than 2 decimals`() {
        val invalid = validRequest.copy(amount = BigDecimal("100.123"))
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject empty currency`() {
        val invalid = validRequest.copy(currency = "")
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
    
    @Test
    fun `should reject currency not 3 characters`() {
        val invalid = validRequest.copy(currency = "BR")
        assertThrows<InvalidTransferException> {
            TransferValidator.validate(invalid)
        }
    }
}
