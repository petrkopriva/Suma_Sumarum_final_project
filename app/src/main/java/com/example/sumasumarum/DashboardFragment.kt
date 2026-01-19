package com.example.sumasumarum

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumasumarum.databinding.FragmentDashboardBinding
import com.example.sumasumarum.databinding.ItemInvestmentBinding
import kotlinx.coroutines.coroutineScope // DŮLEŽITÝ IMPORT
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var selectedDate = Calendar.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvLiquidAssets.layoutManager = LinearLayoutManager(context)

        updateMonthLabel()

        // Spustíme načítání dat bezpečně v rámci životního cyklu
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                loadData()
            }
        }

        binding.btnPrevMonth.setOnClickListener {
            selectedDate.add(Calendar.MONTH, -1)
            updateMonthLabel()
            restartDataLoad()
        }

        binding.btnNextMonth.setOnClickListener {
            selectedDate.add(Calendar.MONTH, 1)
            updateMonthLabel()
            restartDataLoad()
        }

        // Tlačítka
        binding.cardIncome.setOnClickListener { openHistoryWithFilter("INCOME") }
        binding.cardExpense.setOnClickListener { openHistoryWithFilter("EXPENSE") }
        binding.cardInvestment.setOnClickListener { openHistoryWithFilter("INVESTMENT") }

        binding.cardTotal.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NetWorthFragment())
                .addToBackStack(null).commit()
        }

        binding.btnLogout.setOnClickListener {
            if (context == null) return@setOnClickListener
            AppDatabase.resetDatabase()
            val pref = requireActivity().getSharedPreferences("SumaPrefs", android.content.Context.MODE_PRIVATE)
            pref.edit().remove("IS_LOGGED_IN").apply()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            requireActivity().finish()
        }
    }

    private fun updateMonthLabel() {
        if (_binding == null) return
        val format = SimpleDateFormat("LLLL yyyy", Locale("cs", "CZ"))
        val dateStr = format.format(selectedDate.time)
        binding.tvCurrentMonth.text = dateStr.replaceFirstChar { it.uppercase() }
    }

    private fun restartDataLoad() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Znovu zavoláme loadData, což aktualizuje filtry pro nový měsíc
            loadData()
        }
    }

    // OPRAVA ZDE: Přidáno "= coroutineScope"
    private suspend fun loadData() = coroutineScope {
        val db = (requireActivity().application as SumaApp).database

        // job1: Načítání transakcí (Příjmy/Výdaje)
        launch {
            db.transactionDao().getAllTransactions().collect { list ->
                if (_binding != null) {
                    calculateMonthlyFlows(list)
                }
            }
        }

        // job2: Načítání majetku (Celkové jmění)
        launch {
            db.investmentDao().getAllAccounts().collect { accounts ->
                if (_binding != null) {
                    var totalNetWorth = 0.0
                    val assetsList = mutableListOf<Pair<String, Double>>()

                    for (acc in accounts) {
                        if (acc.type != "CASH" && acc.type != "BANK" && acc.type != "INVESTMENT" && acc.type != "PROPERTY") {
                            continue
                        }

                        val balance = if (acc.type == "CASH" || acc.type == "BANK") {
                            db.transactionDao().getBalanceForBank(acc.name) ?: 0.0
                        } else {
                            db.transactionDao().getBalanceForAsset(acc.name) ?: 0.0
                        }

                        totalNetWorth += balance
                        assetsList.add(Pair(acc.name, balance))
                    }

                    _binding?.tvTotalBalance?.text = CurrencyUtils.format(totalNetWorth)
                    _binding?.rvLiquidAssets?.adapter = AssetAdapter(assetsList)
                }
            }
        }
    }

    private fun calculateMonthlyFlows(list: List<Transaction>) {
        if (_binding == null) return

        var income = 0.0
        var expense = 0.0
        var investmentFlow = 0.0

        val calendar = selectedDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        for (t in list) {
            if (t.title == "Počáteční stav") continue
            // Ignorujeme nákupy majetku v měsíčním toku
            if (t.title.startsWith("Nákup:")) continue

            if (t.date >= startOfMonth && t.date < endOfMonth) {
                when (t.type) {
                    "INCOME" -> income += t.amount
                    "EXPENSE" -> expense += t.amount
                    "INVESTMENT" -> investmentFlow += t.amount
                }
            }
        }

        binding.tvIncomeVal.text = "+${CurrencyUtils.format(income)}"
        binding.tvExpenseVal.text = "-${CurrencyUtils.format(expense)}"
        binding.tvInvestmentVal.text = CurrencyUtils.format(investmentFlow)
    }

    private fun openHistoryWithFilter(type: String) {
        val calendar = selectedDate.clone() as Calendar
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endDate = calendar.timeInMillis

        val fragment = HistoryFragment()
        val bundle = Bundle()
        bundle.putString("FILTER_TYPE", type)
        bundle.putLong("START_DATE", startDate)
        bundle.putLong("END_DATE", endDate)
        fragment.arguments = bundle
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null).commit()
    }

    inner class AssetAdapter(private val items: List<Pair<String, Double>>) : RecyclerView.Adapter<AssetAdapter.Holder>() {
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