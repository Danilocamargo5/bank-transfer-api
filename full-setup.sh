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
echo "📊 Next Steps:"
echo ""
echo "1️⃣  Open ANOTHER TERMINAL and start the app:"
echo "   ./scripts/start-app.sh"
echo ""
echo "2️⃣  Get your Codespace URL:"
echo "   • In VS Code: Click the 'Ports' tab at bottom"
echo "   • Find port 8080 (Spring Boot)"
echo "   • Right-click → 'Open in Browser' OR copy the URL"
echo ""
echo "3️⃣  Test if app is running (replace CODESPACE_URL with your URL):"
echo "   curl https://CODESPACE_URL-8080.app.github.dev/actuator/health"
echo ""
echo "📍 Access Points (use your Codespace URL):"
echo "  🔗 Kafka UI:           https://CODESPACE_URL-8081.app.github.dev"
echo "  📊 Metrics Dashboard:  https://CODESPACE_URL-8080.app.github.dev/metrics-dashboard.html"
echo "  🔗 Spring Boot API:    https://CODESPACE_URL-8080.app.github.dev"
echo "  📈 Health Check:       https://CODESPACE_URL-8080.app.github.dev/actuator/health"
echo ""
echo "💡 Example (if your Codespace URL is psychic-pancake-wvgjw6v7w74f9vjw):"
echo "   curl https://psychic-pancake-wvgjw6v7w74f9vjw-8080.app.github.dev/actuator/health"
echo "   https://psychic-pancake-wvgjw6v7w74f9vjw-8080.app.github.dev/metrics-dashboard.html"
echo ""
echo "📝 To run additional tests later:"
echo "   ./scripts/DEMO2.sh"
echo ""
echo "🛑 To STOP everything:"
echo "   ./scripts/stop-infra.sh"
echo ""
