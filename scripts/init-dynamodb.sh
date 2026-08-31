#!/bin/bash

set -e

echo "💾 Creating sample accounts in DynamoDB..."
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
echo "🧪 Test commands:"
echo ""
echo "Success transfer (acc-123 → acc-456):"
echo '  curl -X POST http://localhost:8080/api/v1/transfers -H "Content-Type: application/json" -d '"'"'{
    "transferId":"tf-test-001",
    "sourceAccountId":"acc-123",
    "destinationAccountId":"acc-456",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'"'"''
echo ""
echo "Failed transfer (inactive account):"
echo '  curl -X POST http://localhost:8080/api/v1/transfers -H "Content-Type: application/json" -d '"'"'{
    "transferId":"tf-fail-001",
    "sourceAccountId":"acc-000",
    "destinationAccountId":"acc-456",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'"'"''
echo ""
echo "Insufficient balance:"
echo '  curl -X POST http://localhost:8080/api/v1/transfers -H "Content-Type: application/json" -d '"'"'{
    "transferId":"tf-fail-002",
    "sourceAccountId":"acc-789",
    "destinationAccountId":"acc-456",
    "amount":1000.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T20:00:00Z"
  }'"'"''
echo ""
echo "View metrics:"
echo "  curl http://localhost:8080/actuator/metrics"
echo ""
