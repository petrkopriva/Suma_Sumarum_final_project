package com.example.sumasumarum

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String,      // "INCOME", "EXPENSE", "INVESTMENT"
    val category: String,
    val account: String,
    val date: Long,
    val imageUri: String? = null
)