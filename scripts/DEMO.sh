#!/bin/bash

##############################################################################
# Bank Transfer API - Demo Script (Kafka Publisher)
# Publica mensagens direto no tópico Kafka transfer-requested
#
# Uso: ./DEMO.sh
##############################################################################

set -e

KAFKA_BROKER="kafka:29092"

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================="
echo "Bank Transfer API - Demo (Publishing to Kafka)"
echo "==========================================${NC}"
echo ""

# Transfer 1
echo -e "${GREEN}1️⃣ Transfer SUCESSO: acc-123 → acc-456${NC}"
echo ""
TRANSFER_1=$(cat <<'JSON'
{
  "transferId":"tf-demo-success-001",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":100.00,
  "currency":"BRL",
  "requestedAt":"2026-08-28T20:00:00Z"
}
JSON
)

echo "$TRANSFER_1" | docker-compose exec -T kafka kafka-console-producer \
  --broker-list localhost:9092 \
  --topic transfer-requested

echo -e "${GREEN}✅ Transfer 1 publicada${NC}"
echo ""

# Transfer 2
echo -e "${GREEN}2️⃣ Transfer SUCESSO: acc-456 → acc-789${NC}"
echo ""
TRANSFER_2=$(cat <<'JSON'
{
  "transferId":"tf-demo-success-002",
  "sourceAccountId":"acc-456",
  "destinationAccountId":"acc-789",
  "amount":50.00,
  "currency":"BRL",
  "requestedAt":"2026-08-28T20:05:00Z"
}
JSON
)

echo "$TRANSFER_2" | docker-compose exec -T kafka kafka-console-producer \
  --broker-list localhost:9092 \
  --topic transfer-requested

echo -e "${GREEN}✅ Transfer 2 publicada${NC}"
echo ""

# Transfer 3
echo -e "${GREEN}3️⃣ Transfer FAIL: saldo insuficiente${NC}"
echo ""
TRANSFER_3=$(cat <<'JSON'
{
  "transferId":"tf-demo-fail-001",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":10000.00,
  "currency":"BRL",
  "requestedAt":"2026-08-28T20:10:00Z"
}
JSON
)

echo "$TRANSFER_3" | docker-compose exec -T kafka kafka-console-producer \
  --broker-list localhost:9092 \
  --topic transfer-requested

echo -e "${GREEN}✅ Transfer 3 publicada${NC}"
echo ""

# Transfer 4
echo -e "${GREEN}4️⃣ Transfer FAIL: conta inativa${NC}"
echo ""
TRANSFER_4=$(cat <<'JSON'
{
  "transferId":"tf-demo-fail-002",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-000",
  "amount":500.00,
  "currency":"BRL",
  "requestedAt":"2026-08-28T20:15:00Z"
}
JSON
)

echo "$TRANSFER_4" | docker-compose exec -T kafka kafka-console-producer \
  --broker-list localhost:9092 \
  --topic transfer-requested

echo -e "${GREEN}✅ Transfer 4 publicada${NC}"
echo ""

# Transfer 5-8
for i in {5..8}; do
  TRANSFER=$(cat <<JSON
{
  "transferId":"tf-demo-batch-00$i",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":$((i * 50)).00,
  "currency":"BRL",
  "requestedAt":"2026-08-28T20:$((i*5)):00Z"
}
JSON
)
  
  echo "$TRANSFER" | docker-compose exec -T kafka kafka-console-producer \
    --broker-list localhost:9092 \
    --topic transfer-requested
  
  echo -e "${GREEN}✅ Transfer $i publicada${NC}"
done

echo ""
echo -e "${BLUE}=========================================="
echo "✅ DEMO: 8 Transfers publicadas no Kafka!"
echo "==========================================${NC}"
echo ""
