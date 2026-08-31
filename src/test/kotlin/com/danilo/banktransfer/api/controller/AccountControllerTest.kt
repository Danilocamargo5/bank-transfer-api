package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AccountControllerTest {
    
    private lateinit var accountRepository: AccountRepository
    private lateinit var accountController: AccountController
    
    private val testAccount = Account(
        accountId = "acc-001",
        balance = BigDecimal("5000.00"),
        currency = "BRL",
        status = AccountStatus.ACTIVE,
        customerName = "Test User",
        createdAt = Instant.now()
    )
    
    @BeforeEach
    fun setup() {
        accountRepository = mockk()
        accountController = AccountController(accountRepository)
    }
    
    @Test
    fun `should fetch account by id`() {
        // Given
        every { accountRepository.findById("acc-001") } returns Optional.of(testAccount)
        
        // When
        val response = accountController.getAccount("acc-001")
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("acc-001", response.body?.accountId)
        assertEquals(BigDecimal("5000.00"), response.body?.balance)
    }
    
    @Test
    fun `should return 404 when account not found`() {
        // Given
        every { accountRepository.findById("acc-999") } returns Optional.empty()
        
        // When
        val response = accountController.getAccount("acc-999")
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
    
    @Test
    fun `should return account with correct status`() {
        // Given
        val activeAccount = testAccount.copy(status = AccountStatus.ACTIVE)
        every { accountRepository.findById("acc-001") } returns Optional.of(activeAccount)
        
        // When
        val response = accountController.getAccount("acc-001")
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(AccountStatus.ACTIVE.name, response.body?.status)
    }
}
