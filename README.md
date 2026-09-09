# Bank Transfer API

A production-grade microservice for processing bank transfers with guaranteed atomicity, idempotency, and resilience patterns.

## Overview

This is a Spring Boot 3.3.5 application built with Kotlin that processes financial transfers asynchronously via Apache Kafka. The system ensures data consistency through multiple layers of validation, error handling, and recovery mechanisms.

**Tech Stack:**
- **Language:** Kotlin 2.0.0
- **Framework:** Spring Boot 3.3.5
- **Runtime:** Java 21
- **Database:** DynamoDB (AWS/LocalStack)
- **Messaging:** Apache Kafka (KRaft mode)
- **Error Queue:** SQS (AWS/LocalStack)
- **Build:** Gradle 8.8

## Architecture

```
External Sources (Scripts)
        ↓
   [Kafka Topic: transfer-requested]
        ↓
[TransferKafkaConsumer] ← Manual Acknowledgment
        ↓
[TransferService] → Retry with Exponential Backoff
        ├─ Idempotency Check
        ├─ Validation (format + business rules)
        ├─ Account Lookup
        ├─ Balance Validation
        ├─ Atomic Debit/Credit (with Rollback)
        └─ Persist Result
        ↓
   [Success: Kafka topic: transfer-completed]
   [Failure: SQS queue: transfer-failed]
   [Critical: SQS DLQ: transfer-failed-dlq]
        ↓
   [DynamoDB: transfers table]
```

## Key Features

- **Distributed Locking:** Prevents race conditions with DynamoDB atomic locks
- **Unified Transactions:** Single DynamoDB transaction for accounts + transfer (all-or-nothing)
- **Idempotency:** No duplicate processing with transferId checks
- **Retry Logic:** 3 attempts with exponential backoff (100ms, 200ms, 400ms)
- **Manual Kafka ACK:** Only advances offset on successful processing
- **Dead Letter Queue:** Critical failures sent to SQS for investigation
- **MDC Logging:** Request tracing with transferId correlation
- **JSON Logs:** Structured logging for production
- **Validation:** API, service, and database level checks

## API Endpoints

### Transfer Management

**POST** `/api/v1/transfers` - Validate and accept transfer (returns 202 ACCEPTED)
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId": "tf-123",
    "sourceAccountId": "acc-123",
    "destinationAccountId": "acc-456",
    "amount": 100.00,
    "currency": "BRL"
  }'
```

**GET** `/api/v1/transfers` - List all transfers
```bash
curl http://localhost:8080/api/v1/transfers
```

**GET** `/api/v1/transfers/{transferId}` - Get transfer status
```bash
curl http://localhost:8080/api/v1/transfers/tf-123
```

### Account Management

**GET** `/api/v1/accounts` - List all accounts
```bash
curl http://localhost:8080/api/v1/accounts
```

**GET** `/api/v1/accounts/{accountId}` - Get account details
```bash
curl http://localhost:8080/api/v1/accounts/acc-123
```

**POST** `/api/v1/accounts` - Create new account
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H "Content-Type: application/json" \
  -d '{
    "accountId": "acc-999",
    "customerName": "João Silva",
    "balance": 5000.00
  }'
```

**PUT** `/api/v1/accounts/{accountId}` - Update account
```bash
curl -X PUT http://localhost:8080/api/v1/accounts/acc-123 \
  -H "Content-Type: application/json" \
  -d '{"balance": 7500.00}'
```

**DELETE** `/api/v1/accounts/{accountId}` - Delete account
```bash
curl -X DELETE http://localhost:8080/api/v1/accounts/acc-999
```

## Setup & Running

### Prerequisites
- Docker & Docker Compose
- Java 21 (via SDKMAN)
- Gradle 8.8

### Local Development

**1. Start Infrastructure**
```bash
./full-setup.sh
```

**2. Start Application (in separate terminal)**
```bash
./scripts/start-app.sh
```

**3. Test with Additional Messages (optional)**
```bash
./scripts/DEMO2.sh
```

### Monitoring

**Health Check:**
```bash
curl http://localhost:8080/actuator/health
```

**Kafka UI:**
http://localhost:8081

## Testing

**Run all unit tests:**
```bash
./gradlew test
```

**Current Status:** 101/104 tests passing
- Atomic Transactions: ✅ 5 tests
- Race Condition Prevention: ✅ 7 tests
- Validation: ✅ 5 tests
- Service Logic: ✅ 25+ tests
- Integration: ⏳ Requires running infrastructure

## Validation Rules

### At API Level
- `transferId`: non-empty, max 100 chars
- `sourceAccountId` & `destinationAccountId`: non-empty
- Source ≠ Destination
- `amount`: > 0, max 2 decimals
- `currency`: BRL only

### At Service Level
- Idempotency: transferId must be unique (no duplicates)
- Accounts must exist and be ACTIVE
- Source account must have sufficient balance
- Currency validation (BRL only)

## Error Handling

### API Level (400 BAD_REQUEST)
- Invalid format
- Business rule violations

### Service Level (202 ACCEPTED → Event)
- Account not found → FAILED event to SQS
- Insufficient balance → FAILED event to SQS
- Invalid currency → FAILED event to SQS
- Account inactive → FAILED event to SQS

### Critical Level (DLQ)
- Save + Rollback failed (data inconsistency)
- Kafka publish failed (completion event lost)
- Malformed JSON (poison message)

## Production Features

✅ **Distributed Locking** - Prevents race conditions
✅ **Unified Transactions** - All-or-nothing atomicity  
✅ **Idempotency** - No duplicate processing  
✅ **Resilience** - Retry with exponential backoff  
✅ **Observability** - MDC logging + JSON structured logs
✅ **Auditability** - Complete transfer history  
✅ **Recoverability** - DLQ for manual intervention  

## Troubleshooting

**Transfer stuck in PENDING:**
→ App crashed mid-processing. Restart app to resume.

**Account balance inconsistent:**
→ Check DLQ for failed rollback events. Manual DBA intervention needed.

**Messages not being consumed:**
→ Check app logs for errors. Verify Kafka topics exist.

## Development Notes

### Project Structure
```
src/main/kotlin/com/danilo/banktransfer/
├── api/controller/           # REST endpoints
├── application/              # Business logic
├── domain/
│   ├── model/               # Entities
│   ├── enums/               # Status, Currency
│   └── validator/           # Validation
├── infrastructure/
│   ├── repository/          # DynamoDB
│   ├── messaging/           # Kafka consumer
│   ├── service/             # DLQ
│   └── config/              # Spring config
└── application/exception/   # Custom exceptions
```

### Key Implementation Details

**Distributed Locking:** `LockService.kt` uses PutItem + ConditionExpression for atomic lock acquisition

**Unified Transactions:** `TransferRepository.saveTransferWithAccountsAtomically()` writes 3 items in one transaction

**Idempotency:** `transferRepository.hasCompletedTransfer(transferId)` checked within lock

**Retry Strategy:** 3 attempts with exponential backoff in `saveTransferWithAccountsAtomically()`

**DLQ Routing:** `DeadLetterService` sends critical failures to SQS after retries exhausted

**Logging:** MDC context with transferId + JSON structured output via logback-spring.xml

## Tempo Investido

Total de desenvolvimento (Sessões 1-14):

| Atividade | Tempo |
|-----------|-------|
| Análise e Design | 2h |
| Core Implementation | 8h |
| Testes Unitários | 3h |
| Integração e Configuração | 2h |
| Transação Unificada | 2h |
| Distributed Locking | 2h |
| MDC + JSON Logging | 1h |
| Documentação | 2h |
| Apresentação e Review | 2h |
| **TOTAL** | **24.5h** |

### Principais Componentes:

- TransferService (retry + locking): 6h
- LockService (distributed locks): 3h
- DynamoDB Integration: 3h
- Testes (104 casos): 4h
- Infrastructure (LocalStack): 2h
- Logging e Observabilidade: 2h
- Documentação: 2h
- Outros: 2.5h

---

## License

Internal use only.
