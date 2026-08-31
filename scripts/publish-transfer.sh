#!/bin/bash

# Publish transfer requests to Kafka topic "transfer-requested"
# External script (not from API) - complies with RF#1

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Kafka broker
KAFKA_BROKER="${KAFKA_BROKER:-localhost:9092}"
TOPIC="transfer-requested"

# Helper function to print colored output
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

# Check if Kafka is running
check_kafka() {
    info "Checking Kafka connectivity to $KAFKA_BROKER..."
    
    if ! nc -z localhost 9092 2>/dev/null; then
        error "Kafka is not running on $KAFKA_BROKER. Start it with: ./scripts/start-infra.sh"
    fi
    
    success "Kafka is running"
}

# Publish a single transfer
publish_transfer() {
    local transfer_id=$1
    local source_account=$2
    local dest_account=$3
    local amount=$4
    
    local message=$(cat <<EOF
{
  "transferId": "$transfer_id",
  "sourceAccountId": "$source_account",
  "destinationAccountId": "$dest_account",
  "amount": $amount,
  "currency": "BRL",
  "requestedAt": "$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
}
EOF
)
    
    info "Publishing transfer: $transfer_id"
    
    echo "$message" | docker-compose exec -T kafka kafka-console-producer \
        --broker-list localhost:9092 \
        --topic "$TOPIC" \
        2>/dev/null
    
    success "Published: $transfer_id"
}

# Show usage
usage() {
    cat <<EOF
Usage: $0 <transfer-id> <source-account> <dest-account> <amount>

Example:
    $0 tf-ext-001 acc-123 acc-456 250.50

This script publishes transfer requests to Kafka from an external source,
complying with RF#1 (external publisher, app only consumes).

Environment:
    KAFKA_BROKER - Kafka broker address (default: localhost:9092)
EOF
}

# Main
main() {
    if [ $# -lt 4 ]; then
        usage
        exit 1
    fi
    
    check_kafka
    
    local transfer_id=$1
    local source=$2
    local dest=$3
    local amount=$4
    
    publish_transfer "$transfer_id" "$source" "$dest" "$amount"
}

main "$@"
