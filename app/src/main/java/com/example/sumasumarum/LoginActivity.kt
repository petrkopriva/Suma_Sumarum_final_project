package com.example.sumasumarum

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.sumasumarum.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPref = getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE)
        val savedUsers = sharedPref.getString("ALL_USERS", "") ?: ""

        // --- OPRAVA: Filtrujeme prázdné znaky, aby seznam byl opravdu čistý ---
        val userList = savedUsers.split(",").filter { it.isNotBlank() }

        // --- DIAGNOSTIKA: Tohle ti řekne, co telefon vidí ---
        if (userList.isNotEmpty()) {
            Toast.makeText(this, "Nalezeno: $userList", Toast.LENGTH_LONG).show()

            // Zobrazíme tlačítko
            binding.btnLogin.visibility = View.VISIBLE
            binding.btnLogin.text = "Přihlásit se"
            binding.btnLogin.setOnClickListener { showUserSelectionDialog(userList) }

            // Upravíme text nadpisu
            binding.tvWelcomeBack.text = "Vítejte zpět"
        } else {
            Toast.makeText(this, "Zatím žádný uživatel", Toast.LENGTH_SHORT).show()
            binding.btnLogin.visibility = View.GONE
            binding.tvWelcomeBack.text = "Vítejte v Suma Sumárum"
        }

        binding.btnCreateNew.setOnClickListener { showRegisterDialog() }
    }

    private fun showUserSelectionDialog(users: List<String>) {
        AlertDialog.Builder(this)
            .setTitle("Kdo jsi?")
            .setItems(users.toTypedArray()) { _, which ->
                askForPassword(users[which])
            }
            .show()
    }

    private fun askForPassword(username: String) {
        val input = EditText(this)
        input.hint = "Zadejte heslo"
        input.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Přihlášení: $username")
            .setView(container)
            .setPositiveButton("OK") { _, _ ->
                val enteredPass = input.text.toString()
                if (checkPassword(username, enteredPass)) {
                    loginSuccess(username)
                } else {
                    Toast.makeText(this, "Špatné heslo!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }

    private fun showRegisterDialog() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 20)
        }
        val etName = EditText(this).apply { hint = "Jméno (např. Petr)" }
        val etPass = EditText(this).apply {
            hint = "Heslo"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        layout.addView(etName)
        layout.addView(etPass)

        AlertDialog.Builder(this)
            .setTitle("Nový uživatel")
            .setView(layout)
            .setPositiveButton("Vytvořit") { _, _ ->
                val name = etName.text.toString().trim()
                val pass = etPass.text.toString()

                // Kontrola duplicity
                val sharedPref = getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE)
                val currentUsers = sharedPref.getString("ALL_USERS", "") ?: ""

                if (currentUsers.contains(name)) {
                    Toast.makeText(this, "Tento uživatel už existuje!", Toast.LENGTH_SHORT).show()
                } else if (name.isNotEmpty() && pass.isNotEmpty()) {
                    saveNewUser(name, pass)
                    // Jdeme do Setupu
                    loginSuccessForSetup(name)
                } else {
                    Toast.makeText(this, "Vyplňte jméno a heslo", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }

    private fun saveNewUser(name: String, pass: String) {
        val sharedPref = getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE)
        val users = sharedPref.getString("ALL_USERS", "") ?: ""

        // Jednoduché přidání s čárkou
        val newUsers = if (users.isEmpty()) name else "$users,$name"

        // DŮLEŽITÉ: Používáme commit() pro okamžitý zápis
        sharedPref.edit()
            .putString("ALL_USERS", newUsers)
            .putString("PASS_$name", pass)
            .commit()
    }

    private fun checkPassword(name: String, pass: String): Boolean {
        val sharedPref = getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE)
        return sharedPref.getString("PASS_$name", "") == pass
    }

    private fun loginSuccess(name: String) {
        AppDatabase.resetDatabase()
        getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE).edit()
            .putString("USER_NAME", name)
            .putBoolean("IS_LOGGED_IN", true)
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun loginSuccessForSetup(name: String) {
        AppDatabase.resetDatabase()
        getSharedPreferences("SumaPrefs", Context.MODE_PRIVATE).edit()
            .putString("USER_NAME", name)
            .apply()
        startActivity(Intent(this, SetupActivity::class.java))
        finish()
    }
}