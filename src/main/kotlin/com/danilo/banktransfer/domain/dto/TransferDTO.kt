package com.danilo.banktransfer.domain.dto

import java.math.BigDecimal
import java.time.Instant

data class TransferDTO(
    val transferId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val currency: String,
    val status: String,
    val requestedAt: Instant,
    val completedAt: Instant?,
    val failureReason: String?
)

data class TransferRequestDTO(
    val transferId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val currency: String,
    val requestedAt: Instant
)

data class TransferResponseDTO(
    val transferId: String,
    val status: String,
    val message: String
)
