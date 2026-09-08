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

info "Generating JaCoCo coverage report..."
cd "$(dirname "$0")/.."
./gradlew jacocoTestReport -q

success "Coverage report generated!"

# Start HTTP server on port 8888 to serve coverage report
info "Starting coverage HTTP server on port 8888..."
cd build/reports/jacoco/test/html

python3 -m http.server 8888 2>/dev/null &
SERVER_PID=$!

success "Coverage server started (PID: $SERVER_PID)"
echo ""
echo "📊 Coverage Report:"
echo "  🔗 http://localhost:8888"
echo ""
echo "Press Ctrl+C to stop the server"
wait $SERVER_PID
