package com.ai.bookkeeping.ui.notebook

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.ItemNotebookBinding
import com.ai.bookkeeping.model.NotebookWithStats
import java.text.NumberFormat
import java.util.Locale

/**
 * 账本列表适配器
 */
class NotebookAdapter(
    private val onItemClick: (NotebookWithStats) -> Unit,
    private val onMoreClick: (NotebookWithStats) -> Unit,
    private var currentNotebookId: Long = 1
) : ListAdapter<NotebookWithStats, NotebookAdapter.NotebookViewHolder>(NotebookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotebookViewHolder {
        val binding = ItemNotebookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotebookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NotebookViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateCurrentNotebook(id: Long) {
        currentNotebookId = id
        notifyDataSetChanged()
    }

    inner class NotebookViewHolder(
        private val binding: ItemNotebookBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(getItem(position))
                }
            }
            binding.btnMore.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onMoreClick(getItem(position))
                }
            }
        }

        fun bind(notebookWithStats: NotebookWithStats) {
            val notebook = notebookWithStats.notebook
            val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)

            binding.tvNotebookName.text = notebook.name
            binding.tvTransactionCount.text = "${notebookWithStats.transactionCount}笔记录"

            // 当前账本标签
            binding.tvDefaultTag.visibility = if (notebook.id == currentNotebookId) View.VISIBLE else View.GONE

            // 设置图标颜色
            try {
                val color = Color.parseColor(notebook.color)
                binding.ivNotebookIcon.setColorFilter(color)
            } catch (e: Exception) {
                binding.ivNotebookIcon.setColorFilter(Color.parseColor("#5B5FE3"))
            }

            binding.ivNotebookIcon.setImageResource(R.drawable.ic_notebook)

            // 统计数据
            binding.tvTotalIncome.text = formatter.format(notebookWithStats.totalIncome)
            binding.tvTotalExpense.text = formatter.format(notebookWithStats.totalExpense)
            binding.tvBalance.text = formatter.format(notebookWithStats.balance)
        }
    }

    class NotebookDiffCallback : DiffUtil.ItemCallback<NotebookWithStats>() {
        override fun areItemsTheSame(oldItem: NotebookWithStats, newItem: NotebookWithStats): Boolean {
            return oldItem.notebook.id == newItem.notebook.id
        }

        override fun areContentsTheSame(oldItem: NotebookWithStats, newItem: NotebookWithStats): Boolean {
            return oldItem == newItem
        }
    }
}
