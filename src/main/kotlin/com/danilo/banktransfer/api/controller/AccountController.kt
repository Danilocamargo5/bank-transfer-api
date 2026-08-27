package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.AccountDTO
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/accounts")
class AccountController(
    private val accountRepository: AccountRepository
) {

    @GetMapping("/{accountId}")
    fun getAccount(@PathVariable accountId: String): ResponseEntity<AccountDTO> {
        return accountRepository.findById(accountId)
            .map { ResponseEntity.ok(toDTO(it)) }
            .orElse(ResponseEntity.notFound().build())
    }

    private fun toDTO(account: com.danilo.banktransfer.domain.model.Account): AccountDTO {
        return AccountDTO(
            accountId = account.accountId,
            balance = account.balance,
            currency = account.currency.name,
            status = account.status.name,
            customerName = account.customerName
        )
    }
}
