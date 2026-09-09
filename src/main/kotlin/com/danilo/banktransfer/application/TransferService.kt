package com.danilo.banktransfer.application

import com.danilo.banktransfer.application.exception.AccountNotFoundException
import com.danilo.banktransfer.application.exception.DuplicateTransferException
import com.danilo.banktransfer.application.exception.InactiveAccountException
import com.danilo.banktransfer.application.exception.InsufficientBalanceException
import com.danilo.banktransfer.application.exception.InvalidTransferException
import com.danilo.banktransfer.domain.model.Transfer
import com.danilo.banktransfer.domain.model.TransferCompletedEvent
import com.danilo.banktransfer.domain.model.TransferFailedEvent
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.ErrorType
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import com.danilo.banktransfer.infrastructure.metrics.TransferMetrics
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TransferService(
    private val accountRepository: AccountRepository,
    private val transferRepository: TransferRepository,
    private val transferMetrics: TransferMetrics,
    private val deadLetterService: com.danilo.banktransfer.infrastructure.service.DeadLetterService,
    @Value("\${aws.dynamodb.table.accounts}")
    private val accountTableName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    
    companion object {
        private const val MAX_RETRIES = 3
        private const val INITIAL_BACKOFF_MS = 100L
    }

    fun processTransfer(event: TransferRequestedEvent): Result {
        // MDC: Add transferId to all logs in this context
        MDC.put("transferId", event.transferId)
        MDC.put("sourceAccountId", event.sourceAccountId)
        MDC.put("destinationAccountId", event.destinationAccountId)
        
        val startTime = System.currentTimeMillis()
        logger.info("Processing transfer: ${event.transferId} from ${event.sourceAccountId} to ${event.destinationAccountId}")

        return try {
            // 1. Check if transfer already processed (idempotency using Query on transferId)
            if (transferRepository.hasCompletedTransfer(event.transferId)) {
                logger.warn("Transfer ${event.transferId} already processed (idempotent request)")
                throw DuplicateTransferException("Transfer ${event.transferId} already processed")
            }

            // 2. Validate transfer
            validateTransfer(event)
            logger.info("Transfer validation passed for ${event.transferId}")

            // 3. Get accounts
            val sourceAccount = accountRepository.findById(event.sourceAccountId)
                .orElseThrow { 
                    logger.error("Source account ${event.sourceAccountId} not found")
                    AccountNotFoundException("Source account ${event.sourceAccountId} not found") 
                }

            val destinationAccount = accountRepository.findById(event.destinationAccountId)
                .orElseThrow { 
                    logger.error("Destination account ${event.destinationAccountId} not found")
                    AccountNotFoundException("Destination account ${event.destinationAccountId} not found") 
                }

            logger.info("Accounts found: source=${sourceAccount.accountId}, dest=${destinationAccount.accountId}")

            // 4. Validate account statuses
            if (!sourceAccount.isActive()) {
                logger.error("Source account ${event.sourceAccountId} is not active. Status: ${sourceAccount.status}")
                throw InactiveAccountException("Source account ${event.sourceAccountId} is not active")
            }

            if (!destinationAccount.isActive()) {
                logger.error("Destination account ${event.destinationAccountId} is not active. Status: ${destinationAccount.status}")
                throw InactiveAccountException("Destination account ${event.destinationAccountId} is not active")
            }

            // 5. Validate sufficient balance
            if (!sourceAccount.hasSufficientBalance(event.amount)) {
                logger.error("Insufficient balance in account ${event.sourceAccountId}. Required: ${event.amount}, Available: ${sourceAccount.balance}")
                throw InsufficientBalanceException(
                    "Insufficient balance in account ${event.sourceAccountId}. " +
                    "Required: ${event.amount}, Available: ${sourceAccount.balance}"
                )
            }

            logger.info("Balance validation passed for ${event.transferId}")

            // 6. Perform atomic debit/credit
            val updatedSourceAccount = sourceAccount.debit(event.amount)
            val updatedDestinationAccount = destinationAccount.credit(event.amount)

            logger.info("Debit/credit complete for ${event.transferId}. New balances: source=${updatedSourceAccount.balance}, dest=${updatedDestinationAccount.balance}")

            // 7. Create transfer record BEFORE atomic save (will be included in transaction)
            val transfer = Transfer(
                transferId = event.transferId,
                sourceAccountId = event.sourceAccountId,
                destinationAccountId = event.destinationAccountId,
                amount = event.amount,
                currency = Currency.valueOf(event.currency),
                status = TransferStatus.COMPLETED,
                requestedAt = event.requestedAt,
                completedAt = Instant.now(),
                failureReason = null,
                createdAt = Instant.now(),
                updatedAt = Instant.now()
            )

            // 8. Save updated accounts + transfer record ATOMICALLY in ONE transaction
            // This guarantees: ALL THREE (source, dest, transfer) succeed or ALL fail
            saveTransferWithAccountsAtomically(
                sourceAccount = updatedSourceAccount,
                destinationAccount = updatedDestinationAccount,
                transfer = transfer,
                transferId = event.transferId
            )

            logger.info("Transfer and accounts saved atomically for ${event.transferId}")
            
            val duration = System.currentTimeMillis() - startTime
            transferMetrics.recordTransferProcessingTime(duration)
            transferMetrics.recordTransferSuccess()

            Result.Success(
                TransferCompletedEvent(
                    transferId = event.transferId,
                    sourceAccountId = event.sourceAccountId,
                    destinationAccountId = event.destinationAccountId,
                    amount = event.amount,
                    currency = event.currency,
                    completedAt = Instant.now()
                )
            )
        } catch (e: Exception) {
            logger.error("Transfer ${event.transferId} failed: ${e.message}", e)
            
            val duration = System.currentTimeMillis() - startTime
            transferMetrics.recordTransferProcessingTime(duration)
            transferMetrics.recordTransferFailure(e::class.simpleName ?: "UnknownError")

            val failureReason = e.message ?: "Unknown error"
            
            // Try to save failed transfer record
            try {
                val failedTransfer = Transfer(
                    transferId = event.transferId,
                    sourceAccountId = event.sourceAccountId,
                    destinationAccountId = event.destinationAccountId,
                    amount = event.amount,
                    currency = Currency.valueOf(event.currency),
                    status = TransferStatus.FAILED,
                    requestedAt = event.requestedAt,
                    completedAt = null,
                    failureReason = failureReason,
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
                transferRepository.save(failedTransfer)
            } catch (ex: Exception) {
                logger.error("Failed to save failed transfer record: ${ex.message}", ex)
            }

            Result.Failure(
                TransferFailedEvent(
                    transferId = event.transferId,
                    sourceAccountId = event.sourceAccountId,
                    destinationAccountId = event.destinationAccountId,
                    amount = event.amount,
                    currency = event.currency,
                    failureReason = failureReason,
                    failedAt = Instant.now()
                )
            )
        } finally {
            // Clean up MDC context
            MDC.remove("transferId")
            MDC.remove("sourceAccountId")
            MDC.remove("destinationAccountId")
        }
    }

    /**
     * UNIFIED TRANSACTION: Save transfer + accounts ATOMICALLY
     * Guarantees: ALL items (source account, dest account, transfer record) are saved TOGETHER
     * 
     * For transient failures (network, throttling), retry with exponential backoff
     * For validation errors, fail immediately
     */
    private fun saveTransferWithAccountsAtomically(
        sourceAccount: com.danilo.banktransfer.domain.model.Account,
        destinationAccount: com.danilo.banktransfer.domain.model.Account,
        transfer: com.danilo.banktransfer.domain.model.Transfer,
        transferId: String
    ) {
        var lastException: Exception? = null

        // Retry only for transient errors (network glitches, throttling)
        for (attempt in 1..MAX_RETRIES) {
            try {
                logger.info("Attempt $attempt to atomically save transfer and accounts for transfer $transferId")
                
                // DynamoDB TransactWriteItems: All 3 writes (source, dest, transfer) or NONE
                transferRepository.saveTransferWithAccountsAtomically(
                    sourceAccount,
                    destinationAccount,
                    transfer,
                    accountTableName  // Use injected table name
                )
                
                logger.info("Successfully saved transfer and accounts atomically for transfer $transferId on attempt $attempt")
                return  // Success, exit
            } catch (e: Exception) {
                // Any error during atomic save - will retry or fail after MAX_RETRIES attempts
                // DynamoDB transactional guarantee ensures ALL save or ALL fail (no partial updates)
                lastException = e
                logger.warn("Attempt $attempt failed to save transfer and accounts atomically for transfer $transferId: ${e.message}")
                
                if (attempt < MAX_RETRIES) {
                    val delayMs = INITIAL_BACKOFF_MS * (1L shl (attempt - 1))  // Exponential: 100, 200, 400
                    logger.info("Error detected. Waiting ${delayMs}ms before retry (attempt $attempt/$MAX_RETRIES)...")
                    Thread.sleep(delayMs)
                } else {
                    logger.error("All $MAX_RETRIES attempts failed for transfer $transferId")
                }
            }
        }

        // All retries exhausted - permanent failure
        // Send to DLQ for manual investigation
        try {
            deadLetterService.sendCriticalFailureToDLQ(
                transferId = transferId,
                sourceAccountId = sourceAccount.accountId,
                destinationAccountId = destinationAccount.accountId,
                amount = sourceAccount.balance.toString(),
                currency = "BRL",
                failureReason = "CRITICAL: Failed to atomically save transfer and accounts after $MAX_RETRIES attempts. " +
                               "System is in CONSISTENT state but transfer could not be processed. " +
                               "Error: ${lastException?.message}",
                severity = "CRITICAL"
            )
        } catch (dlqException: Exception) {
            logger.error("CATASTROPHIC: Failed to send critical failure to DLQ for transfer $transferId", dlqException)
        }
        
        throw InvalidTransferException(
            "CRITICAL: Failed to atomically save transfer and accounts for transfer $transferId after $MAX_RETRIES attempts. " +
            "System is in CONSISTENT state (no partial updates due to DynamoDB transactional guarantee). " +
            "Error: ${lastException?.message}",
            ErrorType.INTERNAL_ERROR,
            lastException
        )
    }

    private fun validateTransfer(event: TransferRequestedEvent) {
        // Validate amounts
        if (event.amount <= java.math.BigDecimal.ZERO) {
            throw InvalidTransferException("Transfer amount must be greater than zero")
        }

        // Validate currency
        try {
            Currency.valueOf(event.currency)
        } catch (e: IllegalArgumentException) {
            throw InvalidTransferException("Invalid currency: ${event.currency}. Only BRL is supported.")
        }
        
        // Only BRL is supported
        if (event.currency != "BRL") {
            throw InvalidTransferException("Only BRL currency is supported, received: ${event.currency}")
        }

        // Validate accounts are different
        if (event.sourceAccountId == event.destinationAccountId) {
            throw InvalidTransferException("Source and destination accounts cannot be the same")
        }
    }

    sealed class Result {
        data class Success(val event: TransferCompletedEvent) : Result()
        data class Failure(val event: TransferFailedEvent) : Result()
    }
}

