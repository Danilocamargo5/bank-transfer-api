#!/bin/bash

# Open Metrics Dashboard with correct URL for Codespaces or localhost

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
    # Codespaces mode - Spring Boot serves on 8080
    DASHBOARD_URL="https://${CODESPACE_NAME}-8080.app.github.dev/metrics-dashboard.html"
    info "Running in Codespaces"
    success "Dashboard URL: $DASHBOARD_URL"
    echo ""
    echo "Open this link in your browser:"
    echo "$DASHBOARD_URL"
else
    # Local development
    DASHBOARD_URL="http://localhost:8080/metrics-dashboard.html"
    info "Running locally"
    success "Dashboard URL: $DASHBOARD_URL"
    echo ""
    echo "Open this link in your browser:"
    echo "$DASHBOARD_URL"
    
    # Try to open browser automatically on macOS
    if command -v open &> /dev/null; then
        open "$DASHBOARD_URL"
    fi
fi
