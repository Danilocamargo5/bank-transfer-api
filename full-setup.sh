#!/bin/bash

set -e

echo "=========================================="
echo "Bank Transfer API - Full Setup"
echo "=========================================="
echo ""

echo "✅ ORDEREDSTEPS:"
echo "1️⃣ Start services (./services.sh)"
echo "2️⃣ Create Kafka topics (./init-kafka.sh)"
echo "3️⃣ Create SQS queue (./init-sqs.sh)"
echo "4️⃣ Setup API Gateway (./init-api-gateway.sh)"
echo "5️⃣ Start Spring Boot (./start.sh) - IN SEPARATE TERMINAL"
echo "6️⃣ Insert sample data (./init-dynamodb.sh) - AFTER Spring Boot is ready"
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
echo "4️⃣ Setting up API Gateway..."
./init-api-gateway.sh

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
echo "Then you can test via API Gateway!"
