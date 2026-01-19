package com.example.sumasumarum

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Transaction::class, InvestmentAccount::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun investmentDao(): InvestmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Metoda nyní přijímá jméno uživatele, aby vytvořila unikátní soubor
        fun getDatabase(context: Context, username: String = "default_user"): AppDatabase {
            val dbName = "suma_database_$username" // Např. suma_database_Petr

            // Pokud se přihlašuje jiný uživatel, resetujeme instanci
            if (INSTANCE?.openHelper?.databaseName != dbName) {
                INSTANCE = null
            }

            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    dbName
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun resetDatabase() {
            INSTANCE = null
        }
    }
}