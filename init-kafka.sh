#!/bin/bash

set -e

echo "📬 Creating Kafka topics..."
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-requested --partitions 3 --replication-factor 1
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-completed --partitions 3 --replication-factor 1
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --create --if-not-exists --topic transfer-failed --partitions 1 --replication-factor 1

echo "✅ Kafka topics created!"
echo ""
echo "Topics:"
docker-compose exec kafka kafka-topics --bootstrap-server kafka:29092 --list
echo ""
echo "Next step: ./init-dynamodb.sh"
