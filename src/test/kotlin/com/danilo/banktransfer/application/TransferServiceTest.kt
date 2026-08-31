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
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.justRuns
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
        transferService = TransferService(accountRepository, transferRepository, transferMetrics)
    }
    
    @Test
    fun `should process transfer successfully`() {
        // Given
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(sourceAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        every { accountRepository.save(any()) } justRuns
        every { transferRepository.save(any()) } justRuns
        every { transferMetrics.recordTransferProcessingTime(any()) } justRuns
        every { transferMetrics.recordTransferSuccess() } justRuns
        
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
        every { transferMetrics.recordTransferProcessingTime(any()) } justRuns
        every { transferMetrics.recordTransferFailure(any()) } justRuns
        
        // When & Then
        assertThrows<DuplicateTransferException> {
            transferService.processTransfer(transferEvent)
        }
    }
    
    @Test
    fun `should fail when source account not found`() {
        // Given
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.empty()
        every { transferRepository.save(any()) } justRuns
        every { transferMetrics.recordTransferProcessingTime(any()) } justRuns
        every { transferMetrics.recordTransferFailure(any()) } justRuns
        
        // When & Then
        assertThrows<AccountNotFoundException> {
            transferService.processTransfer(transferEvent)
        }
    }
    
    @Test
    fun `should fail when insufficient balance`() {
        // Given
        val poorAccount = sourceAccount.copy(balance = BigDecimal("50.00"))
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(poorAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        every { transferRepository.save(any()) } justRuns
        every { transferMetrics.recordTransferProcessingTime(any()) } justRuns
        every { transferMetrics.recordTransferFailure(any()) } justRuns
        
        // When & Then
        assertThrows<InsufficientBalanceException> {
            transferService.processTransfer(transferEvent)
        }
    }
    
    @Test
    fun `should fail when account is inactive`() {
        // Given
        val inactiveAccount = sourceAccount.copy(status = AccountStatus.INACTIVE)
        every { transferRepository.hasCompletedTransfer("tf-001") } returns false
        every { accountRepository.findById("acc-001") } returns Optional.of(inactiveAccount)
        every { accountRepository.findById("acc-002") } returns Optional.of(destinationAccount)
        every { transferRepository.save(any()) } justRuns
        every { transferMetrics.recordTransferProcessingTime(any()) } justRuns
        every { transferMetrics.recordTransferFailure(any()) } justRuns
        
        // When & Then
        assertThrows<InactiveAccountException> {
            transferService.processTransfer(transferEvent)
        }
    }
}
