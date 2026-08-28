#!/bin/bash

set -e

echo "🚀 Starting Docker containers..."
docker-compose up -d

# Wait for kafka-init to complete (silently)
echo "⏳ Initializing infrastructure..."

MAX_WAIT=120
ELAPSED=0

while [ $ELAPSED -lt $MAX_WAIT ]; do
  STATUS=$(docker compose ps kafka-init --format "{{.State}}" 2>/dev/null || echo "unknown")
  
  if [ "$STATUS" = "exited" ]; then
    EXIT_CODE=$(docker compose ps kafka-init --format "{{.ExitCode}}" 2>/dev/null || echo "1")
    if [ "$EXIT_CODE" = "0" ]; then
      break
    else
      echo "❌ Infrastructure initialization failed"
      exit 1
    fi
  fi
  
  ELAPSED=$((ELAPSED + 2))
  sleep 2
done

if [ $ELAPSED -ge $MAX_WAIT ]; then
  echo "❌ Infrastructure initialization timed out"
  exit 1
fi

echo "✅ Infrastructure ready!"
echo ""
echo "📦 Starting Bank Transfer API..."
./gradlew bootRun
