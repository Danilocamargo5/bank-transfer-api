package com.danilo.banktransfer.infrastructure.mapper

import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.model.Account
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccountMapperTest {
    
    private val now = Instant.now()
    
    private val testAccount = Account(
        accountId = "acc-001",
        balance = BigDecimal("5000.00"),
        currency = Currency.BRL,
        status = AccountStatus.ACTIVE,
        customerName = "João Silva",
        createdAt = now,
        updatedAt = now
    )
    
    @Test
    fun `should map account to DynamoDB item`() {
        // When
        val item = AccountMapper.toDynamoDBItem(testAccount)
        
        // Then
        assertNotNull(item)
        assertEquals("acc-001", item["accountId"]?.s())
        assertEquals("5000.00", item["balance"]?.n())
        assertEquals("BRL", item["currency"]?.s())
        assertEquals("ACTIVE", item["status"]?.s())
        assertEquals("João Silva", item["customerName"]?.s())
    }
    
    @Test
    fun `should include all fields in DynamoDB item`() {
        // When
        val item = AccountMapper.toDynamoDBItem(testAccount)
        
        // Then
        assertTrue(item.containsKey("accountId"))
        assertTrue(item.containsKey("balance"))
        assertTrue(item.containsKey("currency"))
        assertTrue(item.containsKey("status"))
        assertTrue(item.containsKey("customerName"))
        assertTrue(item.containsKey("createdAt"))
        assertTrue(item.containsKey("updatedAt"))
    }
    
    @Test
    fun `should map different account statuses to DynamoDB`() {
        // When
        val active = AccountMapper.toDynamoDBItem(testAccount.copy(status = AccountStatus.ACTIVE))
        val inactive = AccountMapper.toDynamoDBItem(testAccount.copy(status = AccountStatus.INACTIVE))
        
        // Then
        assertEquals("ACTIVE", active["status"]?.s())
        assertEquals("INACTIVE", inactive["status"]?.s())
    }
    
    @Test
    fun `should map different balances to DynamoDB`() {
        // When
        val item1 = AccountMapper.toDynamoDBItem(testAccount.copy(balance = BigDecimal("100.00")))
        val item2 = AccountMapper.toDynamoDBItem(testAccount.copy(balance = BigDecimal("9999.99")))
        
        // Then
        assertEquals("100.00", item1["balance"]?.n())
        assertEquals("9999.99", item2["balance"]?.n())
    }
    
    @Test
    fun `should map different customer names to DynamoDB`() {
        // When
        val item1 = AccountMapper.toDynamoDBItem(testAccount.copy(customerName = "Maria Santos"))
        val item2 = AccountMapper.toDynamoDBItem(testAccount.copy(customerName = "Pedro Costa"))
        
        // Then
        assertEquals("Maria Santos", item1["customerName"]?.s())
        assertEquals("Pedro Costa", item2["customerName"]?.s())
    }
    
    @Test
    fun `should map DynamoDB item to account`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "accountId" to AttributeValue.builder().s("acc-001").build(),
            "balance" to AttributeValue.builder().n("5000.00").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("ACTIVE").build(),
            "customerName" to AttributeValue.builder().s("João Silva").build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val account = AccountMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals("acc-001", account.accountId)
        assertEquals(BigDecimal("5000.00"), account.balance)
        assertEquals(Currency.BRL, account.currency)
        assertEquals(AccountStatus.ACTIVE, account.status)
        assertEquals("João Silva", account.customerName)
    }
    
    @Test
    fun `should map all fields from DynamoDB item to account`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "accountId" to AttributeValue.builder().s("acc-002").build(),
            "balance" to AttributeValue.builder().n("10000.50").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("INACTIVE").build(),
            "customerName" to AttributeValue.builder().s("Maria Santos").build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val account = AccountMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals("acc-002", account.accountId)
        assertEquals(BigDecimal("10000.50"), account.balance)
        assertEquals(Currency.BRL, account.currency)
        assertEquals(AccountStatus.INACTIVE, account.status)
        assertEquals("Maria Santos", account.customerName)
    }
    
    @Test
    fun `should handle missing balance and use default zero`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "accountId" to AttributeValue.builder().s("acc-001").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("ACTIVE").build(),
            "customerName" to AttributeValue.builder().s("Test User").build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val account = AccountMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals(BigDecimal("0"), account.balance)
    }
    
    @Test
    fun `should handle missing currency and use default BRL`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "accountId" to AttributeValue.builder().s("acc-001").build(),
            "balance" to AttributeValue.builder().n("5000.00").build(),
            "status" to AttributeValue.builder().s("ACTIVE").build(),
            "customerName" to AttributeValue.builder().s("Test User").build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val account = AccountMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals(Currency.BRL, account.currency)
    }
    
    @Test
    fun `should handle missing status and use default ACTIVE`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "accountId" to AttributeValue.builder().s("acc-001").build(),
            "balance" to AttributeValue.builder().n("5000.00").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "customerName" to AttributeValue.builder().s("Test User").build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val account = AccountMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals(AccountStatus.ACTIVE, account.status)
    }
    
    @Test
    fun `should handle missing customerName and use empty string`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "accountId" to AttributeValue.builder().s("acc-001").build(),
            "balance" to AttributeValue.builder().n("5000.00").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("ACTIVE").build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val account = AccountMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals("", account.customerName)
    }
    
    @Test
    fun `should roundtrip account through DynamoDB mapping`() {
        // When
        val item = AccountMapper.toDynamoDBItem(testAccount)
        val reconstructed = AccountMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals(testAccount.accountId, reconstructed.accountId)
        assertEquals(testAccount.balance, reconstructed.balance)
        assertEquals(testAccount.currency, reconstructed.currency)
        assertEquals(testAccount.status, reconstructed.status)
        assertEquals(testAccount.customerName, reconstructed.customerName)
    }
    
    @Test
    fun `should throw when accountId is missing from DynamoDB item`() {
        // Given
        val item = mapOf(
            "balance" to AttributeValue.builder().n("5000.00").build()
        )
        
        // When & Then
        try {
            AccountMapper.fromDynamoDBItem(item)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Missing accountId", e.message)
        }
    }
}
