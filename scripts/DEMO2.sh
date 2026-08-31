#!/bin/bash

##############################################################################
# Bank Transfer API - Extended Demo Script
# Gera MUITO MAIS dados para alimentar o dashboard com métricas reais
#
# Uso: ./scripts/DEMO2.sh
##############################################################################

set -e

API_URL="http://localhost:8080"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${BLUE}=========================================="
echo "Bank Transfer API - Extended Demo"
echo "Gerando MUITOS dados para o Dashboard"
echo "==========================================${NC}"
echo ""

# Array de testes de sucesso
echo -e "${GREEN}✅ Rodando 10 transferências com SUCESSO${NC}"
for i in {1..10}; do
    TRANSFER_ID="tf-demo-success-$(printf "%03d" $i)"
    SOURCE="acc-123"
    DEST="acc-456"
    AMOUNT=$((50 + RANDOM % 450))
    
    curl -s -X POST "$API_URL/api/v1/transfers" \
      -H "Content-Type: application/json" \
      -d "{
        \"transferId\":\"$TRANSFER_ID\",
        \"sourceAccountId\":\"$SOURCE\",
        \"destinationAccountId\":\"$DEST\",
        \"amount\":$AMOUNT.00,
        \"currency\":\"BRL\",
        \"requestedAt\":\"2026-08-28T20:00:00Z\"
      }" > /dev/null
    
    echo -n "."
    sleep 0.5
done
echo " ✅ Concluído!"
echo ""

# Array de testes de falha - conta inativa
echo -e "${RED}❌ Rodando 5 transferências com FALHA (conta inativa)${NC}"
for i in {1..5}; do
    TRANSFER_ID="tf-demo-inactive-$(printf "%03d" $i)"
    
    curl -s -X POST "$API_URL/api/v1/transfers" \
      -H "Content-Type: application/json" \
      -d "{
        \"transferId\":\"$TRANSFER_ID\",
        \"sourceAccountId\":\"acc-000\",
        \"destinationAccountId\":\"acc-456\",
        \"amount\":100.00,
        \"currency\":\"BRL\",
        \"requestedAt\":\"2026-08-28T20:00:00Z\"
      }" > /dev/null
    
    echo -n "."
    sleep 0.5
done
echo " ✅ Concluído!"
echo ""

# Falha - saldo insuficiente
echo -e "${RED}❌ Rodando 5 transferências com FALHA (saldo insuficiente)${NC}"
for i in {1..5}; do
    TRANSFER_ID="tf-demo-insufficient-$(printf "%03d" $i)"
    
    curl -s -X POST "$API_URL/api/v1/transfers" \
      -H "Content-Type: application/json" \
      -d "{
        \"transferId\":\"$TRANSFER_ID\",
        \"sourceAccountId\":\"acc-789\",
        \"destinationAccountId\":\"acc-456\",
        \"amount\":999.99,
        \"currency\":\"BRL\",
        \"requestedAt\":\"2026-08-28T20:00:00Z\"
      }" > /dev/null
    
    echo -n "."
    sleep 0.5
done
echo " ✅ Concluído!"
echo ""

# Validação - conta não existe
echo -e "${RED}❌ Rodando 5 transferências com FALHA (conta não existe)${NC}"
for i in {1..5}; do
    TRANSFER_ID="tf-demo-notfound-$(printf "%03d" $i)"
    
    curl -s -X POST "$API_URL/api/v1/transfers" \
      -H "Content-Type: application/json" \
      -d "{
        \"transferId\":\"$TRANSFER_ID\",
        \"sourceAccountId\":\"acc-999-$i\",
        \"destinationAccountId\":\"acc-456\",
        \"amount\":100.00,
        \"currency\":\"BRL\",
        \"requestedAt\":\"2026-08-28T20:00:00Z\"
      }" > /dev/null
    
    echo -n "."
    sleep 0.5
done
echo " ✅ Concluído!"
echo ""

# Validação - valores inválidos
echo -e "${YELLOW}⚠️  Rodando 5 transferências com FALHA (validação)${NC}"
for i in {1..5}; do
    TRANSFER_ID="tf-demo-validation-$(printf "%03d" $i)"
    
    curl -s -X POST "$API_URL/api/v1/transfers" \
      -H "Content-Type: application/json" \
      -d "{
        \"transferId\":\"$TRANSFER_ID\",
        \"sourceAccountId\":\"acc-123\",
        \"destinationAccountId\":\"acc-123\",
        \"amount\":100.00,
        \"currency\":\"BRL\",
        \"requestedAt\":\"2026-08-28T20:00:00Z\"
      }" > /dev/null
    
    echo -n "."
    sleep 0.5
done
echo " ✅ Concluído!"
echo ""

# Mais sucessos pra balancear
echo -e "${GREEN}✅ Rodando mais 10 transferências com SUCESSO${NC}"
for i in {11..20}; do
    TRANSFER_ID="tf-demo-success-$(printf "%03d" $i)"
    SOURCE="acc-456"
    DEST="acc-789"
    AMOUNT=$((30 + RANDOM % 270))
    
    curl -s -X POST "$API_URL/api/v1/transfers" \
      -H "Content-Type: application/json" \
      -d "{
        \"transferId\":\"$TRANSFER_ID\",
        \"sourceAccountId\":\"$SOURCE\",
        \"destinationAccountId\":\"$DEST\",
        \"amount\":$AMOUNT.00,
        \"currency\":\"BRL\",
        \"requestedAt\":\"2026-08-28T20:00:00Z\"
      }" > /dev/null
    
    echo -n "."
    sleep 0.5
done
echo " ✅ Concluído!"
echo ""

# Aguarda processamento
echo -e "${YELLOW}⏳ Aguardando processamento das transferências...${NC}"
sleep 5

# Mostra métricas finais
echo -e "${BLUE}=========================================="
echo "📊 MÉTRICAS FINAIS"
echo "==========================================${NC}"
echo ""

echo "Sucessos:"
curl -s "http://localhost:8080/actuator/metrics/transfer.success.total" | jq '.measurements[0].value'

echo ""
echo "Falhas:"
curl -s "http://localhost:8080/actuator/metrics/transfer.failure.total" | jq '.measurements[0].value'

echo ""
echo "Tempo médio:"
curl -s "http://localhost:8080/actuator/metrics/transfer.processing.time" | jq '.measurements[] | select(.statistic=="MEAN") | .value'

echo ""
echo -e "${GREEN}=========================================="
echo "✅ Demo Extended completa!"
echo "=========================================="
echo ""
echo "🎯 Dashboard deve estar alimentado com:"
echo "   - ~20 sucessos"
echo "   - ~20 falhas"
echo "   - Gráficos com dados reais"
echo "   - Métricas visíveis"
echo ""
echo "Abra: http://localhost:8080/metrics-dashboard.html"
echo "==========================================${NC}"
