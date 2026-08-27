package com.danilo.banktransfer.application.exception

open class TransferException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InsufficientBalanceException(message: String) : TransferException(message)

class AccountNotFoundException(message: String) : TransferException(message)

class InactiveAccountException(message: String) : TransferException(message)

class InvalidTransferException(message: String) : TransferException(message)

class DuplicateTransferException(message: String) : TransferException(message)

class TransferProcessingException(message: String, cause: Throwable? = null) : TransferException(message, cause)
