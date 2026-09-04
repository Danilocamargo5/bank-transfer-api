package com.danilo.banktransfer.infrastructure.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageResponse

class DeadLetterServiceTest {
    
    private lateinit var sqsClient: SqsClient
    private lateinit var objectMapper: ObjectMapper
    private lateinit var deadLetterService: DeadLetterService
    
    private val dlqUrl = "http://localhost:4566/queue/transfer-failed-dlq"
    
    @BeforeEach
    fun setup() {
        sqsClient = mockk()
        objectMapper = ObjectMapper()
        deadLetterService = DeadLetterService(sqsClient, objectMapper, dlqUrl)
    }
    
    @Test
    fun `should send critical failure to DLQ successfully`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        
        // When
        deadLetterService.sendCriticalFailureToDLQ(
            transferId = "tf-001",
            sourceAccountId = "acc-123",
            destinationAccountId = "acc-456",
            amount = "100.00",
            currency = "BRL",
            failureReason = "Data inconsistency"
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should send critical failure with custom severity`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        
        // When
        deadLetterService.sendCriticalFailureToDLQ(
            transferId = "tf-002",
            sourceAccountId = "acc-123",
            destinationAccountId = "acc-456",
            amount = "200.00",
            currency = "BRL",
            failureReason = "Insufficient balance",
            severity = "HIGH"
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should throw when critical failure send fails`() {
        // Given
        every { sqsClient.sendMessage(any()) } throws RuntimeException("SQS error")
        
        // When & Then
        assertThrows<IllegalStateException> {
            deadLetterService.sendCriticalFailureToDLQ(
                transferId = "tf-003",
                sourceAccountId = "acc-123",
                destinationAccountId = "acc-456",
                amount = "100.00",
                currency = "BRL",
                failureReason = "Test failure"
            )
        }
    }
    
    @Test
    fun `should send Kafka failure to DLQ successfully`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        val exception = RuntimeException("Test exception")
        
        // When
        deadLetterService.sendKafkaFailureToDLQ(
            topic = "transfer-requested",
            partition = 0,
            offset = 123L,
            messageBody = "{\"transferId\":\"tf-001\"}",
            exception = exception
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should send Kafka failure with different topic`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        val exception = RuntimeException("Kafka processing failed")
        
        // When
        deadLetterService.sendKafkaFailureToDLQ(
            topic = "transfer-completed",
            partition = 1,
            offset = 456L,
            messageBody = "{\"transferId\":\"tf-002\"}",
            exception = exception
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should handle exception with null message in Kafka failure`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        val exception = RuntimeException()  // No message
        
        // When
        deadLetterService.sendKafkaFailureToDLQ(
            topic = "transfer-requested",
            partition = 0,
            offset = 789L,
            messageBody = "{}",
            exception = exception
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should throw when Kafka failure send fails`() {
        // Given
        every { sqsClient.sendMessage(any()) } throws RuntimeException("SQS error")
        val exception = RuntimeException("Test")
        
        // When & Then
        assertThrows<RuntimeException> {
            deadLetterService.sendKafkaFailureToDLQ(
                topic = "transfer-requested",
                partition = 0,
                offset = 123L,
                messageBody = "{}",
                exception = exception
            )
        }
    }
    
    @Test
    fun `should send multiple critical failures without interference`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        
        // When - send 2 different critical failures
        deadLetterService.sendCriticalFailureToDLQ(
            transferId = "tf-001",
            sourceAccountId = "acc-123",
            destinationAccountId = "acc-456",
            amount = "100.00",
            currency = "BRL",
            failureReason = "First failure"
        )
        
        deadLetterService.sendCriticalFailureToDLQ(
            transferId = "tf-002",
            sourceAccountId = "acc-789",
            destinationAccountId = "acc-999",
            amount = "200.00",
            currency = "BRL",
            failureReason = "Second failure"
        )
        
        // Then - verify both were sent
        verify(exactly = 2) { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should send multiple Kafka failures without interference`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        val exception = RuntimeException("Test")
        
        // When - send 2 different Kafka failures
        deadLetterService.sendKafkaFailureToDLQ(
            topic = "transfer-requested",
            partition = 0,
            offset = 100L,
            messageBody = "{}",
            exception = exception
        )
        
        deadLetterService.sendKafkaFailureToDLQ(
            topic = "transfer-completed",
            partition = 1,
            offset = 200L,
            messageBody = "{}",
            exception = exception
        )
        
        // Then - verify both were sent
        verify(exactly = 2) { sqsClient.sendMessage(any()) }
    }
}
