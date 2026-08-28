#!/bin/bash

set -e

echo "🚀 Starting infrastructure containers..."
docker-compose up -d kafka localstack

echo "⏳ Waiting for Kafka to be ready..."
sleep 90

echo "Creating Kafka topics..."
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-requested --partitions 3 --replication-factor 1
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-completed --partitions 3 --replication-factor 1
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-failed --partitions 1 --replication-factor 1
echo "✅ Kafka topics created!"

echo ""
echo "Creating sample accounts..."
docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-001"},"balance":{"N":"5000"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"João Silva"}}' \
  --endpoint-url http://localhost:4566 2>/dev/null || true

docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-002"},"balance":{"N":"1000"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"Maria Santos"}}' \
  --endpoint-url http://localhost:4566 2>/dev/null || true

echo "✅ Sample accounts created!"

echo ""
echo "Setting up API Gateway..."
API_ID=$(docker-compose exec -T localstack aws apigateway create-rest-api \
  --name "bank-transfer-api" \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text 2>/dev/null)

echo "✅ API created: $API_ID"

# Get root resource
ROOT_ID=$(docker-compose exec -T localstack aws apigateway get-resources \
  --rest-api-id $API_ID \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'items[0].id' \
  --output text 2>/dev/null)

# Create /api resource
API_RESOURCE=$(docker-compose exec -T localstack aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $ROOT_ID \
  --path-part api \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text 2>/dev/null)

# Create /api/v1 resource
V1_RESOURCE=$(docker-compose exec -T localstack aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $API_RESOURCE \
  --path-part v1 \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text 2>/dev/null)

# Create /api/v1/transfers resource
TRANSFERS_RESOURCE=$(docker-compose exec -T localstack aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $V1_RESOURCE \
  --path-part transfers \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text 2>/dev/null)

# Create POST method
docker-compose exec -T localstack aws apigateway put-method \
  --rest-api-id $API_ID \
  --resource-id $TRANSFERS_RESOURCE \
  --http-method POST \
  --authorization-type NONE \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 > /dev/null 2>&1

# Create method response
docker-compose exec -T localstack aws apigateway put-method-response \
  --rest-api-id $API_ID \
  --resource-id $TRANSFERS_RESOURCE \
  --http-method POST \
  --status-code 200 \
  --response-parameters '{"method.response.header.Content-Type": true}' \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 > /dev/null 2>&1

# Create integration
docker-compose exec -T localstack aws apigateway put-integration \
  --rest-api-id $API_ID \
  --resource-id $TRANSFERS_RESOURCE \
  --http-method POST \
  --type HTTP \
  --integration-http-method POST \
  --uri http://localhost:8080/api/v1/transfers \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 > /dev/null 2>&1

# Create integration response
docker-compose exec -T localstack aws apigateway put-integration-response \
  --rest-api-id $API_ID \
  --resource-id $TRANSFERS_RESOURCE \
  --http-method POST \
  --status-code 200 \
  --response-templates '{"application/json": "$input.json($)"}' \
  --response-parameters '{"method.response.header.Content-Type": "integration.response.header.Content-Type"}' \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 > /dev/null 2>&1

# Create deployment
DEPLOYMENT_ID=$(docker-compose exec -T localstack aws apigateway create-deployment \
  --rest-api-id $API_ID \
  --stage-name dev \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text 2>/dev/null)

echo "✅ API Gateway configured!"
echo ""
echo "=========================================="
echo "✅ Infrastructure initialization complete"
echo "=========================================="
echo ""
echo "API Endpoint: http://localhost:4566/restapis/$API_ID/dev/api/v1/transfers"
echo "Direct Spring Boot: http://localhost:8080/api/v1/transfers"
echo ""
echo "Next: run ./start.sh"
