package com.danilo.banktransfer.infrastructure.mapper

import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.domain.model.Transfer
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TransferMapperTest {
    
    private val now = Instant.now()
    
    private val testTransfer = Transfer(
        transferId = "tf-001",
        sourceAccountId = "acc-123",
        destinationAccountId = "acc-456",
        amount = BigDecimal("100.00"),
        currency = Currency.BRL,
        status = TransferStatus.COMPLETED,
        requestedAt = now,
        completedAt = now.plusSeconds(60),
        failureReason = null,
        createdAt = now,
        updatedAt = now
    )
    
    @Test
    fun `should map transfer to DynamoDB item`() {
        // When
        val item = TransferMapper.toDynamoDBItem(testTransfer)
        
        // Then
        assertNotNull(item)
        assertEquals("tf-001", item["transferId"]?.s())
        assertEquals("acc-123", item["sourceAccountId"]?.s())
        assertEquals("acc-456", item["destinationAccountId"]?.s())
        assertEquals("100.00", item["amount"]?.n())
        assertEquals("BRL", item["currency"]?.s())
        assertEquals("COMPLETED", item["status"]?.s())
    }
    
    @Test
    fun `should include all fields in DynamoDB item`() {
        // When
        val item = TransferMapper.toDynamoDBItem(testTransfer)
        
        // Then
        assertTrue(item.containsKey("transferId"))
        assertTrue(item.containsKey("sourceAccountId"))
        assertTrue(item.containsKey("destinationAccountId"))
        assertTrue(item.containsKey("amount"))
        assertTrue(item.containsKey("currency"))
        assertTrue(item.containsKey("status"))
        assertTrue(item.containsKey("requestedAt"))
        assertTrue(item.containsKey("completedAt"))
        assertTrue(item.containsKey("failureReason"))
        assertTrue(item.containsKey("createdAt"))
        assertTrue(item.containsKey("updatedAt"))
    }
    
    @Test
    fun `should handle null completedAt in DynamoDB item`() {
        // Given
        val transfer = testTransfer.copy(completedAt = null)
        
        // When
        val item = TransferMapper.toDynamoDBItem(transfer)
        
        // Then
        assertTrue(item["completedAt"]?.nul() == true)
    }
    
    @Test
    fun `should handle null failureReason in DynamoDB item`() {
        // Given
        val transfer = testTransfer.copy(failureReason = null)
        
        // When
        val item = TransferMapper.toDynamoDBItem(transfer)
        
        // Then
        assertTrue(item["failureReason"]?.nul() == true)
    }
    
    @Test
    fun `should map different transfer statuses to DynamoDB`() {
        // When
        val pending = TransferMapper.toDynamoDBItem(testTransfer.copy(status = TransferStatus.PENDING))
        val failed = TransferMapper.toDynamoDBItem(testTransfer.copy(status = TransferStatus.FAILED))
        
        // Then
        assertEquals("PENDING", pending["status"]?.s())
        assertEquals("FAILED", failed["status"]?.s())
    }
    
    @Test
    fun `should map different amounts to DynamoDB`() {
        // When
        val item1 = TransferMapper.toDynamoDBItem(testTransfer.copy(amount = BigDecimal("50.00")))
        val item2 = TransferMapper.toDynamoDBItem(testTransfer.copy(amount = BigDecimal("999.99")))
        
        // Then
        assertEquals("50.00", item1["amount"]?.n())
        assertEquals("999.99", item2["amount"]?.n())
    }
    
    @Test
    fun `should map DynamoDB item to transfer`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "transferId" to AttributeValue.builder().s("tf-001").build(),
            "sourceAccountId" to AttributeValue.builder().s("acc-123").build(),
            "destinationAccountId" to AttributeValue.builder().s("acc-456").build(),
            "amount" to AttributeValue.builder().n("100.00").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("COMPLETED").build(),
            "requestedAt" to AttributeValue.builder().s(now.toString()).build(),
            "completedAt" to AttributeValue.builder().s(now.toString()).build(),
            "failureReason" to AttributeValue.builder().nul(true).build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val transfer = TransferMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals("tf-001", transfer.transferId)
        assertEquals("acc-123", transfer.sourceAccountId)
        assertEquals("acc-456", transfer.destinationAccountId)
        assertEquals(BigDecimal("100.00"), transfer.amount)
        assertEquals(Currency.BRL, transfer.currency)
        assertEquals(TransferStatus.COMPLETED, transfer.status)
    }
    
    @Test
    fun `should map all fields from DynamoDB item to transfer`() {
        // Given
        val now = Instant.now()
        val completedAt = now.plusSeconds(60)
        val item = mapOf(
            "transferId" to AttributeValue.builder().s("tf-002").build(),
            "sourceAccountId" to AttributeValue.builder().s("acc-111").build(),
            "destinationAccountId" to AttributeValue.builder().s("acc-222").build(),
            "amount" to AttributeValue.builder().n("250.50").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("PENDING").build(),
            "requestedAt" to AttributeValue.builder().s(now.toString()).build(),
            "completedAt" to AttributeValue.builder().s(completedAt.toString()).build(),
            "failureReason" to AttributeValue.builder().s("Test failure").build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val transfer = TransferMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals("tf-002", transfer.transferId)
        assertEquals("acc-111", transfer.sourceAccountId)
        assertEquals("acc-222", transfer.destinationAccountId)
        assertEquals(BigDecimal("250.50"), transfer.amount)
        assertEquals("Test failure", transfer.failureReason)
        assertEquals(completedAt, transfer.completedAt)
    }
    
    @Test
    fun `should handle null completedAt when mapping from DynamoDB`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "transferId" to AttributeValue.builder().s("tf-001").build(),
            "sourceAccountId" to AttributeValue.builder().s("acc-123").build(),
            "destinationAccountId" to AttributeValue.builder().s("acc-456").build(),
            "amount" to AttributeValue.builder().n("100.00").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("PENDING").build(),
            "requestedAt" to AttributeValue.builder().s(now.toString()).build(),
            "completedAt" to AttributeValue.builder().nul(true).build(),
            "failureReason" to AttributeValue.builder().nul(true).build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val transfer = TransferMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals(null, transfer.completedAt)
        assertEquals(null, transfer.failureReason)
    }
    
    @Test
    fun `should handle null failureReason when mapping from DynamoDB`() {
        // Given
        val now = Instant.now()
        val item = mapOf(
            "transferId" to AttributeValue.builder().s("tf-001").build(),
            "sourceAccountId" to AttributeValue.builder().s("acc-123").build(),
            "destinationAccountId" to AttributeValue.builder().s("acc-456").build(),
            "amount" to AttributeValue.builder().n("100.00").build(),
            "currency" to AttributeValue.builder().s("BRL").build(),
            "status" to AttributeValue.builder().s("COMPLETED").build(),
            "requestedAt" to AttributeValue.builder().s(now.toString()).build(),
            "completedAt" to AttributeValue.builder().s(now.toString()).build(),
            "failureReason" to AttributeValue.builder().nul(true).build(),
            "createdAt" to AttributeValue.builder().s(now.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(now.toString()).build()
        )
        
        // When
        val transfer = TransferMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals(null, transfer.failureReason)
    }
    
    @Test
    fun `should roundtrip transfer through DynamoDB mapping`() {
        // When
        val item = TransferMapper.toDynamoDBItem(testTransfer)
        val reconstructed = TransferMapper.fromDynamoDBItem(item)
        
        // Then
        assertEquals(testTransfer.transferId, reconstructed.transferId)
        assertEquals(testTransfer.sourceAccountId, reconstructed.sourceAccountId)
        assertEquals(testTransfer.destinationAccountId, reconstructed.destinationAccountId)
        assertEquals(testTransfer.amount, reconstructed.amount)
        assertEquals(testTransfer.currency, reconstructed.currency)
        assertEquals(testTransfer.status, reconstructed.status)
    }
    
    @Test
    fun `should throw when transferId is missing from DynamoDB item`() {
        // Given
        val item = mapOf(
            "sourceAccountId" to AttributeValue.builder().s("acc-123").build()
        )
        
        // When & Then
        try {
            TransferMapper.fromDynamoDBItem(item)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Missing transferId", e.message)
        }
    }
    
    @Test
    fun `should throw when sourceAccountId is missing from DynamoDB item`() {
        // Given
        val item = mapOf(
            "transferId" to AttributeValue.builder().s("tf-001").build()
        )
        
        // When & Then
        try {
            TransferMapper.fromDynamoDBItem(item)
            throw AssertionError("Should have thrown IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertEquals("Missing sourceAccountId", e.message)
        }
    }
}
