package com.danilo.banktransfer.integration

import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import com.danilo.banktransfer.application.TransferService
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.infrastructure.metrics.TransferMetrics
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse


@SpringBootTest
@ActiveProfiles("test")
class TransferIntegrationTest {
    
    @Autowired
    private lateinit var accountRepository: AccountRepository
    
    @Autowired
    private lateinit var transferRepository: TransferRepository
    
    @Autowired
    private lateinit var transferService: TransferService
    
    @BeforeEach
    fun setup() {
        val sourceAccount = Account(
            accountId = "acc-test-001",
            balance = BigDecimal("5000.00"),
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE,
            customerName = "Test Source",
            createdAt = Instant.now()
        )
        
        val destAccount = Account(
            accountId = "acc-test-002",
            balance = BigDecimal("1000.00"),
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE,
            customerName = "Test Dest",
            createdAt = Instant.now()
        )
        
        accountRepository.save(sourceAccount)
        accountRepository.save(destAccount)
    }
    
    @Test
    fun `should successfully process a valid transfer`() {
        // Given
        val event = TransferRequestedEvent(
            transferId = "tf-integration-001",
            sourceAccountId = "acc-test-001",
            destinationAccountId = "acc-test-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            requestedAt = Instant.now()
        )
        
        // When
        val result = transferService.processTransfer(event)
        
        // Then
        assertTrue(result is TransferService.Result.Success)
        
        // Verify transfer was recorded
        val transfers = transferRepository.findByTransferId("tf-integration-001")
        assertEquals(1, transfers.size)
        assertEquals(TransferStatus.COMPLETED, transfers[0].status)
        
        // Verify account balances were updated
        val sourceAccount = accountRepository.findById("acc-test-001").get()
        val destAccount = accountRepository.findById("acc-test-002").get()
        assertEquals(BigDecimal("4900.00"), sourceAccount.balance)
        assertEquals(BigDecimal("1100.00"), destAccount.balance)
    }
    
    @Test
    fun `should fail transfer due to insufficient balance`() {
        // Given
        val event = TransferRequestedEvent(
            transferId = "tf-integration-002",
            sourceAccountId = "acc-test-001",
            destinationAccountId = "acc-test-002",
            amount = BigDecimal("10000.00"),
            currency = "BRL",
            requestedAt = Instant.now()
        )
        
        // When
        val result = transferService.processTransfer(event)
        
        // Then
        assertTrue(result is TransferService.Result.Failure)
        
        // Verify transfer was recorded as failed
        val transfers = transferRepository.findByTransferId("tf-integration-002")
        assertEquals(1, transfers.size)
        assertEquals(TransferStatus.FAILED, transfers[0].status)
        assertTrue(transfers[0].failureReason?.contains("Insufficient balance") ?: false)
        
        // Verify account balances were NOT changed
        val sourceAccount = accountRepository.findById("acc-test-001").get()
        val destAccount = accountRepository.findById("acc-test-002").get()
        assertEquals(BigDecimal("5000.00"), sourceAccount.balance)
        assertEquals(BigDecimal("1000.00"), destAccount.balance)
    }
    
    @Test
    fun `should handle idempotent transfer requests`() {
        // Given
        val event = TransferRequestedEvent(
            transferId = "tf-integration-003",
            sourceAccountId = "acc-test-001",
            destinationAccountId = "acc-test-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            requestedAt = Instant.now()
        )
        
        // When - first transfer
        val result1 = transferService.processTransfer(event)
        assertTrue(result1 is TransferService.Result.Success)
        
        // Verify balances after first transfer
        var sourceAccount = accountRepository.findById("acc-test-001").get()
        assertEquals(BigDecimal("4900.00"), sourceAccount.balance)
        
        // When - retry with same transferId
        val result2 = transferService.processTransfer(event)
        assertTrue(result2 is TransferService.Result.Failure) // Should be rejected as duplicate
        
        // Then - verify balance didn't change
        sourceAccount = accountRepository.findById("acc-test-001").get()
        assertEquals(BigDecimal("4900.00"), sourceAccount.balance) // Should remain same
    }
}
