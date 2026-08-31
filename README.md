# Bank Transfer API

Microserviço de processamento de transferências bancárias internas com Kafka, DynamoDB e Spring Boot.

## 🎯 Visão Geral

Sistema core bancário para processar transferências entre contas do mesmo banco. Implementa arquitetura baseada em eventos com processamento assíncrono, garantindo atomicidade, idempotência e tratamento robusto de erros.

## ✨ Features

- ✅ **Transferências Atômicas** - Debit + credit em transação única (tudo ou nada)
- ✅ **Idempotência** - Mesma transferência não é processada 2x
- ✅ **Error Handling** - Retry com backoff exponencial, DLQ para falhas
- ✅ **Validação Completa** - Saldo, status de conta, formato de dados
- ✅ **Processamento Assíncrono** - Kafka para escalabilidade
- ✅ **Observabilidade** - Métricas em tempo real via Micrometer
- ✅ **Testes Automatizados** - 30+ casos cobrindo sucesso/falha/edge cases
- ✅ **Dashboard Interativo** - Visualização de métricas em tempo real

## 🏗️ Arquitetura

```
POST /api/v1/transfers (Spring Boot 8080)
  ↓
Kafka Topic: transfer-requested
  ↓
TransferKafkaConsumer
  ├─ Validate (account exists, balance, status)
  ├─ Execute atomically (@Transactional)
  ├─ Update DynamoDB
  └─ Publish result
  
✅ Success → Kafka: transfer-completed
❌ Failure → SQS: transfer-failed (DLQ)
```

## 💻 Tech Stack

- **Language:** Kotlin 2.0.0
- **Framework:** Spring Boot 3.3.5
- **Runtime:** Java 21
- **Database:** DynamoDB (LocalStack)
- **Messaging:** Apache Kafka (KRaft), AWS SQS (LocalStack)
- **Build:** Gradle 8.8
- **Testing:** JUnit 5 (30+ test cases)
- **Metrics:** Micrometer

## 📊 Dados de Exemplo

| accountId | balance | currency | status | customerName |
|-----------|---------|----------|--------|--------------|
| acc-123 | 5000.00 | BRL | ACTIVE | João Silva |
| acc-456 | 1200.50 | BRL | ACTIVE | Maria Santos |
| acc-789 | 300.00 | BRL | ACTIVE | Pedro Costa |
| acc-000 | 0.00 | BRL | INACTIVE | Conta Encerrada |

## 🚀 Quick Start

### Automated Setup
```bash
./full-setup.sh
./scripts/start-app.sh
./scripts/DEMO2.sh
python3 -m http.server 8888
# Open: http://localhost:8888/metrics-dashboard.html
```

For detailed setup instructions, see [Setup Guide](./SETUP.md) or [Scripts Documentation](./scripts/README.md)

## 📈 Metrics & Monitoring

Real-time dashboard available at:
```
http://localhost:8080/metrics-dashboard.html
```

Metrics:
- Transfer processing time
- Success/failure counts
- Error type breakdown
- Application health status

## 🧪 Testing

Run automated tests:
```bash
./scripts/DEMO.sh          # Basic demo (8 scenarios)
./scripts/DEMO2.sh         # Extended demo (40+ test data points)
```

Run unit tests:
```bash
./gradlew test
```

## 📚 Documentation

- [Setup Guide](./SETUP.md) - Local development setup
- [Scripts Documentation](./scripts/README.md) - How to run scripts
- [Code Review](./docs/CODE_REVIEW_LEGACY.md) - Analysis of common pitfalls
- [Error Handling](./docs/ERROR_HANDLING.md) - Error strategy

## ✅ Project Status

**Completed:**
- PR #1-6: Core features + error handling + DLQ
- PR #7: Metrics & observability (Micrometer)
- PR #8: Automated tests (30+ cases)
- Infrastructure: Docker-compose with Kafka + LocalStack
- Dashboard: Real-time metrics visualization

**Test Coverage:**
- TransferValidator: 10 test cases
- TransferService: 6 test cases
- TransferController: 5 test cases
- TransferKafkaConsumer: 3 test cases
- AccountController: 3 test cases
- Integration Tests: 3 scenarios
- **Total: 30+ automated tests**

## 🎯 Key Design Decisions

1. **Async Processing** - Kafka for scalability and decoupling
2. **Atomicity** - @Transactional ensures debit+credit succeed together
3. **Idempotency** - transferId as idempotency key prevents duplicates
4. **Error Isolation** - DLQ separates business errors from system errors
5. **Observability** - Metrics at every step for debugging

## 📝 License

Internal use only - Bank Transfer Processing System
