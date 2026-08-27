#!/bin/bash

echo "🛑 Stopping Bank Transfer API and Docker containers..."

# Kill any running gradlew process
pkill -f "gradlew bootRun" 2>/dev/null || true

# Stop docker-compose
docker-compose down

echo "✅ Everything stopped!"
