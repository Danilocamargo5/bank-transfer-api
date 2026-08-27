#!/bin/bash

echo "Waiting for Kafka to be ready..."
sleep 10

KAFKA_BROKER="kafka:29092"

echo "Creating Kafka topics..."

# Create transfer-requested topic
kafka-topics --bootstrap-server $KAFKA_BROKER \
  --create \
  --if-not-exists \
  --topic transfer-requested \
  --partitions 3 \
  --replication-factor 1

echo "✓ Topic 'transfer-requested' created"

# Create transfer-completed topic
kafka-topics --bootstrap-server $KAFKA_BROKER \
  --create \
  --if-not-exists \
  --topic transfer-completed \
  --partitions 3 \
  --replication-factor 1

echo "✓ Topic 'transfer-completed' created"

# Create transfer-failed topic (for dead letter queue)
kafka-topics --bootstrap-server $KAFKA_BROKER \
  --create \
  --if-not-exists \
  --topic transfer-failed \
  --partitions 1 \
  --replication-factor 1

echo "✓ Topic 'transfer-failed' created"

echo ""
echo "Kafka topics initialization complete!"
kafka-topics --bootstrap-server $KAFKA_BROKER --list
