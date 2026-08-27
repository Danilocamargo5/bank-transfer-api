# Development Guide

## Project Structure

The project follows a layered architecture:

```
com.danilo.banktransfer
├── domain/           # Domain models and entities
├── application/      # Use cases and services
├── infrastructure/   # Repositories, Kafka consumers, AWS clients
└── config/          # Spring configuration
```

## Running Locally

### Prerequisites
- Java 21+
- Docker & Docker Compose

### Start Infrastructure
```bash
make dev-up
```

### Run Application
```bash
make run
```

### Stop Infrastructure
```bash
make dev-down
```

## Testing

```bash
make test
```

## Building

```bash
make build
```

## More Information

See README.md for architecture and feature phases.
