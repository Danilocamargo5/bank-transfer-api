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
echo "  Bank Transfer API - Setup & Test"
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

# Step 4: Publish test messages
info "STEP 5: Publishing test messages from DEMO.sh..."
"$SCRIPTS_DIR/DEMO.sh"
success "DEMO.sh messages published!"

sleep 2

info "STEP 6: Publishing test messages from DEMO2.sh..."
"$SCRIPTS_DIR/DEMO2.sh"
success "DEMO2.sh messages published!"

echo ""
echo "=========================================="
echo "✅ SETUP COMPLETE!"
echo "=========================================="
echo ""
echo "📊 Next Steps:"
echo ""
echo "1️⃣  Check Kafka UI to verify messages:"
echo "   http://localhost:8081"
echo "   → Click 'Topics' → 'transfer-requested'"
echo ""
echo "2️⃣  Open ANOTHER TERMINAL and start the app:"
echo "   ./scripts/start-app.sh"
echo ""
echo "3️⃣  After app starts, initialize DynamoDB:"
echo "   ./scripts/init-dynamodb.sh"
echo ""
echo "4️⃣  Run tests:"
echo "   ./gradlew test"
echo ""
echo "📍 Access Points:"
echo "  🔗 Kafka UI:    http://localhost:8081"
echo "  📈 Dashboard:   http://localhost:8080/metrics-dashboard.html"
echo ""
echo "🛑 To STOP everything:"
echo "   ./scripts/stop-infra.sh"
echo ""


