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
import io.github.microutils.kotlin.logging.KotlinLogging

private val logger = KotlinLogging.logger {}

@Component
class DynamoDBInitializer(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.table.accounts}")
    private val accountsTableName: String,
    @Value("\${aws.dynamodb.table.transfers}")
    private val transfersTableName: String
) : ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        logger.info { "Initializing DynamoDB tables..." }
        
        createAccountsTable()
        createTransfersTable()
        
        logger.info { "DynamoDB tables initialized successfully!" }
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
            logger.info { "Table '$accountsTableName' created successfully" }
        } catch (e: Exception) {
            if (e is ResourceNotFoundException || e.message?.contains("ResourceInUseException") == true) {
                logger.info { "Table '$accountsTableName' already exists" }
            } else {
                logger.error(e) { "Error creating table '$accountsTableName'" }
            }
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
            logger.info { "Table '$transfersTableName' created successfully" }
        } catch (e: Exception) {
            if (e is ResourceNotFoundException || e.message?.contains("ResourceInUseException") == true) {
                logger.info { "Table '$transfersTableName' already exists" }
            } else {
                logger.error(e) { "Error creating table '$transfersTableName'" }
            }
        }
    }
}
