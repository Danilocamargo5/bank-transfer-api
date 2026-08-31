# Bank Transfer API

Microserviço de processamento de transferências bancárias internas com Kafka, DynamoDB e Spring Boot.

## Quick Start

```bash
# Terminal 1: Start infrastructure
./scripts/start-infra.sh

# Terminal 2: Initialize infrastructure
./scripts/init-infrastructure.sh

# Terminal 1: Start application
./scripts/start-app.sh
```

Or use automated setup:
```bash
./scripts/full-setup.sh
```

See [scripts/README.md](scripts/README.md) for detailed script documentation.

## Testing

### Success Transfer
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-test-001",
    "sourceAccountId":"acc-001",
    "destinationAccountId":"acc-002",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'
```

### Failed Transfer (DLQ)
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-fail-001",
    "sourceAccountId":"acc-invalid",
    "destinationAccountId":"acc-002",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'
```

### Check DLQ
```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --endpoint-url http://localhost:4566 \
  --region us-east-1
```

### View Metrics
```bash
# All metrics
curl http://localhost:8080/actuator/metrics

# Transfer processing time
curl http://localhost:8080/actuator/metrics/transfer.processing.time

# Success count
curl http://localhost:8080/actuator/metrics/transfer.success.total

# Failure count
curl http://localhost:8080/actuator/metrics/transfer.failure.total

# Health
curl http://localhost:8080/actuator/health
```

## Architecture

```
POST /api/v1/transfers
  ↓
Kafka: transfer-requested
  ↓
TransferKafkaConsumer
  ├─ Validate (account, balance, status)
  ├─ Execute atomically (@Transactional)
  └─ Update DynamoDB
  
Success → Kafka: transfer-completed
Failure → SQS: transfer-failed (DLQ)
```

## Tech Stack

- **Language:** Kotlin 2.0
- **Framework:** Spring Boot 3.3.5
- **Database:** DynamoDB (LocalStack)
- **Messaging:** Kafka (KRaft), SQS (LocalStack)
- **Runtime:** Java 21
- **Build:** Gradle 8.8

## Features

- ✅ Atomic transfers (debit + credit in single transaction)
- ✅ Idempotency (same transferId won't duplicate)
- ✅ Error handling with retry & backoff
- ✅ DLQ for failed transfers
- ✅ Input validation
- ✅ Metrics & observability (Micrometer)
- ✅ Comprehensive tests (24 test cases)
- ✅ Structured JSON logging

## Documentation

- `SETUP.md` - Setup & running instructions
- `docs/CODE_REVIEW_LEGACY.md` - Analysis of 8 problems & solutions
- `docs/ERROR_HANDLING.md` - Error handling strategy

## Scripts

All automation scripts organized by purpose in the `scripts/` folder:

**Infrastructure:**
- `scripts/start-infra.sh` - Start Docker (Kafka + LocalStack)
- `scripts/stop-infra.sh` - Stop all containers

**Application:**
- `scripts/start-app.sh` - Start Spring Boot app
- `scripts/stop-app.sh` - Stop Spring Boot app

**Initialization:**
- `scripts/init-infrastructure.sh` - Create topics, queue, data
- `scripts/init-kafka.sh`, `init-sqs.sh`, `init-dynamodb.sh` - Individual init scripts

**Testing:**
- `scripts/DEMO.sh` - Basic demo (8 test scenarios)
- `scripts/DEMO2.sh` - Extended demo (40+ test data points) - **Best for presentations!**

**Automation:**
- `scripts/full-setup.sh` - Automated setup

See [scripts/README.md](scripts/README.md) for complete documentation.

## Status

- ✅ PR #1-6: Core features + error handling
- ✅ PR #7: Metrics & observability
- ✅ PR #8: Automated tests (24 cases)
