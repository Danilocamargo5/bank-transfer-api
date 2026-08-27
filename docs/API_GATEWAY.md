# API Gateway Integration (PR #5)

## Overview

This PR integrates AWS API Gateway (LocalStack) as the entry point for all client requests. The architecture now is:

```
Client Request
     ↓
API Gateway (LocalStack)
     ↓
Spring Boot Controller
     ↓
Kafka (Event Publishing)
     ↓
Processing + DynamoDB
```

## Architecture

### Before (without API Gateway)
```
Client → Spring Boot Controller → Kafka
```

### After (with API Gateway)
```
Client → API Gateway → Spring Boot Controller → Kafka
```

## How It Works

1. **API Gateway receives request** (http://localhost:4566/restapis/{API_ID}/dev/api/v1/transfers)
2. **Forwards to Spring Boot** (http://localhost:8080/api/v1/transfers)
3. **Controller processes** and publishes to Kafka
4. **Kafka Consumer** processes the transfer event
5. **DynamoDB** stores the transfer record

## API Gateway Resources

The init-api-gateway container creates:

- **REST API**: `bank-transfer-api`
- **Stage**: `dev`
- **Resources**:
  - `POST /api/v1/transfers` → HTTP Integration to localhost:8080
  - `GET /api/v1/transfers/{transferId}` → HTTP Integration to localhost:8080
  - `GET /api/v1/accounts/{accountId}` → HTTP Integration to localhost:8080

## Testing

### Get API ID

```bash
aws apigateway get-rest-apis \
  --endpoint-url http://localhost:4566 \
  --region us-east-1 \
  --query 'items[0].id' \
  --output text
```

### Call via API Gateway

```bash
API_ID="<from-above>"

# Create Transfer via API Gateway
curl -X POST "http://localhost:4566/restapis/$API_ID/dev/api/v1/transfers" \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-gateway-001",
    "sourceAccountId":"acc-001",
    "destinationAccountId":"acc-002",
    "amount":25.00,
    "currency":"BRL",
    "requestedAt":"2026-08-27T21:00:00Z"
  }'

sleep 3

# Check Transfer Status via API Gateway
curl "http://localhost:4566/restapis/$API_ID/dev/api/v1/transfers/tf-gateway-001"

# Check Account via API Gateway
curl "http://localhost:4566/restapis/$API_ID/dev/api/v1/accounts/acc-001"
```

## Direct Access vs API Gateway

Both still work in this implementation:

- **Direct**: `http://localhost:8080/api/v1/transfers` (direct to Spring Boot)
- **Via Gateway**: `http://localhost:4566/restapis/{API_ID}/dev/api/v1/transfers` (through API Gateway)

In a real production environment, you would restrict direct access and only allow through API Gateway.

## Notes

- API Gateway integrations use HTTP backend (not Lambda)
- Requests pass through gateway metadata (headers, etc)
- Response codes are handled by the backend service
- This allows for future API Gateway policies, rate limiting, etc
