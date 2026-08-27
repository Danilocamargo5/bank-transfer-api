# Local Infrastructure Setup

Complete guide for running the bank-transfer-api with all local dependencies.

## 📋 Prerequisites

- Docker & Docker Compose
- Java 21+
- Git

## 🚀 Quick Start

### 1. Start Infrastructure

```bash
docker-compose up -d
```

This will start:
- **LocalStack** (DynamoDB + SQS)
- **Kafka** + **Zookeeper**
- **Kafka UI** (web interface)
- **DynamoDB Admin** (web interface)
- **Kafka Topic Initializer** (auto-creates topics)

### 2. Verify Services

Check if all services are healthy:

```bash
docker-compose ps
```

Expected output:
```
NAME              STATUS
localstack        Up (healthy)
zookeeper         Up (healthy)
kafka             Up (healthy)
kafka-init        Exited (0)
kafka-ui          Up (healthy)
dynamodb-admin    Up (healthy)
```

### 3. Run the Application

```bash
./gradlew bootRun --args='--spring.profiles.active=dev'
```

Or use the Makefile:

```bash
make run
```

### 4. Test Application Health

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

## 🛠️ Infrastructure Services

### LocalStack (AWS Emulation)
- **Port**: 4566
- **Services**: DynamoDB, SQS
- **Credentials**: 
  - Access Key: `local`
  - Secret Key: `local`
  - Region: `us-east-1`

**Tables Created**:
- `accounts` - Account information
- `transfers` - Transfer transactions

**Queues Created**:
- `transfer-failed` - Failed transfers queue (with retry policy)
- `transfer-failed-dlq` - Dead Letter Queue for failed retries

### Kafka
- **Bootstrap Server**: localhost:9092 (localhost)
- **Internal**: kafka:29092 (from containers)
- **Zookeeper**: localhost:2181

**Topics Created**:
- `transfer-requested` - Incoming transfer requests
- `transfer-completed` - Successful transfers
- `transfer-failed` - Transfers that failed

### Kafka UI (Web Interface)
- **URL**: http://localhost:8081
- **Purpose**: Monitor Kafka topics, messages, and consumer groups
- **Features**:
  - View topics and partitions
  - See message content
  - Monitor consumer groups
  - Check lag

### DynamoDB Admin (Web Interface)
- **URL**: http://localhost:8001
- **Purpose**: Browse and manage DynamoDB tables
- **Features**:
  - View table schemas
  - Browse items
  - Create/edit/delete items
  - Execute queries and scans

### Application
- **URL**: http://localhost:8080
- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

## 📊 Sample Data

### Insert Test Account via DynamoDB Admin

1. Go to http://localhost:8001
2. Select `accounts` table
3. Create item:
```json
{
  "accountId": "acc-123",
  "balance": 5000.00,
  "currency": "BRL",
  "status": "ACTIVE",
  "customerName": "João Silva"
}
```

### Send Test Message via Kafka UI

1. Go to http://localhost:8081
2. Select cluster "local"
3. Go to "Topics" → "transfer-requested"
4. Send message:
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

## 🔍 Monitoring

### View Kafka Topics

```bash
# Inside container
docker exec kafka kafka-topics --list --bootstrap-server localhost:9092

# Or using docker-compose
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### View Kafka Messages

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transfer-requested \
  --from-beginning
```

### View DynamoDB Tables

```bash
# List tables
docker exec localstack awslocal dynamodb list-tables

# Scan accounts
docker exec localstack awslocal dynamodb scan --table-name accounts

# Scan transfers
docker exec localstack awslocal dynamodb scan --table-name transfers
```

### View SQS Queues

```bash
# List queues
docker exec localstack awslocal sqs list-queues

# Receive messages from DLQ
docker exec localstack awslocal sqs receive-message \
  --queue-url http://localhost:4566/000000000000/transfer-failed-dlq
```

### Application Logs

```bash
# View application logs
docker-compose logs -f app

# Or using make
make logs
```

## 🧹 Cleanup

### Stop Services

```bash
docker-compose down
```

### Stop and Remove Data

```bash
docker-compose down -v
```

### Full Reset

```bash
# Remove all volumes and containers
docker-compose down -v

# Remove LocalStack data
rm -rf .localstack/

# Start fresh
docker-compose up -d
```

## 🐛 Troubleshooting

### Services Won't Start

```bash
# Check logs
docker-compose logs localstack
docker-compose logs kafka

# Restart services
docker-compose restart
```

### LocalStack Not Ready

```bash
# Wait for LocalStack
docker-compose exec localstack awslocal dynamodb list-tables
```

### Kafka Topics Not Created

```bash
# Manually create topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 \
  --create --topic transfer-requested --partitions 1 --replication-factor 1
```

### Connection Refused

- Ensure all containers are running: `docker-compose ps`
- Check firewall/network settings
- Verify ports are not in use: `lsof -i :4566,9092,2181,8001,8081`

## 📚 Configuration Files

- `docker-compose.yml` - Infrastructure definition
- `src/main/resources/application.properties` - Application config
- `src/main/resources/application-dev.properties` - Development profile
- `scripts/init-localstack.sh` - LocalStack initialization script

## 🔗 Useful Commands

```bash
# Start infrastructure with logs
docker-compose up

# Start in background
docker-compose up -d

# View logs
docker-compose logs -f

# Stop all services
docker-compose stop

# Restart services
docker-compose restart

# Remove containers but keep volumes
docker-compose down

# Remove everything including volumes
docker-compose down -v

# Execute command in container
docker-compose exec <service> <command>

# Build/rebuild services
docker-compose build

# Build and start
docker-compose up -d --build
```

## 🎯 Next Steps

1. ✅ Infrastructure running
2. ✅ Application started
3. 📝 Insert test data via DynamoDB Admin
4. 📤 Send test messages via Kafka UI
5. 📊 Monitor application logs
6. 🧪 Verify application processing

See README.md for application architecture and development workflow.
