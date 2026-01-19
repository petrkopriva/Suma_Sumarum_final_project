package com.example.sumasumarum

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class User(
    @PrimaryKey val username: String, // Jméno musí být unikátní
    val password: String
)