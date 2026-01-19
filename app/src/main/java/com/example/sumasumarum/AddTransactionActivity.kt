package com.example.sumasumarum

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.sumasumarum.databinding.ActivityAddTransactionBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AddTransactionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddTransactionBinding
    private var selectedImageUri: String? = null

    // Pro Editaci
    private var isEditMode = false
    private var transactionId: Long = 0

    // Proměnná pro datum (defaultně teď)
    private var transactionTime: Long = System.currentTimeMillis()

    private var categoryList = mutableListOf<String>()
    private lateinit var categorySpinnerAdapter: ArrayAdapter<String>

    private var accountList = mutableListOf<String>()
    private lateinit var accountSpinnerAdapter: ArrayAdapter<String>

    private var loadingJob: Job? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null) {
                try {
                    contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (e: Exception) { e.printStackTrace() }
                selectedImageUri = uri.toString()
                binding.ivReceiptPreview.visibility = android.view.View.VISIBLE
                binding.ivReceiptPreview.setImageURI(uri)
                binding.btnPhoto.text = "Obrázek vybrán"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddTransactionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()

        // Načteme účty
        loadAccounts()

        // Defaultně načteme kategorie pro Výdaj
        loadCategories("EXPENSE")

        binding.btnAddAccount.setOnClickListener { showAddAccountDialog() }
        binding.btnAddCategory.setOnClickListener { showAddCategoryDialog() }

        binding.rgType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbExpense -> {
                    binding.ivCategoryIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                    loadCategories("EXPENSE")
                }
                R.id.rbIncome -> {
                    binding.ivCategoryIcon.setImageResource(android.R.drawable.ic_input_add)
                    loadCategories("INCOME")
                }
                R.id.rbInvestment -> {
                    binding.ivCategoryIcon.setImageResource(android.R.drawable.ic_menu_compass)
                    loadCategories("INVESTMENT")
                }
            }
        }

        binding.btnPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
            pickImageLauncher.launch(intent)
        }

        binding.btnSave.setOnClickListener { saveTransaction() }

        // --- LOGIKA EDITACE ---
        if (intent.hasExtra("TRANS_ID")) {
            isEditMode = true
            transactionId = intent.getLongExtra("TRANS_ID", 0)
            binding.btnSave.text = "Aktualizovat"

            // Načtení dat z databáze
            loadTransactionData(transactionId)
        }
    }

    private fun loadTransactionData(id: Long) {
        lifecycleScope.launch {
            val db = (application as SumaApp).database
            val transaction = db.transactionDao().getTransactionById(id)

            // 1. Texty a Datum
            binding.etTitle.setText(transaction.title)
            binding.etAmount.setText(transaction.amount.toString())
            transactionTime = transaction.date

            // 2. Typ
            when (transaction.type) {
                "EXPENSE" -> binding.rbExpense.isChecked = true
                "INCOME" -> binding.rbIncome.isChecked = true
                "INVESTMENT" -> binding.rbInvestment.isChecked = true
            }

            // 3. Zobrazení fotky (pokud existuje)
            if (transaction.imageUri != null) {
                selectedImageUri = transaction.imageUri
                binding.ivReceiptPreview.visibility = android.view.View.VISIBLE
                try {
                    val uri = android.net.Uri.parse(transaction.imageUri)
                    binding.ivReceiptPreview.setImageURI(uri)
                    binding.btnPhoto.text = "Změnit obrázek"

                    // Kliknutí na náhled otevře velký obrázek
                    binding.ivReceiptPreview.setOnClickListener {
                        val viewIntent = Intent(Intent.ACTION_VIEW)
                        viewIntent.setDataAndType(uri, "image/*")
                        viewIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        try {
                            startActivity(viewIntent)
                        } catch (e: Exception) {
                            Toast.makeText(this@AddTransactionActivity, "Nelze otevřít obrázek", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 4. Nastavení Spinnerů
            delay(200)
            setSpinnerValue(binding.spinnerAccount, accountList, transaction.account)
            setSpinnerValue(binding.spinnerCategory, categoryList, transaction.category)
        }
    }

    private fun setSpinnerValue(spinner: Spinner, list: List<String>, valueToSelect: String) {
        val index = list.indexOf(valueToSelect)
        if (index >= 0) {
            spinner.setSelection(index)
        }
    }

    private fun setupSpinners() {
        categorySpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryList)
        categorySpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategory.adapter = categorySpinnerAdapter

        accountSpinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accountList)
        accountSpinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerAccount.adapter = accountSpinnerAdapter
    }

    private fun loadAccounts() {
        val dao = (application as SumaApp).database.investmentDao()
        lifecycleScope.launch {
            dao.getAllAccounts().collect { accounts ->
                accountList.clear()

                // FILTR: Zobrazit jen BANK a CASH jako zdroje peněz (pro platby)
                val validAccounts = accounts.filter { it.type == "BANK" || it.type == "CASH" }

                if (validAccounts.isEmpty()) {
                    accountList.add("Hotovost")
                } else {
                    accountList.addAll(validAccounts.map { it.name })
                }
                accountSpinnerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadCategories(type: String) {
        loadingJob?.cancel()

        val defaultCategories = when(type) {
            "EXPENSE" -> listOf("Jídlo", "Bydlení", "Doprava", "Nákupy", "Zábava")
            "INCOME" -> listOf("Mzda", "Ostatní")
            "INVESTMENT" -> emptyList()
            else -> listOf("Ostatní")
        }

        loadingJob = lifecycleScope.launch {
            (application as SumaApp).database.investmentDao().getAccountsByType(type).collect { dbAccounts ->
                categoryList.clear()
                val combined = defaultCategories.toMutableList()
                combined.addAll(dbAccounts.map { it.name })
                categoryList.addAll(combined.distinct())

                if (categoryList.isEmpty() && type == "INVESTMENT") {
                    categoryList.add("Vytvořte novou ->")
                }
                categorySpinnerAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showAddAccountDialog() {
        val dialogView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }
        val etName = EditText(this).apply { hint = "Název účtu (např. AirBank)" }
        val etInitialBalance = EditText(this).apply {
            hint = "Počáteční stav (Kč)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }
        val typeSpinner = Spinner(this)
        val types = listOf("Běžný účet (BANK)", "Hotovost (CASH)") // Zde omezíme jen na zdroje
        val typeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        typeSpinner.adapter = typeAdapter

        dialogView.addView(etName)
        dialogView.addView(typeSpinner)
        dialogView.addView(etInitialBalance)

        AlertDialog.Builder(this)
            .setTitle("Nový finanční účet")
            .setView(dialogView)
            .setPositiveButton("Vytvořit") { _, _ ->
                val name = etName.text.toString()
                val balance = etInitialBalance.text.toString().toDoubleOrNull() ?: 0.0
                val selectedTypeStr = typeSpinner.selectedItem.toString()

                val typeCode = if(selectedTypeStr.contains("BANK")) "BANK" else "CASH"

                if (name.isNotEmpty()) {
                    createAccountInDb(name, typeCode, balance)
                }
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }

    private fun showAddCategoryDialog() {
        val input = EditText(this)
        input.hint = "Název (např. Investown, Bitcoin...)"
        val title = when(binding.rgType.checkedRadioButtonId) {
            R.id.rbInvestment -> "Nová investiční platforma"
            else -> "Nová kategorie"
        }
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("Přidat") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) addNewCategoryToDb(newName)
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }

    private fun createAccountInDb(name: String, type: String, balance: Double) {
        val db = (application as SumaApp).database
        lifecycleScope.launch {
            try {
                // DB operace na pozadí
                withContext(Dispatchers.IO) {
                    db.investmentDao().insert(InvestmentAccount(name = name, type = type))
                    if (balance > 0) {
                        db.transactionDao().insert(Transaction(
                            title = "Počáteční stav",
                            amount = balance,
                            type = "INCOME",
                            category = "Počáteční vklad",
                            account = name,
                            date = System.currentTimeMillis()
                        ))
                    }
                }
                // UI operace na hlavním vlákně
                Toast.makeText(this@AddTransactionActivity, "Účet vytvořen", Toast.LENGTH_SHORT).show()
                // loadAccounts() se zavolá automaticky přes flow
            } catch (e: Exception) {
                Toast.makeText(this@AddTransactionActivity, "Chyba: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addNewCategoryToDb(name: String) {
        val type = when (binding.rgType.checkedRadioButtonId) {
            R.id.rbIncome -> "INCOME"
            R.id.rbInvestment -> "INVESTMENT"
            else -> "EXPENSE"
        }
        val db = (application as SumaApp).database
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                db.investmentDao().insert(InvestmentAccount(name = name, type = type))
            }
            // Zde by to chtělo Toast na hlavním vlákně, ale flow to aktualizuje, tak stačí
        }
    }

    private fun saveTransaction() {
        val title = binding.etTitle.text.toString()
        val amount = binding.etAmount.text.toString().replace(",", ".").toDoubleOrNull() ?: 0.0

        var category = binding.spinnerCategory.selectedItem?.toString() ?: ""
        if (category == "Vytvořte novou ->") category = "Neznámá"
        val account = binding.spinnerAccount.selectedItem?.toString() ?: "Neznámý účet"

        if (title.isEmpty() || amount == 0.0) {
            Toast.makeText(this, "Vyplňte název a částku", Toast.LENGTH_SHORT).show()
            return
        }

        val type = when (binding.rgType.checkedRadioButtonId) {
            R.id.rbIncome -> "INCOME"
            R.id.rbInvestment -> "INVESTMENT"
            else -> "EXPENSE"
        }

        val transaction = Transaction(
            id = if (isEditMode) transactionId else 0,
            title = title,
            amount = amount,
            type = type,
            category = category,
            account = account,
            date = transactionTime,
            imageUri = selectedImageUri
        )

        lifecycleScope.launch {
            try {
                // 1. Práce s DB na pozadí (IO vlákno)
                withContext(Dispatchers.IO) {
                    val dao = (application as SumaApp).database.transactionDao()
                    if (isEditMode) dao.update(transaction) else dao.insert(transaction)
                }

                // 2. UI operace na hlavním vlákně (Main vlákno)
                Toast.makeText(this@AddTransactionActivity, "Uloženo", Toast.LENGTH_SHORT).show()
                finish()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AddTransactionActivity, "Chyba při ukládání", Toast.LENGTH_SHORT).show()
            }
        }
    }
}