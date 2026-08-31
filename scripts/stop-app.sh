#!/bin/bash

echo "⏹️  Stopping Spring Boot application..."
pkill -f "java.*bank-transfer" || echo "No running application found"
echo "✅ Application stopped"
