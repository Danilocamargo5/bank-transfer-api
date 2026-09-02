package com.danilo.banktransfer.infrastructure.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition
import software.amazon.awssdk.services.dynamodb.model.BillingMode
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement
import software.amazon.awssdk.services.dynamodb.model.KeyType
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DynamoDBInitializer")

// DISABLED: Tables are now created and populated by ./scripts/init-dynamodb.sh in full-setup.sh
// This runs before app starts, so tables already exist when consumer processes messages
// Tables must exist BEFORE consumer tries to process messages from Kafka
/*
@Component
class DynamoDBInitializer(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.table.accounts}")
    private val accountsTableName: String,
    @Value("\${aws.dynamodb.table.transfers}")
    private val transfersTableName: String
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        logger.info("Initializing DynamoDB tables...")
        
        try {
            createAccountsTable()
            createTransfersTable()
            logger.info("DynamoDB tables initialized successfully!")
        } catch (e: Exception) {
            logger.warn("Failed to initialize DynamoDB tables: ${e.message}. Tables may already exist.", e)
        }
    }

    private fun createAccountsTable() {
        try {
            val request = CreateTableRequest.builder()
                .tableName(accountsTableName)
                .attributeDefinitions(
                    AttributeDefinition.builder()
                        .attributeName("accountId")
                        .attributeType(ScalarAttributeType.S)
                        .build()
                )
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName("accountId")
                        .keyType(KeyType.HASH)
                        .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build()

            dynamoDbClient.createTable(request)
            logger.info("Table '$accountsTableName' created successfully")
        } catch (e: Exception) {
            logger.debug("Table '$accountsTableName' creation: ${e.message}")
        }
    }

    private fun createTransfersTable() {
        try {
            val request = CreateTableRequest.builder()
                .tableName(transfersTableName)
                .attributeDefinitions(
                    AttributeDefinition.builder()
                        .attributeName("transferId")
                        .attributeType(ScalarAttributeType.S)
                        .build(),
                    AttributeDefinition.builder()
                        .attributeName("requestedAt")
                        .attributeType(ScalarAttributeType.S)
                        .build()
                )
                .keySchema(
                    KeySchemaElement.builder()
                        .attributeName("transferId")
                        .keyType(KeyType.HASH)
                        .build(),
                    KeySchemaElement.builder()
                        .attributeName("requestedAt")
                        .keyType(KeyType.RANGE)
                        .build()
                )
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build()

            dynamoDbClient.createTable(request)
            logger.info("Table '$transfersTableName' created successfully")
        } catch (e: Exception) {
            logger.debug("Table '$transfersTableName' creation: ${e.message}")
        }
    }
}
*/
