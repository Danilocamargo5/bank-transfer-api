package com.danilo.banktransfer.application

import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TESTE DE INTEGRAÇÃO - Race Condition
 * 
 * Testa o cenário onde 2 threads simultâneas tentam processar
 * a mesma transferência ao mesmo tempo.
 * 
 * SEM AWS LockClient (ATUAL):
 * ❌ Ambas passam na validação (race condition!)
 * ❌ Saldos incorretos (duplicação de transferência)
 * 
 * COM AWS LockClient (DEPOIS):
 * ✅ Uma passa, uma é bloqueada como duplicada
 * ✅ Saldos corretos
 */
@SpringBootTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
class RaceConditionIntegrationTest {

    @Autowired
    private lateinit var transferService: TransferService

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var transferRepository: TransferRepository

    private val sourceAccountId = "ACC-SRC-001"
    private val destAccountId = "ACC-DST-001"
    private val transferId = "RACE-COND-TEST-001"

    @BeforeEach
    fun setup() {
        // Criar contas com saldos iniciais
        val sourceAccount = Account(
            accountId = sourceAccountId,
            customerName = "João",
            balance = BigDecimal("1000.00"),
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE
        )

        val destAccount = Account(
            accountId = destAccountId,
            customerName = "Maria",
            balance = BigDecimal("0.00"),
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE
        )

        accountRepository.save(sourceAccount)
        accountRepository.save(destAccount)
    }

    /**
     * TESTE: 2 Threads simultâneas, mesmo transferId
     * 
     * ❌ ESPERADO SEM LOCKS (atual):
     *   - Ambas passam: 2 Success
     *   - Saldo errado: origem fica -100 (debitou 2x)
     * 
     * ✅ ESPERADO COM LOCKS (depois):
     *   - 1 Success + 1 Failure (DuplicateTransferException)
     *   - Saldo correto: origem -100 (debitou 1x)
     */
    @Test
    fun `deve demonstrar race condition com 2 threads mesmo transferId`() {
        val transferAmount = BigDecimal("100.00")
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // Thread 1
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = transferId,
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = transferAmount,
                    currency = "BRL",
                    requestedAt = Instant.now()
                )

                val result = transferService.processTransfer(event)

                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                    println("✅ Thread 1: Transferência bem-sucedida")
                } else {
                    failureCount.incrementAndGet()
                    println("❌ Thread 1: Falha - ${(result as TransferService.Result.Failure).event.failureReason}")
                }
            } catch (e: Exception) {
                println("❌ Thread 1 Exception: ${e.message}")
            } finally {
                latch.countDown()
            }
        }.start()

        // Thread 2
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = transferId,  // MESMO ID!
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = transferAmount,
                    currency = "BRL",
                    requestedAt = Instant.now()
                )

                val result = transferService.processTransfer(event)

                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                    println("✅ Thread 2: Transferência bem-sucedida")
                } else {
                    failureCount.incrementAndGet()
                    println("❌ Thread 2: Falha - ${(result as TransferService.Result.Failure).event.failureReason}")
                }
            } catch (e: Exception) {
                println("❌ Thread 2 Exception: ${e.message}")
            } finally {
                latch.countDown()
            }
        }.start()

        latch.await()

        println("\n╔════════════════════════════════════╗")
        println("║        RESULTADOS DO TESTE        ║")
        println("╚════════════════════════════════════╝")
        println("Sucessos: ${successCount.get()}")
        println("Falhas: ${failureCount.get()}")

        // Verifica saldo final
        val sourceAccountFinal = accountRepository.findById(sourceAccountId).orElse(null)
        println("\nSaldo Origem (esperado 900.00): ${sourceAccountFinal?.balance}")

        println("\n📋 ANÁLISE:")
        if (successCount.get() == 2) {
            println("❌ RACE CONDITION DETECTADA!")
            println("   - Ambas transferências passaram (duplicação)")
            println("   - Saldo está errado: ${sourceAccountFinal?.balance} (deveria ser 900.00)")
            println("   → AWS LockClient NÃO está implementado!")
        } else if (successCount.get() == 1 && failureCount.get() == 1) {
            println("✅ LOCKS FUNCIONANDO!")
            println("   - 1 passou, 1 foi bloqueada (correto)")
            println("   - Saldo está correto: ${sourceAccountFinal?.balance}")
            println("   → AWS LockClient está implementado corretamente!")
        }
    }

    /**
     * TESTE: Verificar que transferências com IDs diferentes funcionam
     */
    @Test
    fun `deve processar 2 transferencias com IDs diferentes sem problemas`() {
        val transferAmount = BigDecimal("50.00")
        val successCount = AtomicInteger(0)
        val latch = CountDownLatch(2)

        // Thread 1 - Transferência 1
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = "TRANSFER-001",
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = transferAmount,
                    currency = "BRL",
                    requestedAt = Instant.now()
                )

                val result = transferService.processTransfer(event)
                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                }
            } finally {
                latch.countDown()
            }
        }.start()

        // Thread 2 - Transferência 2 (ID diferente)
        Thread {
            try {
                val event = TransferRequestedEvent(
                    transferId = "TRANSFER-002",  // ID DIFERENTE
                    sourceAccountId = sourceAccountId,
                    destinationAccountId = destAccountId,
                    amount = transferAmount,
                    currency = "BRL",
                    requestedAt = Instant.now()
                )

                val result = transferService.processTransfer(event)
                if (result is TransferService.Result.Success) {
                    successCount.incrementAndGet()
                }
            } finally {
                latch.countDown()
            }
        }.start()

        latch.await()

        // COM ou SEM locks, IDs diferentes devem ambas passar
        assertEquals(2, successCount.get(), "Ambas transferências devem passar (IDs diferentes)")

        val sourceAccountFinal = accountRepository.findById(sourceAccountId).orElse(null)
        assertEquals(
            0,
            sourceAccountFinal?.balance?.compareTo(BigDecimal("900.00")),
            "Saldo deve ser 900.00 (1000 - 50 - 50)"
        )
    }
}
