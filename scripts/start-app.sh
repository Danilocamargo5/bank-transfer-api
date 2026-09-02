#!/bin/bash

set -e

SCRIPTS_DIR="$(cd "$(dirname "$0")" && pwd)"

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

info "Starting Bank Transfer API..."

# Start Spring Boot in background
./gradlew bootRun &
APP_PID=$!

info "Waiting for app to be ready..."

# Wait for app to respond
for i in {1..30}; do
    if curl -s http://localhost:8080/actuator/health 2>/dev/null | grep -q "UP"; then
        success "Application is ready!"
        break
    fi
    if [ $i -eq 30 ]; then
        echo "❌ App did not start after 30 attempts"
        kill $APP_PID
        exit 1
    fi
    sleep 1
done

# Populate DynamoDB
info "Populating DynamoDB with sample accounts..."
"$SCRIPTS_DIR/init-dynamodb.sh"
success "DynamoDB populated!"

echo ""
echo "=========================================="
echo "✅ Application Ready & Database Populated!"
echo "=========================================="
echo ""
echo "📊 Access Points:"
echo "  📈 Dashboard:   http://localhost:8080/metrics-dashboard.html"
echo "  ❤️  Health:      http://localhost:8080/actuator/health"
echo ""
echo "🔄 Consumer is now processing Kafka messages..."
echo ""

# Keep app running in foreground
wait $APP_PID

