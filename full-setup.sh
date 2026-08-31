#!/bin/bash

set -e

SCRIPTS_DIR="$(cd "$(dirname "$0")/scripts" && pwd)"

echo "=========================================="
echo "Bank Transfer API - Full Setup"
echo "=========================================="
echo ""

echo "✅ ORDERED STEPS:"
echo "1️⃣ Start services (./scripts/services.sh)"
echo "2️⃣ Create Kafka topics (./scripts/init-kafka.sh)"
echo "3️⃣ Create SQS queue (./scripts/init-sqs.sh)"
echo "4️⃣ Start Spring Boot (./scripts/start.sh) - IN SEPARATE TERMINAL"
echo "5️⃣ Insert sample data (./scripts/init-dynamodb.sh) - AFTER Spring Boot is ready"
echo ""
echo "=========================================="
echo "Starting automation..."
echo "=========================================="
echo ""

# Step 1: Start services
echo "1️⃣ Starting Docker services..."
"$SCRIPTS_DIR/services.sh"

echo ""
echo "2️⃣ Creating Kafka topics..."
"$SCRIPTS_DIR/init-kafka.sh"

echo ""
echo "3️⃣ Creating SQS queue..."
"$SCRIPTS_DIR/init-sqs.sh"

echo ""
echo "=========================================="
echo "✅ Infrastructure Ready!"
echo "=========================================="
echo ""
echo "⚠️  NOW DO THIS IN A SEPARATE TERMINAL:"
echo "   ./scripts/start.sh"
echo ""
echo "⚠️  AFTER Spring Boot is ready, run:"
echo "   ./scripts/init-dynamodb.sh"
echo ""
echo "Then test via Spring Boot:"
echo "   curl -X POST http://localhost:8080/api/v1/transfers ..."
