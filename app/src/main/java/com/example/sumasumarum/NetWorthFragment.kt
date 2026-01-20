package com.example.sumasumarum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumasumarum.databinding.FragmentNetWorthBinding
import com.example.sumasumarum.databinding.ItemInvestmentBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NetWorthFragment : Fragment() {

    private var _binding: FragmentNetWorthBinding? = null
    private val binding get() = _binding!!

    private var availablePaymentAccounts: List<String> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNetWorthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvLiquidAssets.layoutManager = LinearLayoutManager(context)
        binding.rvInvestments.layoutManager = LinearLayoutManager(context)
        binding.rvProperty.layoutManager = LinearLayoutManager(context)

        binding.fabAddAsset.setOnClickListener {
            loadAccountsAndShowDialog()
        }

        loadData()
    }

    private fun loadData() {
        val db = (requireActivity().application as SumaApp).database
        val transactionDao = db.transactionDao()
        val investmentDao = db.investmentDao()

        lifecycleScope.launch {
            investmentDao.getAllAccounts().collect { allAccounts ->
                val liquidList = mutableListOf<Pair<String, Double>>()
                val investList = mutableListOf<Pair<String, Double>>()
                val propertyList = mutableListOf<Pair<String, Double>>()

                var totalNetWorth = 0.0
                var sumLiquid = 0.0
                var sumInvest = 0.0
                var sumProperty = 0.0

                for (acc in allAccounts) {
                    var balance = 0.0
                    if (acc.type == "CASH" || acc.type == "BANK") {
                        balance = transactionDao.getBalanceForBank(acc.name) ?: 0.0
                    } else {
                        balance = transactionDao.getBalanceForAsset(acc.name) ?: 0.0
                    }

                    when (acc.type) {
                        "CASH", "BANK" -> { liquidList.add(Pair(acc.name, balance)); sumLiquid += balance }
                        "INVESTMENT" -> { investList.add(Pair(acc.name, balance)); sumInvest += balance }
                        "PROPERTY" -> { propertyList.add(Pair(acc.name, balance)); sumProperty += balance }
                    }
                    totalNetWorth += balance
                }

                binding.rvLiquidAssets.adapter = GenericAdapter(liquidList)
                binding.rvInvestments.adapter = GenericAdapter(investList)
                binding.rvProperty.adapter = GenericAdapter(propertyList)

                binding.tvTotalNetWorth.text = CurrencyUtils.format(totalNetWorth)
                binding.tvCashVal.text = CurrencyUtils.format(sumLiquid)
                binding.tvInvestVal.text = CurrencyUtils.format(sumInvest)
                binding.tvPropertyVal.text = CurrencyUtils.format(sumProperty)
            }
        }
    }

    private fun loadAccountsAndShowDialog() {
        val db = (requireActivity().application as SumaApp).database
        lifecycleScope.launch {
            if (!isAdded) return@launch // Pojistka proti pádu

            try {
                // Získání seznamu účtů (Banka/Hotovost)
                val allAccounts = db.investmentDao().getAllAccountsList()
                availablePaymentAccounts = allAccounts
                    .filter { it.type == "BANK" || it.type == "CASH" }
                    .map { it.name }

                showAddAssetDialog()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAddAssetDialog() {
        if (context == null) return // Další pojistka

        val context = requireContext()
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
        }

        val etName = EditText(context).apply { hint = "Název (např. Byt, Akcie)" }
        val etValue = EditText(context).apply {
            hint = "Hodnota / Cena (Kč)"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        }

        val spinner = Spinner(context)
        val types = listOf("Nemovitost (PROPERTY)", "Investice (INVESTMENT)", "Běžný účet (BANK)", "Hotovost (CASH)")
        spinner.adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, types)

        val cbPayFromAccount = CheckBox(context).apply {
            text = "Zaplaceno z mých peněz (odepsat z účtu)"
            isChecked = false
        }

        val accountSpinner = Spinner(context).apply {
            adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, availablePaymentAccounts)
            visibility = View.GONE
        }

        cbPayFromAccount.setOnCheckedChangeListener { _, isChecked ->
            accountSpinner.visibility = if (isChecked && availablePaymentAccounts.isNotEmpty()) View.VISIBLE else View.GONE
            if (isChecked && availablePaymentAccounts.isEmpty()) {
                Toast.makeText(context, "Nemáte žádný účet pro platbu!", Toast.LENGTH_SHORT).show()
                cbPayFromAccount.isChecked = false
            }
        }

        layout.addView(etName)
        layout.addView(spinner)
        layout.addView(etValue)
        layout.addView(cbPayFromAccount)
        layout.addView(accountSpinner)

        AlertDialog.Builder(context)
            .setTitle("Přidat nové aktivum")
            .setView(layout)
            .setPositiveButton("Přidat") { _, _ ->
                val name = etName.text.toString()
                val value = etValue.text.toString().toDoubleOrNull() ?: 0.0
                val selectedTypeStr = spinner.selectedItem.toString()

                val type = when {
                    selectedTypeStr.contains("PROPERTY") -> "PROPERTY"
                    selectedTypeStr.contains("INVESTMENT") -> "INVESTMENT"
                    selectedTypeStr.contains("CASH") -> "CASH"
                    else -> "BANK"
                }

                val sourceAccount = if (cbPayFromAccount.isChecked && accountSpinner.selectedItem != null) {
                    accountSpinner.selectedItem.toString()
                } else {
                    null
                }

                if (name.isNotEmpty()) {
                    addNewAssetToDb(name, type, value, sourceAccount)
                }
            }
            .setNegativeButton("Zrušit", null)
            .show()
    }

    private fun addNewAssetToDb(name: String, type: String, value: Double, sourceAccount: String?) {
        val db = (requireActivity().application as SumaApp).database
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 1. Vytvoříme záznam o existenci aktiva
                    db.investmentDao().insert(InvestmentAccount(name = name, type = type))

                    if (value > 0) {
                        if (sourceAccount != null) {





                            db.transactionDao().insert(Transaction(
                                title = "Nákup: $name",
                                amount = value,
                                type = "INVESTMENT",
                                category = name,
                                account = sourceAccount,
                                date = System.currentTimeMillis()
                            ))
                        } else {

                            db.transactionDao().insert(Transaction(
                                title = "Počáteční stav",
                                amount = value,
                                type = "INCOME",
                                category = if(type == "PROPERTY") name else "Počáteční vklad",
                                account = name,
                                date = System.currentTimeMillis()
                            ))
                        }
                    }
                }


                activity?.runOnUiThread {
                    Toast.makeText(context, "Uloženo", Toast.LENGTH_SHORT).show()
                    loadData()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                activity?.runOnUiThread {
                    Toast.makeText(context, "Chyba: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    inner class GenericAdapter(private val items: List<Pair<String, Double>>) :
        RecyclerView.Adapter<GenericAdapter.Holder>() {
        inner class Holder(val b: ItemInvestmentBinding) : RecyclerView.ViewHolder(b.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(ItemInvestmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (name, amount) = items[position]
            holder.b.tvInvName.text = name
            holder.b.tvInvBalance.text = CurrencyUtils.format(amount)
            (holder.itemView as androidx.cardview.widget.CardView).cardElevation = 0f
            (holder.itemView as androidx.cardview.widget.CardView).setCardBackgroundColor(android.graphics.Color.WHITE)
        }
        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}