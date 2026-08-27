package com.danilo.banktransfer.infrastructure.repository

import com.danilo.banktransfer.domain.model.Transfer
import com.danilo.banktransfer.infrastructure.mapper.TransferMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
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
}
