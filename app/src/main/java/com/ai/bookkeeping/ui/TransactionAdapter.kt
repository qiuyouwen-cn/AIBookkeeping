package com.ai.bookkeeping.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionAdapter(
    private val onItemClick: (Transaction) -> Unit,
    private val onDeleteClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.ViewHolder>(TransactionDiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewCategoryBg: View = itemView.findViewById(R.id.viewCategoryBg)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvCategory: TextView = itemView.findViewById(R.id.tvCategory)
        private val tvAiTag: TextView = itemView.findViewById(R.id.tvAiTag)
        private val tvDescription: TextView = itemView.findViewById(R.id.tvDescription)
        private val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)

        fun bind(transaction: Transaction) {
            tvCategory.text = transaction.category
            tvDescription.text = transaction.description.ifEmpty { "无备注" }
            tvDate.text = dateFormat.format(Date(transaction.date))

            // Set category icon and color
            val iconResId = getCategoryIcon(transaction.category)
            ivCategoryIcon.setImageResource(iconResId)

            val bgColor = getCategoryColor(transaction.category, transaction.type)
            val bgDrawable = viewCategoryBg.background as? GradientDrawable
                ?: GradientDrawable().also { viewCategoryBg.background = it }
            bgDrawable.shape = GradientDrawable.OVAL
            bgDrawable.setColor(bgColor)

            // Amount
            val amountText = if (transaction.type == TransactionType.EXPENSE) {
                "-${currencyFormat.format(transaction.amount)}"
            } else {
                "+${currencyFormat.format(transaction.amount)}"
            }
            tvAmount.text = amountText

            val amountColor = if (transaction.type == TransactionType.EXPENSE) {
                ContextCompat.getColor(itemView.context, R.color.expense_red)
            } else {
                ContextCompat.getColor(itemView.context, R.color.income_green)
            }
            tvAmount.setTextColor(amountColor)

            // AI tag
            tvAiTag.visibility = if (transaction.aiParsed) View.VISIBLE else View.GONE

            itemView.setOnClickListener { onItemClick(transaction) }
            btnDelete.setOnClickListener { onDeleteClick(transaction) }
        }

        private fun getCategoryIcon(category: String): Int {
            return when {
                category.contains("餐") || category.contains("食") || category.contains("吃") -> R.drawable.ic_food
                category.contains("交通") || category.contains("车") || category.contains("油") -> R.drawable.ic_transport
                category.contains("购物") || category.contains("买") -> R.drawable.ic_shopping
                category.contains("娱乐") || category.contains("游戏") || category.contains("电影") -> R.drawable.ic_entertainment
                category.contains("医") || category.contains("药") -> R.drawable.ic_medical
                category.contains("教育") || category.contains("学") || category.contains("书") -> R.drawable.ic_education
                category.contains("住") || category.contains("房") || category.contains("租") -> R.drawable.ic_housing
                category.contains("通讯") || category.contains("话费") || category.contains("网") -> R.drawable.ic_communication
                category.contains("衣") || category.contains("服") -> R.drawable.ic_clothing
                category.contains("工资") || category.contains("薪") -> R.drawable.ic_salary
                category.contains("奖") -> R.drawable.ic_bonus
                category.contains("投资") || category.contains("理财") -> R.drawable.ic_investment
                category.contains("兼职") -> R.drawable.ic_parttime
                category.contains("红包") -> R.drawable.ic_redpacket
                else -> R.drawable.ic_other
            }
        }

        private fun getCategoryColor(category: String, type: TransactionType): Int {
            return when {
                category.contains("餐") || category.contains("食") || category.contains("吃") -> Color.parseColor("#FF6B6B")
                category.contains("交通") || category.contains("车") || category.contains("油") -> Color.parseColor("#4D96FF")
                category.contains("购物") || category.contains("买") -> Color.parseColor("#C850C0")
                category.contains("娱乐") || category.contains("游戏") || category.contains("电影") -> Color.parseColor("#FF8E53")
                category.contains("医") || category.contains("药") -> Color.parseColor("#2EC4B6")
                category.contains("教育") || category.contains("学") || category.contains("书") -> Color.parseColor("#6C63FF")
                category.contains("住") || category.contains("房") || category.contains("租") -> Color.parseColor("#9D4EDD")
                category.contains("通讯") || category.contains("话费") || category.contains("网") -> Color.parseColor("#00D9FF")
                category.contains("衣") || category.contains("服") -> Color.parseColor("#A66CFF")
                type == TransactionType.INCOME -> Color.parseColor("#6BCB77")
                else -> Color.parseColor("#6C63FF")
            }
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
