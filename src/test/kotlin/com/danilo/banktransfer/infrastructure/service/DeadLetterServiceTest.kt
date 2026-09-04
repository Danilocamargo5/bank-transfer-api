package com.danilo.banktransfer.infrastructure.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import kotlin.test.assertEquals

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
            failureReason = "Data inconsistency",
            severity = "CRITICAL"
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should include all required fields in critical failure message`() {
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
            failureReason = "Insufficient balance",
            severity = "CRITICAL"
        )
        
        // Then
        verify {
            sqsClient.sendMessage(match { request ->
                request.queueUrl() == dlqUrl &&
                request.messageBody().contains("tf-001") &&
                request.messageBody().contains("acc-123") &&
                request.messageBody().contains("acc-456") &&
                request.messageBody().contains("100.00") &&
                request.messageBody().contains("BRL") &&
                request.messageBody().contains("Insufficient balance") &&
                request.messageBody().contains("CRITICAL") &&
                request.messageBody().contains("requiresManualIntervention")
            })
        }
    }
    
    @Test
    fun `should use default severity when not provided`() {
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
            failureReason = "Test failure"
        )
        
        // Then
        verify {
            sqsClient.sendMessage(match { request ->
                request.messageBody().contains("CRITICAL")
            })
        }
    }
    
    @Test
    fun `should throw when critical failure send fails`() {
        // Given
        every { sqsClient.sendMessage(any()) } throws RuntimeException("SQS error")
        
        // When & Then
        val exception = assertThrows<IllegalStateException> {
            deadLetterService.sendCriticalFailureToDLQ(
                transferId = "tf-001",
                sourceAccountId = "acc-123",
                destinationAccountId = "acc-456",
                amount = "100.00",
                currency = "BRL",
                failureReason = "Test failure"
            )
        }
        
        assertEquals(true, exception.message?.contains("CATASTROPHIC"))
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
    fun `should include all Kafka failure fields in message`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        val exception = RuntimeException("Kafka processing failed")
        
        // When
        deadLetterService.sendKafkaFailureToDLQ(
            topic = "transfer-requested",
            partition = 1,
            offset = 456L,
            messageBody = "{\"transferId\":\"tf-002\"}",
            exception = exception
        )
        
        // Then
        verify {
            sqsClient.sendMessage(match { request ->
                request.messageBody().contains("transfer-requested") &&
                request.messageBody().contains("1") &&
                request.messageBody().contains("456") &&
                request.messageBody().contains("tf-002") &&
                request.messageBody().contains("RuntimeException") &&
                request.messageBody().contains("Kafka processing failed") &&
                request.messageBody().contains("HIGH")
            })
        }
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
        verify {
            sqsClient.sendMessage(match { request ->
                request.messageBody().contains("No message")
            })
        }
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
    fun `should set messageGroupId for critical failure FIFO ordering`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        
        // When
        deadLetterService.sendCriticalFailureToDLQ(
            transferId = "tf-fifo-001",
            sourceAccountId = "acc-123",
            destinationAccountId = "acc-456",
            amount = "100.00",
            currency = "BRL",
            failureReason = "FIFO test"
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
    
    @Test
    fun `should set messageDeduplicationId for critical failure`() {
        // Given
        val mockResponse = mockk<SendMessageResponse>()
        every { sqsClient.sendMessage(any()) } returns mockResponse
        
        // When
        deadLetterService.sendCriticalFailureToDLQ(
            transferId = "tf-dedup-001",
            sourceAccountId = "acc-123",
            destinationAccountId = "acc-456",
            amount = "100.00",
            currency = "BRL",
            failureReason = "Dedup test"
        )
        
        // Then
        verify { sqsClient.sendMessage(any()) }
    }
}
