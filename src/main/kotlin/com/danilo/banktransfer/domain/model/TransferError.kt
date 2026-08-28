package com.danilo.banktransfer.domain.model

import com.danilo.banktransfer.domain.enums.ErrorType
import java.time.Instant

/**
 * Represents a transfer error for tracking and auditing
 * 
 * Stores detailed information about failures for:
 * - Retry logic
 * - Dead Letter Queue processing
 * - Error analysis and monitoring
 */
data class TransferError(
    val transferId: String,
    val errorType: ErrorType,
    val errorMessage: String,
    val errorCode: String = errorType.code,
    val stackTrace: String? = null,
    val isRetryable: Boolean = errorType.isRetryable,
    val retryAttempt: Int = 0,
    val maxRetries: Int = 3,
    val occurredAt: Instant = Instant.now(),
    val causeException: String? = null
) {
    val canRetry: Boolean get() = isRetryable && retryAttempt < maxRetries
    val nextRetryIn: Long get() = if (canRetry) exponentialBackoff() else 0
    
    /**
     * Calculate exponential backoff: 1s, 2s, 4s
     * Formula: initialDelay * (multiplier ^ retryAttempt)
     */
    private fun exponentialBackoff(): Long {
        val initialDelayMs = 1000L  // 1 second
        val multiplier = 2.0
        return (initialDelayMs * Math.pow(multiplier, retryAttempt.toDouble())).toLong()
    }
    
    /**
     * Human-readable error description
     */
    fun getErrorDescription(): String {
        return "${errorType.description} (${errorCode}). Retryable: $isRetryable, Attempt: $retryAttempt/$maxRetries"
    }
}
