package com.example.promptbooks.model

data class TransactionResponse(
    val type: String,
    val category: String,
    val amount: Double,
    val currency: String,
    val date: String,
    val note: String?
)
