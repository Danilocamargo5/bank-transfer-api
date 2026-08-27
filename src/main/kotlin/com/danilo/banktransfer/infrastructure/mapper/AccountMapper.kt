package com.danilo.banktransfer.infrastructure.mapper

import com.danilo.banktransfer.domain.enums.AccountStatus
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.model.Account
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.math.BigDecimal
import java.time.Instant

object AccountMapper {

    fun toDynamoDBItem(account: Account): Map<String, AttributeValue> {
        return mapOf(
            "accountId" to AttributeValue.builder().s(account.accountId).build(),
            "balance" to AttributeValue.builder().n(account.balance.toPlainString()).build(),
            "currency" to AttributeValue.builder().s(account.currency.name).build(),
            "status" to AttributeValue.builder().s(account.status.name).build(),
            "customerName" to AttributeValue.builder().s(account.customerName).build(),
            "createdAt" to AttributeValue.builder().s(account.createdAt.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(account.updatedAt.toString()).build()
        )
    }

    fun fromDynamoDBItem(item: Map<String, AttributeValue>): Account {
        return Account(
            accountId = item["accountId"]?.s() ?: throw IllegalArgumentException("Missing accountId"),
            balance = BigDecimal(item["balance"]?.n() ?: "0"),
            currency = Currency.valueOf(item["currency"]?.s() ?: "BRL"),
            status = AccountStatus.valueOf(item["status"]?.s() ?: "ACTIVE"),
            customerName = item["customerName"]?.s() ?: "",
            createdAt = Instant.parse(item["createdAt"]?.s() ?: Instant.now().toString()),
            updatedAt = Instant.parse(item["updatedAt"]?.s() ?: Instant.now().toString())
        )
    }
}
