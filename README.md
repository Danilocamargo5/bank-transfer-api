# Bank Transfer API

A microservice for processing bank transfers with Kafka event streaming, DynamoDB persistence, and robust error handling.

## Technology Stack

- **Language**: Kotlin
- **Framework**: Spring Boot 3.2.4
- **Runtime**: Java 21
- **Build Tool**: Gradle (Kotlin DSL)
- **Database**: AWS DynamoDB
- **Message Queue**: Apache Kafka
- **Logging**: kotlin-logging

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                      LOCAL DEVELOPMENT ENVIRONMENT              │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Kafka Topics (via Docker)                                      │
│  ├─ transfer-requested → [Event Input]                          │
│  ├─ transfer-completed → [Success Event]                        │
│  └─ transfer-failed → [Business Failure Event]                  │
│                                                                 │
│  DynamoDB (via LocalStack)                                      │
│  ├─ accounts table (account data & balance)                     │
│  └─ transfers table (transaction history)                       │
│                                                                 │
│  SQS (via LocalStack)                                           │
│  ├─ transfer-failed queue (retry logic)                         │
│  └─ transfer-failed-dlq (dead letter queue)                     │
│                                                                 │
│  Microservice (Spring Boot 3.2.4 + Kotlin)                      │
│  └─ Consumes transfer-requested                                 │
│     ├─ Validates (balance, accounts, amount, currency)          │
│     ├─ Updates DynamoDB atomically                              │
│     ├─ Publishes transfer-completed on success                  │
│     └─ Publishes to SQS transfer-failed on business errors      │
│                                                                 │
│  Admin Tools (Web UIs)                                          │
│  ├─ Kafka UI (http://localhost:8081) - Monitor topics           │
│  └─ DynamoDB Admin (http://localhost:8001) - Browse data        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

## Functional Requirements

1. Consume events from Kafka topic `transfer-requested`
2. Validate transfer (sufficient balance, active accounts, positive amount, BRL currency)
3. Execute atomic transfer (debit and credit)
4. Guarantee idempotence (same transfer won't be processed twice)
5. Send failed transfers to DLQ with business reason
6. Send SQS message to `transfer-failed` for rejected transfers
7. Persist transfer record with final status (completed/failed)

## Non-Functional Requirements

1. Application built in Kotlin (1.9+) with Spring Boot 3.x
2. Persistence using DynamoDB (LocalStack for development)
3. Messaging with Kafka (Testcontainers or docker-compose)
4. Build with Gradle (Kotlin DSL preferred)
5. Automated tests covering success, failure, idempotence, and atomicity scenarios
6. All infrastructure via docker-compose
7. No hardcoded credentials or endpoints (externalized configuration)

## Project Structure

```
bank-transfer-api/
├── src/
│   ├── main/
│   │   ├── kotlin/com/danilo/banktransfer/
│   │   │   ├── domain/              # Domain models and entities
│   │   │   ├── application/         # Use cases and services
│   │   │   ├── infrastructure/      # Repositories, Kafka consumers, AWS clients
│   │   │   └── config/              # Spring configuration
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── kotlin/com/danilo/banktransfer/
├── build.gradle.kts
├── docker-compose.yml
└── README.md
```

## Quick Start (Local Development)

### Prerequisites

- **Java 21+**
- **Docker & Docker Compose** (for local infrastructure)
- **Gradle 8.0+** (or use `./gradlew`)
- **curl** (for testing endpoints)

### 1️⃣ Start Local Infrastructure

All services run in Docker (LocalStack, Kafka, Zookeeper, Admin Tools):

```bash
make dev-up
```

Or manually:
```bash
docker-compose up -d
```

**Wait 30-60 seconds** for services to initialize.

Verify all services are healthy:
```bash
docker-compose ps
```

Expected output:
```
NAME              STATUS
localstack        Up (healthy)
zookeeper         Up (healthy)
kafka             Up (healthy)
kafka-init        Exited
dynamodb-admin    Up (healthy)
kafka-ui          Up (healthy)
```

### 2️⃣ Build the Project

```bash
make build
```

Or:
```bash
./gradlew clean build
```

### 3️⃣ Run the Application

Open a new terminal and run:

```bash
make run
```

Or:
```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Wait for the application to start. You should see:
```
Started BankTransferApplication in X seconds
```

### 4️⃣ Verify Application is Running

```bash
curl http://localhost:8080/actuator/health
```

Expected response:
```json
{
  "status": "UP",
  "components": {
    "kafka": { "status": "UP" },
    "dynamodb": { "status": "UP" }
  }
}
```

### 5️⃣ Access Admin Tools

Open these URLs in your browser:

| Tool | URL | Purpose |
|------|-----|---------|
| **Kafka UI** | http://localhost:8081 | Monitor topics, messages, consumer groups |
| **DynamoDB Admin** | http://localhost:8001 | Browse tables, view/edit items |
| **App Metrics** | http://localhost:8080/actuator/metrics | Application metrics |

### 6️⃣ Test the Application

#### Insert Sample Account Data

1. Go to http://localhost:8001 (DynamoDB Admin)
2. Select `accounts` table
3. Create item with sample data:

```json
{
  "accountId": "acc-123",
  "balance": 5000.00,
  "currency": "BRL",
  "status": "ACTIVE",
  "customerName": "João Silva"
}
```

4. Create another account for destination:

```json
{
  "accountId": "acc-456",
  "balance": 1000.00,
  "currency": "BRL",
  "status": "ACTIVE",
  "customerName": "Maria Santos"
}
```

#### Send Test Transfer Message

1. Go to http://localhost:8081 (Kafka UI)
2. Click on `transfer-requested` topic
3. Send message:

```json
{
  "transferId": "550e8400-e29b-41d4-a716-446655440000",
  "sourceAccountId": "acc-123",
  "destinationAccountId": "acc-456",
  "amount": 150.75,
  "currency": "BRL",
  "requestedAt": "2025-01-15T10:30:00Z"
}
```

#### Monitor Results

1. **Check Kafka Topics** (http://localhost:8081):
   - Look at `transfer-completed` for successful transfers
   - Look at `transfer-failed` for business logic failures

2. **Check DynamoDB Tables** (http://localhost:8001):
   - View `transfers` table to see transaction history
   - View `accounts` table to see updated balances

3. **View Application Logs**:
```bash
docker-compose logs -f
```

### 7️⃣ Stop Everything

```bash
make dev-down
```

Or:
```bash
docker-compose down
```

### ✨ Full Cleanup (Fresh Start)

```bash
make clean
# or
docker-compose down -v
rm -rf .localstack/
```

---

## 📚 Local Infrastructure Services

### LocalStack (AWS Emulation)
- **Port**: 4566
- **Services**: DynamoDB + SQS
- **Access Key**: `local`
- **Secret Key**: `local`
- **Region**: `us-east-1`

**Resources Created**:
- `accounts` table - Account information
- `transfers` table - Transfer history
- `transfer-failed` queue - Failed transfers with retry (max 3 attempts)
- `transfer-failed-dlq` - Dead letter queue

### Kafka + Zookeeper
- **Kafka**: localhost:9092
- **Zookeeper**: localhost:2181

**Topics**:
- `transfer-requested` - Incoming transfer requests
- `transfer-completed` - Successful transfers
- `transfer-failed` - Business logic failures

### Admin Web UIs
- **Kafka UI**: http://localhost:8081
  - View topics and partitions
  - Monitor messages and consumer groups
  - Send test messages
  
- **DynamoDB Admin**: http://localhost:8001
  - Browse table schemas
  - Create/view/edit/delete items
  - Run queries and scans

---

## 🔍 Useful Commands

```bash
# Infrastructure
make dev-up              # Start all services
make dev-down           # Stop all services
make clean              # Stop and clean everything

# Build & Run
make build              # Build project
make run                # Run application
make test               # Run tests

# View logs
docker-compose logs -f  # Follow all container logs
docker-compose logs -f kafka    # Follow specific service

# Check services
docker-compose ps       # List running containers
docker-compose ps -a    # List all containers

# Access containers
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092
docker exec localstack awslocal dynamodb scan --table-name accounts
docker exec localstack awslocal sqs list-queues
```

---

## 📋 Complete Workflow Example

```bash
# 1. Terminal 1: Start infrastructure
make dev-up
# Wait 30-60 seconds...

# 2. Terminal 2: Run application
make run
# Wait for "Started BankTransferApplication"

# 3. Terminal 3: Test application
curl http://localhost:8080/actuator/health

# 4. Browser: Insert test data
# http://localhost:8001 (DynamoDB Admin)
# Create accounts acc-123 and acc-456

# 5. Browser: Send transfer message
# http://localhost:8081 (Kafka UI)
# Send message to transfer-requested topic

# 6. Monitor: Check results
# - Kafka UI: monitor transfer-completed topic
# - DynamoDB Admin: check updated balances
# - Application logs: Terminal 2

# 7. Stop everything
# Ctrl+C on Terminal 2
make dev-down
```

---

## ⚙️ Configuration Files

- **`docker-compose.yml`** - Infrastructure definition
- **`application.properties`** - Main application config
- **`application-dev.properties`** - Development profile config
- **`scripts/init-localstack.sh`** - LocalStack table/queue initialization
- **`Makefile`** - Convenient commands

---

## 🐛 Troubleshooting

### Services won't start
```bash
# Check logs
docker-compose logs localstack
docker-compose logs kafka

# Restart
docker-compose restart
```

### Port already in use
```bash
# Find what's using the port (example: 9092)
lsof -i :9092

# Or change the port in docker-compose.yml
```

### Application can't connect to infrastructure
```bash
# Verify containers are running
docker-compose ps

# Verify network connectivity
docker exec <container> ping kafka
```

### Kafka topics not created
```bash
# Manually create topics
docker-compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --topic transfer-requested \
  --partitions 1 --replication-factor 1
```

### Clear everything and start fresh
```bash
docker-compose down -v
rm -rf .localstack/
docker-compose up -d
```

See [INFRASTRUCTURE.md](./INFRASTRUCTURE.md) for detailed documentation.

## Development Phases

- **PR #1**: Project setup and dependencies ✅
- **PR #2**: Domain models and entities
- **PR #3**: DynamoDB repositories
- **PR #4**: Kafka consumer (transfer-requested)
- **PR #5**: Transfer processing logic
- **PR #6**: Error handling, retry, and DLQ
- **PR #7**: Metrics and observability
- **PR #8**: Automated tests and final validation

## Testing

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport
```

## Contributing

1. Create feature branch from `develop`
2. Implement changes with tests
3. Submit Pull Request with clear description
4. All tests must pass before merge

## License

MIT
