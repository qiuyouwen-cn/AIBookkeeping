package com.ai.bookkeeping.ui.account

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.ItemAccountBinding
import com.ai.bookkeeping.model.Account
import com.ai.bookkeeping.model.AccountType
import java.text.NumberFormat
import java.util.Locale

/**
 * 账户列表适配器
 */
class AccountAdapter(
    private val onItemClick: (Account) -> Unit,
    private val onItemLongClick: (Account) -> Unit
) : ListAdapter<Account, AccountAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val binding = ItemAccountBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return AccountViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AccountViewHolder(
        private val binding: ItemAccountBinding
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

        fun bind(account: Account) {
            binding.tvAccountName.text = account.name
            binding.tvAccountType.text = getAccountTypeName(account.type)

            // 设置余额
            val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)
            binding.tvAccountBalance.text = formatter.format(account.balance)

            // 设置图标颜色
            try {
                val color = Color.parseColor(account.color)
                binding.ivAccountIcon.setColorFilter(color)
            } catch (e: Exception) {
                binding.ivAccountIcon.setColorFilter(Color.parseColor("#5B5FE3"))
            }

            // 设置图标
            val iconRes = getAccountIconRes(account.type)
            binding.ivAccountIcon.setImageResource(iconRes)

            // 默认标签
            binding.tvDefaultTag.visibility = if (account.isDefault) View.VISIBLE else View.GONE
        }

        private fun getAccountTypeName(type: AccountType): String {
            return when (type) {
                AccountType.CASH -> "现金账户"
                AccountType.BANK_CARD -> "储蓄卡"
                AccountType.CREDIT_CARD -> "信用卡"
                AccountType.ALIPAY -> "支付宝"
                AccountType.WECHAT -> "微信钱包"
                AccountType.OTHER -> "其他账户"
            }
        }

        private fun getAccountIconRes(type: AccountType): Int {
            return when (type) {
                AccountType.CASH -> R.drawable.ic_cash
                AccountType.BANK_CARD -> R.drawable.ic_bank
                AccountType.CREDIT_CARD -> R.drawable.ic_credit_card
                AccountType.ALIPAY -> R.drawable.ic_alipay
                AccountType.WECHAT -> R.drawable.ic_wechat
                AccountType.OTHER -> R.drawable.ic_wallet
            }
        }
    }

    class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem == newItem
        }
    }
}
