#!/bin/bash

##############################################################################
# Bank Transfer API - Demo Script 2 (Bulk Publisher)
# Publica 40 mensagens direto no tópico Kafka transfer-requested
#
# Uso: ./DEMO2.sh
##############################################################################

set -e

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}=========================================="
echo "Bank Transfer API - Demo 2 (Bulk Kafka Publishing)"
echo "==========================================${NC}"
echo ""
echo -e "${GREEN}Publishing 40 test transfers...${NC}"
echo ""

# Publish 40 transfers
for i in {1..40}; do
  TRANSFER=$(cat <<JSON
{
  "transferId":"tf-demo-bulk-$(printf "%03d" $i)",
  "sourceAccountId":"acc-123",
  "destinationAccountId":"acc-456",
  "amount":$((RANDOM % 1000 + 10)).00,
  "currency":"BRL",
  "requestedAt":"2026-08-28T21:$((i / 60)):$(printf "%02d" $((i % 60)))Z"
}
JSON
)
  
  echo "$TRANSFER" | docker-compose exec -T kafka kafka-console-producer \
    --broker-list localhost:9092 \
    --topic transfer-requested 2>/dev/null
  
  # Print progress
  if [ $((i % 10)) -eq 0 ]; then
    echo -e "${GREEN}✅ $i/40 transfers publicadas${NC}"
  fi
done

echo ""
echo -e "${BLUE}=========================================="
echo "✅ DEMO2: 40 Bulk Transfers publicadas no Kafka!"
echo "==========================================${NC}"
echo ""
