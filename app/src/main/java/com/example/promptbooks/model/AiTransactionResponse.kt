package com.example.promptbooks.model

data class AiTransactionResponse(
    val intent: String,
    val item: String?,
    val quantity: Double?,
    val amount: Double?,
    val payment_mode: String?,
    val counterparty: String?,
    val date: String? = null
)
