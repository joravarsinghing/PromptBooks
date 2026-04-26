package com.example.promptbooks

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "records")
data class Record(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String,
    val account: String,
    val description: String,
    val amount: Double
)
