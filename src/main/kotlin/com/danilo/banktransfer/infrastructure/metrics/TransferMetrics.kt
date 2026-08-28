package com.danilo.banktransfer.infrastructure.metrics

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class TransferMetrics(
    private val meterRegistry: MeterRegistry
) {
    
    fun recordTransferProcessingTime(durationMillis: Long) {
        meterRegistry.timer("transfer.processing.time")
            .record(durationMillis, TimeUnit.MILLISECONDS)
    }
    
    fun recordTransferSuccess() {
        meterRegistry.counter("transfer.success.total").increment()
    }
    
    fun recordTransferFailure(errorType: String) {
        meterRegistry.counter("transfer.failure.total", "error_type", errorType).increment()
    }
    
    fun recordAccountFetch(accountId: String, success: Boolean) {
        meterRegistry.counter(
            "account.fetch",
            "status", if (success) "success" else "not_found"
        ).increment()
    }
    
    fun recordKafkaPublish(topic: String, success: Boolean) {
        meterRegistry.counter(
            "kafka.publish",
            "topic", topic,
            "status", if (success) "success" else "failure"
        ).increment()
    }
    
    fun recordSQSPublish(success: Boolean) {
        meterRegistry.counter(
            "sqs.publish",
            "status", if (success) "success" else "failure"
        ).increment()
    }
}
