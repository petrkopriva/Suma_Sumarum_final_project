package com.example.sumasumarum

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sumasumarum.databinding.ActivitySetupBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Předvyplníme jméno z registrace (pokud chceme)
        val sharedPref = getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE)
        val registeredName = sharedPref.getString("USER_NAME", "")
        if (!registeredName.isNullOrEmpty()) {
            binding.etSetupName.setText(registeredName)
        }

        binding.btnAddBankRow.setOnClickListener { addAccountRow(binding.llBanksContainer, "Např. AirBank") }
        binding.btnAddInvRow.setOnClickListener { addAccountRow(binding.llInvestmentsContainer, "Např. Bitcoin") }
        binding.btnAddPropRow.setOnClickListener { addAccountRow(binding.llPropertiesContainer, "Např. Byt Praha") }

        addAccountRow(binding.llBanksContainer, "Např. Běžný účet")

        binding.btnCompleteSetup.setOnClickListener { saveAndStart() }
    }

    private fun addAccountRow(container: LinearLayout, hintText: String) {
        val rowView = LayoutInflater.from(this).inflate(R.layout.item_setup_account, container, false)
        rowView.findViewById<EditText>(R.id.etRowName).hint = hintText
        rowView.findViewById<android.view.View>(R.id.btnRemoveRow).setOnClickListener { container.removeView(rowView) }
        container.addView(rowView)
    }

    private fun saveAndStart() {

        val displayName = binding.etSetupName.text.toString().trim()

        if (displayName.isEmpty()) {
            Toast.makeText(this, "Vyplňte jméno", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnCompleteSetup.isEnabled = false
        binding.btnCompleteSetup.text = "Ukládám..."

        val sharedPref = getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE)



        val loggedInUser = sharedPref.getString("USER_NAME", null)

        if (loggedInUser == null) {

            Toast.makeText(this, "Chyba přihlášení! Vraťte se na start.", Toast.LENGTH_LONG).show()
            finish()
            return
        }


        sharedPref.edit()
            .putString("DISPLAY_NAME", displayName) // Uložíme jako "Přezdívku"
            .putBoolean("IS_SETUP_DONE", true)
            .apply()

        // Data z formuláře
        val cashAmount = binding.etSetupCash.text.toString().toDoubleOrNull() ?: 0.0
        val banksToSave = extractDataFromContainer(binding.llBanksContainer)
        val invToSave = extractDataFromContainer(binding.llInvestmentsContainer)
        val propsToSave = extractDataFromContainer(binding.llPropertiesContainer)

        // Databáze (už je navázaná na loggedInUser díky SumaApp logice)
        val db = (application as SumaApp).database

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    db.clearAllTables()

                    db.investmentDao().insert(InvestmentAccount(name = "Hotovost", type = "CASH"))
                    if (cashAmount > 0) createInitTransaction(db, "Hotovost", cashAmount, "CASH")

                    saveAccountList(db, banksToSave, "BANK")
                    saveAccountList(db, invToSave, "INVESTMENT")
                    saveAccountList(db, propsToSave, "PROPERTY")
                }

                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                finish()

            } catch (e: Exception) {
                e.printStackTrace()
                binding.btnCompleteSetup.isEnabled = true
                binding.btnCompleteSetup.text = "Zkusit znovu"
                Toast.makeText(this@SetupActivity, "Chyba DB: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun saveAccountList(db: AppDatabase, data: List<AccountRowData>, type: String) {
        for (item in data) {
            db.investmentDao().insert(InvestmentAccount(name = item.name, type = type))
            if (item.balance > 0) {
                createInitTransaction(db, item.name, item.balance, type)
            }
        }
    }

    private suspend fun createInitTransaction(db: AppDatabase, accountName: String, amount: Double, type: String) {
        db.transactionDao().insert(Transaction(
            title = "Počáteční stav",
            amount = amount,
            type = "INCOME",
            category = if (type == "BANK" || type == "CASH") "Počáteční stav" else accountName,
            account = accountName,
            date = System.currentTimeMillis()
        ))
    }

    data class AccountRowData(val name: String, val balance: Double)

    private fun extractDataFromContainer(container: LinearLayout): List<AccountRowData> {
        val list = mutableListOf<AccountRowData>()
        for (i in 0 until container.childCount) {
            val row = container.getChildAt(i)
            val etName = row.findViewById<EditText>(R.id.etRowName)
            val etBalance = row.findViewById<EditText>(R.id.etRowBalance)
            val accName = etName.text.toString()
            val balance = etBalance.text.toString().toDoubleOrNull() ?: 0.0
            if (accName.isNotEmpty()) list.add(AccountRowData(accName, balance))
        }
        return list
    }
}