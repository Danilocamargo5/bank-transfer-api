package com.danilo.banktransfer.api.controller

import com.danilo.banktransfer.domain.dto.TransferDTO
import com.danilo.banktransfer.domain.dto.TransferRequestDTO
import com.danilo.banktransfer.domain.enums.Currency
import com.danilo.banktransfer.domain.enums.TransferStatus
import com.danilo.banktransfer.domain.model.Transfer
import com.danilo.banktransfer.infrastructure.repository.TransferRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/v1/transfers")
class TransferController(
    private val transferRepository: TransferRepository
) {

    @PostMapping
    fun createTransfer(@RequestBody request: TransferRequestDTO): ResponseEntity<TransferDTO> {
        val transfer = Transfer(
            transferId = request.transferId,
            sourceAccountId = request.sourceAccountId,
            destinationAccountId = request.destinationAccountId,
            amount = request.amount,
            currency = Currency.valueOf(request.currency),
            status = TransferStatus.PENDING,
            requestedAt = request.requestedAt,
            completedAt = null,
            failureReason = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        val saved = transferRepository.save(transfer)

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(toDTO(saved))
    }

    @GetMapping("/{transferId}")
    fun getTransfer(@PathVariable transferId: String): ResponseEntity<TransferDTO> {
        return transferRepository.findById(transferId)
            .map { ResponseEntity.ok(toDTO(it)) }
            .orElse(ResponseEntity.notFound().build())
    }

    private fun toDTO(transfer: Transfer): TransferDTO {
        return TransferDTO(
            transferId = transfer.transferId,
            sourceAccountId = transfer.sourceAccountId,
            destinationAccountId = transfer.destinationAccountId,
            amount = transfer.amount,
            currency = transfer.currency.name,
            status = transfer.status.name,
            requestedAt = transfer.requestedAt,
            completedAt = transfer.completedAt,
            failureReason = transfer.failureReason
        )
    }
}
