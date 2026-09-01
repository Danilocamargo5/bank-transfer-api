#!/bin/bash

# Open all monitoring UIs with correct URLs for Codespaces or localhost

set -e

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
echo "========================================="
echo "  BANK TRANSFER API - MONITORING URLs"
echo "========================================="
echo ""

# Determine environment
if [[ -n "$CODESPACE_NAME" ]]; then
    info "Running in Codespaces: $CODESPACE_NAME"
    BASE_URL="https://${CODESPACE_NAME}"
    KAFKA_UI_URL="${BASE_URL}-8081.app.github.dev"
    DASHBOARD_URL="${BASE_URL}-8080.app.github.dev/metrics-dashboard.html"
    HEALTH_URL="${BASE_URL}-8080.app.github.dev/actuator/health"
else
    info "Running locally"
    BASE_URL="http://localhost"
    KAFKA_UI_URL="${BASE_URL}:8081"
    DASHBOARD_URL="${BASE_URL}:8080/metrics-dashboard.html"
    HEALTH_URL="${BASE_URL}:8080/actuator/health"
fi

echo ""
echo "🔗 MONITORING ENDPOINTS:"
echo ""

echo "  1️⃣  KAFKA UI (Topics & Messages)"
success "     $KAFKA_UI_URL"
echo ""

echo "  2️⃣  DASHBOARD (Real-time Metrics)"
success "     $DASHBOARD_URL"
echo ""

echo "  3️⃣  HEALTH CHECK"
success "     $HEALTH_URL"
echo ""

echo "📊 USEFUL COMMANDS:"
echo ""
echo "  # Check health"
echo "  curl $HEALTH_URL"
echo ""
echo "  # View metrics"
echo "  curl $HEALTH_URL/../metrics"
echo ""
echo "  # Publish transfer"
echo "  ./scripts/publish-transfer.sh tf-001 acc-123 acc-456 100.00"
echo ""

echo "========================================="
echo ""

# Try to open browser automatically on macOS
if command -v open &> /dev/null; then
    read -p "Open Kafka UI in browser? (y/n) " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        open "$KAFKA_UI_URL"
        sleep 2
        open "$DASHBOARD_URL"
    fi
fi
