package com.danilo.banktransfer.application.service

import com.danilo.banktransfer.application.TransferService
import com.danilo.banktransfer.application.exception.DuplicateTransferException
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.infrastructure.persistence.repository.AccountRepository
import com.danilo.banktransfer.infrastructure.persistence.repository.TransferRepository
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

@SpringBootTest
@TestPropertySource(locations = ["classpath:application-test.properties"])
class RaceConditionTransferTest {

    @Autowired
    private lateinit var transferService: TransferService

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var transferRepository: TransferRepository

    private val sourceAccountId = "ACC-SOURCE-001"
    private val destAccountId = "ACC-DEST-001"
    private val transferId = "TRF-RACE-CONDITION-001"

    @BeforeEach
    fun setup() {
        // Cria contas de teste
        val sourceAccount = Account(
            accountId = sourceAccountId,
            customerName = "João Silva",
            balance = BigDecimal("1000.00"),
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE
        )

        val destAccount = Account(
            accountId = destAccountId,
            customerName = "Maria Santos",
            balance = BigDecimal("0.00"),
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE
        )

        accountRepository.save(sourceAccount)
        accountRepository.save(destAccount)
    }

    /**
     * TESTE 1: Demonstra Race Condition SEM Locks
     * 
     * Resultado esperado (SEM locks):
     * ❌ Ambas transferências passam (duplicação)
     * ❌ Saldo final incorreto
     * 
     * Resultado esperado (COM locks - DEPOIS):
     * ✅ Uma transferência sucede
     * ✅ Outra é bloqueada/duplicada é detectada
     * ✅ Saldo correto
     */
    @Test
    fun `deve detectar race condition quando duas transferencias chegam simultaneamente mesma origem e destino`() {
        // Setup
        val transferAmount = BigDecimal("100.00")
        val latch = CountDownLatch(2) // Sincroniza 2 threads
        val startLatch = CountDownLatch(2) // Garante que ambas iniciam no mesmo tempo
        val successCount = AtomicInteger(0)
        val duplicateCount = AtomicInteger(0)
        val exceptions = mutableListOf<Exception>()
        
        // Timestamps para validar SEQUENCIAMENTO
        val thread1StartTime = AtomicInteger(0)
        val thread1EndTime = AtomicInteger(0)
        val thread2StartTime = AtomicInteger(0)
        val thread2EndTime = AtomicInteger(0)

        // Cria 2 eventos idênticos
        val event1 = TransferRequestedEvent(
            transferId = transferId,
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccountId,
            amount = transferAmount,
            currency = "BRL",
            requestedAt = Instant.now()
        )

        val event2 = TransferRequestedEvent(
            transferId = transferId,  // ⚠️ MESMO ID!
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccountId,
            amount = transferAmount,
            currency = "BRL",
            requestedAt = Instant.now()
        )

        // Thread 1
        Thread {
            try {
                startLatch.countDown() // Sinaliza que está pronta
                startLatch.await()     // Espera ambas estarem prontas
                
                thread1StartTime.set(System.currentTimeMillis().toInt())
                println("🔵 Thread 1: Iniciando processamento...")
                
                transferService.processTransfer(event1)
                
                thread1EndTime.set(System.currentTimeMillis().toInt())
                successCount.incrementAndGet()
                println("✅ Thread 1: SUCESSO! Transferência processada")
                
            } catch (e: DuplicateTransferException) {
                thread1EndTime.set(System.currentTimeMillis().toInt())
                duplicateCount.incrementAndGet()
                println("⏳ Thread 1: DuplicateTransferException (estava esperando lock)")
            } catch (e: Exception) {
                exceptions.add(e)
                println("❌ Thread 1: Erro inesperado: ${e.message}")
            } finally {
                latch.countDown()
            }
        }.start()

        // Thread 2
        Thread {
            try {
                startLatch.countDown() // Sinaliza que está pronta
                startLatch.await()     // Espera ambas estarem prontas
                
                thread2StartTime.set(System.currentTimeMillis().toInt())
                println("🔵 Thread 2: Iniciando processamento...")
                
                transferService.processTransfer(event2)
                
                thread2EndTime.set(System.currentTimeMillis().toInt())
                successCount.incrementAndGet()
                println("✅ Thread 2: SUCESSO! Transferência processada")
                
            } catch (e: DuplicateTransferException) {
                thread2EndTime.set(System.currentTimeMillis().toInt())
                duplicateCount.incrementAndGet()
                println("⏳ Thread 2: DuplicateTransferException (estava esperando lock)")
            } catch (e: Exception) {
                exceptions.add(e)
                println("❌ Thread 2: Erro inesperado: ${e.message}")
            } finally {
                latch.countDown()
            }
        }.start()

        // Aguarda ambas threads finalizarem
        latch.await()

        // VALIDAÇÕES PRINCIPAIS
        println("\n╔════════════════════════════════════════╗")
        println("║         RESULTADOS DO TESTE           ║")
        println("╚════════════════════════════════════════╝")
        
        println("\n📊 CONTADORES:")
        println("  Transferências bem-sucedidas: ${successCount.get()}")
        println("  Duplicadas detectadas: ${duplicateCount.get()}")
        println("  Exceções inesperadas: ${exceptions.size}")

        println("\n⏱️  TIMING (validar sequenciamento com locks):")
        println("  Thread 1: ${thread1EndTime.get() - thread1StartTime.get()}ms")
        println("  Thread 2: ${thread2EndTime.get() - thread2StartTime.get()}ms")
        
        val isSequential = thread1EndTime.get() < thread2StartTime.get() || 
                          thread2EndTime.get() < thread1StartTime.get()
        println("  Executadas sequencialmente (COM locks)?: $isSequential")

        // ASSERTIONS - O que prova que LOCKS funcionam
        assertEquals(
            1, 
            successCount.get(), 
            "❌ FALHA: Duas transferências passaram! Locks não estão funcionando!"
        )
        assertEquals(
            1, 
            duplicateCount.get(), 
            "❌ FALHA: Nenhuma foi bloqueada! Locks não estão funcionando!"
        )
        assertEquals(
            0, 
            exceptions.size, 
            "❌ FALHA: Houve exceções inesperadas!"
        )

        // Verifica saldo final (prova que foi atômico)
        val sourceAccountFinal = accountRepository.findByAccountId(sourceAccountId)
        val destAccountFinal = accountRepository.findByAccountId(destAccountId)

        assertEquals(
            BigDecimal("900.00"),
            sourceAccountFinal?.balance,
            "❌ FALHA: Saldo de origem incorreto (race condition afetou atomicidade!)"
        )
        assertEquals(
            BigDecimal("100.00"),
            destAccountFinal?.balance,
            "❌ FALHA: Saldo de destino incorreto (race condition afetou atomicidade!)"
        )

        println("\n╔════════════════════════════════════════╗")
        println("║  ✅ TESTE PASSOU - LOCKS FUNCIONAM!  ║")
        println("╚════════════════════════════════════════╝")
        println("\n📝 CONCLUSÃO:")
        println("  • Apenas 1 transferência foi processada")
        println("  • 1 foi bloqueada como duplicata")
        println("  • Saldos estão corretos (atomicidade garantida)")
        println("  • AWS LockClient está funcionando corretamente!")
    }

    /**
     * TESTE 2: Transferências simultâneas para contas DIFERENTES
     * 
     * Resultado esperado:
     * ✅ Ambas devem passar (sem conflito)
     * ✅ Saldos finais corretos
     */
    @Test
    fun `deve processar simultáneas para contas diferentes sem problemas`() {
        // Setup
        val transferAmount = BigDecimal("50.00")
        val latch = CountDownLatch(2)
        val successCount = AtomicInteger(0)
        val exceptions = mutableListOf<Exception>()

        val destAccount2 = "ACC-DEST-002"
        val account2 = Account(
            accountId = destAccount2,
            customerName = "Carlos Costa",
            balance = BigDecimal("0.00"),
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE
        )
        accountRepository.save(account2)

        // Evento 1: Para dest 1
        val event1 = TransferRequestedEvent(
            transferId = "TRF-DIFF-001",
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccountId,
            amount = transferAmount,
            currency = "BRL",
            requestedAt = Instant.now()
        )

        // Evento 2: Para dest 2 (conta diferente!)
        val event2 = TransferRequestedEvent(
            transferId = "TRF-DIFF-002",
            sourceAccountId = sourceAccountId,
            destinationAccountId = destAccount2,
            amount = transferAmount,
            currency = "BRL",
            requestedAt = Instant.now()
        )

        // Thread 1
        Thread {
            try {
                transferService.processTransfer(event1)
                successCount.incrementAndGet()
            } catch (e: Exception) {
                exceptions.add(e)
            } finally {
                latch.countDown()
            }
        }.start()

        // Thread 2
        Thread {
            try {
                transferService.processTransfer(event2)
                successCount.incrementAndGet()
            } catch (e: Exception) {
                exceptions.add(e)
            } finally {
                latch.countDown()
            }
        }.start()

        latch.await()

        println("\n=== TESTE 2: CONTAS DIFERENTES ===")
        println("Transferências bem-sucedidas: ${successCount.get()}")

        assertEquals(2, successCount.get(), "Ambas transferências devem passar")
        assertEquals(0, exceptions.size, "Nenhuma exceção")

        // Verifica saldos
        val sourceFinal = accountRepository.findByAccountId(sourceAccountId)
        assertEquals(BigDecimal("900.00"), sourceFinal?.balance, "Origem: 1000 - 50 - 50 = 900")

        val dest1Final = accountRepository.findByAccountId(destAccountId)
        assertEquals(BigDecimal("50.00"), dest1Final?.balance, "Dest 1: 0 + 50 = 50")

        val dest2Final = accountRepository.findByAccountId(destAccount2)
        assertEquals(BigDecimal("50.00"), dest2Final?.balance, "Dest 2: 0 + 50 = 50")

        println("✅ TESTE PASSOU: Ambas transferências processadas corretamente!")
    }

    /**
     * TESTE 3: Stresstest - 10 threads simultâneas mesma conta
     * 
     * Resultado esperado (COM locks):
     * ✅ Apenas 1 passa
     * ✅ 9 são bloqueadas como duplicadas
     * ✅ Saldo correto
     */
    @Test
    fun `deve bloquear 10 threads simultaneas com mesmo transferId stresstest`() {
        val transferAmount = BigDecimal("10.00")
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val successCount = AtomicInteger(0)
        val duplicateCount = AtomicInteger(0)
        val exceptions = mutableListOf<Exception>()

        repeat(threadCount) { index ->
            Thread {
                try {
                    val event = TransferRequestedEvent(
                        transferId = "TRF-STRESS-001", // ⚠️ MESMO PARA TODAS!
                        sourceAccountId = sourceAccountId,
                        destinationAccountId = destAccountId,
                        amount = transferAmount,
                        currency = "BRL",
                        requestedAt = Instant.now()
                    )
                    transferService.processTransfer(event)
                    successCount.incrementAndGet()
                } catch (e: DuplicateTransferException) {
                    duplicateCount.incrementAndGet()
                } catch (e: Exception) {
                    exceptions.add(e)
                } finally {
                    latch.countDown()
                }
            }.start()
        }

        latch.await()

        println("\n=== STRESS TEST: 10 THREADS ===")
        println("Sucesso: ${successCount.get()}")
        println("Duplicadas: ${duplicateCount.get()}")
        println("Erros: ${exceptions.size}")

        assertEquals(1, successCount.get(), "Apenas 1 deve passar")
        assertEquals(9, duplicateCount.get(), "9 devem ser bloqueadas")
        assertEquals(0, exceptions.size, "Nenhuma exceção inesperada")

        println("✅ TESTE PASSOU: Race condition bloqueada mesmo com 10 threads!")
    }
}
