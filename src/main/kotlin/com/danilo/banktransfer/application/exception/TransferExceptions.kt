package com.danilo.banktransfer.application.exception

import com.danilo.banktransfer.domain.enums.ErrorType

/**
 * Base exception for transfer operations with error type tracking
 */
open class TransferException(
    message: String,
    val errorType: ErrorType = ErrorType.UNKNOWN_ERROR,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    val isRetryable: Boolean get() = errorType.isRetryable
}

class InsufficientBalanceException(
    message: String,
    cause: Throwable? = null
) : TransferException(message, ErrorType.INSUFFICIENT_BALANCE, cause)

class AccountNotFoundException(
    message: String,
    errorType: ErrorType = ErrorType.SOURCE_ACCOUNT_NOT_FOUND,
    cause: Throwable? = null
) : TransferException(message, errorType, cause)

class InactiveAccountException(
    message: String,
    errorType: ErrorType = ErrorType.SOURCE_ACCOUNT_INACTIVE,
    cause: Throwable? = null
) : TransferException(message, errorType, cause)

class InvalidTransferException(
    message: String,
    errorType: ErrorType = ErrorType.INVALID_TRANSFER_ID,
    cause: Throwable? = null
) : TransferException(message, errorType, cause)

class DuplicateTransferException(
    message: String,
    cause: Throwable? = null
) : TransferException(message, ErrorType.DUPLICATE_TRANSFER, cause)

class TransferProcessingException(
    message: String,
    errorType: ErrorType = ErrorType.INTERNAL_ERROR,
    cause: Throwable? = null
) : TransferException(message, errorType, cause)

class DatabaseException(
    message: String,
    cause: Throwable? = null
) : TransferException(message, ErrorType.DATABASE_ERROR, cause)

class KafkaException(
    message: String,
    cause: Throwable? = null
) : TransferException(message, ErrorType.KAFKA_ERROR, cause)
