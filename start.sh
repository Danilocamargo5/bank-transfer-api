#!/bin/bash

set -e

echo "🚀 Starting Docker containers..."
docker-compose up -d

# Wait for LocalStack to be ready
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

# Wait for Kafka topics to be created (up to 5 min)
echo "⏳ Waiting for Kafka topics to be created..."

MAX_KAFKA_ATTEMPTS=150
KAFKA_ATTEMPT=0

while [ $KAFKA_ATTEMPT -lt $MAX_KAFKA_ATTEMPTS ]; do
  TOPICS=$(docker compose exec -T kafka kafka-topics --bootstrap-server kafka:29092 --list 2>/dev/null | grep -E "transfer-" | wc -l)
  
  if [ "$TOPICS" -eq 3 ]; then
    echo "✅ All Kafka topics created!"
    break
  fi
  
  KAFKA_ATTEMPT=$((KAFKA_ATTEMPT + 1))
  echo "  Attempt $KAFKA_ATTEMPT/$MAX_KAFKA_ATTEMPTS - Topics not ready ($TOPICS/3), waiting 2s..."
  sleep 2
done

if [ $KAFKA_ATTEMPT -eq $MAX_KAFKA_ATTEMPTS ]; then
  echo "❌ Kafka topics failed to create after $((MAX_KAFKA_ATTEMPTS * 2))s"
  echo "❌ Aborting application startup"
  exit 1
fi

# Wait for sample data to be initialized
echo "⏳ Waiting for sample data to be initialized..."
sleep 5

echo "✅ All services ready!"
echo ""
echo "📦 Starting Bank Transfer API..."
./gradlew bootRun
