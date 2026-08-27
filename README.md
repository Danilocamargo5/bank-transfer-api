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
API Gateway / External Service
        ↓ (public event)
    Kafka Topic "transfer-requested"
        ↓
Microservice (Kotlin + Spring Boot)
    - Validates transfer
    - Debits source account
    - Credits destination account
    ↓ (business logic failures)
SQS DLQ "transfer-failed"
    ↓ (success)
Kafka Topic "transfer-completed"
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

1. Start infrastructure:
```bash
docker-compose up -d
```

2. Build project:
```bash
./gradlew build
```

3. Run application:
```bash
./gradlew bootRun
```

4. Check health:
```bash
curl http://localhost:8080/actuator/health
```

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
