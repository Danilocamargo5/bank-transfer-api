# Bank Transfer API - Setup & Running Locally

Step-by-step guide to setup and run the application locally.

## Requirements

- Docker & Docker Compose
- Java 21
- Python 3 (for serving dashboard)
- Git

## Quick Start (Recommended)

### Terminal 1: Clean and Setup Infrastructure
```bash
# Clean everything (optional, only if previous setup failed)
./scripts/stop-infra.sh
docker system prune -af --volumes
sudo rm -rf /tmp/localstack

# Update code
git pull origin develop

# Start infrastructure automatically
./full-setup.sh

# Wait for: "✅ Infrastructure Ready!"
# This creates Kafka topics and SQS queue
```

### Terminal 2: Start Application
```bash
./scripts/start-app.sh

# Wait for: "DynamoDB tables initialized successfully!"
# ⚠️ IMPORTANT: This automatically:
#    - Creates DynamoDB tables
#    - Loads sample account data (acc-123, acc-456, acc-789, acc-000)
#    - Starts Spring Boot API on port 8080
```

### Terminal 3: Generate Test Data (Optional)
```bash
# Generate 40+ test transfers to populate dashboard
./scripts/DEMO2.sh

# Or just 8 basic scenarios
./scripts/DEMO.sh
```

### Terminal 4: View Dashboard
```bash
# Dashboard is automatically served by Spring Boot
# Open in browser: http://localhost:8080/metrics-dashboard.html
```

## Manual Setup (Step-by-step)

If you prefer to run each step manually:

### Terminal 1: Start Infrastructure
```bash
./scripts/start-infra.sh

# Wait for: "✅ Services ready!"
# This starts Kafka and LocalStack (DynamoDB, SQS)
```

### Terminal 2: Initialize Kafka and SQS
```bash
./scripts/init-kafka.sh
./scripts/init-sqs.sh

# This creates:
# - Kafka topics (transfer-requested, transfer-completed)
# - SQS queue (transfer-failed DLQ)
```

### Terminal 1: Start Application
```bash
./scripts/start-app.sh

# Wait for: "DynamoDB tables initialized successfully!"
# ⚠️ IMPORTANT: This automatically creates and populates DynamoDB:
#    - Creates 'accounts' table
#    - Creates 'transactions' table
#    - Loads sample data:
#      * acc-123: João Silva - 5000.00 BRL (ACTIVE)
#      * acc-456: Maria Santos - 1200.50 BRL (ACTIVE)
#      * acc-789: Pedro Costa - 300.00 BRL (ACTIVE)
#      * acc-000: Conta Encerrada - 0.00 BRL (INACTIVE)
```

### (Optional) Reinitialize Sample Data
If you need to reload sample data after running demos:
```bash
./scripts/init-dynamodb.sh
```

## Initialization Scripts

### init-infrastructure.sh
Runs all initialization at once:
```bash
./scripts/init-infrastructure.sh
```

This executes in order:
1. `init-kafka.sh` - Creates Kafka topics
2. `init-sqs.sh` - Creates SQS queue
3. `init-dynamodb.sh` - Inserts sample account data

### Data Loading Timeline

```
Terminal 1: ./scripts/start-infra.sh
  → Docker starts
  → Kafka initializes (KRaft mode)
  → LocalStack starts (DynamoDB, SQS)
  ✅ "Services ready!"

Terminal 2: ./scripts/init-kafka.sh && ./scripts/init-sqs.sh
  → Kafka topics created
  → SQS queue created
  ✅ Ready for messages

Terminal 1: ./scripts/start-app.sh
  → Spring Boot starts
  → DynamoDBInitializer runs (Spring Boot startup)
  → Creates 'accounts' table
  → Creates 'transactions' table
  → Loads 4 sample accounts (acc-123, acc-456, acc-789, acc-000)
  ✅ "DynamoDB tables initialized successfully!"
  ✅ API ready on http://localhost:8080

Terminal 3: ./scripts/DEMO2.sh
  → Sends 40+ transfer requests
  → Kafka processes messages
  → Data stored in DynamoDB
  → Metrics collected

Terminal 4: Open dashboard
  → See all metrics populated
```

## Available Scripts

### Infrastructure Lifecycle
- `start-infra.sh` - Start Docker containers (Kafka + LocalStack)
- `stop-infra.sh` - Stop all Docker containers

### Application Lifecycle
- `start-app.sh` - Start Spring Boot application
- `stop-app.sh` - Stop Spring Boot application

### Initialization
- `init-infrastructure.sh` - Create topics, queue, and sample data (all at once)
- `init-kafka.sh` - Create Kafka topics only
- `init-sqs.sh` - Create SQS queue only
- `init-dynamodb.sh` - Insert sample account data (or re-insert after demos)

### Testing & Demo
- `DEMO.sh` - Run basic demo (8 test scenarios)
- `DEMO2.sh` - Run extended demo (40+ test data points) ⭐ Best for presentations

### Automation
- `full-setup.sh` - Automated setup (calls infrastructure scripts)

## Testing

### Run Demo Tests

**Basic demo:**
```bash
./scripts/DEMO.sh
```

**Extended demo (recommended for presentations):**
```bash
./scripts/DEMO2.sh
```

### Manual API Tests

Success transfer:
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-test-001",
    "sourceAccountId":"acc-123",
    "destinationAccountId":"acc-456",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'
```

Failed transfer (account not found):
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-fail-001",
    "sourceAccountId":"acc-999",
    "destinationAccountId":"acc-456",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'
```

Check DLQ (failed transfers):
```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --endpoint-url http://localhost:4566 \
  --region us-east-1
```

## Metrics & Monitoring

### Dashboard
```bash
# Serve dashboard
python3 -m http.server 8888

# Open: http://localhost:8888/metrics-dashboard.html
```

### API Endpoints
```bash
# All available metrics
curl http://localhost:8080/actuator/metrics

# Transfer processing time
curl http://localhost:8080/actuator/metrics/transfer.processing.time

# Successful transfers count
curl http://localhost:8080/actuator/metrics/transfer.success.total

# Failed transfers count
curl http://localhost:8080/actuator/metrics/transfer.failure.total

# Application health
curl http://localhost:8080/actuator/health
```

## Stopping Services

### Stop Application Only
Keeps infrastructure running (can restart without losing data):
```bash
./scripts/stop-app.sh
```

### Stop All Infrastructure
Stops all Docker containers:
```bash
./scripts/stop-infra.sh
```

## Troubleshooting

### LocalStack crashes with "Device or resource busy"
```bash
./scripts/stop-infra.sh
docker system prune -af --volumes
sudo rm -rf /tmp/localstack
./scripts/start-infra.sh
```

### Spring Boot won't connect to Kafka
- Wait 120 seconds after `start-infra.sh` - Kafka needs time to stabilize
- Check Kafka is healthy: logs should show KRaft mode active

### No data in dashboard
- Run `./scripts/DEMO2.sh` to generate test data
- Wait 2 seconds for Kafka to process messages
- Refresh dashboard (F5)

### Port already in use
```bash
# Find what's using the port
lsof -i :8080
lsof -i :9092
lsof -i :4566

# Kill if needed
kill -9 <PID>
```

## Development Workflow

### Restart app (keep data):
```bash
./scripts/stop-app.sh
./scripts/start-app.sh
```

### Full clean restart (lose data):
```bash
./scripts/stop-infra.sh
docker system prune -af --volumes
sudo rm -rf /tmp/localstack
./full-setup.sh
./scripts/start-app.sh
```

### Add more test data:
```bash
./scripts/DEMO2.sh
```

## Database Schema

### DynamoDB Tables
- **accounts** - Account data (accountId, balance, status, etc)
- **transactions** - Transfer records (transferId, status, result)

### Kafka Topics
- **transfer-requested** - Incoming transfer events
- **transfer-completed** - Successful transfer events

### SQS Queues
- **transfer-failed** - Dead Letter Queue for failed transfers

## Next Steps

For detailed project information, see:
- [../README.md](../README.md) - Project overview and features
- [../docs/CODE_REVIEW_LEGACY.md](../docs/CODE_REVIEW_LEGACY.md) - Code review insights
- [../docs/ERROR_HANDLING.md](../docs/ERROR_HANDLING.md) - Error handling strategy
