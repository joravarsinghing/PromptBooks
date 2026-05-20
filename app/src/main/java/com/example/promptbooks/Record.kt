package com.example.promptbooks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val account: String,
    val description: String,
    val amount: Double,
    // Milestone 2: extended bookkeeping fields
    val type: String? = null,
    val currency: String? = "AED",
    val counterpartyName: String? = null,
    val counterpartyType: String? = null,
    val paymentMode: String? = null,
    val isPaid: Boolean = false,
    val referenceNumber: String? = null,
    val vatApplicable: Boolean = false,
    val vatRate: Double? = null,
    val vatAmount: Double? = null,
    val taxCode: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val source: String? = null,
    val location: String? = null,
    val notes: String? = null,
    val attachmentUri: String? = null,
    val attachmentType: String? = null
)
