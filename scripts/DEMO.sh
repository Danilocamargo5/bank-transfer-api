#!/bin/bash

##############################################################################
# Bank Transfer API - Demo Script
# Publica 30 mensagens do arquivo test-transfers-demo.json para Kafka
#
# Uso: ./DEMO.sh
##############################################################################

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
JSON_FILE="$SCRIPT_DIR/test-transfers-demo.json"

# Cores para output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

if [ ! -f "$JSON_FILE" ]; then
    echo "❌ File not found: $JSON_FILE"
    exit 1
fi

echo -e "${BLUE}=========================================="
echo "Bank Transfer API - Demo (30 Test Transfers)"
echo "==========================================${NC}"
echo ""
echo "Publishing from: $JSON_FILE"
echo ""

# Count total lines
TOTAL=$(wc -l < "$JSON_FILE")
COUNT=0

# Read and publish each line
while IFS= read -r line; do
    COUNT=$((COUNT + 1))
    
    # Publish to Kafka
    echo "$line" | docker-compose exec -T kafka kafka-console-producer \
        --broker-list localhost:9092 \
        --topic transfer-requested 2>/dev/null
    
    # Progress every 10
    if [ $((COUNT % 10)) -eq 0 ]; then
        echo -e "${GREEN}✅ $COUNT/$TOTAL transfers published${NC}"
    fi
done < "$JSON_FILE"

echo ""
echo -e "${BLUE}=========================================="
echo "✅ DEMO: $TOTAL Transfers publicadas no Kafka!"
echo "==========================================${NC}"
echo ""
