package com.danilo.banktransfer.domain.enums

/**
 * Error Types for Transfer Processing
 * 
 * Categorizes different failure scenarios for proper handling and logging
 */
enum class ErrorType(
    val code: String,
    val description: String,
    val isRetryable: Boolean
) {
    // Validation errors (not retryable)
    INVALID_TRANSFER_ID("ERR_001", "Transfer ID is invalid or missing", false),
    INVALID_AMOUNT("ERR_002", "Transfer amount must be greater than 0", false),
    INVALID_CURRENCY("ERR_003", "Currency is not supported", false),
    INVALID_DATE("ERR_004", "Request date is invalid", false),
    SOURCE_ACCOUNT_NOT_FOUND("ERR_005", "Source account does not exist", false),
    DESTINATION_ACCOUNT_NOT_FOUND("ERR_006", "Destination account does not exist", false),
    SAME_ACCOUNT_TRANSFER("ERR_007", "Cannot transfer to the same account", false),
    DUPLICATE_TRANSFER("ERR_008", "Transfer with this ID already exists", false),
    
    // Account status errors (not retryable)
    SOURCE_ACCOUNT_INACTIVE("ERR_101", "Source account is not active", false),
    DESTINATION_ACCOUNT_INACTIVE("ERR_102", "Destination account is not active", false),
    INSUFFICIENT_BALANCE("ERR_103", "Source account has insufficient balance", false),
    
    // System/transient errors (retryable)
    DATABASE_ERROR("ERR_201", "Database operation failed", true),
    KAFKA_ERROR("ERR_202", "Kafka message publishing failed", true),
    NETWORK_ERROR("ERR_203", "Network communication failed", true),
    TIMEOUT_ERROR("ERR_204", "Operation timed out", true),
    
    // Unknown errors (retryable)
    INTERNAL_ERROR("ERR_500", "Unexpected internal error", true),
    UNKNOWN_ERROR("ERR_999", "Unknown error occurred", true);
}
