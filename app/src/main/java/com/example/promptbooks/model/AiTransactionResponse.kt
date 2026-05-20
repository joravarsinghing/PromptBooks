package com.example.promptbooks.model

data class AiTransactionResponse(
    val type: String?,
    val amount: Double?,
    val currency: String?,
    val description: String?,
    val counterpartyName: String?,
    val counterpartyType: String?,
    val paymentMode: String?,
    val account: String?,
    val isPaid: Boolean?,
    val referenceNumber: String?,
    val vatApplicable: Boolean?,
    val vatRate: Double?,
    val vatAmount: Double?,
    val taxCode: String?,
    val date: String?,
    val location: String?,
    val notes: String?,
    val attachmentUri: String?,
    val attachmentType: String?
)
