#!/bin/bash

echo "🚀 Starting Docker containers..."
docker-compose up -d

# Wait for services to be ready
echo "⏳ Waiting for services to be ready..."
sleep 15

echo "📦 Starting Bank Transfer API..."
./gradlew bootRun --args='--spring.profiles.active=dev'
