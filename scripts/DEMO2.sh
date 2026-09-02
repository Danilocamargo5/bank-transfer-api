#!/bin/bash

##############################################################################
# Bank Transfer API - Demo Script 2 (Additional Transfers)
# Publica 20 mensagens adicionais direto no tópico Kafka transfer-requested
# Uso: ./DEMO2.sh (execute manualmente se quiser mais mensagens)
##############################################################################

set -e

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================="
echo "Bank Transfer API - Demo 2 (20 Additional Transfers)"
echo "==========================================${NC}"
echo ""
echo -e "${GREEN}Publishing 20 additional transfers...${NC}"
echo ""

for i in {1..20}; do
  AMOUNT=$((RANDOM % 2000 + 50))
  SOURCE_ACC=$(["acc-123", "acc-456", "acc-789"] | sed -n "$((i % 3 + 1))p")
  DEST_ACC=$(["acc-456", "acc-789", "acc-123"] | sed -n "$((i % 3 + 2))p")
  
  TRANSFER=$(cat <<JSON
{
  "transferId":"tf-demo-additional-$(printf "%02d" $i)",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":$AMOUNT.00,
  "currency":"BRL",
  "requestedAt":"2026-09-02T19:$(printf "%02d" $((i / 20))):$(printf "%02d" $((i % 60)))Z"
}
JSON
)
  
  echo "$TRANSFER" | docker-compose exec -T kafka kafka-console-producer \
    --broker-list localhost:9092 \
    --topic transfer-requested 2>/dev/null
  
  if [ $((i % 5)) -eq 0 ]; then
    echo -e "${GREEN}✅ $i/20 additional transfers published${NC}"
  fi
done

echo ""
echo -e "${BLUE}=========================================="
echo "✅ DEMO2: 20 Additional Transfers publicadas no Kafka!"
echo "==========================================${NC}"
echo ""
