package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.AccountDTO
import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountRepository: AccountRepository
) {

    @GetMapping
    fun getAllAccounts(): ResponseEntity<List<AccountDTO>> {
        val accounts = accountRepository.findAll()
        return ResponseEntity.ok(accounts.map { toDTO(it) })
    }

    @GetMapping("/{accountId}")
    fun getAccount(@PathVariable accountId: String): ResponseEntity<AccountDTO> {
        return accountRepository.findById(accountId)
            .map { ResponseEntity.ok(toDTO(it)) }
            .orElse(ResponseEntity.notFound().build())
    }

    @PostMapping
    fun createAccount(@RequestBody request: CreateAccountRequest): ResponseEntity<AccountDTO> {
        val account = Account(
            accountId = request.accountId,
            balance = request.balance,
            currency = Currency.BRL,
            status = AccountStatus.ACTIVE,
            customerName = request.customerName
        )
        val saved = accountRepository.save(account)
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(saved))
    }

    @PutMapping("/{accountId}")
    fun updateAccount(
        @PathVariable accountId: String,
        @RequestBody request: UpdateAccountRequest
    ): ResponseEntity<AccountDTO> {
        return accountRepository.findById(accountId)
            .map { account ->
                val status = if (request.status != null) {
                    try {
                        AccountStatus.valueOf(request.status)
                    } catch (e: IllegalArgumentException) {
                        account.status
                    }
                } else {
                    account.status
                }

                val updated = account.copy(
                    balance = request.balance ?: account.balance,
                    status = status,
                    customerName = request.customerName ?: account.customerName
                )
                val saved = accountRepository.save(updated)
                ResponseEntity.ok(toDTO(saved))
            }
            .orElse(ResponseEntity.notFound().build())
    }

    @DeleteMapping("/{accountId}")
    fun deleteAccount(@PathVariable accountId: String): ResponseEntity<Void> {
        return if (accountRepository.existsById(accountId)) {
            accountRepository.deleteById(accountId)
            ResponseEntity.noContent().build()
        } else {
            ResponseEntity.notFound().build()
        }
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

data class CreateAccountRequest(
    val accountId: String,
    val customerName: String,
    val balance: BigDecimal
)

data class UpdateAccountRequest(
    val customerName: String? = null,
    val balance: BigDecimal? = null,
    val status: String? = null
)
