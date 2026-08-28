#!/bin/bash

set -e

echo "💾 Creating sample accounts in DynamoDB..."

docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-001"},"balance":{"N":"5000"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"João Silva"}}' \
  --endpoint-url http://localhost:4566

docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-002"},"balance":{"N":"1000"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"Maria Santos"}}' \
  --endpoint-url http://localhost:4566

echo "✅ Sample accounts created!"
echo ""
echo "Accounts:"
docker-compose exec localstack aws dynamodb scan --table-name accounts --endpoint-url http://localhost:4566 --query 'Items[*].[accountId.S, balance.N, customerName.S]' --output table
echo ""
echo "Next step: ./init-sqs.sh"
