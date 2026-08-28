#!/bin/bash

set -e

echo "📋 Initializing infrastructure..."
echo ""

echo "1️⃣ Creating Kafka topics..."
./init-kafka.sh

echo ""
echo "2️⃣ Creating SQS queue..."
./init-sqs.sh

echo ""
echo "3️⃣ Inserting sample data..."
./init-dynamodb.sh

echo ""
echo "=========================================="
echo "✅ Infrastructure initialized!"
echo "=========================================="
echo ""
echo "Ready to start: ./start.sh"
