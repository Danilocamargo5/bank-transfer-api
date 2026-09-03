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
echo "2️⃣  Open ANOTHER TERMINAL and start HTTP server (for metrics/dashboard):"
echo "   python3 -m http.server 8000 --directory build/resources/main/static"
echo "   OR if no static files:"
echo "   python3 -m http.server 3000"
echo ""
echo "📍 Access Points:"
echo "  🔗 Kafka UI:    http://localhost:8081"
echo "  🌐 HTTP Server: http://localhost:3000 (or 8000 if using static)"
echo "  🔗 Spring Boot API: http://localhost:8080"
echo "  📈 Health Check: http://localhost:8080/actuator/health"
echo ""
echo "To run additional tests later:"
echo "   ./scripts/DEMO2.sh"
echo ""
echo "To STOP everything:"
echo "   ./scripts/stop-infra.sh"
echo ""
