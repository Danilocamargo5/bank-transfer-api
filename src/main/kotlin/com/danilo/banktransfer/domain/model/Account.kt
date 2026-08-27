package com.danilo.banktransfer.domain.model

import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import java.math.BigDecimal
import java.time.Instant

data class Account(
    val accountId: String,
    val balance: BigDecimal,
    val currency: Currency,
    val status: AccountStatus,
    val customerName: String,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now()
) {
    fun isActive(): Boolean = status == AccountStatus.ACTIVE

    fun hasSufficientBalance(amount: BigDecimal): Boolean = balance >= amount

    fun debit(amount: BigDecimal): Account {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        require(hasSufficientBalance(amount)) { "Insufficient balance" }
        
        return this.copy(
            balance = balance - amount,
            updatedAt = Instant.now()
        )
    }

    fun credit(amount: BigDecimal): Account {
        require(amount > BigDecimal.ZERO) { "Amount must be positive" }
        
        return this.copy(
            balance = balance + amount,
            updatedAt = Instant.now()
        )
    }
}
