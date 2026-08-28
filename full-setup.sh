#!/bin/bash

set -e

echo "=========================================="
echo "Bank Transfer API - Full Setup"
echo "=========================================="
echo ""

echo "✅ ORDERED STEPS:"
echo "1️⃣ Start services (./services.sh)"
echo "2️⃣ Create Kafka topics (./init-kafka.sh)"
echo "3️⃣ Create SQS queue (./init-sqs.sh)"
echo "4️⃣ Start Spring Boot (./start.sh) - IN SEPARATE TERMINAL"
echo "5️⃣ Insert sample data (./init-dynamodb.sh) - AFTER Spring Boot is ready"
echo ""
echo "=========================================="
echo "Starting automation..."
echo "=========================================="
echo ""

# Step 1: Start services
echo "1️⃣ Starting Docker services..."
./services.sh

echo ""
echo "2️⃣ Creating Kafka topics..."
./init-kafka.sh

echo ""
echo "3️⃣ Creating SQS queue..."
./init-sqs.sh

echo ""
echo "=========================================="
echo "✅ Infrastructure Ready!"
echo "=========================================="
echo ""
echo "⚠️  NOW DO THIS IN A SEPARATE TERMINAL:"
echo "   ./start.sh"
echo ""
echo "⚠️  AFTER Spring Boot is ready, run:"
echo "   ./init-dynamodb.sh"
echo ""
echo "Then test via Spring Boot:"
echo "   curl -X POST http://localhost:8080/api/v1/transfers ..."
