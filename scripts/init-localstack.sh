#!/bin/bash

set -e

# Wait for LocalStack to be ready
echo "Waiting for LocalStack to be ready..."
sleep 10

ENDPOINT="http://localhost:4566"
REGION="us-east-1"

echo "=========================================="
echo "Creating DynamoDB tables..."
echo "=========================================="

# Create accounts table
aws dynamodb create-table \
    --table-name accounts \
    --attribute-definitions \
        AttributeName=accountId,AttributeType=S \
    --key-schema \
        AttributeName=accountId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url $ENDPOINT \
    --region $REGION 2>/dev/null || echo "Table 'accounts' already exists"

echo "✓ Table 'accounts' created"

# Create transfers table with composite key (transferId + requestedAt)
aws dynamodb create-table \
    --table-name transfers \
    --attribute-definitions \
        AttributeName=transferId,AttributeType=S \
        AttributeName=requestedAt,AttributeType=S \
    --key-schema \
        AttributeName=transferId,KeyType=HASH \
        AttributeName=requestedAt,KeyType=RANGE \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url $ENDPOINT \
    --region $REGION 2>/dev/null || echo "Table 'transfers' already exists"

echo "✓ Table 'transfers' created"

echo ""
echo "=========================================="
echo "Creating SQS queues..."
echo "=========================================="

# Create transfer-failed queue
aws sqs create-queue \
    --queue-name transfer-failed \
    --attributes MessageRetentionPeriod=86400 \
    --endpoint-url $ENDPOINT \
    --region $REGION 2>/dev/null || echo "Queue 'transfer-failed' already exists"

echo "✓ Queue 'transfer-failed' created"

# Create transfer-failed-dlq (Dead Letter Queue)
aws sqs create-queue \
    --queue-name transfer-failed-dlq \
    --attributes MessageRetentionPeriod=1209600 \
    --endpoint-url $ENDPOINT \
    --region $REGION 2>/dev/null || echo "Queue 'transfer-failed-dlq' already exists"

echo "✓ Queue 'transfer-failed-dlq' created"

echo ""
echo "=========================================="
echo "Creating API Gateway..."
echo "=========================================="

# Create REST API
API_ID=$(aws apigateway create-rest-api \
    --name bank-transfer-api \
    --description "Bank Transfer Microservice API" \
    --endpoint-url $ENDPOINT \
    --region $REGION \
    --query 'id' \
    --output text 2>/dev/null || echo "")

if [ -z "$API_ID" ] || [ "$API_ID" == "None" ]; then
    echo "ℹ API Gateway already exists or error creating"
else
    echo "✓ API Gateway created: $API_ID"
    
    # Get root resource
    ROOT_ID=$(aws apigateway get-resources \
        --rest-api-id $API_ID \
        --endpoint-url $ENDPOINT \
        --region $REGION \
        --query 'items[0].id' \
        --output text)
    
    echo "✓ Root resource: $ROOT_ID"
fi

echo ""
echo "=========================================="
echo "✅ LocalStack initialization complete!"
echo "=========================================="
echo ""
echo "Services available at:"
echo "  - DynamoDB: $ENDPOINT"
echo "  - SQS: $ENDPOINT"
echo "  - API Gateway: $ENDPOINT"
echo ""
