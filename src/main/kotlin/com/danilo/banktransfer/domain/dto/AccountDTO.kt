package com.danilo.banktransfer.domain.dto

import java.math.BigDecimal

data class AccountDTO(
    val accountId: String,
    val balance: BigDecimal,
    val currency: String,
    val status: String,
    val customerName: String
)

data class CreateAccountRequest(
    val accountId: String,
    val balance: BigDecimal,
    val currency: String,
    val customerName: String
)
