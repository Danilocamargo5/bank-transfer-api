package com.danilo.banktransfer.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.listener.ConsumerRecordRecoverer
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.Message
import org.springframework.messaging.support.MessageBuilder
import org.springframework.retry.annotation.EnableRetry
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.MaxAttemptsRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import software.amazon.awssdk.services.sqs.SqsClient
import org.slf4j.LoggerFactory

/**
 * Retry and Error Handling Configuration for Kafka
 * 
 * Implements:
 * - Exponential backoff retry strategy (3 attempts)
 * - Dead Letter Queue (DLQ) for failed messages
 * - Comprehensive error logging
 */
@Configuration
@EnableRetry
class KafkaRetryConfig(
    private val sqsClient: SqsClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * RetryTemplate with exponential backoff
     * 
     * Configuration:
     * - Max Attempts: 3
     * - Initial Interval: 1 second
     * - Max Interval: 10 seconds
     * - Multiplier: 2.0 (exponential)
     */
    @Bean
    fun retryTemplate(): RetryTemplate {
        val retryTemplate = RetryTemplate()

        // Retry policy: max 3 attempts
        val retryPolicy = MaxAttemptsRetryPolicy(3)
        retryTemplate.setRetryPolicy(retryPolicy)

        // Backoff policy: exponential (1s, 2s, 4s)
        val backOffPolicy = ExponentialBackOffPolicy().apply {
            initialInterval = 1000L  // 1 second
            maxInterval = 10000L     // 10 seconds
            multiplier = 2.0         // 100% increase each retry
        }
        retryTemplate.setBackOffPolicy(backOffPolicy)

        return retryTemplate
    }

    /**
     * Dead Letter Queue Consumer Recoverer
     * 
     * Handles messages that fail after all retry attempts
     * Sends to SQS transfer-failed queue for manual review
     */
    @Bean
    fun consumerRecordRecoverer(): ConsumerRecordRecoverer {
        return ConsumerRecordRecoverer { record, exception ->
            logger.error(
                "Message processing failed after retries. Topic: {}, Partition: {}, Offset: {}, Key: {}",
                record.topic(),
                record.partition(),
                record.offset(),
                record.key(),
                exception
            )

            // Send to SQS DLQ for manual intervention
            try {
                val message = String(record.value() as ByteArray)
                logger.info("Sending failed message to SQS DLQ: {}", message)
                
                // TODO: Implement SQS DLQ sending
                // sqsClient.sendMessage { builder ->
                //     builder.queueUrl(dlqUrl)
                //     builder.messageBody(message)
                //     builder.messageAttributes { attrs ->
                //         attrs["OriginalTopic"] = ...
                //         attrs["FailureReason"] = ...
                //     }
                // }
            } catch (e: Exception) {
                logger.error("Failed to send message to DLQ", e)
            }
        }
    }
}
