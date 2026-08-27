#!/bin/bash

set -e

echo "🚀 Starting Docker containers..."
docker-compose up -d

# Wait for LocalStack to be ready by checking health endpoint
echo "⏳ Waiting for LocalStack to be fully ready..."

MAX_ATTEMPTS=60
ATTEMPT=0

while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
  if curl -s http://localhost:4566/_localstack/health > /dev/null 2>&1; then
    echo "✅ LocalStack is ready!"
    break
  fi
  
  ATTEMPT=$((ATTEMPT + 1))
  echo "  Attempt $ATTEMPT/$MAX_ATTEMPTS - LocalStack not ready yet, retrying in 1s..."
  sleep 1
done

if [ $ATTEMPT -eq $MAX_ATTEMPTS ]; then
  echo "❌ LocalStack failed to start after ${MAX_ATTEMPTS}s"
  exit 1
fi

# Wait a bit more to ensure all services (DynamoDB, SQS, API Gateway) are initialized
echo "⏳ Waiting for all services to initialize..."
sleep 5

echo "✅ All services ready!"
echo ""
echo "📦 Starting Bank Transfer API..."
./gradlew bootRun --args='--spring.profiles.active=dev'
