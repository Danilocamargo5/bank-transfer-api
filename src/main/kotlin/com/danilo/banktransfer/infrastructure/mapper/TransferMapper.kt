package com.danilo.banktransfer.infrastructure.mapper

import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.domain.model.Transfer
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import java.math.BigDecimal
import java.time.Instant

object TransferMapper {

    fun toDynamoDBItem(transfer: Transfer): Map<String, AttributeValue> {
        val item = mutableMapOf<String, AttributeValue>()
        
        item["transferId"] = AttributeValue.builder().s(transfer.transferId).build()
        item["requestedAt"] = AttributeValue.builder().s(transfer.requestedAt.toString()).build()
        item["sourceAccountId"] = AttributeValue.builder().s(transfer.sourceAccountId).build()
        item["destinationAccountId"] = AttributeValue.builder().s(transfer.destinationAccountId).build()
        item["amount"] = AttributeValue.builder().n(transfer.amount.toPlainString()).build()
        item["currency"] = AttributeValue.builder().s(transfer.currency.name).build()
        item["status"] = AttributeValue.builder().s(transfer.status.name).build()
        item["completedAt"] = transfer.completedAt?.let { AttributeValue.builder().s(it.toString()).build() }
            ?: AttributeValue.builder().nul(true).build()
        item["failureReason"] = transfer.failureReason?.let { AttributeValue.builder().s(it).build() }
            ?: AttributeValue.builder().nul(true).build()
        item["createdAt"] = AttributeValue.builder().s(transfer.createdAt.toString()).build()
        item["updatedAt"] = AttributeValue.builder().s(transfer.updatedAt.toString()).build()
        
        return item
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
