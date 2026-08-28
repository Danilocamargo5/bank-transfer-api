package com.danilo.banktransfer.domain.validator

import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.application.exception.InvalidTransferException
import java.math.BigDecimal

object TransferValidator {
    
    fun validate(request: TransferRequestDTO) {
        validateTransferId(request.transferId)
        validateAccountIds(request.sourceAccountId, request.destinationAccountId)
        validateAmount(request.amount)
        validateCurrency(request.currency)
    }
    
    private fun validateTransferId(transferId: String) {
        if (transferId.isBlank()) {
            throw InvalidTransferException("transferId cannot be empty")
        }
        if (transferId.length > 100) {
            throw InvalidTransferException("transferId cannot exceed 100 characters")
        }
    }
    
    private fun validateAccountIds(source: String, destination: String) {
        if (source.isBlank()) {
            throw InvalidTransferException("sourceAccountId cannot be empty")
        }
        if (destination.isBlank()) {
            throw InvalidTransferException("destinationAccountId cannot be empty")
        }
        if (source == destination) {
            throw InvalidTransferException("source and destination accounts cannot be the same")
        }
    }
    
    private fun validateAmount(amount: BigDecimal) {
        if (amount <= BigDecimal.ZERO) {
            throw InvalidTransferException("amount must be greater than zero")
        }
        if (amount.scale() > 2) {
            throw InvalidTransferException("amount cannot have more than 2 decimal places")
        }
    }
    
    private fun validateCurrency(currency: String) {
        if (currency.isBlank()) {
            throw InvalidTransferException("currency cannot be empty")
        }
        if (currency.length != 3) {
            throw InvalidTransferException("currency must be a 3-letter code (e.g., BRL)")
        }
    }
}
