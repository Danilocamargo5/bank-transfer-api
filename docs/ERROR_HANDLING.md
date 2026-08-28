# Error Handling & Retry Strategy (PR #6)

## Overview

Implements comprehensive error handling with exponential backoff retry logic and Dead Letter Queue (DLQ) for failed transfers.

## Architecture

```
Transfer Request
    ↓
[Processing]
    ↓
┌─────────────────────────────────┐
│ Error Occurs?                   │
└──────────┬──────────────────────┘
           │
      ┌────┴────┐
      │          │
   [YES]       [NO]
    │           │
    ↓           ↓
[Is Retryable?] [COMPLETED]
    │
 ┌──┴──┐
 │     │
[YES] [NO]
 │     │
 ↓     ↓
[Retry] → [Max Retries?] → [YES] → [SQS DLQ]
 ↑                             ↓
 └─── Exponential Backoff    [NO] → [FAILED]
```

## Error Types

### Non-Retryable Errors (Fail immediately)

- **INVALID_TRANSFER_ID** (ERR_001) - Transfer ID missing/invalid
- **INVALID_AMOUNT** (ERR_002) - Amount ≤ 0
- **INVALID_CURRENCY** (ERR_003) - Unsupported currency
- **INVALID_DATE** (ERR_004) - Invalid request date
- **SOURCE_ACCOUNT_NOT_FOUND** (ERR_005) - Source doesn't exist
- **DESTINATION_ACCOUNT_NOT_FOUND** (ERR_006) - Destination doesn't exist
- **SAME_ACCOUNT_TRANSFER** (ERR_007) - Source = Destination
- **DUPLICATE_TRANSFER** (ERR_008) - Transfer ID already processed
- **SOURCE_ACCOUNT_INACTIVE** (ERR_101) - Source not active
- **DESTINATION_ACCOUNT_INACTIVE** (ERR_102) - Destination not active
- **INSUFFICIENT_BALANCE** (ERR_103) - Not enough funds

### Retryable Errors (With exponential backoff)

- **DATABASE_ERROR** (ERR_201) - DB operation failed
- **KAFKA_ERROR** (ERR_202) - Message publishing failed
- **NETWORK_ERROR** (ERR_203) - Network communication failed
- **TIMEOUT_ERROR** (ERR_204) - Operation timed out
- **INTERNAL_ERROR** (ERR_500) - Unexpected error
- **UNKNOWN_ERROR** (ERR_999) - Unknown error

## Retry Strategy

### Exponential Backoff

- **Formula**: `initialDelay × (multiplier ^ retryAttempt)`
- **Initial Delay**: 1 second
- **Multiplier**: 2.0
- **Max Retries**: 3 attempts

#### Timeline:
1. First Attempt: Immediate
2. Retry 1: After 1 second
3. Retry 2: After 2 seconds
4. Retry 3: After 4 seconds
5. After 3 retries: Send to DLQ

## Dead Letter Queue (DLQ)

Failed transfers after all retries are sent to SQS queue:
- **Queue**: `transfer-failed-dlq`
- **Purpose**: Manual review and intervention
- **Contents**: Complete transfer record + error details

### DLQ Message Structure
```json
{
  "transferId": "tf-001",
  "sourceAccountId": "acc-001",
  "destinationAccountId": "acc-002",
  "amount": 100.00,
  "currency": "BRL",
  "status": "FAILED",
  "error": {
    "code": "ERR_201",
    "type": "DATABASE_ERROR",
    "message": "Failed to persist transfer record",
    "retryAttempts": 3,
    "timestamp": "2026-08-28T14:30:00Z"
  }
}
```

## Implementation Details

### TransferError Model
```kotlin
data class TransferError(
    val transferId: String,
    val errorType: ErrorType,
    val errorMessage: String,
    val isRetryable: Boolean,
    val retryAttempt: Int,
    val maxRetries: Int = 3,
    val occurredAt: Instant,
    val stackTrace: String?
)
```

### Error Handling Flow

1. **Exception Caught** → Classify by ErrorType
2. **Is Retryable?** → Route accordingly
   - YES → Exponential backoff retry
   - NO → Send to DLQ immediately
3. **Max Retries Exceeded?** → Send to DLQ
4. **Success** → Update status to COMPLETED

## Usage Example

```kotlin
try {
    transferService.processTransfer(transferRequest)
} catch (e: TransferException) {
    when {
        e.isRetryable && retryCount < 3 -> {
            // Exponential backoff will be applied by RetryTemplate
            retry(e)
        }
        !e.isRetryable -> {
            // Send to DLQ immediately
            dlqPublisher.publish(transfer, e)
        }
        else -> {
            // Max retries exceeded
            dlqPublisher.publish(transfer, e)
        }
    }
}
```

## Testing Error Scenarios

### Non-Retryable (Should fail immediately)
```bash
curl -X POST http://localhost:8080/api/v1/transfers \
  -H "Content-Type: application/json" \
  -d '{
    "transferId":"tf-test",
    "sourceAccountId":"nonexistent",  # Triggers AccountNotFoundException
    "destinationAccountId":"acc-002",
    "amount":100.00,
    "currency":"BRL",
    "requestedAt":"2026-08-28T13:00:00Z"
  }'
```

### Retryable (Should retry with backoff)
Simulate by:
- Stopping LocalStack temporarily
- Observing Kafka connection errors
- Watching exponential backoff in logs

## Monitoring & Logging

All errors are logged with:
- **Error Code** - Unique identifier
- **Transfer ID** - For tracing
- **Retry Attempt** - Current attempt number
- **Stack Trace** - Full exception trace
- **Timestamp** - When error occurred

### Log Levels
- **ERROR** - Non-retryable failures
- **WARN** - Retryable errors on first attempt
- **INFO** - Retry attempts
- **DEBUG** - Detailed error context

## Future Enhancements

- [ ] Metrics collection for error rates
- [ ] Alert system for DLQ messages
- [ ] Batch retry processing
- [ ] Error analysis dashboard
- [ ] Automatic recovery strategies
