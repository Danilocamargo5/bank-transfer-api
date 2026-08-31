#!/bin/bash

set -e

echo "🚀 Starting Bank Transfer API..."
docker-compose up -d

echo "⏳ Waiting for services..."
sleep 5

echo "📦 Starting application..."
./gradlew bootRun
