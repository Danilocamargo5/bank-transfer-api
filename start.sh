#!/bin/bash

set -e

echo "🚀 Starting Docker containers..."
docker-compose up -d

# FIRST: Wait for kafka-init container to complete successfully
echo "⏳ Waiting for Kafka topics initialization..."

MAX_WAIT=600  # 10 minutes max
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
  # Check if kafka-init container has exited
  STATUS=$(docker compose ps kafka-init --format "{{.State}}" 2>/dev/null || echo "unknown")
  
  if [ "$STATUS" = "exited" ]; then
    # Check exit code
    EXIT_CODE=$(docker compose ps kafka-init --format "{{.ExitCode}}" 2>/dev/null || echo "1")
    if [ "$EXIT_CODE" = "0" ]; then
      echo "✅ Kafka topics initialized successfully!"
      break
    else
      echo "❌ Kafka initialization failed with exit code $EXIT_CODE"
      docker compose logs kafka-init | tail -20
      exit 1
    fi
  fi
  
  ELAPSED=$((ELAPSED + 5))
  echo "  Waiting... (${ELAPSED}s elapsed)"
  sleep 5
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
  echo "❌ Kafka initialization timed out after ${MAX_WAIT}s"
  exit 1
fi

# SECOND: Wait for LocalStack to be ready
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

# THIRD: Wait for sample data initialization
echo "⏳ Waiting for sample data initialization..."
sleep 5

echo "✅ All infrastructure ready!"
echo ""
echo "📦 Starting Bank Transfer API..."
./gradlew bootRun
