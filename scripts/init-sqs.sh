#!/bin/bash

set -e

echo "📤 Creating SQS queues..."

# Create transfer-failed queue (for business failures: saldo insuficiente, conta inativa, etc)
QUEUE_URL=$(docker-compose exec -T localstack aws sqs create-queue \
  --queue-name transfer-failed \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'QueueUrl' \
  --output text)

echo "✅ SQS queue created: $QUEUE_URL"

# Create transfer-failed-dlq queue (for critical failures: save failed, publish failed, poison messages)
DLQ_URL=$(docker-compose exec -T localstack aws sqs create-queue \
  --queue-name transfer-failed-dlq \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'QueueUrl' \
  --output text)

echo "✅ SQS DLQ queue created: $DLQ_URL"
echo ""
echo "Queues:"
docker-compose exec localstack aws sqs list-queues --endpoint-url http://localhost:4566 --region us-east-1
echo ""
