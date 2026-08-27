package com.danilo.banktransfer.domain.model

import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import java.math.BigDecimal
import java.time.Instant

data class Transfer(
    val transferId: String,
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val currency: Currency,
    val status: TransferStatus,
    val requestedAt: Instant,
    val completedAt: Instant? = null,
    val failureReason: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun isCompleted(): Boolean = status == TransferStatus.COMPLETED
    
    fun isFailed(): Boolean = status == TransferStatus.FAILED
    
    fun isPending(): Boolean = status == TransferStatus.PENDING
    
    fun markCompleted(): Transfer {
        return this.copy(
            status = TransferStatus.COMPLETED,
            completedAt = Instant.now(),
            updatedAt = Instant.now()
        )
    }
    
    fun markFailed(reason: String): Transfer {
        return this.copy(
            status = TransferStatus.FAILED,
            failureReason = reason,
            updatedAt = Instant.now()
        )
    }
}
