package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.infrastructure.repository.AccountRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
        currency = Currency.BRL,
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
    
    @Test
    fun `should get all accounts successfully`() {
        // Given
        val account1 = testAccount.copy(accountId = "acc-001")
        val account2 = testAccount.copy(accountId = "acc-002", balance = BigDecimal("3000.00"))
        every { accountRepository.findAll() } returns listOf(account1, account2)
        
        // When
        val response = accountController.getAllAccounts()
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(2, response.body?.size)
        assertEquals("acc-001", response.body?.get(0)?.accountId)
        assertEquals("acc-002", response.body?.get(1)?.accountId)
    }
    
    @Test
    fun `should return empty list when no accounts exist`() {
        // Given
        every { accountRepository.findAll() } returns emptyList()
        
        // When
        val response = accountController.getAllAccounts()
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(0, response.body?.size)
    }
    
    @Test
    fun `should create account successfully`() {
        // Given
        val request = CreateAccountRequest(
            accountId = "acc-new",
            customerName = "New User",
            balance = BigDecimal("1000.00")
        )
        val newAccount = testAccount.copy(
            accountId = "acc-new",
            customerName = "New User",
            balance = BigDecimal("1000.00")
        )
        every { accountRepository.save(any()) } returns newAccount
        
        // When
        val response = accountController.createAccount(request)
        
        // Then
        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertNotNull(response.body)
        assertEquals("acc-new", response.body?.accountId)
        assertEquals("New User", response.body?.customerName)
        assertEquals(BigDecimal("1000.00"), response.body?.balance)
    }
    
    @Test
    fun `should update account successfully`() {
        // Given
        val updateRequest = UpdateAccountRequest(
            customerName = "Updated Name",
            balance = BigDecimal("7000.00"),
            status = "ACTIVE"
        )
        val updatedAccount = testAccount.copy(
            customerName = "Updated Name",
            balance = BigDecimal("7000.00")
        )
        every { accountRepository.findById("acc-001") } returns Optional.of(testAccount)
        every { accountRepository.save(any()) } returns updatedAccount
        
        // When
        val response = accountController.updateAccount("acc-001", updateRequest)
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals("Updated Name", response.body?.customerName)
        assertEquals(BigDecimal("7000.00"), response.body?.balance)
    }
    
    @Test
    fun `should return 404 when updating non-existent account`() {
        // Given
        val updateRequest = UpdateAccountRequest(
            customerName = "Updated Name"
        )
        every { accountRepository.findById("acc-999") } returns Optional.empty()
        
        // When
        val response = accountController.updateAccount("acc-999", updateRequest)
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
    
    @Test
    fun `should update only balance when provided`() {
        // Given
        val updateRequest = UpdateAccountRequest(
            balance = BigDecimal("9999.00"),
            customerName = null,
            status = null
        )
        val updatedAccount = testAccount.copy(balance = BigDecimal("9999.00"))
        every { accountRepository.findById("acc-001") } returns Optional.of(testAccount)
        every { accountRepository.save(any()) } returns updatedAccount
        
        // When
        val response = accountController.updateAccount("acc-001", updateRequest)
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(BigDecimal("9999.00"), response.body?.balance)
        assertEquals("Test User", response.body?.customerName)
    }
    
    @Test
    fun `should change account status to INACTIVE`() {
        // Given
        val updateRequest = UpdateAccountRequest(
            status = "INACTIVE"
        )
        val inactiveAccount = testAccount.copy(status = AccountStatus.INACTIVE)
        every { accountRepository.findById("acc-001") } returns Optional.of(testAccount)
        every { accountRepository.save(any()) } returns inactiveAccount
        
        // When
        val response = accountController.updateAccount("acc-001", updateRequest)
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(AccountStatus.INACTIVE.name, response.body?.status)
    }
    
    @Test
    fun `should ignore invalid status and keep current status`() {
        // Given
        val updateRequest = UpdateAccountRequest(
            status = "INVALID_STATUS"
        )
        every { accountRepository.findById("acc-001") } returns Optional.of(testAccount)
        every { accountRepository.save(any()) } returns testAccount
        
        // When
        val response = accountController.updateAccount("acc-001", updateRequest)
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(AccountStatus.ACTIVE.name, response.body?.status)
    }
    
    @Test
    fun `should delete account successfully`() {
        // Given
        every { accountRepository.existsById("acc-001") } returns true
        every { accountRepository.deleteById("acc-001") } returns Unit
        
        // When
        val response = accountController.deleteAccount("acc-001")
        
        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify { accountRepository.deleteById("acc-001") }
    }
    
    @Test
    fun `should return 404 when deleting non-existent account`() {
        // Given
        every { accountRepository.existsById("acc-999") } returns false
        
        // When
        val response = accountController.deleteAccount("acc-999")
        
        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
    
    @Test
    fun `should convert account to DTO with correct currency`() {
        // Given
        val account = testAccount.copy(currency = Currency.BRL)
        every { accountRepository.findById("acc-001") } returns Optional.of(account)
        
        // When
        val response = accountController.getAccount("acc-001")
        
        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals("BRL", response.body?.currency)
    }
}
