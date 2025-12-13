package com.ai.bookkeeping.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 账户类型枚举
 */
enum class AccountType {
    CASH,           // 现金
    BANK_CARD,      // 储蓄卡
    CREDIT_CARD,    // 信用卡
    ALIPAY,         // 支付宝
    WECHAT,         // 微信
    OTHER           // 其他
}

/**
 * 账户实体类 - 管理不同的钱包/账户
 */
@Parcelize
@Entity(
    tableName = "accounts",
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notebookId")]
)
data class Account(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long = 1,           // 所属账本ID
    val name: String,                    // 账户名称
    val type: AccountType,               // 账户类型
    val balance: Double = 0.0,           // 当前余额
    val icon: String = "ic_wallet",      // 图标
    val color: String = "#5B5FE3",       // 颜色
    val isDefault: Boolean = false,      // 是否默认账户
    val isActive: Boolean = true,        // 是否启用
    val sortOrder: Int = 0,              // 排序顺序
    val createdAt: Long = System.currentTimeMillis()  // 创建时间
) : Parcelable

/**
 * 预设账户
 */
object DefaultAccounts {
    val list = listOf(
        Account(name = "现金", type = AccountType.CASH, icon = "ic_cash", color = "#26DE81", isDefault = true, sortOrder = 1),
        Account(name = "储蓄卡", type = AccountType.BANK_CARD, icon = "ic_bank", color = "#45AAF2", sortOrder = 2),
        Account(name = "信用卡", type = AccountType.CREDIT_CARD, icon = "ic_credit_card", color = "#FC5C65", sortOrder = 3),
        Account(name = "支付宝", type = AccountType.ALIPAY, icon = "ic_alipay", color = "#00AAEE", sortOrder = 4),
        Account(name = "微信", type = AccountType.WECHAT, icon = "ic_wechat", color = "#07C160", sortOrder = 5)
    )
}
