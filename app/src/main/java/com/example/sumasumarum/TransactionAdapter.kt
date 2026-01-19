package com.example.sumasumarum

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.sumasumarum.databinding.ItemTransactionBinding

class TransactionAdapter(
    private var transactions: List<Transaction>,
    private val onItemClick: (Transaction) -> Unit // Funkce pro kliknutí
) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(val binding: ItemTransactionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemTransactionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val item = transactions[position]

        holder.binding.tvItemTitle.text = item.title
        // Zobrazíme Kategorii i Účet
        holder.binding.tvItemCategory.text = "${item.category} (${item.account})"

        // Barvy podle typu
        when (item.type) {
            "EXPENSE" -> {
                holder.binding.tvItemAmount.text = "-${item.amount} Kč"
                holder.binding.tvItemAmount.setTextColor(Color.RED)
            }
            "INCOME" -> {
                holder.binding.tvItemAmount.text = "+${item.amount} Kč"
                holder.binding.tvItemAmount.setTextColor(Color.parseColor("#4CAF50")) // Zelená
            }
            "INVESTMENT" -> {
                holder.binding.tvItemAmount.text = "${item.amount} Kč"
                holder.binding.tvItemAmount.setTextColor(Color.parseColor("#FF9800")) // Oranžová
            }
            else -> {
                holder.binding.tvItemAmount.text = "${item.amount} Kč"
                holder.binding.tvItemAmount.setTextColor(Color.BLACK)
            }
        }

        // Kliknutí na položku
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<Transaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}