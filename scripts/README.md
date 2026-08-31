# Bank Transfer API - Scripts

All automation scripts organized by purpose.

## Quick Start

### Option 1: Automated Setup (Recommended)
```bash
./scripts/full-setup.sh
# Follow instructions in separate terminal
```

### Option 2: Step-by-Step

**Terminal 1 - Start Infrastructure:**
```bash
./scripts/start-infra.sh
# Wait for "✅ Services ready!"
```

**Terminal 2 - Initialize Infrastructure:**
```bash
./scripts/init-infrastructure.sh
# Creates Kafka topics, SQS queue, and sample data
```

**Terminal 1 - Start Application:**
```bash
./scripts/start-app.sh
```

## Available Scripts

### Infrastructure Scripts
| Script | Purpose |
|--------|---------|
| `start-infra.sh` | Start Docker containers (Kafka + LocalStack) |
| `stop-infra.sh` | Stop all Docker containers |

### Initialization Scripts
| Script | Purpose |
|--------|---------|
| `init-kafka.sh` | Create Kafka topics |
| `init-sqs.sh` | Create SQS queue |
| `init-dynamodb.sh` | Insert sample account data |
| `init-infrastructure.sh` | Run all init scripts together |

### Application Scripts
| Script | Purpose |
|--------|---------|
| `start-app.sh` | Start Spring Boot application |

### Demo & Testing
| Script | Purpose |
|--------|---------|
| `DEMO.sh` | Run all demo tests with metrics |

### Automation
| Script | Purpose |
|--------|---------|
| `full-setup.sh` | Automated setup (calls other scripts) |

## Usage

All scripts should be run from the project root:
```bash
./scripts/[script-name].sh
```

NOT from inside the scripts folder.

## Workflow

**First Time Setup:**
```bash
./scripts/full-setup.sh
# Then in separate terminal:
./scripts/start-app.sh
# Then in another terminal:
./scripts/init-dynamodb.sh
```

**Restart (Keep Data):**
```bash
# Just restart app (infra stays running):
./scripts/start-app.sh

# Stop everything:
./scripts/stop-infra.sh
```

**Clean Restart (Lose Data):**
```bash
./scripts/stop-infra.sh
./scripts/start-infra.sh
./scripts/init-infrastructure.sh
./scripts/start-app.sh
```
