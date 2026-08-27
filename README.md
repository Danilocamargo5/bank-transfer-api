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

## Getting Started

### Prerequisites

- Java 21+
- Gradle 8.0+
- Docker & Docker Compose

### Local Development

1. **Start Infrastructure** (LocalStack, Kafka, Zookeeper, Admin Tools):
```bash
make dev-up
# or
docker-compose up -d
```

   This starts:
   - LocalStack (DynamoDB + SQS) on http://localhost:4566
   - Kafka on localhost:9092
   - Kafka UI on http://localhost:8081
   - DynamoDB Admin on http://localhost:8001

2. **Build Project**:
```bash
make build
# or
./gradlew clean build
```

3. **Run Application**:
```bash
make run
# or
./gradlew bootRun --args='--spring.profiles.active=dev'
```

4. **Check Application Health**:
```bash
curl http://localhost:8080/actuator/health
```

5. **Access Admin Tools**:
   - **Kafka UI**: http://localhost:8081 (monitor topics and messages)
   - **DynamoDB Admin**: http://localhost:8001 (browse tables and data)
   - **Application Metrics**: http://localhost:8080/actuator/metrics

6. **Stop Infrastructure**:
```bash
make dev-down
# or
docker-compose down
```

### For Detailed Infrastructure Setup

See [INFRASTRUCTURE.md](./INFRASTRUCTURE.md) for:
- Service details and ports
- Sample data insertion
- Monitoring and debugging
- Troubleshooting guide

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
