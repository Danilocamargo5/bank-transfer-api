# Bank Transfer API - Scripts

All automation scripts are organized here.

## Quick Start

### Option 1: Automated Setup (Recommended)
```bash
./scripts/full-setup.sh
# Follow instructions in separate terminal
```

### Option 2: Step-by-Step

**Terminal 1:**
```bash
./scripts/services.sh
# Wait for "✅ Services ready!"

./scripts/start.sh
```

**Terminal 2:**
```bash
./scripts/init-infrastructure.sh
# Creates Kafka topics, SQS queue, and sample data
```

## Available Scripts

| Script | Purpose |
|--------|---------|
| `services.sh` | Start Docker containers (Kafka + LocalStack) |
| `init-kafka.sh` | Create Kafka topics |
| `init-sqs.sh` | Create SQS queue |
| `init-dynamodb.sh` | Insert sample account data |
| `init-infrastructure.sh` | Run all init scripts together |
| `start.sh` | Start Spring Boot application |
| `stop.sh` | Stop all containers |
| `full-setup.sh` | Automated setup (calls other scripts) |
| `DEMO.sh` | Run all demo tests with metrics |

## Usage

All scripts should be run from the project root:
```bash
./scripts/[script-name].sh
```

NOT from inside the scripts folder.
