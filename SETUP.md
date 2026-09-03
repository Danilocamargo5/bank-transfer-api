# Bank Transfer API - Setup & Running

## Prerequisites

- Docker & Docker Compose
- Java 21+
- Gradle 8.8+
- Bash

---

## Quick Start (Automated - Recommended)

```bash
cd /workspaces/bank-transfer-api

# Terminal 1: Start all infrastructure + application
./scripts/full-setup.sh

# Wait for message: ✅ Application started successfully!
```

---

## Manual Setup (For Understanding Components)

### Terminal 1: Start Infrastructure

```bash
./scripts/start-infra.sh
```

Wait until you see:
```
✅ Infrastructure ready!
   - LocalStack: http://localhost:4566
   - Kafka: localhost:9092
```

### Terminal 2: Start Application

```bash
./scripts/start-app.sh
```

Wait until you see:
```
✅ Application started successfully!
   - API: http://localhost:8080
   - Health: http://localhost:8080/actuator/health
```

---

## Publishing Transfers

**Important:** Transfers are published via EXTERNAL SCRIPTS, not via API POST.

### Terminal 3: Publish a Transfer

```bash
./scripts/publish-transfer.sh tf-demo-001 acc-123 acc-456 100.00
```

**Parameters:**
- `tf-demo-001`: Transfer ID
- `acc-123`: Source account (from sample data)
- `acc-456`: Destination account (from sample data)
- `100.00`: Amount in BRL

**Output:**
```
Published to Kafka:
{
  "transferId": "tf-demo-001",
  "sourceAccountId": "acc-123",
  "destinationAccountId": "acc-456",
  "amount": 100.00,
  "currency": "BRL",
  "requestedAt": "2026-09-03T15:30:00Z"
}
```

---

## Monitoring & Validation

### Health Check

```bash
curl http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{
  "status": "UP"
}
```

### Query Transfer Status

```bash
curl http://localhost:8080/api/v1/transfers/tf-demo-001
```

**Success Response:**
```json
{
  "transferId": "tf-demo-001",
  "sourceAccountId": "acc-123",
  "destinationAccountId": "acc-456",
  "amount": 100.00,
  "status": "COMPLETED",
  "completedAt": "2026-09-03T15:30:05Z"
}
```

### Check Failed Transfers (DLQ)

```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --endpoint-url http://localhost:4566 \
  --region us-east-1
```

### Monitor Kafka Topics

```bash
# List all topics
docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Watch messages on transfer-requested topic
docker-compose exec kafka kafka-console-consumer.sh \
  --topic transfer-requested \
  --bootstrap-server localhost:9092 \
  --from-beginning

# Watch messages on transfer-completed topic
docker-compose exec kafka kafka-console-consumer.sh \
  --topic transfer-completed \
  --bootstrap-server localhost:9092 \
  --from-beginning
```

---

## Architecture

```
External Script
./scripts/publish-transfer.sh
       ↓
Kafka Topic: transfer-requested
       ↓
TransferKafkaConsumer (Manual ACK)
       ↓
TransferService (Application Logic)
   ├─ Validate transfer
   ├─ Get source & destination accounts
   ├─ Calculate debit/credit
   └─ Save ATOMICALLY via DynamoDB TransactWriteItems
       ├─ Guarantee: BOTH save OR NEITHER saves
       └─ Retry up to 3 times (exponential backoff)
       ↓
   ├─ Success: Publish to transfer-completed topic
   ├─ Failure: Send to transfer-failed queue (SQS)
   └─ Critical: Send to transfer-failed-dlq (SQS DLQ)

DynamoDB (Accounts + Transfer Records)
```

---

## Scripts Reference

| Script | Purpose | Wait Time |
|--------|---------|-----------|
| `scripts/start-infra.sh` | Start Docker (LocalStack, Kafka) | ~30s |
| `scripts/start-app.sh` | Start Spring Boot application | ~10s |
| `scripts/stop-infra.sh` | Stop Docker containers | ~5s |
| `scripts/stop-app.sh` | Stop Spring Boot | ~3s |
| `scripts/full-setup.sh` | Automated: infra + app + init data | ~60s |
| `scripts/init-dynamodb.sh` | Insert sample account data | ~5s |
| `scripts/init-kafka.sh` | Create Kafka topics | ~2s |
| `scripts/init-sqs.sh` | Create SQS queues | ~2s |
| `scripts/publish-transfer.sh` | Publish transfer to Kafka | ~1s |
| `scripts/DEMO.sh` | Run demo with 5 transfers | ~30s |
| `scripts/DEMO2.sh` | Run demo with failure scenarios | ~30s |

---

## Sample Accounts (Pre-loaded)

| Account ID | Balance | Currency | Status |
|-----------|---------|----------|--------|
| acc-123 | 5000.00 | BRL | ACTIVE |
| acc-456 | 1200.50 | BRL | ACTIVE |
| acc-789 | 300.00 | BRL | ACTIVE |
| acc-000 | 0.00 | BRL | INACTIVE |

---

## Running Tests

### All Tests

```bash
./gradlew test
```

### Atomicity Tests Only

```bash
./gradlew test --tests AtomicityGuaranteeTest -v
```

### With Code Coverage

```bash
./gradlew test jacocoTestReport coverageSummary
```

Report: `build/reports/jacoco/test/html/index.html`

---

## Troubleshooting

### LocalStack crashes ("Device or resource busy")

```bash
./scripts/stop-infra.sh
docker system prune -af --volumes
sudo rm -rf /tmp/localstack
./scripts/start-infra.sh
```

### Spring Boot won't start

- Verify LocalStack is running: `docker-compose ps`
- Check port 8080 is available: `lsof -i :8080`
- Read logs: `./scripts/start-app.sh` output

### Transfers not processing

- Verify Kafka is running: `docker-compose exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092`
- Check Spring Boot logs
- Verify sample data exists: `docker-compose exec localstack aws dynamodb scan --table-name Account --endpoint-url http://localhost:4566`

### Published transfer not showing as COMPLETED

- Check CloudWatch logs: `docker-compose logs app`
- Verify account exists in DynamoDB
- Check if transfer hit DLQ: `./scripts/check-dlq.sh` (or manual SQS query)

---

## Performance Validation

### Load Test (Optional)

```bash
# Terminal 3: Publish 100 transfers in a loop
for i in {1..100}; do
  ./scripts/publish-transfer.sh tf-load-$i acc-123 acc-456 10.00
  sleep 0.1
done

# Monitor processing
watch -n 1 'curl -s http://localhost:8080/actuator/health'
```

---

## Cleanup

```bash
# Stop everything
./scripts/stop-app.sh
./scripts/stop-infra.sh

# Remove all containers and volumes
docker-compose down -v
docker system prune -af --volumes

# Remove LocalStack data
sudo rm -rf /tmp/localstack
```

---

## Next Steps

- Read `README.md` for project overview
- Read `OPERACAO.md` for advanced monitoring
- Review test cases: `src/test/kotlin/com/danilo/banktransfer/application/AtomicityGuaranteeTest.kt`
- Check implementation: `src/main/kotlin/com/danilo/banktransfer/application/TransferService.kt`

---

**Ready to transfer!** 🚀

