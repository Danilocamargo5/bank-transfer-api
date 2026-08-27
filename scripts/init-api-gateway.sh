#!/bin/bash

set -e

ENDPOINT="http://localhost:4566"
REGION="us-east-1"
API_NAME="bank-transfer-api"
STAGE="dev"

echo "=========================================="
echo "Creating API Gateway REST API..."
echo "=========================================="

# Wait for API Gateway to be ready
echo "Waiting for API Gateway to be ready..."
sleep 5

# Create REST API
echo "Creating REST API: $API_NAME"
API_ID=$(aws apigateway create-rest-api \
    --name "$API_NAME" \
    --description "Bank Transfer API Gateway" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ API created with ID: $API_ID"

# Get root resource ID
ROOT_ID=$(aws apigateway get-resources \
    --rest-api-id $API_ID \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'items[0].id' \
    --output text)

echo "✓ Root resource ID: $ROOT_ID"

# Create /api resource
API_RESOURCE=$(aws apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $ROOT_ID \
    --path-part "api" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ Created /api resource: $API_RESOURCE"

# Create /api/v1 resource
V1_RESOURCE=$(aws apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $API_RESOURCE \
    --path-part "v1" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ Created /api/v1 resource: $V1_RESOURCE"

# Create /api/v1/transfers resource
TRANSFERS_RESOURCE=$(aws apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $V1_RESOURCE \
    --path-part "transfers" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ Created /api/v1/transfers resource: $TRANSFERS_RESOURCE"

# Create /api/v1/transfers/{transferId} resource
TRANSFER_ID_RESOURCE=$(aws apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $TRANSFERS_RESOURCE \
    --path-part "{transferId}" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ Created /api/v1/transfers/{transferId} resource: $TRANSFER_ID_RESOURCE"

# Create /api/v1/accounts resource
ACCOUNTS_RESOURCE=$(aws apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $V1_RESOURCE \
    --path-part "accounts" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ Created /api/v1/accounts resource: $ACCOUNTS_RESOURCE"

# Create /api/v1/accounts/{accountId} resource
ACCOUNT_ID_RESOURCE=$(aws apigateway create-resource \
    --rest-api-id $API_ID \
    --parent-id $ACCOUNTS_RESOURCE \
    --path-part "{accountId}" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ Created /api/v1/accounts/{accountId} resource: $ACCOUNT_ID_RESOURCE"

# Create POST method on /api/v1/transfers
echo "Creating POST method on /api/v1/transfers"
aws apigateway put-method \
    --rest-api-id $API_ID \
    --resource-id $TRANSFERS_RESOURCE \
    --http-method POST \
    --authorization-type NONE \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ POST method created"

# Create integration for POST /api/v1/transfers
aws apigateway put-integration \
    --rest-api-id $API_ID \
    --resource-id $TRANSFERS_RESOURCE \
    --http-method POST \
    --type HTTP \
    --integration-http-method POST \
    --uri "http://localhost:8080/api/v1/transfers" \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ POST integration created"

# Create method response
aws apigateway put-method-response \
    --rest-api-id $API_ID \
    --resource-id $TRANSFERS_RESOURCE \
    --http-method POST \
    --status-code 200 \
    --response-models '{"application/json":"Empty"}' \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ Method response created"

# Create integration response with template mapping
aws apigateway put-integration-response \
    --rest-api-id $API_ID \
    --resource-id $TRANSFERS_RESOURCE \
    --http-method POST \
    --status-code 200 \
    --response-templates '{"application/json": "$input.json($)"}' \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ Integration response created"

# Create GET method on /api/v1/transfers/{transferId}
echo "Creating GET method on /api/v1/transfers/{transferId}"
aws apigateway put-method \
    --rest-api-id $API_ID \
    --resource-id $TRANSFER_ID_RESOURCE \
    --http-method GET \
    --authorization-type NONE \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ GET method created"

# Create integration for GET /api/v1/transfers/{transferId}
aws apigateway put-integration \
    --rest-api-id $API_ID \
    --resource-id $TRANSFER_ID_RESOURCE \
    --http-method GET \
    --type HTTP \
    --integration-http-method GET \
    --uri "http://localhost:8080/api/v1/transfers/{transferId}" \
    --request-parameters "integration.request.path.transferId=method.request.path.transferId" \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ GET integration created"

# Create method response for GET
aws apigateway put-method-response \
    --rest-api-id $API_ID \
    --resource-id $TRANSFER_ID_RESOURCE \
    --http-method GET \
    --status-code 200 \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

# Create integration response for GET with template
aws apigateway put-integration-response \
    --rest-api-id $API_ID \
    --resource-id $TRANSFER_ID_RESOURCE \
    --http-method GET \
    --status-code 200 \
    --response-templates '{"application/json": "$input.json($)"}' \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ GET integration response created"

# Create GET method on /api/v1/accounts/{accountId}
echo "Creating GET method on /api/v1/accounts/{accountId}"
aws apigateway put-method \
    --rest-api-id $API_ID \
    --resource-id $ACCOUNT_ID_RESOURCE \
    --http-method GET \
    --authorization-type NONE \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ GET method created"

# Create integration for GET /api/v1/accounts/{accountId}
aws apigateway put-integration \
    --rest-api-id $API_ID \
    --resource-id $ACCOUNT_ID_RESOURCE \
    --http-method GET \
    --type HTTP \
    --integration-http-method GET \
    --uri "http://localhost:8080/api/v1/accounts/{accountId}" \
    --request-parameters "integration.request.path.accountId=method.request.path.accountId" \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ GET integration created"

# Create method response for GET
aws apigateway put-method-response \
    --rest-api-id $API_ID \
    --resource-id $ACCOUNT_ID_RESOURCE \
    --http-method GET \
    --status-code 200 \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

# Create integration response for GET with template
aws apigateway put-integration-response \
    --rest-api-id $API_ID \
    --resource-id $ACCOUNT_ID_RESOURCE \
    --http-method GET \
    --status-code 200 \
    --response-templates '{"application/json": "$input.json($)"}' \
    --endpoint-url $ENDPOINT \
    --region $REGION > /dev/null

echo "✓ GET integration response created"

# Deploy API
echo "Deploying API to stage: $STAGE"
DEPLOYMENT_ID=$(aws apigateway create-deployment \
    --rest-api-id $API_ID \
    --stage-name $STAGE \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text)

echo "✓ Deployment created: $DEPLOYMENT_ID"

echo ""
echo "=========================================="
echo "✅ API Gateway setup complete!"
echo "=========================================="
echo ""
echo "API Details:"
echo "  API ID: $API_ID"
echo "  Stage: $STAGE"
echo "  Endpoint: http://localhost:4566/restapis/$API_ID/$STAGE"
echo ""
echo "Available endpoints:"
echo "  POST   http://localhost:4566/restapis/$API_ID/$STAGE/api/v1/transfers"
echo "  GET    http://localhost:4566/restapis/$API_ID/$STAGE/api/v1/transfers/{transferId}"
echo "  GET    http://localhost:4566/restapis/$API_ID/$STAGE/api/v1/accounts/{accountId}"
echo ""
