#!/bin/bash

set -e

echo "🚀 Starting Docker services..."
docker-compose up -d kafka localstack

echo "⏳ Waiting 120s for services to be fully ready..."
sleep 120

echo "✅ Services ready!"
echo ""
echo "Next step: ./init-kafka.sh"
