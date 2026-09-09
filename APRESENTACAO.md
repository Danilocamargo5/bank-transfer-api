# Bank Transfer API - Apresentação Técnica

## 📋 Visão Geral

Microserviço Spring Boot em Kotlin que processa transferências bancárias com garantias de atomicidade, idempotência e resiliência usando DynamoDB, Kafka e SQS.

**Status:** ✅ 105 testes passando | 🔒 Distributed Locks | 📊 Transações unificadas

---

## 🏗️ Classes Principais e Responsabilidades

### 1. **TransferService** (application/TransferService.kt)

**O que é:** Orquestradora do fluxo completo de transferência

**Responsabilidades:**
- Adquire locks distribuídos (previne race conditions)
- Valida idempotência (transferId único)
- Valida regras de negócio (saldo, conta ativa)
- Debita/Credita contas
- Salva em transação unificada (3 writes atomicamente)
- Gerencia retries com backoff exponencial (3x)
- Envia falhas críticas para DLQ
- Correlaciona logs via MDC

**Métodos principais:**
- `processTransfer()` - Fluxo principal com locks
- `saveTransferWithAccountsAtomically()` - Transação unificada (retry 3x)
- `validateTransfer()` - Validações de negócio

---

### 2. **LockService** (infrastructure/service/LockService.kt)

**O que é:** Gerenciador de locks distribuídos baseado em DynamoDB

**Responsabilidades:**
- Cria tabela de locks automaticamente (no init)
- Adquire locks via PutItem + ConditionExpression (atômico)
- Implementa retry com backoff (5 tentativas)
- Mantém TTL de 10s (auto-cleanup se app cair)
- Libera locks em ordem LIFO (previne deadlock)

**Métodos principais:**
- `acquireTransferLocks()` - Adquire 3 locks (transferId, sourceId, destId)
- `releaseLocks()` - Libera em ordem reversa (LIFO)
- `ensureLockTableExists()` - Cria tabela se não existir

---

### 3. **TransferRepository** (infrastructure/repository/TransferRepository.kt)

**O que é:** Camada de persistência de transferências em DynamoDB

**Responsabilidades:**
- Salva transfer record
- Executa queries por transferId
- Verifica idempotência
- **Novo:** Transação unificada com contas (3 writes em 1 transação)

**Métodos principais:**
- `saveTransferWithAccountsAtomically()` - UNIFICADO: source + dest + transfer (all-or-nothing)
- `hasCompletedTransfer()` - Verifica duplicata
- `findByTransferId()` - Query por transferId

---

### 4. **AccountRepository** (infrastructure/repository/AccountRepository.kt)

**O que é:** Camada de persistência de contas em DynamoDB

**Responsabilidades:**
- Salva/recupera contas
- Valida existência de conta
- Suporta transações atômicas (usado em saveTransferWithAccountsAtomically)

**Métodos principais:**
- `saveAtomically()` - Salva múltiplas contas atomicamente
- `findById()` - Busca conta por ID
- `existsById()` - Validação de existência

---

### 5. **TransferKafkaConsumer** (infrastructure/messaging/TransferKafkaConsumer.kt)

**O que é:** Consumer que processa eventos de Kafka

**Responsabilidades:**
- Consome de `transfer-requested`
- Desserializa JSON → TransferRequestedEvent
- Chama TransferService
- Publica em `transfer-completed` ou `transfer-failed`
- Manual ACK: offset avança só em sucesso
- Correlaciona logs com MDC (transferId)

**Métodos principais:**
- `consumeTransferRequest()` - Entry point

---

### 6. **DeadLetterService** (infrastructure/service/DeadLetterService.kt)

**O que é:** Gerenciador de falhas críticas via SQS

**Responsabilidades:**
- Envia transferências com falha crítica para DLQ
- Usado quando save/rollback falha após retries
- Preserva informação completa para investigação manual
- Separa `transfer-failed` (negócio) de `transfer-failed-dlq` (crítico)

**Métodos principais:**
- `sendCriticalFailureToDLQ()` - Envia para SQS com detalhes

---

### 7. **TransferValidator** (domain/validator/TransferValidator.kt)

**O que é:** Validador de dados de entrada

**Responsabilidades:**
- Valida formato de transferId
- Valida accountIds
- Valida amount (> 0, 2 decimais)
- Valida currency (BRL only)
- Previne transferência mesma conta

**Métodos principais:**
- `validate()` - Executa todas validações

---

### 8. **TransferMetrics** (infrastructure/metrics/TransferMetrics.kt)

**O que é:** Coletor de métricas de operação

**Responsabilidades:**
- Registra tempo de processamento (P50, P95, P99)
- Conta sucessos/falhas
- Rastreia erros por tipo
- Integra com Micrometer/Prometheus

**Métodos principais:**
- `recordTransferProcessingTime()` - Tempo total
- `recordTransferSuccess()` - Sucesso
- `recordTransferFailure()` - Falha por tipo

---

## 🔄 Fluxo de Transferência

```
1. [Kafka] transfer-requested
   ↓
2. [TransferKafkaConsumer] desserializa
   ↓
3. [TransferService.processTransfer()]
   ├─ [LockService] acquireTransferLocks()
   ├─ Valida idempotência + regras
   ├─ Debita/Credita
   ├─ [TransferRepository] saveTransferWithAccountsAtomically()
   │  └─ 1 transação: source + dest + transfer
   ├─ [LockService] releaseLocks() (LIFO)
   └─ Publica transfer-completed
   
4. Se erro: transfer-failed
   ├─ Se crítico: DLQ
   └─ Salva com status FAILED
```

---

## 🔒 Distributed Locking

**Como funciona:**
- PutItem com ConditionExpression: "attribute_not_exists(lockId)"
- Atômico - ou consegue o lock ou falha
- Retry 5x com backoff exponencial
- LIFO release (previne deadlock)
- TTL 10s (auto-cleanup)

---

## 📊 Transações Unificadas

**Antes:** 2 transações separadas (contas + transfer) → risco de inconsistência
**Depois:** 1 transação com 3 writes → all-or-nothing

---

## ✅ Garantias Implementadas

| Garantia | Mecanismo |
|----------|-----------|
| Atomicidade | TransactWriteItems + Retry |
| Idempotência | hasCompletedTransfer() com lock |
| Race Condition | Distributed locks |
| Resiliência | Retry 3x exponencial |
| Recoverabilidade | DLQ |
| Auditabilidade | Transfer history + MDC |
| Observabilidade | Métricas + JSON logs |

---

## 🧪 Testes

- **105 testes** ✅ passando
- **Unitários:** 100+ (MockK)
- **Integração:** 5 (DynamoDB real)
  - Transação unificada
  - Concorrência (2 threads, contas diferentes)
  - Idempotência
  - Validações

---

**Sessão 14 - 2026-09-09**
