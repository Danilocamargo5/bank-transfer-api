# Code Review: Legacy Transfer Consumer

## Código Original (legacy-consumer.kt)

```kotlin
@Component
class TransferConsumer(
  private val accountRepository: AccountRepository,
  private val kafkaTemplate: KafkaTemplate<String, String>
) {
  private val logger = LoggerFactory.getLogger(this::class.java)

  @KafkaListener(topics = ["transfer-requested"], groupId = "transfer-service")
  fun handle(message: String) {
    try {
      val transfer = ObjectMapper().readValue(message, TransferRequest::class.java)
      logger.info("Processing transfer: $transfer")

      val source = accountRepository.findById(transfer.sourceAccountId).get()
      val destination = accountRepository.findById(transfer.destinationAccountId).get()

      source.balance = source.balance - transfer.amount
      accountRepository.save(source)

      destination.balance = destination.balance + transfer.amount
      accountRepository.save(destination)

      kafkaTemplate.send("transfer-completed", transfer.toString())
      logger.info("Transfer ${transfer.transferId} completed for user ${transfer.sourceAccountId}")

    } catch (e: Exception) {
      logger.error("Error processing transfer", e)
    }
  }
}
```

---

## Problemas Identificados e Soluções

### 1. ❌ **Falta de Idempotência**

**Problema:**
- Se a mensagem for reprocessada (Kafka rebalance), a transferência será executada NOVAMENTE
- Débito duplicado = inconsistência financeira

**Solução:**
```kotlin
// Guardar o transferId já processado (idempotency key)
@KafkaListener(topics = ["transfer-requested"], groupId = "transfer-service")
fun handle(message: String) {
  val transfer = ObjectMapper().readValue(message, TransferRequest::class.java)
  
  // Verificar se já foi processada
  val existingTransfer = transferRepository.findById(transfer.transferId)
  if (existingTransfer != null) {
    logger.info("Transfer ${transfer.transferId} already processed, skipping")
    return
  }
  
  // ... processar ...
  
  // Salvar com status final
  transferRepository.save(
    Transfer(
      id = transfer.transferId,
      status = COMPLETED,
      processedAt = Instant.now()
    )
  )
}
```

---

### 2. ❌ **Não-Atomicidade: Dois Saves Separados**

**Problema:**
```kotlin
source.balance = source.balance - transfer.amount
accountRepository.save(source)  // ⚠️ Sucesso

destination.balance = destination.balance + transfer.amount
accountRepository.save(destination)  // ❌ Falha aqui = INCONSISTÊNCIA
```
Débito saiu mas crédito não entrou!

**Solução:**
```kotlin
// Transação atômica
@Transactional
fun processTransfer(transfer: TransferRequest) {
  val source = accountRepository.findById(transfer.sourceAccountId).get()
  val destination = accountRepository.findById(transfer.destinationAccountId).get()

  source.balance -= transfer.amount
  destination.balance += transfer.amount
  
  // Um único save ou ambas falham
  accountRepository.saveAll(listOf(source, destination))
}
```

---

### 3. ❌ **Sem Validação de Regras de Negócio**

**Problema:**
```kotlin
val source = accountRepository.findById(transfer.sourceAccountId).get()
// get() = Exception se não existir (ruim!)
// Nenhuma validação de saldo, status ACTIVE, etc.
```

**Solução:**
```kotlin
fun handle(message: String) {
  val transfer = ObjectMapper().readValue(message, TransferRequest::class.java)
  
  // Validações
  val source = accountRepository.findById(transfer.sourceAccountId)
    .orElseThrow { AccountNotFoundException("Source account not found") }
  
  val destination = accountRepository.findById(transfer.destinationAccountId)
    .orElseThrow { AccountNotFoundException("Destination account not found") }
  
  // Verificar saldo
  if (source.balance < transfer.amount) {
    throw InsufficientFundsException("Insufficient balance")
  }
  
  // Verificar status
  if (source.status != ACTIVE || destination.status != ACTIVE) {
    throw InactiveAccountException("One or both accounts are inactive")
  }
  
  // ... processar ...
}
```

---

### 4. ❌ **Sem Tratamento de Erros Específicos**

**Problema:**
```kotlin
} catch (e: Exception) {
  logger.error("Error processing transfer", e)
  // Silencia o erro = mensagem perdida no Kafka!
}
```

**Solução:**
```kotlin
} catch (e: AccountNotFoundException) {
  // Erro de negócio = enviar pra DLQ (não retry)
  sendToDLQ(transfer, e.message)
  logger.error("Business error: ${e.message}", e)
  
} catch (e: InsufficientFundsException) {
  // Erro de negócio = DLQ
  sendToDLQ(transfer, e.message)
  logger.error("Business error: ${e.message}", e)
  
} catch (e: Exception) {
  // Erro transiente = retry com backoff
  throw RuntimeException("Transient error, will retry", e)
}
```

---

### 5. ❌ **ObjectMapper Criado a Cada Mensagem**

**Problema:**
```kotlin
val transfer = ObjectMapper().readValue(message, TransferRequest::class.java)
// Novo ObjectMapper a cada execução = desperdício de CPU
```

**Solução:**
```kotlin
@Component
class TransferConsumer(
  private val accountRepository: AccountRepository,
  private val kafkaTemplate: KafkaTemplate<String, String>,
  private val objectMapper: ObjectMapper  // Injeta
) {
  // Usar objectMapper
  val transfer = objectMapper.readValue(message, TransferRequest::class.java)
}
```

---

### 6. ❌ **Sem Métrica de Tempo de Processamento**

**Problema:**
```kotlin
logger.info("Processing transfer: $transfer")
// ... 5 segundos depois ...
logger.info("Transfer ${transfer.transferId} completed")
// Ninguém sabe quanto tempo levou!
```

**Solução:**
```kotlin
fun handle(message: String) {
  val startTime = System.currentTimeMillis()
  
  try {
    // ... processar ...
    
    val duration = System.currentTimeMillis() - startTime
    logger.info("Transfer ${transfer.transferId} completed in ${duration}ms")
    
    // Métrica
    meterRegistry.timer("transfer.processing.time").record(duration, TimeUnit.MILLISECONDS)
    meterRegistry.counter("transfer.success").increment()
    
  } catch (e: Exception) {
    meterRegistry.counter("transfer.failure").increment()
    throw e
  }
}
```

---

### 7. ❌ **Logs Não-Estruturados**

**Problema:**
```kotlin
logger.info("Processing transfer: $transfer")
// Difícil de parsear em produção, correlação difícil
```

**Solução:**
```kotlin
logger.info(
  "transfer_processing",
  mapOf(
    "transferId" to transfer.transferId,
    "sourceAccountId" to transfer.sourceAccountId,
    "destinationAccountId" to transfer.destinationAccountId,
    "amount" to transfer.amount,
    "status" to "started"
  )
)
// Output: JSON estruturado, fácil de buscar
```

---

### 8. ❌ **Sem Circuito de Proteção (Circuit Breaker)**

**Problema:**
```kotlin
// Se accountRepository falhar, retry infinito = cascata de falhas
```

**Solução:**
```kotlin
@Retry(maxAttempts = 3, backoff = Backoff(delay = 1000))
@CircuitBreaker(
  failureThreshold = 5,
  delay = 60000,
  successThreshold = 2
)
fun handle(message: String) {
  // ... processar ...
}
```

---

## Resumo das Correções

| Problema | Impacto | Solução |
|----------|--------|--------|
| Sem idempotência | Duplicação de transferências | Armazenar transferId processado |
| Dois saves | Inconsistência (debit sem credit) | @Transactional |
| Sem validação | Transferências inválidas | Validar saldo, status, existência |
| Catch genérico | Erros perdidos | Catch específico + DLQ |
| ObjectMapper repetido | Desperdício de CPU | Injetar singleton |
| Sem métricas | Invisibilidade | Timer + Counter |
| Logs não-estruturados | Difícil debugging | JSON estruturado |
| Sem Circuit Breaker | Cascata de falhas | @CircuitBreaker |

---

## Código Corrigido (Aplicado no Projeto)

O código atualmente no projeto JÁ implementa TODAS essas correções:
- ✅ `TransferKafkaConsumer.kt` - Idempotência + validações
- ✅ `TransferService.kt` - @Transactional
- ✅ `ErrorType.kt` + `KafkaRetryConfig.kt` - Tratamento de erros específicos
- ✅ `SQS DLQ` - Falhas de negócio isoladas
- ✅ Logs estruturados em JSON
- ✅ Métricas via Micrometer (PR #7)

