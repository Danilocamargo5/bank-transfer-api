# Guia de Operação - Bank Transfer API

Documento com todos os comandos para monitorar, debugar e operar o sistema em tempo real.

---

## 1. STATUS GERAL

### Health Check da Aplicação
```bash
curl http://localhost:8080/actuator/health
```

**Resposta esperada:**
```json
{
  "status": "UP"
}
```

### Logs em Tempo Real (Terminal onde rodou start-app.sh)
```bash
# Terminal já está mostrando logs
# Procure por:
# - INFO: operações normais
# - WARN: comportamentos inesperados
# - ERROR: problemas que precisam revisão
```

---

## 2. KAFKA - Monitorar Mensagens

### Ver TODOS os tópicos
```bash
docker-compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --list
```

**Tópicos esperados:**
```
transfer-requested      ← Mensagens de entrada
transfer-completed      ← Sucessos
__consumer_offsets      ← Interno
```

---

### Ver MENSAGENS PROCESSADAS (transfer-completed)

**Contar quantas:**
```bash
docker-compose exec kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic transfer-completed \
  --time -1
```

**Resposta:**
```
transfer-completed:0:5   ← Partição 0: 5 mensagens
transfer-completed:1:4   ← Partição 1: 4 mensagens
transfer-completed:2:3   ← Partição 2: 3 mensagens
Total: 12 mensagens
```

---

### VER CONTEÚDO das mensagens (últimas 10)
```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transfer-completed \
  --from-beginning \
  --max-messages 10
```

**Você vê:**
```json
{
  "transferId": "tf-demo-success-01",
  "sourceAccountId": "acc-123",
  "destinationAccountId": "acc-456",
  "amount": 100.00,
  "currency": "BRL",
  "completedAt": "2026-09-03T02:30:00Z"
}
```

---

### Ver FILA DE ENTRADA (transfer-requested) - Mensagens NÃO processadas

```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transfer-requested \
  --from-beginning \
  --max-messages 5
```

---

## 3. SQS - Fila de Falhas

### Contar quantas FALHAS tem
```bash
docker-compose exec localstack aws sqs get-queue-attributes \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --attribute-names ApproximateNumberOfMessages \
  --endpoint-url http://localhost:4566
```

**Resposta:**
```json
{
  "Attributes": {
    "ApproximateNumberOfMessages": "18"
  }
}
```

Significa: 18 transferências falharam

---

### VER DETALHES das falhas (últimas 10)
```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --endpoint-url http://localhost:4566 \
  --max-number-of-messages 10
```

**Você vê cada uma com:**
```json
{
  "transferId": "tf-demo-inactive-07",
  "sourceAccountId": "acc-123",
  "destinationAccountId": "acc-000",
  "failureReason": "Destination account acc-000 is not active",
  "failedAt": "2026-09-03T02:30:15Z"
}
```

---

### DLQ (Fila de Falhas CRÍTICAS) - Anomalias que precisam investigação
```bash
docker-compose exec localstack aws sqs get-queue-attributes \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed-dlq \
  --attribute-names ApproximateNumberOfMessages \
  --endpoint-url http://localhost:4566
```

**Se houver mensagens aqui = PROBLEMA CRÍTICO!**

Ver detalhes:
```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed-dlq \
  --endpoint-url http://localhost:4566 \
  --max-number-of-messages 10
```

---

## 4. DYNAMODB - Banco de Dados

### Ver TODAS as contas
```bash
docker-compose exec localstack aws dynamodb scan \
  --table-name accounts \
  --endpoint-url http://localhost:4566 \
  --output table
```

**Você vê:**
```
accountId    | balance   | status   | customerName
acc-123      | 3900.00   | ACTIVE   | João Silva
acc-456      | 2100.00   | ACTIVE   | Maria Santos
acc-789      | 300.00    | ACTIVE   | Pedro Costa
acc-000      | 0.00      | INACTIVE | Conta Encerrada
```

---

### Ver UMA conta específica
```bash
docker-compose exec localstack aws dynamodb get-item \
  --table-name accounts \
  --key '{"accountId": {"S": "acc-123"}}' \
  --endpoint-url http://localhost:4566
```

---

### Ver TODAS as transferências
```bash
docker-compose exec localstack aws dynamodb scan \
  --table-name transfers \
  --endpoint-url http://localhost:4566 \
  --output table
```

**Você vê:**
```
transferId         | status      | sourceAccount | destAccount | amount
tf-demo-success-01 | COMPLETED   | acc-123       | acc-456     | 100.00
tf-demo-inactive-07| FAILED      | acc-123       | acc-000     | 700.00
tf-demo-invalid-03 | FAILED      | acc-123       | acc-456     | 225.00
```

---

### Ver SÓ as FALHADAS
```bash
docker-compose exec localstack aws dynamodb scan \
  --table-name transfers \
  --endpoint-url http://localhost:4566 \
  --filter-expression "attribute_exists(failureReason)" \
  --query 'Items[*].[transferId.S, status.S, failureReason.S]' \
  --output table
```

---

### Ver SÓ as COMPLETADAS
```bash
docker-compose exec localstack aws dynamodb scan \
  --table-name transfers \
  --endpoint-url http://localhost:4566 \
  --filter-expression "contains(#s, :status)" \
  --expression-attribute-names '{"#s": "status"}' \
  --expression-attribute-values '{":status": {"S": "COMPLETED"}}' \
  --output table
```

---

### Ver UMA transferência específica
```bash
docker-compose exec localstack aws dynamodb get-item \
  --table-name transfers \
  --key '{"transferId": {"S": "tf-demo-success-01"}}' \
  --endpoint-url http://localhost:4566
```

---

## 5. DOCKER - Status dos Serviços

### Ver containers rodando
```bash
docker-compose ps
```

**Esperado:**
```
CONTAINER            STATUS
bank-transfer-api    Up (port 8080)
kafka                Up (port 9092)
localstack           Up (port 4566)
kafka-ui             Up (port 8081)
```

---

### Ver logs de um container específico
```bash
# Kafka
docker-compose logs -f kafka

# LocalStack
docker-compose logs -f localstack

# App
docker-compose logs -f  # (ou terminal onde rodou start-app.sh)
```

---

### Reiniciar um serviço
```bash
docker-compose restart kafka
# ou
docker-compose restart localstack
```

---

## 6. API - Testar Endpoints

### Listar todas as transferências
```bash
curl http://localhost:8080/api/v1/transfers | jq
```

---

### Ver uma transferência específica
```bash
curl http://localhost:8080/api/v1/transfers/tf-demo-success-01 | jq
```

---

### Listar todas as contas
```bash
curl http://localhost:8080/api/v1/accounts | jq
```

---

### Ver saldo de uma conta
```bash
curl http://localhost:8080/api/v1/accounts/acc-123 | jq '.balance'
```

---

### Enviar uma nova transferência
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId": "tf-manual-test-001",
    "sourceAccountId": "acc-123",
    "destinationAccountId": "acc-456",
    "amount": 50.00,
    "currency": "BRL"
  }' | jq
```

**Resposta esperada:**
```json
{
  "status": "ACCEPTED",
  "message": "Transfer request received and will be processed asynchronously"
}
```

Depois checa status:
```bash
curl http://localhost:8080/api/v1/transfers/tf-manual-test-001 | jq
```

---

## 7. DASHBOARD - Visualizar em Tempo Real

### Kafka UI
```
http://localhost:8081
```

Lá você vê:
- Tópicos
- Mensagens
- Consumers
- Partitions

---

### Metrics da Aplicação
```bash
curl http://localhost:8080/actuator/metrics | jq
```

Métricas disponíveis:
```bash
curl http://localhost:8080/actuator/metrics/transfer.processing.time | jq
curl http://localhost:8080/actuator/metrics/transfer.success.count | jq
curl http://localhost:8080/actuator/metrics/transfer.failure.count | jq
```

---

## 8. TROUBLESHOOTING - Procurar Problemas

### Transferência não processou após 1 minuto?

**1. Verificar logs da app:**
```
Procure por: ERROR em logs do terminal
Procure por: transferId específico
Procure por: "Failed to save", "Rollback", "Invalid currency"
```

**2. Verificar se mensagem chegou no Kafka:**
```bash
docker-compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic transfer-requested \
  --from-beginning | grep "tf-seu-id"
```

**3. Verificar se foi pra fila de falha:**
```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --endpoint-url http://localhost:4566 \
  --max-number-of-messages 10 | grep "tf-seu-id"
```

**4. Verificar se foi pra DLQ crítica:**
```bash
docker-compose exec localstack aws sqs receive-message \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed-dlq \
  --endpoint-url http://localhost:4566 \
  --max-number-of-messages 10
```

**5. Verificar balanço das contas:**
```bash
docker-compose exec localstack aws dynamodb get-item \
  --table-name accounts \
  --key '{"accountId": {"S": "acc-123"}}' \
  --endpoint-url http://localhost:4566 | jq '.Item.balance'
```

---

### Saldo da conta está estranho?

**Histórico de transferências dessa conta:**
```bash
docker-compose exec localstack aws dynamodb query \
  --table-name transfers \
  --index-name SourceAccountIdIndex \
  --key-condition-expression "sourceAccountId = :id" \
  --expression-attribute-values '{":id": {"S": "acc-123"}}' \
  --endpoint-url http://localhost:4566 \
  --output table
```

---

### App crashou? Como recuperar?

```bash
# 1. Restart do app
./scripts/start-app.sh

# App vai:
# - Conectar no Kafka
# - Pegar mensagens não processadas (offset anterior)
# - Reprocessar tudo
# - Idempotência garante que não duplica
```

---

## 9. LIMPEZA - Resetar Tudo

### Limpar TODAS as mensagens e começar do zero
```bash
./full-setup.sh
```

### Ou apenas resetar específico:

**Limpar SQS:**
```bash
docker-compose exec localstack aws sqs purge-queue \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --endpoint-url http://localhost:4566
```

**Limpar DynamoDB (criar tabelas vazias):**
```bash
docker-compose down
docker system prune -af --volumes
sudo rm -rf /tmp/localstack
./full-setup.sh
```

---

## 10. ATALHOS ÚTEIS

### Script de diagnóstico rápido
```bash
cat > check-status.sh << 'SCRIPT'
#!/bin/bash

echo "=== HEALTH CHECK ==="
curl -s http://localhost:8080/actuator/health | jq .

echo -e "\n=== SUCESSOS KAFKA ==="
docker-compose exec kafka kafka-run-class kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 --topic transfer-completed --time -1

echo -e "\n=== FALHAS SQS ==="
docker-compose exec localstack aws sqs get-queue-attributes \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --attribute-names ApproximateNumberOfMessages \
  --endpoint-url http://localhost:4566 | jq .Attributes.ApproximateNumberOfMessages

echo -e "\n=== BALANÇO CONTAS ==="
docker-compose exec localstack aws dynamodb scan \
  --table-name accounts \
  --endpoint-url http://localhost:4566 \
  --query 'Items[*].[accountId.S, balance.N]' \
  --output table

SCRIPT

chmod +x check-status.sh
./check-status.sh
```

---

## Resumo Rápido

| O que? | Comando |
|--------|---------|
| App rodando? | `curl localhost:8080/actuator/health` |
| Sucessos? | `kafka-run-class GetOffsetShell --topic transfer-completed` |
| Falhas? | `aws sqs get-queue-attributes --queue-url transfer-failed` |
| Anomalias? | `aws sqs get-queue-attributes --queue-url transfer-failed-dlq` |
| Saldo conta? | `aws dynamodb get-item --table accounts --key acc-123` |
| Ver Kafka UI? | `http://localhost:8081` |
| Ver logs app? | Terminal onde rodou `start-app.sh` |

