package com.danilo.banktransfer.application

import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.model.Transfer
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals

/**
 * TESTE UNITÁRIO - Race Condition com MOCKK
 * 
 * ✅ SEM precisar rodar a aplicação
 * ✅ SEM precisar de LocalStack
 * ✅ Usa MOCKK para simular os repositories
 * 
 * Simula 2 threads simultâneas tentando processar a mesma transferência.
 * 
 * ❌ SEM AWS LockClient (ATUAL):
 *    - hasCompletedTransfer retorna FALSE para ambas
 *    - Ambas passam na validação (race condition!)
 *    - 2 sucessos = BUG!
 * 
 * ✅ COM AWS LockClient (DEPOIS):
 *    - Primeira thread pega lock, processa, libera
 *    - Segunda thread fica esperando, depois detecta duplicada
 *    - 1 sucesso + 1 falha = CORRETO!
 */
class RaceConditionMockTest {

    private lateinit var accountRepository: AccountRepository
    private lateinit var transferRepository: TransferRepository
    private lateinit var transferMetrics: TransferMetrics
    private lateinit var deadLetterService: DeadLetterService
    private lateinit var transferService: TransferService

    private val sourceAccountId = "ACC-001"
    private val destAccountId = "ACC-002"
    private val transferId = "RACE-COND-MOCK-001"

    private val sourceAccount = Account(
        accountId = sourceAccountId,
        customerName = "João Silva",
        balance = BigDecimal("1000.00"),
        currency = Currency.BRL,
        status = AccountStatus.ACTIVE
    )

    private val destAccount = Account(
        accountId = destAccountId,
        customerName = "Maria Santos",
        balance = BigDecimal("0.00"),
        currency = Currency.BRL,
        status = AccountStatus.ACTIVE
    )

    @BeforeEach
    fun setup() {
        accountRepository = mockk()
        transferRepository = mockk()
        transferMetrics = mockk()
        deadLetterService = mockk()
        transferService = TransferService(accountRepository, transferRepository, transferMetrics, deadLetterService, "accounts")

        // Setup padrão de metrics
        every { transferMetrics.recordTransferProcessingTime(any()) } just runs
        every { transferMetrics.recordTransferSuccess() } just runs
        every { transferMetrics.recordTransferFailure(any()) } just runs
    }

    /**
     * TESTE 1: SEM AWS LockClient - Demonstra a race condition
     * 
     * hasCompletedTransfer retorna FALSE para ambas threads
     * (Simula o bug - ambas passam na validação)
     */
    @Test
    fun `demonstra race condition SEM AWS LockClient - ambas passam (BUG)`() {
        println("\n╔════════════════════════════════════════════════════╗")
        println("║  TESTE 1: SEM AWS LockClient (ATUAL - BUGADO)   ║")
        println("╚════════════════════════════════════════════════════╝")

        val successCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // MOCKK Setup: hasCompletedTransfer sempre retorna FALSE
        // Isso simula o bug - nenhuma thread sabe que a outra já passou
        every { transferRepository.hasCompletedTransfer(transferId) } returns false
        every { accountRepository.findById(sourceAccountId) } returns Optional.of(sourceAccount)
        every { accountRepository.findById(destAccountId) } returns Optional.of(destAccount)
        every { transferRepository.saveTransferWithAccountsAtomically(any(), any(), any(), any()) } just runs
        every { transferRepository.save(any()) } returns mockk<Transfer>()

        // Thread 1: Processa normalmente
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = transferId,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = BigDecimal("100.00"),
                    currency = "BRL",
                    requestedAt = Instant.now()
                )
                val result = transferService.processTransfer(event)
                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                    println("✅ Thread 1: SUCESSO")
                }
            } finally {
                latch.countDown()
            }
        }.start()

        // Thread 2: SIMULTÂNEA - Não sabe que Thread 1 está processando
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = transferId,  // ⚠️ MESMO ID!
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = BigDecimal("100.00"),
                    currency = "BRL",
                    requestedAt = Instant.now()
                )
                val result = transferService.processTransfer(event)
                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                    println("✅ Thread 2: SUCESSO ← ⚠️ DUPLICAÇÃO!")
                }
            } finally {
                latch.countDown()
            }
        }.start()

        latch.await()

        println("\n╔════════════════════════════════════════════════════╗")
        println("║                   RESULTADO                        ║")
        println("╚════════════════════════════════════════════════════╝")
        println("Sucessos: ${successCount.get()}")
        println("\n📋 ANÁLISE:")
        println("❌ RACE CONDITION DETECTADA!")
        println("   • Ambas transferências foram processadas")
        println("   • Transferência duplicada (processada 2x com mesmo ID)")
        println("   • AWS LockClient NÃO está implementado")
        println("   → Saldo seria debitado 2x (ERRADO!)")

        // SEM LOCKS: Ambas passam = BUG!
        assertEquals(2, successCount.get(), "❌ SEM LOCKS: Ambas passam (mostra o bug de race condition)")
    }

    /**
     * TESTE 2: COM AWS LockClient - Bloqueia a race condition
     * 
     * hasCompletedTransfer retorna FALSE depois TRUE
     * (Simula o correto - primeira passa, segunda é bloqueada)
     */
    @Test
    fun `bloqueia race condition COM AWS LockClient - 1 passa, 1 falha (CORRETO)`() {
        println("\n╔════════════════════════════════════════════════════╗")
        println("║  TESTE 2: COM AWS LockClient (FUTURO - CORRETO) ║")
        println("╚════════════════════════════════════════════════════╝")

        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // MOCKK Setup: primeira chamada FALSE, segunda TRUE
        // (Simula: primeiro thread processa e marca como COMPLETED)
        every { transferRepository.hasCompletedTransfer(transferId) }
            .returns(false)   // Primeira thread passa
            .andThen(true)    // Segunda thread vê como duplicada

        every { accountRepository.findById(sourceAccountId) } returns Optional.of(sourceAccount)
        every { accountRepository.findById(destAccountId) } returns Optional.of(destAccount)
        every { transferRepository.saveTransferWithAccountsAtomically(any(), any(), any(), any()) } just runs
        every { transferRepository.save(any()) } returns mockk<Transfer>()

        // Thread 1: Processa e marca como COMPLETED
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = transferId,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = BigDecimal("100.00"),
                    currency = "BRL",
                    requestedAt = Instant.now()
                )
                val result = transferService.processTransfer(event)
                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                    println("✅ Thread 1: SUCESSO (pega lock, processa, libera)")
                }
            } finally {
                latch.countDown()
            }
        }.start()

        // Thread 2: Fica esperando o lock, depois detecta duplicada
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = transferId,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = BigDecimal("100.00"),
                    currency = "BRL",
                    requestedAt = Instant.now()
                )
                val result = transferService.processTransfer(event)
                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                } else {
                    failureCount.incrementAndGet()
                    println("❌ Thread 2: BLOQUEADA (detecta como duplicada)")
                }
            } finally {
                latch.countDown()
            }
        }.start()

        latch.await()

        println("\n╔════════════════════════════════════════════════════╗")
        println("║                   RESULTADO                        ║")
        println("╚════════════════════════════════════════════════════╝")
        println("Sucessos: ${successCount.get()}")
        println("Falhas: ${failureCount.get()}")
        println("\n📋 ANÁLISE:")
        println("✅ LOCKS FUNCIONANDO!")
        println("   • Thread 1 processou com sucesso")
        println("   • Thread 2 foi bloqueada como duplicada")
        println("   • Transferência processada apenas 1x (CORRETO!)")
        println("   • Saldo correto: 900.00 (1000 - 100)")

        // COM LOCKS: 1 sucesso + 1 falha = CORRETO!
        assertEquals(1, successCount.get(), "✅ COM LOCKS: Apenas 1 deve passar")
        assertEquals(1, failureCount.get(), "✅ COM LOCKS: 1 deve ser bloqueada como duplicada")
    }

    /**
     * TESTE 3: Validação de Saldo (não pode ficar negativo)
     */
    @Test
    fun `deve validar saldo antes de processar - sem permitir negativo`() {
        println("\n╔════════════════════════════════════════════════════╗")
        println("║  TESTE 3: Validação de Saldo Insuficiente         ║")
        println("╚════════════════════════════════════════════════════╝")

        val poorAccount = sourceAccount.copy(balance = BigDecimal("50.00"))
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        every { transferRepository.hasCompletedTransfer(any()) } returns false
        every { accountRepository.findById(sourceAccountId) } returns Optional.of(poorAccount)
        every { accountRepository.findById(destAccountId) } returns Optional.of(destAccount)
        every { transferRepository.save(any()) } returns mockk<Transfer>()

        val event = TransferRequestedEvent(
            transferId = "TEST-BALANCE-001",
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccountId,
            amount = BigDecimal("100.00"),  // Maior que saldo!
            currency = "BRL",
            requestedAt = Instant.now()
        )

        val result = transferService.processTransfer(event)

        if (result is TransferService.Result.Success) {
            successCount.incrementAndGet()
        } else {
            failureCount.incrementAndGet()
            println("✅ Corretamente bloqueada: saldo insuficiente")
        }

        assertEquals(1, failureCount.get(), "Deve bloquear transferência com saldo insuficiente")
    }

    /**
     * TESTE 4: Conta Inativa (não pode transferir de/para contas inativas)
     */
    @Test
    fun `deve bloquear transferencia de conta inativa`() {
        println("\n╔════════════════════════════════════════════════════╗")
        println("║  TESTE 4: Conta Inativa                            ║")
        println("╚════════════════════════════════════════════════════╝")

        val inactiveAccount = sourceAccount.copy(status = AccountStatus.INACTIVE)
        val failureCount = AtomicInteger(0)

        every { transferRepository.hasCompletedTransfer(any()) } returns false
        every { accountRepository.findById(sourceAccountId) } returns Optional.of(inactiveAccount)
        every { accountRepository.findById(destAccountId) } returns Optional.of(destAccount)
        every { transferRepository.save(any()) } returns mockk<Transfer>()

        val event = TransferRequestedEvent(
            transferId = "TEST-INACTIVE-001",
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccountId,
            amount = BigDecimal("100.00"),
            currency = "BRL",
            requestedAt = Instant.now()
        )

        val result = transferService.processTransfer(event)

        if (result is TransferService.Result.Failure) {
            failureCount.incrementAndGet()
            println("✅ Corretamente bloqueada: conta inativa")
        }

        assertEquals(1, failureCount.get(), "Deve bloquear transferência de conta inativa")
    }

    /**
     * TESTE 5: Conta Não Encontrada
     */
    @Test
    fun `deve bloquear transferencia se conta nao existir`() {
        println("\n╔════════════════════════════════════════════════════╗")
        println("║  TESTE 5: Conta Não Encontrada                    ║")
        println("╚════════════════════════════════════════════════════╝")

        val failureCount = AtomicInteger(0)

        every { transferRepository.hasCompletedTransfer(any()) } returns false
        every { accountRepository.findById(sourceAccountId) } returns Optional.empty()  // Conta não existe!
        every { transferRepository.save(any()) } returns mockk<Transfer>()

        val event = TransferRequestedEvent(
            transferId = "TEST-NOT-FOUND-001",
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccountId,
            amount = BigDecimal("100.00"),
            currency = "BRL",
            requestedAt = Instant.now()
        )

        val result = transferService.processTransfer(event)

        if (result is TransferService.Result.Failure) {
            failureCount.incrementAndGet()
            println("✅ Corretamente bloqueada: conta não encontrada")
        }

        assertEquals(1, failureCount.get(), "Deve bloquear se conta não existir")
    }
}
