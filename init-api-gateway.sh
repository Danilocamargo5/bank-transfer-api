#!/bin/bash

set -e

echo "🌐 Setting up API Gateway..."
echo "Waiting 30s for LocalStack stability..."
sleep 30

echo "Creating REST API..."
API_ID=$(docker-compose exec -T localstack aws apigateway create-rest-api \
  --name "bank-transfer-api" \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text)

echo "✅ API created: $API_ID"

# Get root resource
ROOT_ID=$(docker-compose exec -T localstack aws apigateway get-resources \
  --rest-api-id $API_ID \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'items[0].id' \
  --output text)

# Create /api resource
API_RESOURCE=$(docker-compose exec -T localstack aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $ROOT_ID \
  --path-part api \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text)

# Create /api/v1 resource
V1_RESOURCE=$(docker-compose exec -T localstack aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $API_RESOURCE \
  --path-part v1 \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text)

# Create /api/v1/transfers resource
TRANSFERS_RESOURCE=$(docker-compose exec -T localstack aws apigateway create-resource \
  --rest-api-id $API_ID \
  --parent-id $V1_RESOURCE \
  --path-part transfers \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'id' \
  --output text)

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
  --output text)

echo "✅ API Gateway fully configured!"
echo ""
echo "=========================================="
echo "✅ API Gateway Setup Complete"
echo "=========================================="
echo ""
echo "API Endpoint: http://localhost:4566/restapis/$API_ID/dev/api/v1/transfers"
echo ""
echo "Next step: ./start.sh"
