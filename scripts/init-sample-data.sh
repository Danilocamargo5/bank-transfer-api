#!/bin/bash

set -e

ENDPOINT="http://localhost:4566"
REGION="us-east-1"
TABLE_NAME="accounts"

echo "=========================================="
echo "Creating sample test accounts..."
echo "=========================================="

# Wait for LocalStack to be ready
echo "Waiting for DynamoDB to be ready..."
sleep 10

# Account 1: João Silva
aws dynamodb put-item \
    --table-name $TABLE_NAME \
    --item '{
        "accountId": {"S": "acc-001"},
        "balance": {"N": "5000.00"},
        "currency": {"S": "BRL"},
        "status": {"S": "ACTIVE"},
        "customerName": {"S": "João Silva"},
        "createdAt": {"S": "2026-08-27T21:00:00Z"},
        "updatedAt": {"S": "2026-08-27T21:00:00Z"}
    }' \
    --endpoint-url $ENDPOINT \
    --region $REGION

echo "✓ Account 'acc-001' (João Silva) created with balance 5000.00 BRL"

# Account 2: Maria Santos
aws dynamodb put-item \
    --table-name $TABLE_NAME \
    --item '{
        "accountId": {"S": "acc-002"},
        "balance": {"N": "1000.00"},
        "currency": {"S": "BRL"},
        "status": {"S": "ACTIVE"},
        "customerName": {"S": "Maria Santos"},
        "createdAt": {"S": "2026-08-27T21:00:00Z"},
        "updatedAt": {"S": "2026-08-27T21:00:00Z"}
    }' \
    --endpoint-url $ENDPOINT \
    --region $REGION

echo "✓ Account 'acc-002' (Maria Santos) created with balance 1000.00 BRL"

# Account 3: Pedro Costa
aws dynamodb put-item \
    --table-name $TABLE_NAME \
    --item '{
        "accountId": {"S": "acc-003"},
        "balance": {"N": "3500.00"},
        "currency": {"S": "BRL"},
        "status": {"S": "ACTIVE"},
        "customerName": {"S": "Pedro Costa"},
        "createdAt": {"S": "2026-08-27T21:00:00Z"},
        "updatedAt": {"S": "2026-08-27T21:00:00Z"}
    }' \
    --endpoint-url $ENDPOINT \
    --region $REGION

echo "✓ Account 'acc-003' (Pedro Costa) created with balance 3500.00 BRL"

# Account 4: Inactive account for testing
aws dynamodb put-item \
    --table-name $TABLE_NAME \
    --item '{
        "accountId": {"S": "acc-000"},
        "balance": {"N": "0.00"},
        "currency": {"S": "BRL"},
        "status": {"S": "INACTIVE"},
        "customerName": {"S": "Conta Encerrada"},
        "createdAt": {"S": "2026-08-27T21:00:00Z"},
        "updatedAt": {"S": "2026-08-27T21:00:00Z"}
    }' \
    --endpoint-url $ENDPOINT \
    --region $REGION

echo "✓ Account 'acc-000' (Inactive) created for testing"

echo ""
echo "=========================================="
echo "✅ Sample data initialization complete!"
echo "=========================================="
echo ""
echo "Available accounts:"
echo "  - acc-001: João Silva (5000.00 BRL)"
echo "  - acc-002: Maria Santos (1000.00 BRL)"
echo "  - acc-003: Pedro Costa (3500.00 BRL)"
echo "  - acc-000: Conta Encerrada (INACTIVE)"
echo ""
