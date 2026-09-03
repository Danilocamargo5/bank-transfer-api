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

### 1. Idempotency Guarantee
- Every transfer has a unique `transferId`
- System detects and rejects duplicate processing
- Prevents accidental double-charging
- Implemented at TransferService level

### 2. Atomicity with Rollback
- Both accounts must be updated together (all-or-nothing)
- If any save fails, both are rolled back to original state
- Prevents partial updates leaving system inconsistent

### 3. Retry with Exponential Backoff
- 3 automatic retry attempts with 100ms, 200ms, 400ms delays
- Recovers from transient failures (network glitches, temporary unavailability)
- If all retries fail → system rolls back and sends to DLQ

### 4. Manual Kafka Acknowledgment
- Configuration: `enable-auto-commit=false`
- Offset only advances when transfer completes successfully
- If app crashes mid-processing, message is reprocessed on restart
- Prevents message loss

### 5. Dead Letter Queue (DLQ)
- Critical failures sent to SQS for manual investigation
- Separate tracking for:
  - Data inconsistencies (save + rollback failed)
  - Malformed Kafka messages (poison messages)
  - Unrecoverable errors
- Ops team can investigate and retry manually

### 6. Comprehensive Validation
- API level: format validation (TransferValidator)
- Service level: business rules validation (TransferService)
- Database level: entity constraints

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

**Test Coverage:**
- 32 tests total, 100% passing
- Controller: 8 tests
- Service: 5 tests  
- Validator: 11 tests
- Integration: 3 tests
- Messaging: 5 tests

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

✅ **Atomicity** - All-or-nothing account updates  
✅ **Idempotency** - No duplicate processing  
✅ **Resilience** - Retry with exponential backoff  
✅ **Observability** - Metrics, health checks, logs  
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

**Idempotency:** Checked via `transferRepository.hasCompletedTransfer(transferId)`

**Atomicity:** Implemented with retry + rollback in `saveAccountsWithRetryAndRollback()`

**Manual ACK:** Set `spring.kafka.consumer.enable-auto-commit=false`

**Retry Strategy:** 3 attempts with 100ms, 200ms, 400ms exponential backoff

**DLQ Routing:** Critical failures sent via `DeadLetterService` to SQS

## License

Internal use only.
