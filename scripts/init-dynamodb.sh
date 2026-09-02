#!/bin/bash

set -e

echo "💾 Creating DynamoDB tables..."
echo ""

# Create accounts table
echo "Creating table: accounts"
docker-compose exec localstack aws dynamodb create-table \
  --table-name accounts \
  --attribute-definitions AttributeName=accountId,AttributeType=S \
  --key-schema AttributeName=accountId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:4566 2>/dev/null || echo "  (table may already exist)"

echo ""

# Create transfers table
echo "Creating table: transfers"
docker-compose exec localstack aws dynamodb create-table \
  --table-name transfers \
  --attribute-definitions AttributeName=transferId,AttributeType=S AttributeName=requestedAt,AttributeType=S \
  --key-schema AttributeName=transferId,KeyType=HASH AttributeName=requestedAt,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --endpoint-url http://localhost:4566 2>/dev/null || echo "  (table may already exist)"

echo ""
echo "=========================================="
echo "✅ DynamoDB tables created!"
echo "=========================================="
echo ""

echo "💾 Populating sample accounts in DynamoDB..."
echo ""

# Example 1: João Silva - ACTIVE with good balance
docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-123"},"balance":{"N":"5000.00"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"João Silva"}}' \
  --endpoint-url http://localhost:4566

echo "✅ Created: acc-123 (João Silva - 5000.00 BRL)"

# Example 2: Maria Santos - ACTIVE with moderate balance
docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-456"},"balance":{"N":"1200.50"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"Maria Santos"}}' \
  --endpoint-url http://localhost:4566

echo "✅ Created: acc-456 (Maria Santos - 1200.50 BRL)"

# Example 3: Pedro Costa - ACTIVE with low balance
docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-789"},"balance":{"N":"300.00"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"Pedro Costa"}}' \
  --endpoint-url http://localhost:4566

echo "✅ Created: acc-789 (Pedro Costa - 300.00 BRL)"

# Example 4: Conta Encerrada - INACTIVE (should fail transfers)
docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-000"},"balance":{"N":"0.00"},"currency":{"S":"BRL"},"status":{"S":"INACTIVE"},"customerName":{"S":"Conta Encerrada"}}' \
  --endpoint-url http://localhost:4566

echo "✅ Created: acc-000 (Conta Encerrada - INACTIVE)"

echo ""
echo "=========================================="
echo "✅ All sample accounts created!"
echo "=========================================="
echo ""

echo "📊 Accounts in DynamoDB:"
docker-compose exec localstack aws dynamodb scan --table-name accounts --endpoint-url http://localhost:4566 --query 'Items[*].[accountId.S, customerName.S, balance.N, status.S]' --output table
echo ""
