# PR #2 - Domain Models

## Overview
Added complete domain model layer with entities, enums, and DTOs for the bank transfer API.

## Changes Made

### ✅ Enums Created
- `AccountStatus` - ACTIVE, INACTIVE, SUSPENDED, CLOSED
- `TransferStatus` - PENDING, PROCESSING, COMPLETED, FAILED, REJECTED
- `Currency` - BRL, USD, EUR

### ✅ Domain Models (Entities)

#### Account
- `accountId`: String (unique identifier)
- `balance`: BigDecimal
- `currency`: Currency enum
- `status`: AccountStatus enum
- `customerName`: String
- Methods:
  - `isActive()`: Check if account is active
  - `hasSufficientBalance(amount)`: Validate balance
  - `debit(amount)`: Debit account (immutable operation)
  - `credit(amount)`: Credit account (immutable operation)

#### Transfer
- `transferId`: String (unique identifier)
- `sourceAccountId`: String
- `destinationAccountId`: String
- `amount`: BigDecimal
- `currency`: Currency enum
- `status`: TransferStatus enum
- `requestedAt`: Instant
- `completedAt`: Instant? (nullable)
- `failureReason`: String? (nullable)
- Methods:
  - `isCompleted()`: Check if completed
  - `isFailed()`: Check if failed
  - `isPending()`: Check if pending
  - `markCompleted()`: Mark as completed
  - `markFailed(reason)`: Mark as failed with reason

### ✅ Kafka Events
- `TransferRequestedEvent` - Incoming transfer request
- `TransferCompletedEvent` - Successful transfer
- `TransferFailedEvent` - Failed transfer

### ✅ DTOs (Data Transfer Objects)

#### Account DTOs
- `AccountDTO` - API response
- `CreateAccountRequest` - API request

#### Transfer DTOs
- `TransferDTO` - Transfer details
- `TransferRequestDTO` - Incoming request
- `TransferResponseDTO` - API response

## Project Structure
```
domain/
├── enums/
│   ├── AccountStatus.kt
│   ├── Currency.kt
│   └── TransferStatus.kt
├── model/
│   ├── Account.kt
│   ├── Transfer.kt
│   └── TransferEvent.kt
└── dto/
    ├── AccountDTO.kt
    └── TransferDTO.kt
```

## Key Design Decisions

1. **Immutable Data Classes**: Using Kotlin `data class` for immutability
2. **Business Logic in Models**: Account balance operations (debit/credit) are part of the entity
3. **Enums for States**: Type-safe status and currency handling
4. **Event Model**: Separate event classes for Kafka messaging
5. **DTOs**: Separate from entities for API contracts

## Testing
```bash
./gradlew clean build
```

Should compile without errors.

## Next Steps (PR #3)
- Create DynamoDB Repositories
- Implement Account and Transfer persistence
- Add table initialization scripts

## Related Issues
N/A

## PR Link
https://github.com/Danilocamargo5/bank-transfer-api/pull/2
