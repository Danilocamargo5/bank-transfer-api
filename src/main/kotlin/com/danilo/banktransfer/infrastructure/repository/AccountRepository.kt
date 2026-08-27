package com.danilo.banktransfer.infrastructure.repository

import com.danilo.banktransfer.domain.model.Account
import com.danilo.banktransfer.infrastructure.mapper.AccountMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.util.Optional

@Repository
class AccountRepository(
    private val dynamoDbClient: DynamoDbClient,
    @Value("\${aws.dynamodb.table.accounts}")
    private val tableName: String
) {

    fun save(account: Account): Account {
        val request = PutItemRequest.builder()
            .tableName(tableName)
            .item(AccountMapper.toDynamoDBItem(account))
            .build()

        dynamoDbClient.putItem(request)
        return account
    }

    fun findById(accountId: String): Optional<Account> {
        val request = GetItemRequest.builder()
            .tableName(tableName)
            .key(mapOf("accountId" to AttributeValue.builder().s(accountId).build()))
            .build()

        val response = dynamoDbClient.getItem(request)

        return if (response.hasItem()) {
            Optional.of(AccountMapper.fromDynamoDBItem(response.item()))
        } else {
            Optional.empty()
        }
    }

    fun exists(accountId: String): Boolean {
        return findById(accountId).isPresent
    }
}
