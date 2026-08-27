package com.danilo.banktransfer.domain.model

import java.math.BigDecimal
import java.time.Instant

// Event for transfer requests from Kafka
data class TransferRequestedEvent(
    val transferId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val currency: String,
    val requestedAt: Instant
)

// Event when transfer completes successfully
data class TransferCompletedEvent(
    val transferId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val currency: String,
    val completedAt: Instant
)

// Event when transfer fails
data class TransferFailedEvent(
    val transferId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val currency: String,
    val failureReason: String,
    val failedAt: Instant
)
