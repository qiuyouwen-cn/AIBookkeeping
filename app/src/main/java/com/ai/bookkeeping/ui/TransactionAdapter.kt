package com.ai.bookkeeping.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.ItemTransactionBinding
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 交易记录列表适配器
 */
class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTransactionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(transaction: Transaction) {
            binding.tvCategory.text = transaction.category
            binding.tvDescription.text = transaction.description
            binding.tvDate.text = dateFormat.format(Date(transaction.date))

            val amountText = if (transaction.type == TransactionType.EXPENSE) {
                "-${currencyFormat.format(transaction.amount)}"
            } else {
                "+${currencyFormat.format(transaction.amount)}"
            }
            binding.tvAmount.text = amountText

            val color = if (transaction.type == TransactionType.EXPENSE) {
                ContextCompat.getColor(binding.root.context, R.color.expense_red)
            } else {
                ContextCompat.getColor(binding.root.context, R.color.income_green)
            }
            binding.tvAmount.setTextColor(color)

            // AI标记
            binding.tvAiTag.visibility = if (transaction.aiParsed) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            binding.root.setOnClickListener { onItemClick(transaction) }
            binding.btnDelete.setOnClickListener { onDeleteClick(transaction) }
        }
    }
}

class TransactionDiffCallback : DiffUtil.ItemCallback<Transaction>() {
    override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
        return oldItem == newItem
    }
}
