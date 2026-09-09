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

# Step 6: Setup complete
info "STEP 6: All infrastructure initialized!"

echo ""
echo "=========================================="
echo "✅ SETUP COMPLETE!"
echo "=========================================="
echo ""

# Detecta Codespace URL se estiver rodando em Codespaces
if [ -n "$CODESPACE_NAME" ]; then
    CODESPACE_URL="https://${CODESPACE_NAME}-"
    echo "📊 EXECUTING IN GITHUB CODESPACES"
    echo ""
    echo "🔴 TERMINAL 1 (Main App):"
    echo "   ./scripts/start-app.sh"
    echo ""
    echo "🟡 TERMINAL 2 (Coverage Server - JaCoCo Report):"
    echo "   cd build/reports/jacoco/test/html && python3 -m http.server 3000"
    echo "   (Execute after: ./gradlew test jacocoTestReport in app terminal)"
    echo ""
    echo "📍 URLs TO OPEN IN BROWSER:"
    echo "  🔗 Kafka UI:           ${CODESPACE_URL}8081.app.github.dev"
    echo "  📊 Metrics Dashboard:  ${CODESPACE_URL}8080.app.github.dev/metrics-dashboard.html"
    echo "  📋 Coverage Report:    ${CODESPACE_URL}3000.app.github.dev/index.html"
    echo "  🔗 Spring Boot API:    ${CODESPACE_URL}8080.app.github.dev"
    echo "  📈 Health Check:       ${CODESPACE_URL}8080.app.github.dev/actuator/health"
else
    echo "📊 EXECUTING LOCALLY"
    echo ""
    echo "🔴 TERMINAL 1 (Main App):"
    echo "   ./scripts/start-app.sh"
    echo ""
    echo "🟡 TERMINAL 2 (Coverage Server - JaCoCo Report):"
    echo "   cd build/reports/jacoco/test/html && python3 -m http.server 3000"
    echo "   (Execute after: ./gradlew test jacocoTestReport in app terminal)"
    echo ""
    echo "📍 URLS:"
    echo "  🔗 Kafka UI:           http://localhost:8081"
    echo "  📊 Metrics Dashboard:  http://localhost:8080/metrics-dashboard.html"
    echo "  📋 Coverage Report:    http://localhost:3000/index.html"
    echo "  🔗 Spring Boot API:    http://localhost:8080"
    echo "  📈 Health Check:       http://localhost:8080/actuator/health"
fi

echo ""
echo "=================================="
echo "🚀 NEXT STEPS:"
echo "=================================="
echo ""
echo "TERMINAL 1 (Run the App):"
echo "  ./scripts/start-app.sh"
echo ""
echo "TERMINAL 2 (Run Tests - optional):"
echo "  ./scripts/DEMO.sh    (30 test messages)"
echo "  ./scripts/DEMO2.sh   (additional tests)"
echo ""
echo "=================================="
echo "🔄 RESET & START OVER:"
echo "=================================="
echo ""
echo "Para ZERAR os dados e começar denovo:"
echo ""
echo "Option 1 - Quick (tudo junto):"
echo "  ./scripts/stop-infra.sh && docker volume rm localstack-volume 2>/dev/null && ./full-setup.sh"
echo ""
echo "Option 2 - Step by step:"
echo "  1. Ctrl+C em todos os terminais"
echo "  2. ./scripts/stop-infra.sh"
echo "  3. docker volume rm localstack-volume 2>/dev/null || true"
echo "  4. ./full-setup.sh"
echo ""
echo "🛑 To STOP everything:"
echo "   ./scripts/stop-infra.sh"
echo ""
