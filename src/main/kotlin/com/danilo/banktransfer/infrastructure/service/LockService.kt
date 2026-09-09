package com.danilo.banktransfer.infrastructure.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.DescribeTableRequest
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import java.time.Instant

/**
 * LockService: Pure DynamoDB-based distributed locking
 * 
 * How it works:
 * 1. To acquire lock: PutItem with ConditionExpression (only if NOT exists)
 * 2. Lock TTL: 10 seconds (auto-cleanup if process crashes)
 * 3. To release: DeleteItem
 * 
 * Lock serializes competing threads:
 * - Thread 1: acquires lock → processes transfer → releases lock
 * - Thread 2: waits/retries until lock is gone
 */
@Service
class LockService(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.table.locks:transfers-lock}")
    private val lockTableName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val lockTTL = 10L  // seconds

    init {
        try {
            ensureLockTableExists()
            logger.info("✅ LockService initialized. Lock table: $lockTableName")
        } catch (e: Exception) {
            logger.error("Failed to initialize LockService", e)
            throw e
        }
    }

    /**
     * Ensure lock table exists, create if missing
     */
    private fun ensureLockTableExists() {
        try {
            dynamoDbClient.describeTable { it.tableName(lockTableName) }
            logger.info("Lock table exists: $lockTableName")
        } catch (e: ResourceNotFoundException) {
            logger.info("Lock table not found, creating: $lockTableName")
            createLockTable()
        }
    }

    /**
     * Create lock table with simple schema
     */
    private fun createLockTable() {
        val createRequest = CreateTableRequest.builder()
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

        dynamoDbClient.createTable(createRequest)
        logger.info("✅ Created lock table: $lockTableName")
    }

    /**
     * Acquire locks in order: transferId → sourceAccountId → destinationAccountId
     * 
     * Uses ConditionExpression: "attribute_not_exists(lockId)"
     * This ensures atomicity - lock either exists or doesn't
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
                logger.info("🔒 Attempting to acquire lock: $lockKey")
                
                // Try to acquire lock with retry
                var acquired = false
                for (attempt in 1..5) {
                    try {
                        acquireLock(lockKey)
                        acquiredLocks.add(lockKey)
                        logger.info("✅ Lock acquired: $lockKey (attempt $attempt)")
                        acquired = true
                        break
                    } catch (e: ConditionalCheckFailedException) {
                        if (attempt < 5) {
                            logger.warn("Lock contention for $lockKey, retrying... (attempt $attempt/5)")
                            Thread.sleep(100 * attempt.toLong())  // exponential backoff
                        } else {
                            throw e
                        }
                    }
                }

                if (!acquired) {
                    throw Exception("Could not acquire lock for $lockKey after 5 attempts")
                }
            }

            logger.info("🔐 All locks acquired! Holding: ${acquiredLocks.joinToString(", ")}")
            return acquiredLocks
        } catch (e: Exception) {
            logger.warn("Failed to acquire all locks at position ${acquiredLocks.size}, releasing...")
            releaseLocks(acquiredLocks)
            throw e
        }
    }

    /**
     * Try to acquire single lock atomically
     * 
     * PutItem + ConditionExpression:
     * - Only succeeds if lockId does NOT exist
     * - Fails with ConditionalCheckFailedException if lock already held
     */
    private fun acquireLock(lockKey: String) {
        val expiryTime = (Instant.now().epochSecond + lockTTL).toString()

        val putRequest = PutItemRequest.builder()
            .tableName(lockTableName)
            .item(
                mapOf(
                    "lockId" to AttributeValue.builder().s(lockKey).build(),
                    "ownerId" to AttributeValue.builder().s("transfer-service").build(),
                    "expiryTime" to AttributeValue.builder().n(expiryTime).build(),
                    "acquiredAt" to AttributeValue.builder().s(Instant.now().toString()).build()
                )
            )
            // CRITICAL: Only put if lock does NOT exist (atomicity!)
            .conditionExpression("attribute_not_exists(lockId)")
            .build()

        dynamoDbClient.putItem(putRequest)
    }

    /**
     * Release locks in REVERSE order (LIFO)
     * This prevents deadlock in distributed systems
     */
    fun releaseLocks(locks: List<String>) {
        locks.reversed().forEach { lockKey ->
            try {
                releaseLock(lockKey)
            } catch (e: Exception) {
                logger.warn("Failed to release lock $lockKey: ${e.message}")
            }
        }
    }

    /**
     * Release single lock by deleting it
     */
    private fun releaseLock(lockKey: String) {
        val deleteRequest = DeleteItemRequest.builder()
            .tableName(lockTableName)
            .key(mapOf("lockId" to AttributeValue.builder().s(lockKey).build()))
            .build()

        dynamoDbClient.deleteItem(deleteRequest)
        logger.info("🔓 Lock released: $lockKey")
    }
}
