.PHONY: help setup build test run dev-up dev-down clean

help:
	@echo "Bank Transfer API - Available Commands"
	@echo "======================================="
	@echo "make setup      - Initialize project (gradle wrapper)"
	@echo "make build      - Build project"
	@echo "make test       - Run tests"
	@echo "make run        - Run application"
	@echo "make dev-up     - Start Docker Compose (DynamoDB, Kafka, Zookeeper)"
	@echo "make dev-down   - Stop Docker Compose"
	@echo "make clean      - Clean build artifacts"
	@echo "make logs       - Show Docker Compose logs"

setup:
	gradle wrapper

build:
	./gradlew clean build

test:
	./gradlew test

run:
	./gradlew bootRun --args='--spring.profiles.active=dev'

dev-up:
	docker-compose up -d
	@echo "Infrastructure started. Kafka UI available at http://localhost:8080"

dev-down:
	docker-compose down

clean:
	./gradlew clean
	docker-compose down

logs:
	docker-compose logs -f
