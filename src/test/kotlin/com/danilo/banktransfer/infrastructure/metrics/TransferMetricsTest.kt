package com.danilo.banktransfer.infrastructure.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit

class TransferMetricsTest {
    
    private lateinit var meterRegistry: MeterRegistry
    private lateinit var transferMetrics: TransferMetrics
    private lateinit var mockTimer: Timer
    private lateinit var mockCounter: Counter
    
    @BeforeEach
    fun setup() {
        meterRegistry = mockk()
        mockTimer = mockk()
        mockCounter = mockk()
        transferMetrics = TransferMetrics(meterRegistry)
    }
    
    @Test
    fun `should record transfer processing time in milliseconds`() {
        // Given
        every { meterRegistry.timer("transfer.processing.time") } returns mockTimer
        every { mockTimer.record(any<Long>(), any<TimeUnit>()) } returns Unit
        
        // When
        transferMetrics.recordTransferProcessingTime(150L)
        
        // Then
        verify { meterRegistry.timer("transfer.processing.time") }
        verify { mockTimer.record(150L, TimeUnit.MILLISECONDS) }
    }
    
    @Test
    fun `should record transfer processing time with different durations`() {
        // Given
        every { meterRegistry.timer("transfer.processing.time") } returns mockTimer
        every { mockTimer.record(any<Long>(), any<TimeUnit>()) } returns Unit
        
        // When
        transferMetrics.recordTransferProcessingTime(250L)
        transferMetrics.recordTransferProcessingTime(500L)
        
        // Then
        verify { mockTimer.record(250L, TimeUnit.MILLISECONDS) }
        verify { mockTimer.record(500L, TimeUnit.MILLISECONDS) }
    }
    
    @Test
    fun `should record transfer success`() {
        // Given
        every { meterRegistry.counter("transfer.success.total") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordTransferSuccess()
        
        // Then
        verify { meterRegistry.counter("transfer.success.total") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record multiple transfer successes`() {
        // Given
        every { meterRegistry.counter("transfer.success.total") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordTransferSuccess()
        transferMetrics.recordTransferSuccess()
        transferMetrics.recordTransferSuccess()
        
        // Then
        verify(exactly = 3) { mockCounter.increment() }
    }
    
    @Test
    fun `should record transfer failure with error type`() {
        // Given
        every { meterRegistry.counter("transfer.failure.total", "error_type", "INSUFFICIENT_BALANCE") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordTransferFailure("INSUFFICIENT_BALANCE")
        
        // Then
        verify { meterRegistry.counter("transfer.failure.total", "error_type", "INSUFFICIENT_BALANCE") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record different failure types`() {
        // Given
        val mockCounterBalance = mockk<Counter>()
        val mockCounterNotFound = mockk<Counter>()
        every { meterRegistry.counter("transfer.failure.total", "error_type", "INSUFFICIENT_BALANCE") } returns mockCounterBalance
        every { meterRegistry.counter("transfer.failure.total", "error_type", "ACCOUNT_NOT_FOUND") } returns mockCounterNotFound
        every { mockCounterBalance.increment() } returns Unit
        every { mockCounterNotFound.increment() } returns Unit
        
        // When
        transferMetrics.recordTransferFailure("INSUFFICIENT_BALANCE")
        transferMetrics.recordTransferFailure("ACCOUNT_NOT_FOUND")
        
        // Then
        verify { mockCounterBalance.increment() }
        verify { mockCounterNotFound.increment() }
    }
    
    @Test
    fun `should record account fetch success`() {
        // Given
        every { meterRegistry.counter("account.fetch", "status", "success") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordAccountFetch("acc-001", success = true)
        
        // Then
        verify { meterRegistry.counter("account.fetch", "status", "success") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record account fetch not found`() {
        // Given
        every { meterRegistry.counter("account.fetch", "status", "not_found") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordAccountFetch("acc-999", success = false)
        
        // Then
        verify { meterRegistry.counter("account.fetch", "status", "not_found") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record multiple account fetches with different results`() {
        // Given
        val mockCounterSuccess = mockk<Counter>()
        val mockCounterNotFound = mockk<Counter>()
        every { meterRegistry.counter("account.fetch", "status", "success") } returns mockCounterSuccess
        every { meterRegistry.counter("account.fetch", "status", "not_found") } returns mockCounterNotFound
        every { mockCounterSuccess.increment() } returns Unit
        every { mockCounterNotFound.increment() } returns Unit
        
        // When
        transferMetrics.recordAccountFetch("acc-001", success = true)
        transferMetrics.recordAccountFetch("acc-002", success = true)
        transferMetrics.recordAccountFetch("acc-999", success = false)
        
        // Then
        verify(exactly = 2) { mockCounterSuccess.increment() }
        verify { mockCounterNotFound.increment() }
    }
    
    @Test
    fun `should record Kafka publish success`() {
        // Given
        every { meterRegistry.counter("kafka.publish", "topic", "transfer-requested", "status", "success") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordKafkaPublish("transfer-requested", success = true)
        
        // Then
        verify { meterRegistry.counter("kafka.publish", "topic", "transfer-requested", "status", "success") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record Kafka publish failure`() {
        // Given
        every { meterRegistry.counter("kafka.publish", "topic", "transfer-requested", "status", "failure") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordKafkaPublish("transfer-requested", success = false)
        
        // Then
        verify { meterRegistry.counter("kafka.publish", "topic", "transfer-requested", "status", "failure") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record Kafka publish to different topics`() {
        // Given
        val mockCounterRequested = mockk<Counter>()
        val mockCounterCompleted = mockk<Counter>()
        every { meterRegistry.counter("kafka.publish", "topic", "transfer-requested", "status", "success") } returns mockCounterRequested
        every { meterRegistry.counter("kafka.publish", "topic", "transfer-completed", "status", "success") } returns mockCounterCompleted
        every { mockCounterRequested.increment() } returns Unit
        every { mockCounterCompleted.increment() } returns Unit
        
        // When
        transferMetrics.recordKafkaPublish("transfer-requested", success = true)
        transferMetrics.recordKafkaPublish("transfer-completed", success = true)
        
        // Then
        verify { mockCounterRequested.increment() }
        verify { mockCounterCompleted.increment() }
    }
    
    @Test
    fun `should record SQS publish success`() {
        // Given
        every { meterRegistry.counter("sqs.publish", "status", "success") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordSQSPublish(success = true)
        
        // Then
        verify { meterRegistry.counter("sqs.publish", "status", "success") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record SQS publish failure`() {
        // Given
        every { meterRegistry.counter("sqs.publish", "status", "failure") } returns mockCounter
        every { mockCounter.increment() } returns Unit
        
        // When
        transferMetrics.recordSQSPublish(success = false)
        
        // Then
        verify { meterRegistry.counter("sqs.publish", "status", "failure") }
        verify { mockCounter.increment() }
    }
    
    @Test
    fun `should record multiple SQS publishes with different results`() {
        // Given
        val mockCounterSuccess = mockk<Counter>()
        val mockCounterFailure = mockk<Counter>()
        every { meterRegistry.counter("sqs.publish", "status", "success") } returns mockCounterSuccess
        every { meterRegistry.counter("sqs.publish", "status", "failure") } returns mockCounterFailure
        every { mockCounterSuccess.increment() } returns Unit
        every { mockCounterFailure.increment() } returns Unit
        
        // When
        transferMetrics.recordSQSPublish(success = true)
        transferMetrics.recordSQSPublish(success = true)
        transferMetrics.recordSQSPublish(success = false)
        
        // Then
        verify(exactly = 2) { mockCounterSuccess.increment() }
        verify { mockCounterFailure.increment() }
    }
}
