package com.example.sumasumarum

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(account: InvestmentAccount)

    // Flow verze pro automatické aktualizace v UI
    @Query("SELECT * FROM investment_accounts ORDER BY name ASC")
    fun getAllAccounts(): Flow<List<InvestmentAccount>>

    // Synchronní verze (Suspend) - pro jednorázové načtení (např. do Spinneru)
    @Query("SELECT * FROM investment_accounts ORDER BY name ASC")
    suspend fun getAllAccountsList(): List<InvestmentAccount>

    // Pro filtrování ve formuláři (zobrazit jen výdajové kategorie atd.)
    @Query("SELECT * FROM investment_accounts WHERE type = :type ORDER BY name ASC")
    fun getAccountsByType(type: String): Flow<List<InvestmentAccount>>
}