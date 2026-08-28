#!/bin/bash

set -e

echo "🚀 Starting infrastructure containers..."
docker-compose up -d kafka localstack

echo "⏳ Waiting for Kafka to be ready..."
sleep 90

echo "Creating Kafka topics..."
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-requested --partitions 3 --replication-factor 1
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-completed --partitions 3 --replication-factor 1
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-failed --partitions 1 --replication-factor 1
echo "✅ Kafka topics created!"

echo ""
echo "Creating sample accounts..."
docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-001"},"balance":{"N":"5000"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"João Silva"}}' \
  --endpoint-url http://localhost:4566 2>/dev/null || true

docker-compose exec localstack aws dynamodb put-item \
  --table-name accounts \
  --item '{"accountId":{"S":"acc-002"},"balance":{"N":"1000"},"currency":{"S":"BRL"},"status":{"S":"ACTIVE"},"customerName":{"S":"Maria Santos"}}' \
  --endpoint-url http://localhost:4566 2>/dev/null || true

echo "✅ Sample accounts created!"

echo ""
echo "=========================================="
echo "✅ Infrastructure initialization complete"
echo "=========================================="
echo ""
echo "Next: run ./start.sh"
