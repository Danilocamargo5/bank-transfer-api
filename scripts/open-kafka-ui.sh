#!/bin/bash

# Open Kafka UI with correct URL for Codespaces or localhost

set -e

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m'

info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

success() {
    echo -e "${GREEN}✅ $1${NC}"
}

# Check if running in Codespaces
if [[ -n "$CODESPACE_NAME" ]]; then
    # Codespaces mode
    KAFKA_UI_URL="https://${CODESPACE_NAME}-8081.app.github.dev"
    info "Running in Codespaces"
    success "Kafka UI URL: $KAFKA_UI_URL"
    echo ""
    echo "Open this link in your browser:"
    echo "$KAFKA_UI_URL"
else
    # Local development
    KAFKA_UI_URL="http://localhost:8081"
    info "Running locally"
    success "Kafka UI URL: $KAFKA_UI_URL"
    echo ""
    echo "Open this link in your browser:"
    echo "$KAFKA_UI_URL"
    
    # Try to open browser automatically on macOS
    if command -v open &> /dev/null; then
        open "$KAFKA_UI_URL"
    fi
fi
