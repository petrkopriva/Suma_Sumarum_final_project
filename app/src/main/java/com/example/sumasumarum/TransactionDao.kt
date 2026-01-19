package com.example.sumasumarum

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    // NOVÉ: Potřebujeme pro načtení dat při editaci
    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction

    // Historie: Ignorujeme "Počáteční stav", aby to nerušilo
    @Query("SELECT * FROM transactions WHERE title != 'Počáteční stav' ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    // 1. Zůstatek BANKY / HOTOVOSTI
    // (Příjmy) - (Výdaje) - (Investice odeslané z tohoto účtu)
    @Query("""
        SELECT 
        (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account = :accountName AND type = 'INCOME') 
        - 
        (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account = :accountName AND (type = 'EXPENSE' OR type = 'INVESTMENT'))
    """)
    suspend fun getBalanceForBank(accountName: String): Double?

    // 2. Hodnota INVESTICE / NEMOVITOSTI
    // (Počáteční vklad uložený jako INCOME na tento účet) + (Příhozy uložené jako INVESTMENT do této kategorie)
    @Query("""
        SELECT 
        (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE account = :assetName AND type = 'INCOME')
        +
        (SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE category = :assetName AND type = 'INVESTMENT')
    """)
    suspend fun getBalanceForAsset(assetName: String): Double?
}