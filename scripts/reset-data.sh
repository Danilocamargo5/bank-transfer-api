#!/bin/bash

set -e

echo "🧹 Cleaning all data (WITHOUT stopping app)..."
echo ""

# Delete DynamoDB tables
echo "🗑️  Deleting DynamoDB tables..."
docker-compose exec localstack awslocal dynamodb delete-table --table-name accounts 2>/dev/null || echo "  ⚠️  accounts table not found"
docker-compose exec localstack awslocal dynamodb delete-table --table-name transfers 2>/dev/null || echo "  ⚠️  transfers table not found"
docker-compose exec localstack awslocal dynamodb delete-table --table-name transfers-lock 2>/dev/null || echo "  ⚠️  transfers-lock table not found"

echo ""

# Delete Kafka topics
echo "🗑️  Deleting Kafka topics..."
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic transfer-requested 2>/dev/null || echo "  ⚠️  transfer-requested topic not found"
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic transfer-completed 2>/dev/null || echo "  ⚠️  transfer-completed topic not found"
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic transfer-failed 2>/dev/null || echo "  ⚠️  transfer-failed topic not found"

echo ""

# Purge SQS queue
echo "🗑️  Purging SQS queue..."
docker-compose exec localstack awslocal sqs purge-queue --queue-url http://localhost:4566/000000000000/transfer-failed-dlq 2>/dev/null || echo "  ⚠️  SQS queue not found"

echo ""
echo "✅ Data cleaned!"
echo "⚠️  App is still running. Tables/Topics will be recreated on next transfer."
echo ""
