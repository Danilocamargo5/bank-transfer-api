#!/bin/bash

# LocalStack Initialization Script
# Creates DynamoDB tables and SQS queues for local development

set -e

echo "Initializing LocalStack services..."

# Wait for LocalStack to be ready
sleep 5

# DynamoDB: Create Accounts table
echo "Creating DynamoDB table: accounts"
awslocal dynamodb create-table \
  --table-name accounts \
  --attribute-definitions AttributeName=accountId,AttributeType=S \
  --key-schema AttributeName=accountId,KeyType=HASH \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1 || echo "Table 'accounts' already exists"

# DynamoDB: Create Transfers table
echo "Creating DynamoDB table: transfers"
awslocal dynamodb create-table \
  --table-name transfers \
  --attribute-definitions \
    AttributeName=transferId,AttributeType=S \
    AttributeName=requestedAt,AttributeType=S \
  --key-schema \
    AttributeName=transferId,KeyType=HASH \
    AttributeName=requestedAt,KeyType=RANGE \
  --billing-mode PAY_PER_REQUEST \
  --region us-east-1 || echo "Table 'transfers' already exists"

# SQS: Create DLQ for failed transfers
echo "Creating SQS queue: transfer-failed-dlq"
awslocal sqs create-queue \
  --queue-name transfer-failed-dlq \
  --region us-east-1 || echo "Queue 'transfer-failed-dlq' already exists"

# SQS: Create main queue for transfer failures
echo "Creating SQS queue: transfer-failed"
awslocal sqs create-queue \
  --queue-name transfer-failed \
  --attributes MaximumMessageRetentionPeriod=1209600,RedrivePolicy='{"deadLetterTargetArn":"arn:aws:sqs:us-east-1:000000000000:transfer-failed-dlq","maxReceiveCount":"3"}' \
  --region us-east-1 || echo "Queue 'transfer-failed' already exists"

echo "LocalStack initialization completed successfully!"
echo ""
echo "Resources created:"
echo "  - DynamoDB: accounts table"
echo "  - DynamoDB: transfers table"
echo "  - SQS: transfer-failed queue (with DLQ)"
echo "  - SQS: transfer-failed-dlq (dead-letter queue)"
