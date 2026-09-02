#!/bin/bash

set -e

SCRIPTS_DIR="$(cd "$(dirname "$0")/scripts" && pwd)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

echo ""
echo "=========================================="
echo "  Bank Transfer API - Complete Test"
echo "=========================================="
echo ""

# Step 1: Infrastructure
info "STEP 1: Starting infrastructure..."
"$SCRIPTS_DIR/start-infra.sh"
success "Infrastructure ready!"

sleep 5

# Step 2: Initialize Kafka topics and SQS
info "STEP 2: Creating Kafka topics..."
"$SCRIPTS_DIR/init-kafka.sh"
success "Kafka topics created!"

info "STEP 3: Creating SQS queue..."
"$SCRIPTS_DIR/init-sqs.sh"
success "SQS queue created!"

# Step 3: Wait for Kafka UI
info "STEP 4: Waiting for Kafka UI to be ready..."
sleep 10
success "Kafka UI ready at: http://localhost:8081"

# Step 4: Start App
info "STEP 5: Starting Spring Boot application..."
"$SCRIPTS_DIR/start-app.sh" &
APP_PID=$!

info "Waiting for app to be ready..."
sleep 15

# Step 5: Initialize DynamoDB
info "STEP 6: Initializing DynamoDB sample data..."
"$SCRIPTS_DIR/init-dynamodb.sh"
success "Sample data loaded!"

# Step 6: Run tests
info "STEP 7: Running all tests..."
./gradlew test
success "All tests passed!"

# Step 7: Run demos
info "STEP 8: Running DEMO.sh..."
"$SCRIPTS_DIR/DEMO.sh"
success "DEMO.sh completed!"

sleep 5

info "STEP 9: Running DEMO2.sh..."
"$SCRIPTS_DIR/DEMO2.sh"
success "DEMO2.sh completed!"

# Step 8: Check SQS
echo ""
info "STEP 10: Checking SQS queue for failed transfers..."
docker-compose exec -T localstack aws sqs get-queue-attributes \
  --queue-url http://sqs.us-east-1.localhost.localstack.cloud:4566/000000000000/transfer-failed \
  --attribute-names ApproximateNumberOfMessages \
  --endpoint-url http://localhost:4566 --region us-east-1

echo ""
echo "=========================================="
echo "✅ COMPLETE TEST FINISHED!"
echo "=========================================="
echo ""
echo "📊 Access Points:"
echo "  🔗 Kafka UI:    http://localhost:8081"
echo "  📈 Dashboard:   http://localhost:8080/metrics-dashboard.html"
echo "  ❤️  Health:      http://localhost:8080/actuator/health"
echo ""
echo "🛑 To STOP everything:"
echo "   ./scripts/stop-infra.sh"
echo ""

