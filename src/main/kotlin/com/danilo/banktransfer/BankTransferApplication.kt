package com.danilo.banktransfer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BankTransferApplication

fun main(args: Array<String>) {
	runApplication<BankTransferApplication>(*args)
}
