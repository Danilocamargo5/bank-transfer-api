#!/bin/bash

set -e

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
info "DynamoDB tables should already be created and populated by full-setup.sh"
echo ""

# Start Spring Boot
./gradlew bootRun


