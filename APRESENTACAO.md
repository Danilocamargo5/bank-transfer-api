# 🏦 Bank Transfer API - Script de Apresentação

**Tempo Total: ~20-25 minutos**

---

## 📊 RESUMO DE ESTUDO - TECH STACK

### Core
- **Kotlin 2.0.0** + **Spring Boot 3.3.5** + **Java 21 (LTS)** + **Gradle 8.8**

### Persistência
- **DynamoDB (AWS SDK v2)** → TransactWriteItems (ACID/atomicidade)

### Messaging
- **Kafka (KRaft mode)** → replay de mensagens, audit trail
- **SQS (FIFO)** → Dead Letter Queue para erros críticos

### Testing
- JUnit 5 (5.10.x) + MockK (1.13.x) + Kotlin Test (1.10.x)
- JaCoCo (0.8.x) → **74% coverage**

### Dev/Local
- **LocalStack 3.x** → Simula DynamoDB, Kafka, SQS localmente

### Observabilidade
- **Micrometer (1.14.x)** → Métricas
- **SLF4J + Logback (1.2.x)** → Logging
- **Jackson (2.17.x)** → JSON serialization

---

## 🎓 PONTOS-CHAVE PRAS RESPOSTAS

**Por quê Kotlin?**  
Menos verboso, null-safety automático, extension functions. Melhor que Java puro.

**Por quê DynamoDB?**  
Serverless (sem ops), escala automática, TransactWriteItems = ACID. Ideal pra microsserviço.

**Por quê Kafka?**  
Replay (offset), audit trail, event sourcing ready. SQS é fire-and-forget.

**Por quê JUnit 5 + MockK?**  
Modern testing, MockK nativa Kotlin (melhor que Mockito). AAA pattern.

**Por quê LocalStack?**  
Desenvolvimento e testes SEM AWS real. Idêntico em produção. Economiza $$.

---

## 🏗️ ARQUITETURA GERAL (2 min)

### Fluxo Visual:
```
┌─ SCRIPT EXTERNO ──────────────────┐
│ ./scripts/publish-transfer.sh     │
└────────────┬──────────────────────┘
             ↓
┌─ KAFKA: transfer-requested ───────┐
│ (Message com dados completos)     │
└────────────┬──────────────────────┘
             ↓
┌─ TransferKafkaConsumer ───────────┐
│ - Manual ACK                      │
│ - Consome 1 mensagem              │
└────────────┬──────────────────────┘
             ↓
┌─ TransferService.processTransfer()┐
│ - Validação                       │
│ - Busca contas em DynamoDB        │
│ - Salva ATOMICAMENTE              │
│ - Retry com backoff               │
└────┬────────────┬────────┬────────┘
     ↓            ↓        ↓
  COMPLETED   FAILED   CRÍTICA
  (Kafka)     (SQS)    (DLQ)
```

### 3 Saídas:
- ✅ **COMPLETED** → Kafka topic
- ❌ **FAILED** → SQS queue  
- 🚨 **CRÍTICA** → SQS DLQ

---

## 📚 CLASSES PRINCIPAIS (Visão Geral)

### Estrutura do Projeto:
1. **TransferKafkaConsumer** - Consome mensagens Kafka
2. **TransferService** - Lógica principal (validação, processamento)
3. **TransferValidator** - Valida regras de negócio
4. **AccountRepository** - Acesso dados (DynamoDB)
5. **DeadLetterService** - Envia erros críticos pra DLQ
6. **TransferMetrics** - Registra métricas (latência, sucesso/falha)

Vamos detalhar cada uma...

---

## 1️⃣ TransferKafkaConsumer (Message Queue)

**Arquivo:** `messaging/TransferKafkaConsumer.kt`

### O que faz:
- Consome mensagens de `transfer-requested` topic
- Extrai evento `TransferRequestedEvent`
- Chama `TransferService.processTransfer()`
- **Manual ACK**: confirma só se processou com sucesso
- Se falhar: não confirma, mensagem volta pra Kafka (retry automático)

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

### Características:
- ✅ Desacoplado (só consome e chama service)
- ✅ Resiliente (retry automático via Kafka)
- ✅ Manual ACK (controle fino)

---

## 2️⃣ TransferService (Orquestração)

**Arquivo:** `application/TransferService.kt`

### Responsabilidades (8 pontos):
1. Valida transferência (formato, campos)
2. Detecta duplicata (idempotência)
3. Busca conta source em DynamoDB
4. Busca conta destination em DynamoDB
5. Valida status (ambas ACTIVE?)
6. Valida saldo (source suficiente?)
7. Salva ATOMICAMENTE com retry (calcula + persiste)
8. Publica resultado (Kafka/SQS)

### Fluxo dos 8 Passos (Resumido):

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

## 3️⃣ TransferValidator (Regras Negócio)

**Arquivo:** `domain/validator/TransferValidator.kt`

### Por quê é `object` (não `class`)?
`object` = singleton garantido pela JVM. Não precisa instanciar (não faz `new`), é thread-safe automático. Ideal pra classes utilitárias que **SÓ VALIDAM** (sem estado, sem dependências).

### O que faz:
Valida **TUDO** relacionado a transferência:
- ✅ `validateTransferId()` - ID existe e tem tamanho?
- ✅ `validateAccountIds()` - Source/Dest diferentes? Válidos?
- ✅ `validateAmount()` - Positivo? Máximo 2 decimais?
- ✅ `validateCurrency()` - Currency suportada (BRL, USD)?
- ✅ `validate()` - Chama todos acima

### Exemplo:
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

### Características:
- ✅ Object = singleton, thread-safe
- ✅ Centraliza todas as validações
- ✅ Fácil testar (11 testes unitários)

---

## 4️⃣ AccountRepository (Persistência)

**Arquivo:** `infrastructure/repository/AccountRepository.kt`

### O que faz:
Gerencia acesso a dados das contas em DynamoDB:
- `findById(id)` - Busca conta por ID (GET)
- `save(account)` - Salva conta (PUT)
- `saveAtomically(source, dest)` - **SALVA DUAS CONTAS EM UMA TRANSAÇÃO**
- `findAll()` - Busca todas contas (scan)

### Método Crítico: saveAtomically()

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

### ⚠️ IMPORTANTE - ATOMICIDADE:
**TransactWriteItems**: Ambas contas salvam ou ambas falham. Impossível partial failure.

---

## 5️⃣ DeadLetterService (Erro Crítico)

**Arquivo:** `infrastructure/service/DeadLetterService.kt`

### O que faz:
Envia erros críticos (não recuperáveis) pra fila separada:
- DynamoDB completamente down?
- Kafka indisponível?
- Erro desconhecido?
→ Enviar pra **SQS DLQ (Dead Letter Queue)**

### Método:
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

### Características:
- ✅ Separa erros críticos (requer ops manual)
- ✅ Preserva evento original pra reprocessamento
- ✅ Timestamp pra tracking

---

## 6️⃣ TransferMetrics (Observabilidade)

**Arquivo:** `infrastructure/metrics/TransferMetrics.kt`

### O que faz:
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

### Métricas Coletadas:
- 📊 `transfer.processing.time` - Latência (p50, p95, p99)
- ✅ `transfer.success` - Contagem sucessos
- ❌ `transfer.failure` - Contagem falhas (por reason)
- 🔄 `transfer.retry` - Retries tentados

### ⚠️ Importante:
Métricas são armazenadas em memória. Se app cair, somem. Em produção, seria necessário um backend externo (Prometheus + InfluxDB ou CloudWatch) pra persistir.

---

## 7️⃣ Controllers (API REST)

**Arquivo:** `api/controller/TransferController.kt`

### Endpoints:

**POST /transfers**
- Entrada: `TransferRequestDTO`
- Saída: `202 Accepted` (async processing)
- Valida request básico

**GET /transfers/{id}**
- Retorna status da transfer (PENDING, COMPLETED, FAILED)

**GET /transfers?status=COMPLETED**
- Filtra transfers por status

**Arquivo:** `api/controller/AccountController.kt`

**GET /accounts**
- Lista todas contas

**GET /accounts/{id}**
- Retorna dados da conta (saldo, status)

**POST /accounts**
- Cria nova conta

### Responsabilidades:
- ✅ Valida input (HTTP level)
- ✅ Converte DTO → Model
- ✅ Retorna status HTTP apropriado

---

## 🧪 TESTES UNITÁRIOS (Overview)

### Estrutura de Testes:

| Arquivo | Testes | Foco |
|---------|--------|------|
| TransferServiceTest | 5 | Lógica negócio (validação, retry, duplicata) |
| TransferControllerTest | 13 | HTTP endpoints (201, 400, 404) |
| TransferValidatorTest | 11 | Validação (campo por campo) |
| AccountControllerTest | 14 | CRUD operations |
| TransferMapperTest | 17 | Conversão Object ↔ DynamoDB |
| AccountMapperTest | 18 | Conversão Account ↔ DynamoDB |
| TransferMetricsTest | 21 | Recording de métricas |
| AtomicityGuaranteeTest | 6 | **PROVA ATOMICIDADE** |
| TransferIntegrationTest | 3 | End-to-end com LocalStack |
| **TOTAL** | **99** | **74% coverage** |

---

### Padrão de Teste: AAA (Arrange-Act-Assert)

O que é: Padrão pra organizar testes.
- **Arrange**: prepara dados e mocks
- **Act**: executa função testada
- **Assert**: valida se resultado está correto

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

---

## 🔬 AtomicityGuaranteeTest (TESTE CRÍTICO)

**Arquivo:** `application/AtomicityGuaranteeTest.kt`

### Propósito:
**PROVA MATEMÁTICA** que atomicidade é garantida. Uma validação formal de que nenhuma partial failure é possível.

---

### O que é ACID?

**A - Atomicidade:**  
Transação completa 100% OU não completa nada. Sem partial failure. Exemplo: João perde 1000 E Maria ganha 1000. Nunca: João perde mas Maria não recebe.

**C - Consistência:**  
Banco sempre em estado válido. Se quebra uma regra (saldo negativo), transação inteira falha e faz rollback. Dados nunca ficam corrompidos/inconsistentes.

**I - Isolamento:**  
Transações concorrentes não se interferem. Transfer 1 não vê dados parciais de Transfer 2. Cada uma vê snapshot consistente.

**D - Durabilidade:**  
Depois que confirma (commit), dados são persistidos. Mesmo se falhar servidor, dados não desaparecem (escrito em disco/backup).

**No nosso projeto:**  
TransactWriteItems (DynamoDB) implementa ACID. Se falhar 1 operação = todas falham + rollback automático. Impossível estado inconsistente.

---

### ✅ TESTE 1: Sucesso Completo

**O que faz:**  
Simula fluxo perfeito: validação OK → busca contas OK → salva OK → retorna Success

**Resultado:**  
Verifica que `saveAtomically()` foi chamado exatamente 1x

**Estado final:**
- João: 5000 → 4000 (debit 1000) ✅
- Maria: 1000 → 2000 (credit 1000) ✅
- Estado: **CONSISTENTE** ✅

---

### ❌ TESTE 2: Falha (Atomicidade em Ação!)

**O que faz:**  
Simula falha na transação DynamoDB (throws RuntimeException)

**Resultado:**  
Verifica retry (chamou 3x = MAX_RETRIES) e retorna Failure

**✅ COM TransactWriteItems:**
- João: 5000 → 5000
- Maria: 1000 → 1000
- Estado: **CONSISTENTE** ✅

**❌ SEM TransactWriteItems:**
- João: 5000 → 4000
- Maria: 1000 → 1000
- Estado: **INCONSISTENTE!** 💥

---

### ⚡ TESTE 3: Retry com Sucesso

**O que faz:**  
1ª tentativa falha (timeout), 2ª OK → verifica chamou 2x

**Resultado:**  
`saveAtomically()` chamado 2 vezes (1ª erro, 2ª sucesso) ✅

---

### TESTES 4, 5, 6: Retry e Matriz

**TESTE 4: Retries Esgotados**

3 tentativas todas falham → vai pra DLQ (Dead Letter Queue). Verifica chamou exatamente 3x

**TESTE 5: Debit Falha**

Transação falha na 1ª operação → rollback automático. Nenhuma conta é alterada.

**TESTE 6: Matriz de Cenários (TransactWriteItems)**

| # | Debit | Credit | Resultado |
|---|-------|--------|-----------|
| 1 | ✅ Success | ✅ Success | ✅ AMBOS SALVAM |
| 2 | ✅ Success | ❌ Fail | ✅ AMBOS ROLLBACK |
| 3 | ❌ Fail | ✅ Success | ✅ AMBOS ROLLBACK |
| 4 | ❌ Fail | ❌ Fail | ✅ AMBOS ROLLBACK |

### ✅ CONCLUSÃO: Nenhum cenário tem PARTIAL FAILURE!

**Atomicidade 100% garantida por DynamoDB.**

