package com.danilo.banktransfer.infrastructure.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awslabs.dynamodb.lock.client.AmazonDynamoDBLockClient
import software.amazon.awslabs.dynamodb.lock.client.LockConfiguration
import java.util.concurrent.TimeUnit

/**
 * LockService: Distributed locking using AWS DynamoDB LockClient
 * 
 * Purpose: Serialize concurrent access to resources (transfers)
 * Prevents race conditions where 2+ threads process the same transfer
 * 
 * Lock Keys: transferId + sourceAccountId + destinationAccountId
 * Lock TTL: 10 seconds (auto-releases if process crashes)
 * Release Order: LIFO (reverse order to prevent deadlock)
 */
@Service
class LockService(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.table.locks:transfers-lock}")
    private val lockTableName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var lockClient: AmazonDynamoDBLockClient

    init {
        try {
            // Ensure lock table exists
            ensureLockTableExists()
            
            // Initialize LockClient
            lockClient = AmazonDynamoDBLockClient.Builder(dynamoDbClient, lockTableName)
                .withTimeUnit(TimeUnit.SECONDS)
                .withLeaseDuration(10L)  // 10 second TTL
                .withHeartbeatInterval(1L)  // Check every 1 second
                .build()
            
            logger.info("LockService initialized with table: $lockTableName")
        } catch (e: Exception) {
            logger.error("Failed to initialize LockService", e)
            throw e
        }
    }

    /**
     * Acquire 3 locks in order (to prevent circular deadlock):
     * 1. transferId
     * 2. sourceAccountId
     * 3. destinationAccountId
     * 
     * Returns: List of acquired locks (in order)
     * IMPORTANT: Must be released in REVERSE order (LIFO)
     */
    fun acquireTransferLocks(
        transferId: String,
        sourceAccountId: String,
        destinationAccountId: String
    ): List<String> {
        val lockKeys = listOf(transferId, sourceAccountId, destinationAccountId)
        val acquiredLocks = mutableListOf<String>()

        try {
            for (lockKey in lockKeys) {
                logger.info("Acquiring lock for: $lockKey")
                val lockConfig = LockConfiguration.builder(lockKey)
                    .withTimeUnit(TimeUnit.SECONDS)
                    .withLeaseDuration(10L)
                    .withRetryInterval(100L)  // 100ms retry
                    .withTimeoutInMillis(5000L)  // 5 second timeout
                    .build()

                val lock = lockClient.acquireLock(lockConfig)
                acquiredLocks.add(lockKey)
                logger.info("✅ Acquired lock for: $lockKey")
            }

            return acquiredLocks
        } catch (e: Exception) {
            // Failed to acquire lock - release what we got
            logger.warn("Failed to acquire lock at position ${acquiredLocks.size}: ${e.message}")
            releaseLocks(acquiredLocks)  // LIFO order
            throw e
        }
    }

    /**
     * Release locks in REVERSE order (LIFO)
     * This prevents deadlock in distributed systems
     * 
     * Example: If acquired [A, B, C], release in order [C, B, A]
     */
    fun releaseLocks(locks: List<String>) {
        // Reverse order (LIFO)
        locks.reversed().forEach { lockKey ->
            try {
                lockClient.releaseLock(LockConfiguration.builder(lockKey).build())
                logger.info("✅ Released lock for: $lockKey")
            } catch (e: Exception) {
                logger.warn("Failed to release lock for $lockKey: ${e.message}")
            }
        }
    }

    /**
     * Ensure lock table exists in DynamoDB
     * Creates if missing (for LocalStack/testing)
     */
    private fun ensureLockTableExists() {
        try {
            dynamoDbClient.describeTable { it.tableName(lockTableName) }
            logger.info("Lock table $lockTableName already exists")
        } catch (e: ResourceNotFoundException) {
            logger.info("Lock table $lockTableName not found, creating...")
            createLockTable()
        }
    }

    /**
     * Create lock table with simple schema:
     * - PK: lockId (String)
     * - Billing: PAY_PER_REQUEST (on-demand)
     */
    private fun createLockTable() {
        val createTableRequest = CreateTableRequest.builder()
            .tableName(lockTableName)
            .keySchema(
                KeySchemaElement.builder()
                    .attributeName("lockId")
                    .keyType(KeyType.HASH)
                    .build()
            )
            .attributeDefinitions(
                AttributeDefinition.builder()
                    .attributeName("lockId")
                    .attributeType(ScalarAttributeType.S)
                    .build()
            )
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build()

        dynamoDbClient.createTable(createTableRequest)
        logger.info("✅ Created lock table: $lockTableName")
    }
}
