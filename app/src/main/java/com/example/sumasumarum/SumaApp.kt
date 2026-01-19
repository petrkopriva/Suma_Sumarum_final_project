package com.example.sumasumarum

import android.app.Application
import android.content.Context

class SumaApp : Application() {
    val database: AppDatabase
        get() {
            val sharedPref = getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE)
            // Načteme jméno aktuálně přihlášeného uživatele
            val currentUser = sharedPref.getString("USER_NAME", "default_user") ?: "default_user"
            return AppDatabase.getDatabase(this, currentUser)
        }
}