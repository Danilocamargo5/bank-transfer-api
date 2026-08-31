#!/bin/bash

##############################################################################
# Bank Transfer API - Demo Script
# Apresentação de Transferências Bancárias
#
# Uso: ./DEMO.sh
##############################################################################

set -e

API_URL="http://localhost:8080"
METRICS_URL="$API_URL/actuator/metrics"
HEALTH_URL="$API_URL/actuator/health"
DLQ_URL="http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo "Bank Transfer API - Demo"
echo "==========================================${NC}"
echo ""

# Check health
echo -e "${BLUE}1️⃣ Verificando saúde da aplicação...${NC}"
curl -s "$HEALTH_URL" | jq .
echo ""

# Success transfer
echo -e "${GREEN}2️⃣ Transferência com SUCESSO${NC}"
echo "De: acc-123 (João Silva - 5000.00) → Para: acc-456 (Maria Santos - 1200.50)"
echo "Valor: 100.00 BRL"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-success-001",
    "sourceAccountId":"acc-123",
    "destinationAccountId":"acc-456",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 2

# Inactive account transfer
echo -e "${RED}3️⃣ Transferência com FALHA - Conta Inativa${NC}"
echo "De: acc-000 (Conta Encerrada - INACTIVE) → Para: acc-456"
echo "Valor: 100.00 BRL"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-inactive-001",
    "sourceAccountId":"acc-000",
    "destinationAccountId":"acc-456",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 2

# Insufficient balance
echo -e "${RED}4️⃣ Transferência com FALHA - Saldo Insuficiente${NC}"
echo "De: acc-789 (Pedro Costa - 300.00) → Para: acc-456"
echo "Valor: 1000.00 BRL (saldo insuficiente!)"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-insufficient-001",
    "sourceAccountId":"acc-789",
    "destinationAccountId":"acc-456",
    "amount":1000.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 2

# Validation error - zero amount
echo -e "${RED}5️⃣ Validação - Valor Zero${NC}"
echo "Tentativa de transferência com valor 0.00"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-zero-001",
    "sourceAccountId":"acc-123",
    "destinationAccountId":"acc-456",
    "amount":0.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 1

# Validation error - same account
echo -e "${RED}6️⃣ Validação - Mesma Conta${NC}"
echo "Tentativa de transferência para a mesma conta"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-same-001",
    "sourceAccountId":"acc-123",
    "destinationAccountId":"acc-123",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 1

# Validation error - account not found
echo -e "${RED}7️⃣ Validação - Conta Não Existe${NC}"
echo "Tentativa de transferência com conta inexistente"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-notfound-001",
    "sourceAccountId":"acc-999",
    "destinationAccountId":"acc-456",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 1

# Idempotency test
echo -e "${YELLOW}8️⃣ IDEMPOTÊNCIA - Mesma transferência 2x${NC}"
echo "Primeira tentativa (vai processar):"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-idempotent-001",
    "sourceAccountId":"acc-123",
    "destinationAccountId":"acc-456",
    "amount":50.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 2

echo -e "${YELLOW}Segunda tentativa (MESMO transferId - será rejeitada como duplicada):${NC}"
echo ""
RESPONSE=$(curl -s -X POST "$API_URL/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-demo-idempotent-001",
    "sourceAccountId":"acc-123",
    "destinationAccountId":"acc-456",
    "amount":50.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }')
echo "$RESPONSE" | jq .
echo ""

sleep 2

# Metrics
echo -e "${BLUE}📊 MÉTRICAS${NC}"
echo ""
echo "Transfer Processing Time:"
curl -s "$METRICS_URL/transfer.processing.time" | jq .
echo ""

echo "Transfer Success Total:"
curl -s "$METRICS_URL/transfer.success.total" | jq .
echo ""

echo "Transfer Failure Total:"
curl -s "$METRICS_URL/transfer.failure.total" | jq .
echo ""

# DLQ
echo -e "${BLUE}📬 DEAD LETTER QUEUE (Mensagens com Falha)${NC}"
echo ""
docker-compose exec localstack aws sqs receive-message \
  --queue-url "$DLQ_URL" \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 | jq '.Messages[0].Body | fromjson' 2>/dev/null || echo "Nenhuma mensagem na DLQ"
echo ""

echo -e "${GREEN}=========================================="
echo "✅ Demo completa!"
echo "==========================================${NC}"
