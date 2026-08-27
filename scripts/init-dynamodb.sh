#!/bin/bash

# Wait for DynamoDB Local to start
echo "Waiting for DynamoDB Local to be ready..."
sleep 10

# DynamoDB endpoint
ENDPOINT="http://localhost:8000"
REGION="us-east-1"

echo "Creating DynamoDB tables..."

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

# Create transfers table
aws dynamodb create-table \
    --table-name transfers \
    --attribute-definitions \
        AttributeName=transferId,AttributeType=S \
    --key-schema \
        AttributeName=transferId,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --endpoint-url $ENDPOINT \
    --region $REGION 2>/dev/null || echo "Table 'transfers' already exists"

echo "DynamoDB tables created successfully!"
