package com.danilo.banktransfer.infrastructure.mapper

import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.domain.model.Transfer
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.math.BigDecimal
import java.time.Instant

object TransferMapper {

    fun toDynamoDBItem(transfer: Transfer): Map<String, AttributeValue> {
        return mapOf(
            "transferId" to AttributeValue.builder().s(transfer.transferId).build(),
            "requestedAt" to AttributeValue.builder().s(transfer.requestedAt.toString()).build(),
            "sourceAccountId" to AttributeValue.builder().s(transfer.sourceAccountId).build(),
            "destinationAccountId" to AttributeValue.builder().s(transfer.destinationAccountId).build(),
            "amount" to AttributeValue.builder().n(transfer.amount.toPlainString()).build(),
            "currency" to AttributeValue.builder().s(transfer.currency.name).build(),
            "status" to AttributeValue.builder().s(transfer.status.name).build(),
            "completedAt" to transfer.completedAt?.let { AttributeValue.builder().s(it.toString()).build() }
                ?: AttributeValue.builder().nul(true).build(),
            "failureReason" to transfer.failureReason?.let { AttributeValue.builder().s(it).build() }
                ?: AttributeValue.builder().nul(true).build(),
            "createdAt" to AttributeValue.builder().s(transfer.createdAt.toString()).build(),
            "updatedAt" to AttributeValue.builder().s(transfer.updatedAt.toString()).build()
        )
    }

    fun fromDynamoDBItem(item: Map<String, AttributeValue>): Transfer {
        return Transfer(
            transferId = item["transferId"]?.s() ?: throw IllegalArgumentException("Missing transferId"),
            sourceAccountId = item["sourceAccountId"]?.s() ?: throw IllegalArgumentException("Missing sourceAccountId"),
            destinationAccountId = item["destinationAccountId"]?.s() ?: throw IllegalArgumentException("Missing destinationAccountId"),
            amount = BigDecimal(item["amount"]?.n() ?: "0"),
            currency = Currency.valueOf(item["currency"]?.s() ?: "BRL"),
            status = TransferStatus.valueOf(item["status"]?.s() ?: "PENDING"),
            requestedAt = Instant.parse(item["requestedAt"]?.s() ?: Instant.now().toString()),
            completedAt = item["completedAt"]?.s()?.let { Instant.parse(it) },
            failureReason = item["failureReason"]?.s(),
            createdAt = Instant.parse(item["createdAt"]?.s() ?: Instant.now().toString()),
            updatedAt = Instant.parse(item["updatedAt"]?.s() ?: Instant.now().toString())
        )
    }
}
