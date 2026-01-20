package com.example.sumasumarum

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.sumasumarum.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // KONTROLA: Je uživatel "přihlášen" (má nastaveno)?
        val sharedPref = getSharedPreferences("SumaPrefs", MODE_PRIVATE)
        val isSetupDone = sharedPref.getBoolean("IS_SETUP_DONE", false)

        if (!isSetupDone) {
            // Pokud ne, spusť SetupActivity a ukonči MainActivity
            val intent = Intent(this, SetupActivity::class.java)
            startActivity(intent)
            finish()
            return
        }


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Načtení jména uživatele (volitelné zobrazení do titulku nebo Logu)
        val userName = sharedPref.getString("USER_NAME", "Uživatel")
        // Můžeme nastavit titulek okna
        supportActionBar?.title = "Vítejte, $userName"

        replaceFragment(DashboardFragment())

        binding.btnNavDashboard.setOnClickListener {
            replaceFragment(DashboardFragment())
        }

        binding.btnNavHistory.setOnClickListener {
            replaceFragment(HistoryFragment())
        }

        binding.fabAdd.setOnClickListener {
            val intent = Intent(this, AddTransactionActivity::class.java)
            startActivity(intent)
        }
    }

    // Pomocná metoda pro výměnu fragmentů v kontejneru
    private fun replaceFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.commit()
    }
}