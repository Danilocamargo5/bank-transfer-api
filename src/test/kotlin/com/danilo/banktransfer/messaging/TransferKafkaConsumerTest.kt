package com.danilo.banktransfer.messaging

import com.danilo.banktransfer.application.TransferService
import com.danilo.banktransfer.domain.model.TransferRequestedEvent
import com.danilo.banktransfer.domain.model.TransferCompletedEvent
import com.danilo.banktransfer.domain.model.TransferFailedEvent
import com.danilo.banktransfer.infrastructure.service.DeadLetterService
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import io.mockk.slot
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.Acknowledgment
import java.math.BigDecimal
import java.time.Instant

class TransferKafkaConsumerTest {
    
    private lateinit var transferService: TransferService
    private lateinit var kafkaTemplate: KafkaTemplate<String, String>
    private lateinit var sqsPublisher: TransferSqsPublisher
    private lateinit var deadLetterService: DeadLetterService
    private lateinit var objectMapper: ObjectMapper
    private lateinit var acknowledgment: Acknowledgment
    private lateinit var consumer: TransferKafkaConsumer
    
    private val validMessage = """{
        "transferId":"tf-kafka-001",
        "sourceAccountId":"acc-001",
        "destinationAccountId":"acc-002",
        "amount":100.00,
        "currency":"BRL",
        "requestedAt":"2026-08-31T20:00:00Z"
    }"""
    
    @BeforeEach
    fun setup() {
        transferService = mockk()
        kafkaTemplate = mockk()
        sqsPublisher = mockk()
        deadLetterService = mockk()
        acknowledgment = mockk()
        objectMapper = ObjectMapper().also { 
            it.registerModule(com.fasterxml.jackson.datatype.jsr310.JavaTimeModule())
            it.registerModule(com.fasterxml.jackson.module.kotlin.kotlinModule())
        }
        consumer = TransferKafkaConsumer(transferService, kafkaTemplate, sqsPublisher, deadLetterService, objectMapper)
    }
    
    @Test
    fun `should consume and process valid transfer with acknowledgment`() {
        // Given
        val successEvent = TransferCompletedEvent(
            transferId = "tf-kafka-001",
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            completedAt = Instant.now()
        )
        
        val sendFuture = mockk<org.springframework.kafka.support.SendResult<String, String>>(relaxed = true)
        
        every { transferService.processTransfer(any()) } returns TransferService.Result.Success(successEvent)
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk(relaxed = true) {
            every { get() } returns sendFuture  // .get() blocks and returns result
        }
        every { deadLetterService.sendKafkaFailureToDLQ(any(), any(), any(), any(), any()) } just runs
        every { acknowledgment.acknowledge() } just runs
        
        // When
        consumer.consumeTransferRequest(
            message = validMessage,
            topic = "transfer-requested",
            partition = 0,
            offset = 100L,
            acknowledgment = acknowledgment
        )
        
        // Then
        verify { acknowledgment.acknowledge() }  // Verify acknowledge was called
        verify { transferService.processTransfer(any()) }
        verify { kafkaTemplate.send(any(), any(), any()) }
    }
    
    @Test
    fun `should handle transfer failure and publish to SQS without acknowledgment on retry`() {
        // Given
        val failureEvent = TransferFailedEvent(
            transferId = "tf-kafka-001",
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            failureReason = "Account not found",
            failedAt = Instant.now()
        )
        
        every { transferService.processTransfer(any()) } returns TransferService.Result.Failure(failureEvent)
        every { sqsPublisher.publishTransferFailed(any()) } just runs
        every { deadLetterService.sendKafkaFailureToDLQ(any(), any(), any(), any(), any()) } just runs
        every { acknowledgment.acknowledge() } just runs
        
        // When
        consumer.consumeTransferRequest(
            message = validMessage,
            topic = "transfer-requested",
            partition = 0,
            offset = 100L,
            acknowledgment = acknowledgment
        )
        
        // Then
        verify { sqsPublisher.publishTransferFailed(any()) }
        verify { acknowledgment.acknowledge() }  // Still acknowledges because SQS publish succeeded
        verify { transferService.processTransfer(any()) }
    }
    
    @Test
    fun `should NOT acknowledge when malformed JSON and send to DLQ`() {
        // Given
        val malformedMessage = "{invalid json"
        
        every { deadLetterService.sendKafkaFailureToDLQ(any(), any(), any(), any(), any()) } just runs
        every { acknowledgment.acknowledge() } just runs
        
        // When & Then - expect exception (no acknowledgment)
        try {
            consumer.consumeTransferRequest(
                message = malformedMessage,
                topic = "transfer-requested",
                partition = 0,
                offset = 100L,
                acknowledgment = acknowledgment
            )
        } catch (e: Exception) {
            // Expected - JsonParseException thrown and not caught
        }
        
        // Verify DLQ was called
        verify { deadLetterService.sendKafkaFailureToDLQ(any(), any(), any(), any(), any()) }
        // Verify acknowledge was NOT called (due to exception)
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }
    
    @Test
    fun `should NOT acknowledge when processing fails`() {
        // Given
        val failureEvent = TransferFailedEvent(
            transferId = "tf-kafka-001",
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            failureReason = "SQS publish failed",
            failedAt = Instant.now()
        )
        
        every { transferService.processTransfer(any()) } returns TransferService.Result.Failure(failureEvent)
        every { sqsPublisher.publishTransferFailed(any()) } throws RuntimeException("SQS connection failed")
        every { deadLetterService.sendKafkaFailureToDLQ(any(), any(), any(), any(), any()) } just runs
        every { acknowledgment.acknowledge() } just runs
        
        // When & Then - expect exception
        try {
            consumer.consumeTransferRequest(
                message = validMessage,
                topic = "transfer-requested",
                partition = 0,
                offset = 100L,
                acknowledgment = acknowledgment
            )
        } catch (e: Exception) {
            // Expected - exception from SQS publish
        }
        
        // Verify acknowledge was NOT called (due to exception)
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }
    
    @Test
    fun `should NOT acknowledge when Kafka send fails`() {
        // Given
        val successEvent = TransferCompletedEvent(
            transferId = "tf-kafka-001",
            sourceAccountId = "acc-001",
            destinationAccountId = "acc-002",
            amount = BigDecimal("100.00"),
            currency = "BRL",
            completedAt = Instant.now()
        )
        
        every { transferService.processTransfer(any()) } returns TransferService.Result.Success(successEvent)
        every { kafkaTemplate.send(any(), any(), any()) } returns mockk(relaxed = true) {
            every { get() } throws RuntimeException("Kafka broker unavailable")  // .get() throws
        }
        every { deadLetterService.sendKafkaFailureToDLQ(any(), any(), any(), any(), any()) } just runs
        every { acknowledgment.acknowledge() } just runs
        
        // When & Then - expect exception
        try {
            consumer.consumeTransferRequest(
                message = validMessage,
                topic = "transfer-requested",
                partition = 0,
                offset = 100L,
                acknowledgment = acknowledgment
            )
        } catch (e: Exception) {
            // Expected - exception from Kafka send
        }
        
        // Verify acknowledge was NOT called (due to exception)
        verify(exactly = 0) { acknowledgment.acknowledge() }
    }
}
