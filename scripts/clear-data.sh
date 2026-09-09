#!/bin/bash

set -e

echo "🧹 Clearing data from tables (keeping table structure)..."
echo ""

# Clear accounts table (key: accountId)
echo "🗑️  Clearing table: accounts"
docker-compose exec localstack awslocal dynamodb scan --table-name accounts --projection-expression "accountId" 2>/dev/null | \
  jq -r '.Items[].accountId.S' 2>/dev/null | \
  xargs -I {} docker-compose exec localstack awslocal dynamodb delete-item --table-name accounts --key '{"accountId":{"S":"{}"}}' 2>/dev/null || true
echo "  ✅ Cleared: accounts"

# Clear transfers table (key: transferId)
echo "🗑️  Clearing table: transfers"
docker-compose exec localstack awslocal dynamodb scan --table-name transfers --projection-expression "transferId" 2>/dev/null | \
  jq -r '.Items[].transferId.S' 2>/dev/null | \
  xargs -I {} docker-compose exec localstack awslocal dynamodb delete-item --table-name transfers --key '{"transferId":{"S":"{}"}}' 2>/dev/null || true
echo "  ✅ Cleared: transfers"

# Clear transfers-lock table (key: lockId)
echo "🗑️  Clearing table: transfers-lock"
docker-compose exec localstack awslocal dynamodb scan --table-name transfers-lock --projection-expression "lockId" 2>/dev/null | \
  jq -r '.Items[].lockId.S' 2>/dev/null | \
  xargs -I {} docker-compose exec localstack awslocal dynamodb delete-item --table-name transfers-lock --key '{"lockId":{"S":"{}"}}' 2>/dev/null || true
echo "  ✅ Cleared: transfers-lock"

echo ""

# Clear Kafka topics
echo "Clearing Kafka topics..."
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic transfer-requested 2>/dev/null || echo "  ⚠️  transfer-requested topic not found"
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic transfer-completed 2>/dev/null || echo "  ⚠️  transfer-completed topic not found"
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic transfer-failed 2>/dev/null || echo "  ⚠️  transfer-failed topic not found"

sleep 1

# Recreate empty topics
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic transfer-requested --partitions 1 --replication-factor 1 2>/dev/null || true
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic transfer-completed --partitions 1 --replication-factor 1 2>/dev/null || true
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --create --topic transfer-failed --partitions 1 --replication-factor 1 2>/dev/null || true

echo "  ✅ Kafka topics cleared and recreated"

echo ""

# Clear SQS queue
echo "Clearing SQS queue..."
docker-compose exec localstack awslocal sqs purge-queue --queue-url http://localhost:4566/000000000000/transfer-failed-dlq 2>/dev/null || echo "  ⚠️  SQS queue not found"

echo ""
echo "✅ All data cleared!"
echo "✅ Tables still exist - no restart needed!"
echo ""
