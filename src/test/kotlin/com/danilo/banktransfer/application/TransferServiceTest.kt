package com.danilo.banktransfer.application

import com.danilo.banktransfer.application.exception.AccountNotFoundException
import com.danilo.banktransfer.application.exception.DuplicateTransferException
import com.danilo.banktransfer.application.exception.InactiveAccountException
import com.danilo.banktransfer.application.exception.InsufficientBalanceException
import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import com.danilo.banktransfer.infrastructure.metrics.TransferMetrics
import com.danilo.banktransfer.infrastructure.service.DeadLetterService
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.BeforeEach
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class TransferServiceTest {
    
    private lateinit var accountRepository: AccountRepository
    private lateinit var transferRepository: TransferRepository
    private lateinit var transferMetrics: TransferMetrics
    private lateinit var deadLetterService: DeadLetterService
    private lateinit var transferService: TransferService
    
    private val sourceAccount = Account(
        accountId = "acc-001",
        balance = BigDecimal("5000.00"),
        currency = Currency.BRL,
        status = AccountStatus.ACTIVE,
        customerName = "João Silva",
        createdAt = Instant.now()
    )
    
    private val destinationAccount = Account(
        accountId = "acc-002",
        balance = BigDecimal("1000.00"),
        currency = Currency.BRL,
        status = AccountStatus.ACTIVE,
        customerName = "Maria Santos",
        createdAt = Instant.now()
    )
    
    private val transferEvent = TransferRequestedEvent(
        transferId = "tf-001",
        sourceAccountId = "acc-001",
        destinationAccountId = "acc-002",
        amount = BigDecimal("100.00"),
        currency = "BRL",
        requestedAt = Instant.now()
    )
    
    @BeforeEach
    fun setup() {
        accountRepository = mockk()
        transferRepository = mockk()
        transferMetrics = mockk()
        deadLetterService = mockk()
        transferService = TransferService(accountRepository, transferRepository, transferMetrics, deadLetterService)
    }
    
    @Test
    fun `should process transfer successfully with atomic transaction`() {
        // Given
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(sourceAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        every { accountRepository.saveAtomically(any(), any()) } just runs
        every { transferRepository.save(any()) } returns mockk()
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferSuccess() } just runs
        
        // When
        val result = transferService.processTransfer(transferEvent)
        
        // Then
        assertTrue(result is TransferService.Result.Success)
        verify { transferMetrics.recordTransferSuccess() }
    }
    
    @Test
    fun `should reject duplicate transfer`() {
        // Given
        every { transferRepository.hasCompletedTransfer("tf-001") } returns true
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferFailure(any()) } just runs
        
        // When
        val result = transferService.processTransfer(transferEvent)
        
        // Then
        assertTrue(result is TransferService.Result.Failure)
    }
    
    @Test
    fun `should fail when source account not found`() {
        // Given
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.empty()
        every { transferRepository.save(any()) } returns mockk()
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferFailure(any()) } just runs
        
        // When
        val result = transferService.processTransfer(transferEvent)
        
        // Then
        assertTrue(result is TransferService.Result.Failure)
    }
    
    @Test
    fun `should fail when insufficient balance`() {
        // Given
        val poorAccount = sourceAccount.copy(balance = BigDecimal("50.00"))
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(poorAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        every { transferRepository.save(any()) } returns mockk()
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferFailure(any()) } just runs
        
        // When
        val result = transferService.processTransfer(transferEvent)
        
        // Then
        assertTrue(result is TransferService.Result.Failure)
    }
    
    @Test
    fun `should fail when account is inactive`() {
        // Given
        val inactiveAccount = sourceAccount.copy(status = AccountStatus.INACTIVE)
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(inactiveAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        every { transferRepository.save(any()) } returns mockk()
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferFailure(any()) } just runs
        
        // When
        val result = transferService.processTransfer(transferEvent)
        
        // Then
        assertTrue(result is TransferService.Result.Failure)
    }
    
    @Test
    fun `should retry on transient failure and succeed on second attempt`() {
        // Given
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(sourceAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        
        // First call fails (transient error), second succeeds
        every { accountRepository.saveAtomically(any(), any()) } 
            .throws(RuntimeException("Network timeout"))
            .andThen { just(Unit)() }
        
        every { transferRepository.save(any()) } returns mockk()
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferSuccess() } just runs
        
        // When
        val result = transferService.processTransfer(transferEvent)
        
        // Then
        assertTrue(result is TransferService.Result.Success)
        verify(exactly = 2) { accountRepository.saveAtomically(any(), any()) }
    }
    
    @Test
    fun `should fail after all retries exhausted`() {
        // Given
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(sourceAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        
        // All attempts fail (transient errors)
        every { accountRepository.saveAtomically(any(), any()) } 
            .throws(RuntimeException("DynamoDB timeout"))
        
        every { transferRepository.save(any()) } returns mockk()
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferFailure(any()) } just runs
        
        // When
        val result = transferService.processTransfer(transferEvent)
        
        // Then
        assertTrue(result is TransferService.Result.Failure)
        // Should have attempted 3 times (MAX_RETRIES)
        verify(exactly = 3) { accountRepository.saveAtomically(any(), any()) }
    }
}
