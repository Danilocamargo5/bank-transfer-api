package com.danilo.banktransfer.infrastructure.config

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.slf4j.LoggerFactory
import java.util.concurrent.TimeUnit

/**
 * Creates Kafka topics on application startup
 */
@Configuration
class KafkaTopicConfig(
    private val kafkaProperties: KafkaProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun kafkaTopicInitializer(): KafkaTopicInitializer {
        return KafkaTopicInitializer(kafkaProperties)
    }
}

class KafkaTopicInitializer(
    private val kafkaProperties: KafkaProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    init {
        try {
            createTopics()
        } catch (e: Exception) {
            logger.error("Failed to create Kafka topics", e)
            throw RuntimeException("Kafka topics initialization failed", e)
        }
    }

    private fun createTopics() {
        logger.info("Initializing Kafka topics...")

        val adminConfig = mutableMapOf<String, Any>()
        adminConfig[AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG] = 
            kafkaProperties.bootstrap.joinToString(",")

        val adminClient = AdminClient.create(adminConfig)

        try {
            // Define topics
            val topics = listOf(
                NewTopic("transfer-requested", 3, 1.toShort()),
                NewTopic("transfer-completed", 3, 1.toShort()),
                NewTopic("transfer-failed", 1, 1.toShort())
            )

            // Create topics
            val result = adminClient.createTopics(topics)
            result.all().get(30, TimeUnit.SECONDS)

            logger.info("✅ All Kafka topics created successfully")

            // List topics
            val listResult = adminClient.listTopics()
            val topicList = listResult.names().get(10, TimeUnit.SECONDS)
            logger.info("Current Kafka topics: {}", topicList)

        } catch (e: Exception) {
            // Topics might already exist, which is fine
            if (e.message?.contains("already exists") != true) {
                logger.warn("⚠️ Error creating topics: {}", e.message)
            } else {
                logger.info("✅ Topics already exist")
            }
        } finally {
            adminClient.close()
        }
    }
}
