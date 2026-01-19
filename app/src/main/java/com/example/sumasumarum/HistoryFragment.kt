package com.example.sumasumarum

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.sumasumarum.databinding.FragmentHistoryBinding
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TransactionAdapter

    // Filtry
    private var allTransactions: List<Transaction> = emptyList() // Všechna data
    private var currentTypeFilter: String? = null
    private var startDate: Long = 0
    private var endDate: Long = Long.MAX_VALUE
    private var selectedCategory: String = "Vše"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Načteme argumenty z Dashboardu
        currentTypeFilter = arguments?.getString("FILTER_TYPE")
        startDate = arguments?.getLong("START_DATE") ?: 0
        endDate = arguments?.getLong("END_DATE") ?: Long.MAX_VALUE

        // Nastavení názvu podle filtru
        binding.tvHistoryTitle.text = when(currentTypeFilter) {
            "INCOME" -> "Historie Příjmů"
            "EXPENSE" -> "Historie Výdajů"
            else -> "Celková Historie"
        }

        adapter = TransactionAdapter(emptyList()) { transaction ->
            val intent = Intent(requireContext(), AddTransactionActivity::class.java)
            intent.putExtra("TRANS_ID", transaction.id)
            startActivity(intent)
        }

        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewHistory.adapter = adapter

        // Listener pro filtr kategorií
        binding.spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = parent?.getItemAtPosition(position).toString()
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        loadData()
    }

    private fun loadData() {
        val dao = (requireActivity().application as SumaApp).database.transactionDao()
        lifecycleScope.launch {
            dao.getAllTransactions().collect { list ->
                allTransactions = list
                setupCategorySpinner(list)
                applyFilters()
            }
        }
    }

    private fun setupCategorySpinner(list: List<Transaction>) {
        // Získáme unikátní kategorie z aktuálního seznamu
        val categories = mutableSetOf("Vše")

        // Filtrujeme kategorie jen pro relevantní typ (např. jen výdaje)
        val relevantList = if (currentTypeFilter != null) list.filter { it.type == currentTypeFilter } else list

        categories.addAll(relevantList.map { it.category })

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categories.toList())
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerCategoryFilter.adapter = adapter
    }

    private fun applyFilters() {
        var result = allTransactions

        // 1. Filtr Typu (Příjem/Výdaj)
        if (currentTypeFilter != null) {
            result = result.filter { it.type == currentTypeFilter }
        }

        // 2. Filtr Data (Měsíc z Dashboardu)
        result = result.filter { it.date in startDate until endDate }

        // 3. Filtr Kategorie (Spinner)
        if (selectedCategory != "Vše") {
            result = result.filter { it.category == selectedCategory }
        }

        adapter.updateData(result)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}