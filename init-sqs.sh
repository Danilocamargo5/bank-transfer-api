#!/bin/bash

set -e

echo "📤 Creating SQS queues..."

# Create transfer-failed queue (DLQ)
QUEUE_URL=$(docker-compose exec -T localstack aws sqs create-queue \
  --queue-name transfer-failed \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'QueueUrl' \
  --output text)

echo "✅ SQS queue created: $QUEUE_URL"
echo ""
echo "Queues:"
docker-compose exec localstack aws sqs list-queues --endpoint-url http://localhost:4566 --region us-east-1
echo ""
echo "Next step: ./init-api-gateway.sh"
