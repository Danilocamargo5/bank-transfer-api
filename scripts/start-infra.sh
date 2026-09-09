#!/bin/bash

set -e

echo "🧹 Cleaning up old containers..."
docker-compose down 2>/dev/null || true

echo "🚀 Starting Docker services..."
docker-compose up -d kafka localstack kafka-ui

echo "⏳ Waiting 120s for services to be fully ready..."
sleep 120

echo "✅ Services ready!"
echo ""
echo "Next step: ./init-kafka.sh"
