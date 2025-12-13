package com.ai.bookkeeping.ui.budget

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.ItemBudgetBinding
import com.ai.bookkeeping.model.BudgetWithUsage
import java.text.NumberFormat
import java.util.Locale

/**
 * 预算列表适配器
 */
class BudgetAdapter(
    private val onItemClick: (BudgetWithUsage) -> Unit,
    private val onItemLongClick: (BudgetWithUsage) -> Unit
) : ListAdapter<BudgetWithUsage, BudgetAdapter.BudgetViewHolder>(BudgetDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BudgetViewHolder {
        val binding = ItemBudgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BudgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BudgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BudgetViewHolder(
        private val binding: ItemBudgetBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            binding.root.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemLongClick(getItem(position))
                }
                true
            }
        }

        fun bind(budgetWithUsage: BudgetWithUsage) {
            val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)

            binding.tvCategoryName.text = budgetWithUsage.categoryName ?: "总预算"
            binding.tvBudgetAmount.text = formatter.format(budgetWithUsage.budget.amount)

            val remaining = budgetWithUsage.remainingAmount
            binding.tvBudgetDetail.text = if (remaining >= 0) {
                "剩余 ${formatter.format(remaining)}"
            } else {
                "超支 ${formatter.format(-remaining)}"
            }

            // 进度条
            val progress = budgetWithUsage.usagePercentage.toInt().coerceIn(0, 100)
            binding.progressBudget.progress = progress

            // 根据使用比例设置颜色
            val progressColor = when {
                budgetWithUsage.usagePercentage > 100 -> Color.parseColor("#FC5C65")
                budgetWithUsage.usagePercentage > 80 -> Color.parseColor("#FD9644")
                else -> Color.parseColor("#5B5FE3")
            }
            binding.progressBudget.setIndicatorColor(progressColor)

            // 设置图标颜色
            try {
                val color = Color.parseColor(budgetWithUsage.categoryColor ?: "#5B5FE3")
                binding.ivCategoryIcon.setColorFilter(color)
            } catch (e: Exception) {
                binding.ivCategoryIcon.setColorFilter(Color.parseColor("#5B5FE3"))
            }

            // 设置图标
            val iconRes = getCategoryIconRes(budgetWithUsage.categoryIcon)
            binding.ivCategoryIcon.setImageResource(iconRes)
        }

        private fun getCategoryIconRes(iconName: String?): Int {
            return when (iconName) {
                "ic_food" -> R.drawable.ic_food
                "ic_transport" -> R.drawable.ic_transport
                "ic_shopping" -> R.drawable.ic_shopping
                "ic_entertainment" -> R.drawable.ic_entertainment
                "ic_medical" -> R.drawable.ic_medical
                "ic_education" -> R.drawable.ic_education
                "ic_housing" -> R.drawable.ic_housing
                "ic_communication" -> R.drawable.ic_communication
                "ic_clothing" -> R.drawable.ic_clothing
                else -> R.drawable.ic_other
            }
        }
    }

    class BudgetDiffCallback : DiffUtil.ItemCallback<BudgetWithUsage>() {
        override fun areItemsTheSame(oldItem: BudgetWithUsage, newItem: BudgetWithUsage): Boolean {
            return oldItem.budget.id == newItem.budget.id
        }

        override fun areContentsTheSame(oldItem: BudgetWithUsage, newItem: BudgetWithUsage): Boolean {
            return oldItem == newItem
        }
    }
}
