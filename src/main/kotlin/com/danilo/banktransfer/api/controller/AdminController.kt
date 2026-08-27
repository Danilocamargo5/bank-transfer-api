package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.AccountDTO
import com.danilo.banktransfer.domain.dto.CreateAccountRequest
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/admin/accounts")
class AdminController(
    private val accountRepository: AccountRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostMapping
    fun createAccount(@RequestBody request: CreateAccountRequest): ResponseEntity<AccountDTO> {
        logger.info("Creating account: ${request.accountId}")
        
        val account = Account(
            accountId = request.accountId,
            balance = request.balance,
            currency = Currency.valueOf(request.currency),
            status = AccountStatus.ACTIVE,
            customerName = request.customerName,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = accountRepository.save(account)
        logger.info("Account created: ${request.accountId}")

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDTO(saved))
    }

    @GetMapping("/{accountId}")
    fun getAccount(@PathVariable accountId: String): ResponseEntity<AccountDTO> {
        logger.info("Fetching account: $accountId")
        
        return accountRepository.findById(accountId)
            .map { ResponseEntity.ok(toDTO(it)) }
            .orElse(ResponseEntity.notFound().build())
    }

    private fun toDTO(account: Account): AccountDTO {
        return AccountDTO(
            accountId = account.accountId,
            balance = account.balance,
            currency = account.currency.name,
            status = account.status.name,
            customerName = account.customerName
        )
    }
}
