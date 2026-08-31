#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "📋 Initializing infrastructure..."
echo ""

echo "1️⃣ Creating Kafka topics..."
"$SCRIPT_DIR/init-kafka.sh"

echo ""
echo "2️⃣ Creating SQS queue..."
"$SCRIPT_DIR/init-sqs.sh"

echo ""
echo "3️⃣ Inserting sample data..."
"$SCRIPT_DIR/init-dynamodb.sh"

echo ""
echo "=========================================="
echo "✅ Infrastructure initialized!"
echo "=========================================="
echo ""
echo "Ready to start: ./scripts/start.sh"
