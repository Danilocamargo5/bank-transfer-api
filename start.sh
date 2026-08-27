#!/bin/bash

echo "🚀 Starting Docker containers..."
docker-compose up -d

# Wait for LocalStack to be ready - increased to 45 seconds
echo "⏳ Waiting for LocalStack and services to be fully ready..."
sleep 45

echo "✅ Services should be ready now"
echo "📦 Starting Bank Transfer API..."
./gradlew bootRun --args='--spring.profiles.active=dev'
