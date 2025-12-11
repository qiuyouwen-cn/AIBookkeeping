package com.ai.bookkeeping.adapter

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.model.TransactionType
import java.text.NumberFormat
import java.util.Locale

class CategoryRankAdapter : ListAdapter<CategoryRankAdapter.CategoryRankItem, CategoryRankAdapter.ViewHolder>(DiffCallback()) {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    data class CategoryRankItem(
        val category: String,
        val amount: Double,
        val percentage: Float,
        val color: Int,
        val type: TransactionType
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_category_rank, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val viewCategoryBg: View = itemView.findViewById(R.id.viewCategoryBg)
        private val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        private val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        private val tvAmount: TextView = itemView.findViewById(R.id.tvAmount)
        private val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        private val tvPercent: TextView = itemView.findViewById(R.id.tvPercent)

        fun bind(item: CategoryRankItem) {
            tvCategoryName.text = item.category
            tvAmount.text = currencyFormat.format(item.amount)
            tvPercent.text = String.format("%.1f%%", item.percentage)

            progressBar.progress = item.percentage.toInt()

            // Set progress bar color
            val progressDrawable = progressBar.progressDrawable as? LayerDrawable
            progressDrawable?.findDrawableByLayerId(android.R.id.progress)?.setColorFilter(
                item.color,
                PorterDuff.Mode.SRC_IN
            )

            // Set icon background color
            val bgDrawable = viewCategoryBg.background as? GradientDrawable
                ?: GradientDrawable().also { viewCategoryBg.background = it }
            bgDrawable.shape = GradientDrawable.OVAL
            bgDrawable.setColor(item.color)

            // Set category icon
            val iconResId = getCategoryIcon(item.category)
            ivCategoryIcon.setImageResource(iconResId)
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
    }

    class DiffCallback : DiffUtil.ItemCallback<CategoryRankItem>() {
        override fun areItemsTheSame(oldItem: CategoryRankItem, newItem: CategoryRankItem): Boolean {
            return oldItem.category == newItem.category
        }

        override fun areContentsTheSame(oldItem: CategoryRankItem, newItem: CategoryRankItem): Boolean {
            return oldItem == newItem
        }
    }
}
