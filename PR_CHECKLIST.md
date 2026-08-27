# PR Checklist & Development Guide

## PR #1 - Project Setup ✅

### Changes Made:
- [x] Initialize Spring Boot 3.2.4 project with Java 21 & Kotlin
- [x] Configure Gradle DSL (build.gradle.kts) with dependencies
  - Spring Boot WebFlux
  - Spring Kafka
  - AWS SDK for DynamoDB
  - kotlin-logging
  - TestContainers for integration tests
- [x] Create project structure (domain, application, infrastructure, config)
- [x] Add docker-compose.yml (DynamoDB Local, Kafka, Zookeeper, Kafka UI)
- [x] Configure application.properties with externalized config
- [x] Add development profile (application-dev.properties)
- [x] Create .gitignore for Gradle, IDE, and build artifacts
- [x] Add comprehensive README with architecture and setup guide
- [x] Create gradle.properties for optimization

### How to Test:
```bash
# Navigate to project
cd bank-transfer-api

# Build project
./gradlew clean build

# Start infrastructure
docker-compose up -d

# Run application
./gradlew bootRun --args='--spring.profiles.active=dev'

# Check health endpoint
curl http://localhost:8080/actuator/health

# Stop infrastructure
docker-compose down
```

### Key Files:
- `build.gradle.kts` - Gradle configuration with all dependencies
- `settings.gradle.kts` - Root project configuration
- `src/main/resources/application.properties` - Main configuration
- `src/main/resources/application-dev.properties` - Dev profile
- `docker-compose.yml` - Local infrastructure (DynamoDB, Kafka, Zookeeper)
- `README.md` - Project documentation and architecture

### Next Steps (PR #2):
- Create domain models (Account, Transfer, TransferEvent)
- Define data classes and enums
- Create transfer request/response DTOs
