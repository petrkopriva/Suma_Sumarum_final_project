package com.example.sumasumarum

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sumasumarum.databinding.FragmentInvestmentsBinding
import com.example.sumasumarum.databinding.ItemInvestmentBinding
import kotlinx.coroutines.launch

class InvestmentsFragment : Fragment() {

    private var _binding: FragmentInvestmentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInvestmentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.recyclerViewInvestments.layoutManager = LinearLayoutManager(context)

        loadInvestments()
    }

    private fun loadInvestments() {
        val db = (requireActivity().application as SumaApp).database
        val investmentDao = db.investmentDao()
        val transactionDao = db.transactionDao()

        lifecycleScope.launch {
            // Načteme všechny účty typu INVESTMENT
            investmentDao.getAccountsByType("INVESTMENT").collect { accounts ->
                val listData = mutableListOf<Pair<String, Double>>()
                var totalValue = 0.0

                for (acc in accounts) {
                    // Použijeme NOVOU metodu pro majetek
                    val balance = transactionDao.getBalanceForAsset(acc.name) ?: 0.0
                    listData.add(Pair(acc.name, balance))
                    totalValue += balance
                }

                // Nastavíme adaptér
                binding.recyclerViewInvestments.adapter = InvestmentAdapter(listData)

                // Zobrazíme celkovou sumu nahoře (pokud tam máš TextView, jinak toto smaž)
                // binding.tvTotalInvestments.text = CurrencyUtils.format(totalValue)
            }
        }
    }

    // Adaptér pro seznam
    inner class InvestmentAdapter(private val items: List<Pair<String, Double>>) : RecyclerView.Adapter<InvestmentAdapter.Holder>() {
        inner class Holder(val b: ItemInvestmentBinding) : RecyclerView.ViewHolder(b.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            return Holder(ItemInvestmentBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val (name, amount) = items[position]
            holder.b.tvInvName.text = name
            // NOVÉ FORMÁTOVÁNÍ
            holder.b.tvInvBalance.text = CurrencyUtils.format(amount)
        }

        override fun getItemCount() = items.size
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}