# Bank Transfer API - Setup & Running

## Quick Start (Automated)

```bash
git pull origin develop

./full-setup.sh
```

Segue as instruções que aparecem!

---

## Manual Setup (Recomendado para entender)

### Terminal 1: Start Services
```bash
./services.sh
# Aguarda 120 segundos até aparecer: ✅ Services ready!
```

### Terminal 2: Initialize Infrastructure
```bash
./init-kafka.sh
sleep 30
./init-sqs.sh
```

### Terminal 1: Start Application
```bash
./start.sh
# Aguarda até aparecer: DynamoDB tables initialized successfully!
```

### Terminal 2: Insert Sample Data
```bash
./init-dynamodb.sh
```

---

## Testing

### Health Check
```bash
curl http://localhost:8080/health
```

### Successful Transfer
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

Response:
```json
{
  "transferId":"tf-test-001",
  "status":"PENDING",
  "message":"Transfer request received and queued for processing"
}
```

### Failed Transfer (Goes to DLQ)
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-test-fail-001",
    "sourceAccountId":"acc-invalid",
    "destinationAccountId":"acc-002",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'
```

### Check DLQ (Failed Messages)
```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --endpoint-url http://localhost:4566 \
  --region us-east-1
```

---

## Architecture

```
POST /api/v1/transfers (Spring Boot 8080)
  ↓
Kafka Topic: transfer-requested
  ↓
TransferKafkaConsumer
  ├─ Validate (account exists, balance, status)
  ├─ Execute atomically (@Transactional)
  ├─ Update DynamoDB
  └─ Publish success/failure
  
Success Path:
  → Kafka Topic: transfer-completed
  → Log structured JSON
  
Failure Path:
  → SQS Queue: transfer-failed (DLQ)
  → Log error details
```

---

## Files & Scripts

| Script | Purpose |
|--------|---------|
| `services.sh` | Start Docker containers (Kafka + LocalStack) |
| `init-kafka.sh` | Create Kafka topics |
| `init-dynamodb.sh` | Insert sample account data |
| `init-sqs.sh` | Create SQS queue |
| `start.sh` | Start Spring Boot application |
| `stop.sh` | Stop all containers |
| `full-setup.sh` | Automated setup (combine all) |

---

## Troubleshooting

### LocalStack crashes with "Device or resource busy"
```bash
./stop.sh
docker system prune -af --volumes
sudo rm -rf /tmp/localstack
# Then restart
```

### Spring Boot won't connect to Kafka
- Wait 120 seconds after `./services.sh`
- Kafka needs time to stabilize

### No response from transfers endpoint
- Check Spring Boot logs: `./start.sh` output
- Verify Kafka topics exist: `./init-kafka.sh`
- Verify DynamoDB tables exist and have data

---

## Documentation

- `docs/CODE_REVIEW_LEGACY.md` - Analysis of 8 problems in legacy code and solutions
- `docs/ERROR_HANDLING.md` - Error handling strategy
- `README.md` - Project overview

