# 🏦 Bank Transfer API - Apresentação Técnica

## 📊 Tech Stack

**Linguagem:** Kotlin 2.0.0 | **Framework:** Spring Boot 3.3.5 | **Runtime:** Java 21 | **Build:** Gradle 8.8

**Banco:** DynamoDB via LocalStack (AWS SDK v2) → TransactWriteItems (ACID/atomicidade)
**Mensageria:** Apache Kafka (KRaft mode) → replay de mensagens, audit trail  
**Fila Crítica:** SQS (LocalStack) → transfer-failed (retryable), transfer-failed-dlq (crítico)
**Testing:** JUnit 5 (5.10.x) + MockK (1.13.x) + Kotlin Test (1.10.x) + JaCoCo → 74% coverage
**Observabilidade:** Micrometer + SLF4J/Logback + Jackson
**LocalStack 3.x:** Simula DynamoDB, Kafka, SQS localmente

---

## 🏗️ Arquitetura Geral

```
External Scripts → Kafka "transfer-requested"
    ↓
TransferKafkaConsumer (manual ACK)
    ↓
TransferService (retry + DLQ)
    ↓
DynamoDB (atomic transactions via TransactWriteItems)
    ↓
Success: Kafka "transfer-completed"
Failure: SQS "transfer-failed"
Critical: SQS "transfer-failed-dlq"
```

---

## 📋 Classes Principais

1. **TransferKafkaConsumer** - Consome Kafka, manual ACK
2. **TransferService** - Orquestra 8 passos
3. **TransferValidator** - Valida regras negócio (object singleton)
4. **AccountRepository** - Acesso DynamoDB
5. **DeadLetterService** - Envia pra SQS DLQ
6. **TransferMetrics** - Métricas Micrometer
7. **Controllers** - API REST

---

## 🔄 Fluxo dos 8 Passos

| # | Passo | O quê | Se falhar |
|---|-------|-------|----------|
| 1 | Validação | TransferValidator.validate() | HTTP 400 |
| 2 | Idempotência | hasCompletedTransfer(id)? | SQS |
| 3 | Busca Source | findById(sourceAccountId) | SQS + Retry |
| 4 | Busca Dest | findById(destinationAccountId) | SQS + Retry |
| 5 | Valida Status | Ambas ACTIVE? | SQS + Retry |
| 6 | Valida Saldo | Source tem suficiente? | SQS |
| 7 | Save Atômico | saveAtomically(source, dest) + retry 3x | DLQ |
| 8 | Publicação | publishCompletionEvent() ou publishFailureEvent() | DLQ |

---

## 1️⃣ TransferKafkaConsumer (Message Queue)

**Arquivo:** `messaging/TransferKafkaConsumer.kt`

**O que faz:**
- Consome mensagens do Kafka topic "transfer-requested"
- Manual ACK (confirma só depois de processar)
- Desacoplado (só chama TransferService)

```kotlin
@KafkaListener(topics = ["transfer-requested"])
fun consumeTransfer(message: ConsumerRecord<String, String>) {
    try {
        val event = objectMapper.readValue(message.value(), 
                                           TransferRequestedEvent::class.java)
        
        val result = transferService.processTransfer(event)
        
        // Manual ACK (sucesso)
        acknowledgment.acknowledge()
        
    } catch (e: Exception) {
        logger.error("Erro processando", e)
        // Não confirma → mensagem volta pra Kafka
    }
}
```

**Características:**
- ✅ Desacoplado (só consome e chama service)
- ✅ Manual ACK garante reprocessamento se falhar
- ✅ Simples e focado (responsabilidade única)

---

## 2️⃣ TransferService (Orquestração)

**Arquivo:** `application/TransferService.kt`

**Responsabilidades (8 passos):**
1. Valida entrada (TransferValidator)
2. Detecta duplicata (idempotência)
3. Busca conta source
4. Busca conta destination
5. Valida status (ambas ACTIVE)
6. Valida saldo
7. Salva atomicamente com retry 3x (backoff: 100ms, 200ms, 400ms)
8. Publica resultado (success ou failure)

**Retry com Backoff Exponencial:**
- 1ª tentativa falha → espera 100ms
- 2ª tentativa falha → espera 200ms
- 3ª tentativa falha → espera 400ms
- Todas falham → vai pra DLQ

---

## 3️⃣ TransferValidator (Regras Negócio)

**Arquivo:** `domain/validator/TransferValidator.kt`

**Por quê é `object` (não `class`)?**
`object` = singleton garantido pela JVM. Não precisa instanciar (não faz `new`), é thread-safe automático. Ideal pra classes utilitárias que SÓ VALIDAM (sem estado, sem dependências).

```kotlin
object TransferValidator {
    
    fun validate(request: TransferRequestDTO) {
        validateTransferId(request.transferId)
        validateAccountIds(request.sourceAccountId, request.destinationAccountId)
        validateAmount(request.amount)
        validateCurrency(request.currency)
    }
    
    private fun validateTransferId(id: String) {
        if (id.isBlank() || id.length > 100) {
            throw InvalidTransferException("Invalid transfer ID")
        }
    }
    
    private fun validateAmount(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) {
            throw InvalidTransferException("Amount must be positive")
        }
        if (amount.scale() > 2) {
            throw InvalidTransferException("Max 2 decimals")
        }
    }
}
```

**Valida:**
- transferId (não vazio, tamanho máximo)
- accountIds (diferentes, válidos)
- amount (positivo, máximo 2 decimais)
- currency (BRL apenas)

---

## 4️⃣ AccountRepository (Persistência)

**Arquivo:** `infrastructure/repository/AccountRepository.kt`

**O que faz:**
- Acesso dados (DynamoDB)
- `findById(accountId)` - busca conta
- `saveAtomically(source, dest)` - **SALVA DUAS CONTAS EM UMA TRANSAÇÃO**

**Método Crítico: saveAtomically()**

```kotlin
fun saveAtomically(source: Account, destination: Account) {
    val sourceItem = accountMapper.toItem(source)
    val destItem = accountMapper.toItem(destination)
    
    // TransactWriteItems = ACID transaction
    val request = TransactWriteItemsRequest.builder()
        .transactItems(
            TransactWriteItem.builder()
                .put(Put.builder()
                    .tableName("account")
                    .item(sourceItem)
                    .build())
                .build(),
            TransactWriteItem.builder()
                .put(Put.builder()
                    .tableName("account")
                    .item(destItem)
                    .build())
                .build()
        )
        .build()
    
    // Se falhar QUALQUER PUT: ROLLBACK automático
    dynamoDbClient.transactWriteItems(request)
}
```

**⚠️ IMPORTANTE - ATOMICIDADE:**
TransactWriteItems: Ambas contas salvam ou ambas falham. Impossível partial failure.

---

## 5️⃣ TransferMetrics (Observabilidade)

**Arquivo:** `infrastructure/metrics/TransferMetrics.kt`

**O que faz:**
Registra métricas pra monitoramento em real-time:

```kotlin
class TransferMetrics(private val meterRegistry: MeterRegistry) {
    
    fun recordTransferProcessingTime(duration: Duration) {
        meterRegistry.timer("transfer.processing.time").record(duration)
    }
    
    fun recordTransferSuccess() {
        meterRegistry.counter("transfer.success").increment()
    }
    
    fun recordTransferFailure(reason: String) {
        meterRegistry.counter("transfer.failure", "reason", reason).increment()
    }
}
```

**Métricas Coletadas:**
- 📊 `transfer.processing.time` - Latência (p50, p95, p99)
- ✅ `transfer.success` - Contagem sucessos
- ❌ `transfer.failure` - Contagem falhas (por reason)
- 🔄 `transfer.retry` - Retries tentados

**⚠️ Importante:** Métricas são armazenadas em memória. Se app cair, somem. Em produção, seria necessário um backend externo (Prometheus + InfluxDB ou CloudWatch) pra persistir.

---

## 6️⃣ DeadLetterService (DLQ - Critical Errors)

**Arquivo:** `infrastructure/service/DeadLetterService.kt`

**O que faz:**
- Envia mensagens que falharam para SQS "transfer-failed-dlq"
- Registra log + timestamp + exception
- Admin investiga depois

```kotlin
fun sendToDeadLetter(event: TransferRequestedEvent, exception: Exception) {
    val dlqMessage = DeadLetterMessage(
        transferId = event.transferId,
        errorReason = exception.message,
        timestamp = Instant.now(),
        originalEvent = event
    )
    
    sqsClient.sendMessage(SendMessageRequest.builder()
        .queueUrl(dlqQueueUrl)
        .messageBody(objectMapper.writeValueAsString(dlqMessage))
        .build())
    
    logger.error("Sent to DLQ: ${event.transferId}")
}
```

---

## 7️⃣ Controllers (API REST)

**Arquivo:** `api/controller/`

**Endpoints:**
- `POST /transfers` - Entrada: TransferRequestDTO, Saída: 202 Accepted (async)
- `GET /transfers/{id}` - Retorna status (PENDING, COMPLETED, FAILED)
- `GET /transfers?status=COMPLETED` - Filtra por status
- `POST /accounts` - Cria conta
- `GET /accounts/{id}` - Busca conta
- `GET /accounts` - Lista todas

---

## 🧪 AtomicityGuaranteeTest (TESTE CRÍTICO)

**Arquivo:** `application/AtomicityGuaranteeTest.kt`

**Propósito:**
PROVA MATEMÁTICA que atomicidade é garantida. Uma validação formal de que nenhuma partial failure é possível.

### O que é ACID?

**A - Atomicidade:** Transação completa 100% OU não completa nada. Sem partial failure. Exemplo: João perde 1000 E Maria ganha 1000. Nunca: João perde mas Maria não recebe.

**C - Consistência:** Banco sempre em estado válido. Se quebra uma regra (saldo negativo), transação inteira falha e faz rollback. Dados nunca ficam corrompidos/inconsistentes.

**I - Isolamento:** Transações concorrentes não se interferem. Transfer 1 não vê dados parciais de Transfer 2. Cada uma vê snapshot consistente.

**D - Durabilidade:** Depois que confirma (commit), dados são persistidos. Mesmo se falhar servidor, dados não desaparecem (escrito em disco/backup).

**No nosso projeto:** TransactWriteItems (DynamoDB) implementa ACID. Se falhar 1 operação = todas falham + rollback automático. Impossível estado inconsistente.

### Padrão de Teste: AAA (Arrange-Act-Assert)

**O que é:** Padrão pra organizar testes. **Arrange:** prepara dados e mocks. **Act:** executa função testada. **Assert:** valida se resultado está correto.

```kotlin
@Test
fun `test name`() {
    // ARRANGE: Preparar mocks + dados
    every { repo.find() } returns value
    
    // ACT: Executar método
    val result = service.process()
    
    // ASSERT: Validar
    assertTrue(result is Success)
    verify { repo.find() }
}
```

### TESTE 1: Sucesso Completo

**O que faz:** Simula fluxo perfeito: validação OK → busca contas OK → salva OK → retorna Success
**Resultado:** Verifica que saveAtomically() foi chamado exatamente 1x

**Estado final:** João: 5000 → 4000 (debit 1000) ✅ | Maria: 1000 → 2000 (credit 1000) ✅ | Estado: CONSISTENTE ✅

### TESTE 2: Falha (Atomicidade em Ação!)

**O que faz:** Simula falha na transação DynamoDB (throws RuntimeException)
**Resultado:** Verifica retry (chamou 3x = MAX_RETRIES) e retorna Failure

**✅ COM TransactWriteItems:** João: 5000 → 5000 | Maria: 1000 → 1000 | Estado: CONSISTENTE ✅

**❌ SEM TransactWriteItems:** João: 5000 → 4000 | Maria: 1000 → 1000 | Estado: INCONSISTENTE! 💥

### TESTE 3: Retry com Sucesso

**O que faz:** 1ª tentativa falha (timeout), 2ª OK → verifica chamou 2x
**Resultado:** saveAtomically() chamado 2 vezes (1ª erro, 2ª sucesso) ✅

### TESTE 4, 5, 6: Retry e Matriz

**Teste 4:** Retries esgotados (3 tentativas falham) → vai pra DLQ. Verifica chamou exatamente 3x

**Teste 5:** Debit falha → rollback automático, ambas contas não mudam

**Teste 6:** Matriz de Cenários (TransactWriteItems)

| Cenário | Debit | Credit | Resultado |
|---------|-------|--------|-----------|
| 1 | ✅ Success | ✅ Success | ✅ AMBOS SALVAM |
| 2 | ✅ Success | ❌ Fail | ✅ AMBOS ROLLBACK |
| 3 | ❌ Fail | ✅ Success | ✅ AMBOS ROLLBACK |
| 4 | ❌ Fail | ❌ Fail | ✅ AMBOS ROLLBACK |

**Conclusão:** TransactWriteItems garante ACID. Impossível perder dinheiro! ✅

---

## 📊 Resumo de Testes

| Arquivo | Testes |
|---------|--------|
| TransferMetricsTest | 15 |
| AccountMapperTest | 13 |
| TransferMapperTest | 13 |
| AccountControllerTest | 14 |
| TransferControllerTest | 12 |
| TransferIntegrationTest | 3 |
| TransferKafkaConsumerTest | 5 |
| TransferServiceTest | 7 |
| AtomicityGuaranteeTest | 6 |
| TransferValidatorTest | 11 |
| **TOTAL** | **99** |

**Cobertura:** 74% (JaCoCo)

---

## 🎓 Kotlin Avançado (P15-P18)

**P15: O que é when expression?**
Switch melhorado. Retorna valor, pode ter ranges, quando (when) nenhum case bate usa else.

**P16: O que é Lambda?**
Função anônima. Tipo: `{ x -> x * 2 }`. Usamos em map(), filter(), forEach() (functional programming).

**P17: Scope functions (let, apply, run, also)?**
Funções que executam bloco no contexto do objeto. `let` (transform), `apply` (configure), `run` (execute), `also` (side effect).

**P18: O que é sealed class?**
Restringe quem pode herdar. Só classes dentro do arquivo podem estender. Usado pra Result (Success/Failure).

---

## ⚡ Performance + 🚀 Deployment (P27-P32)

**P27: Qual a escalabilidade? (throughput)**
1000+ transfers/seg. DynamoDB serverless escala automático (RCU/WCU on-demand). Kafka paralelo (múltiplos consumers).

**P28: Como mede performance?**
Micrometer. Métricas: p95/p99 latency, error rate, throughput. Em produção: Prometheus + Grafana.

**P29: Como evita DDoS / rate limit?**
Rate limit por usuário/IP (Spring AOP decorator). Se excede: HTTP 429 (Too Many Requests).

**P30: Como é o deploy?**
GitHub Actions → Build Docker → Push ECR → ECS (blue-green deployment). Zero downtime.

**P31: E se tiver bug em produção?**
Rollback: volta pro container antigo (Docker image anterior). ECS manage isso automaticamente.

**P32: Versionamento da API?**
URL versionada: /v1/transfers, /v2/transfers. Permite quebrar sem afetar clientes antigos.

---

## 🚀 Melhorias Futuras

**Race Condition (P1):**
UpdateExpression direto no DynamoDB: `SET balance = balance - :amount`
Evita GET → CALC → PUT (hoje propenso a race condition)

**Logs Estruturados:**
Implementar JSON logs (hoje plain text)

**Circuit Breaker:**
Implementar padrão pra proteção em cascata

**TransactWriteItems vs @Transactional (P44):**
@Transactional funciona pra aplicação, mas não garante atomicidade no banco
TransactWriteItems = garantia no banco (DynamoDB ACID)

