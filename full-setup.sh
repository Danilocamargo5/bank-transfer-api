#!/bin/bash

set -e

SCRIPTS_DIR="$(cd "$(dirname "$0")/scripts" && pwd)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

error() {
    echo -e "${RED}❌ $1${NC}"
    exit 1
}

echo ""
echo "=========================================="
echo "  Bank Transfer API - Complete Setup"
echo "=========================================="
echo ""

# Step 1: Infrastructure
info "STEP 1: Starting Docker infrastructure..."
"$SCRIPTS_DIR/start-infra.sh"
success "Infrastructure started!"

sleep 5

# Step 2: Initialize Kafka and SQS
info "STEP 2: Creating Kafka topics..."
"$SCRIPTS_DIR/init-kafka.sh"
success "Kafka topics created!"

info "STEP 3: Creating SQS queue..."
"$SCRIPTS_DIR/init-sqs.sh"
success "SQS queue created!"

sleep 5

# Step 3: Validate Kafka UI
info "STEP 4: Validating Kafka UI connection..."
for i in {1..10}; do
    if curl -s http://localhost:8081/api/clusters 2>/dev/null | grep -q "local"; then
        success "Kafka UI is responding!"
        break
    fi
    if [ $i -eq 10 ]; then
        error "Kafka UI did not respond after 10 attempts"
    fi
    sleep 2
done

# Step 4: Initialize DynamoDB BEFORE publishing messages
info "STEP 5: Creating and populating DynamoDB tables..."
"$SCRIPTS_DIR/init-dynamodb.sh"
success "DynamoDB tables created and populated!"

sleep 2

# Step 5: Publish test messages
info "STEP 6: Publishing 30 test messages to Kafka..."
"$SCRIPTS_DIR/DEMO.sh"
success "Test messages published!"

echo ""
echo "=========================================="
echo "✅ SETUP COMPLETE!"
echo "=========================================="
echo ""
echo "📊 NEXT STEPS - EXECUTE IN THIS ORDER:"
echo ""
echo "🔴 TERMINAL 1 (Main App):"
echo "   ./scripts/start-app.sh"
echo ""
echo "🟡 TERMINAL 2 (Coverage Server - JaCoCo Report):"
echo "   ./scripts/start-coverage-server.sh"
echo "   Acesso: http://localhost:8888"
echo ""
echo "🟢 TERMINAL 3 (Dashboard Server - Métricas):"
echo "   ./scripts/start-dashboard-server.sh"
echo "   Acesso: http://localhost:9999"
echo ""
echo "📍 ACCESS POINTS:"
echo "  🔗 Kafka UI:           http://localhost:8081"
echo "  📊 Metrics Dashboard:  http://localhost:9999"
echo "  📋 Coverage Report:    http://localhost:8888"
echo "  🔗 Spring Boot API:    http://localhost:8080"
echo "  📈 Health Check:       http://localhost:8080/actuator/health"
echo ""
echo "🧪 TEST THE APP:"
echo "   curl http://localhost:8080/actuator/health"
echo "   ./scripts/DEMO.sh  (30 test messages)"
echo "   ./scripts/DEMO2.sh (additional tests)"
echo ""
echo "=================================="
echo "🔄 RESET & START OVER:"
echo "=================================="
echo ""
echo "Para ZERAR os dados e começar denovo:"
echo ""
echo "1️⃣  STOP everything:"
echo "   Ctrl+C em todos os terminais"
echo "   ./scripts/stop-infra.sh"
echo ""
echo "2️⃣  REMOVE old volumes (zera dados):"
echo "   docker volume rm localstack-volume 2>/dev/null || true"
echo "   docker volume rm postgres-data 2>/dev/null || true"
echo ""
echo "3️⃣  CLEAN Gradle cache (opcional):"
echo "   ./gradlew clean"
echo ""
echo "4️⃣  RUN full setup again:"
echo "   ./full-setup.sh"
echo ""
echo "=================================="
echo "📝 COMMON COMMANDS:"
echo "=================================="
echo ""
echo "• View logs em tempo real:"
echo "  docker logs -f localstack"
echo "  docker logs -f bank-transfer-api"
echo ""
echo "• Reset e comecar denovo (tudo junto):"
echo "  ./scripts/stop-infra.sh && docker volume rm localstack-volume 2>/dev/null && ./full-setup.sh"
echo ""
echo "• Rodar testes com coverage:"
echo "  ./gradlew test jacocoTestReport"
echo ""
echo "🛑 To STOP everything:"
echo "   ./scripts/stop-infra.sh"
echo ""
