package com.danilo.banktransfer.infrastructure.repository

import com.danilo.banktransfer.domain.model.Transfer
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.infrastructure.mapper.TransferMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest
import software.amazon.awssdk.services.dynamodb.model.ScanRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItemsRequest
import software.amazon.awssdk.services.dynamodb.model.TransactWriteItem
import software.amazon.awssdk.services.dynamodb.model.Put
import com.danilo.banktransfer.infrastructure.mapper.AccountMapper
import com.danilo.banktransfer.domain.model.Account
import java.util.Optional

@Repository
class TransferRepository(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.table.transfers}")
    private val tableName: String
) {

    fun save(transfer: Transfer): Transfer {
        val request = PutItemRequest.builder()
            .tableName(tableName)
            .item(TransferMapper.toDynamoDBItem(transfer))
            .build()

        dynamoDbClient.putItem(request)
        return transfer
    }

    fun findById(transferId: String): Optional<Transfer> {
        val request = GetItemRequest.builder()
            .tableName(tableName)
            .key(mapOf("transferId" to AttributeValue.builder().s(transferId).build()))
            .build()

        val response = dynamoDbClient.getItem(request)

        return if (response.hasItem()) {
            Optional.of(TransferMapper.fromDynamoDBItem(response.item()))
        } else {
            Optional.empty()
        }
    }

    fun findAll(): List<Transfer> {
        val request = ScanRequest.builder()
            .tableName(tableName)
            .build()

        val response = dynamoDbClient.scan(request)
        return response.items().map { TransferMapper.fromDynamoDBItem(it) }
    }

    fun findByTransferId(transferId: String): List<Transfer> {
        val request = QueryRequest.builder()
            .tableName(tableName)
            .keyConditionExpression("transferId = :transferId")
            .expressionAttributeValues(
                mapOf(":transferId" to AttributeValue.builder().s(transferId).build())
            )
            .build()

        val response = dynamoDbClient.query(request)

        return response.items().map { TransferMapper.fromDynamoDBItem(it) }
    }

    fun hasCompletedTransfer(transferId: String): Boolean {
        val transfers = findByTransferId(transferId)
        return transfers.any { it.status == TransferStatus.COMPLETED || it.status == TransferStatus.FAILED }
    }

    /**
     * UNIFIED TRANSACTION: Save transfer + both accounts in ONE DynamoDB transaction
     * 
     * Guarantees:
     * - All 3 items (source account, destination account, transfer record) are saved TOGETHER
     * - If any write fails → ALL rollback (no partial updates)
     * - Solves: Transfer record and account balances are ALWAYS in sync
     * 
     * @param sourceAccount Updated source account (after debit)
     * @param destinationAccount Updated destination account (after credit)
     * @param transfer Transfer record to save
     * @param accountTableName DynamoDB accounts table
     */
    fun saveTransferWithAccountsAtomically(
        sourceAccount: Account,
        destinationAccount: Account,
        transfer: Transfer,
        accountTableName: String
    ) {
        // Create 3 writes for the unified transaction
        val transactItems = listOf(
            // Write 1: Source account
            TransactWriteItem.builder()
                .put(
                    Put.builder()
                        .tableName(accountTableName)
                        .item(AccountMapper.toDynamoDBItem(sourceAccount))
                        .build()
                )
                .build(),
            
            // Write 2: Destination account
            TransactWriteItem.builder()
                .put(
                    Put.builder()
                        .tableName(accountTableName)
                        .item(AccountMapper.toDynamoDBItem(destinationAccount))
                        .build()
                )
                .build(),
            
            // Write 3: Transfer record
            TransactWriteItem.builder()
                .put(
                    Put.builder()
                        .tableName(tableName)
                        .item(TransferMapper.toDynamoDBItem(transfer))
                        .build()
                )
                .build()
        )

        // Execute UNIFIED transaction: all 3 writes or nothing
        val request = TransactWriteItemsRequest.builder()
            .transactItems(transactItems)
            .build()

        dynamoDbClient.transactWriteItems(request)
    }
}
