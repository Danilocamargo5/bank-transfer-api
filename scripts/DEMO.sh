#!/bin/bash

##############################################################################
# Bank Transfer API - Demo Script
# Publica 30 mensagens direto no tópico Kafka transfer-requested
# Cobre: sucesso, saldo insuficiente, conta inativa, transferências normais
#
# Uso: ./DEMO.sh
##############################################################################

set -e

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${BLUE}=========================================="
echo "Bank Transfer API - Demo (30 Test Transfers)"
echo "==========================================${NC}"
echo ""

# Mensagens de sucesso (10)
echo -e "${GREEN}Publishing 10 SUCCESS transfers...${NC}"
for i in {1..10}; do
  AMOUNT=$((i * 50))
  TRANSFER=$(cat <<JSON
{
  "transferId":"tf-demo-success-$(printf "%02d" $i)",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":$AMOUNT.00,
  "currency":"BRL",
  "requestedAt":"2026-09-02T15:$((i*2)):00Z"
}
JSON
)
  echo "$TRANSFER" | docker-compose exec -T kafka kafka-console-producer \
    --broker-list localhost:9092 \
    --topic transfer-requested 2>/dev/null
done
echo -e "${GREEN}✅ 10 success transfers published${NC}"
echo ""

# Mensagens de saldo insuficiente (8)
echo -e "${YELLOW}Publishing 8 INSUFFICIENT_BALANCE transfers...${NC}"
for i in {1..8}; do
  AMOUNT=$((5000 + i * 1000))
  TRANSFER=$(cat <<JSON
{
  "transferId":"tf-demo-insufficient-$(printf "%02d" $i)",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":$AMOUNT.00,
  "currency":"BRL",
  "requestedAt":"2026-09-02T16:$((i*3)):00Z"
}
JSON
)
  echo "$TRANSFER" | docker-compose exec -T kafka kafka-console-producer \
    --broker-list localhost:9092 \
    --topic transfer-requested 2>/dev/null
done
echo -e "${YELLOW}✅ 8 insufficient balance transfers published${NC}"
echo ""

# Mensagens para conta inativa (7)
echo -e "${YELLOW}Publishing 7 INACTIVE_ACCOUNT transfers...${NC}"
for i in {1..7}; do
  AMOUNT=$((i * 100))
  TRANSFER=$(cat <<JSON
{
  "transferId":"tf-demo-inactive-$(printf "%02d" $i)",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-000",
  "amount":$AMOUNT.00,
  "currency":"BRL",
  "requestedAt":"2026-09-02T17:$((i*4)):00Z"
}
JSON
)
  echo "$TRANSFER" | docker-compose exec -T kafka kafka-console-producer \
    --broker-list localhost:9092 \
    --topic transfer-requested 2>/dev/null
done
echo -e "${YELLOW}✅ 7 inactive account transfers published${NC}"
echo ""

# Mensagens com currency inválida (5)
echo -e "${YELLOW}Publishing 5 INVALID_CURRENCY transfers...${NC}"
for i in {1..5}; do
  AMOUNT=$((i * 75))
  TRANSFER=$(cat <<JSON
{
  "transferId":"tf-demo-invalid-currency-$(printf "%02d" $i)",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":$AMOUNT.00,
  "currency":"USD",
  "requestedAt":"2026-09-02T18:$((i*5)):00Z"
}
JSON
)
  echo "$TRANSFER" | docker-compose exec -T kafka kafka-console-producer \
    --broker-list localhost:9092 \
    --topic transfer-requested 2>/dev/null
done
echo -e "${YELLOW}✅ 5 invalid currency transfers published${NC}"
echo ""

echo -e "${BLUE}=========================================="
echo "✅ DEMO: 30 Transfers publicadas no Kafka!"
echo "   - 10 SUCCESS (acc-123 → acc-456)"
echo "   - 8 INSUFFICIENT_BALANCE"
echo "   - 7 INACTIVE_ACCOUNT (acc-000)"
echo "   - 5 INVALID_CURRENCY (USD)"
echo "==========================================${NC}"
echo ""
