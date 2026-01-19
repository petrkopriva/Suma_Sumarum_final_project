package com.example.sumasumarum

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investment_accounts")
data class InvestmentAccount(
    @PrimaryKey val name: String, // Název účtu je unikátní klíč (např. "AirBank")
    val type: String              // Typ: "BANK", "CASH", "INVESTMENT", "PROPERTY"
)