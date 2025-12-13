package com.ai.bookkeeping.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 账本实体类 - 支持多账本管理
 */
@Entity(tableName = "notebooks")
data class Notebook(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // 账本名称
    val icon: String = "ic_notebook",    // 图标
    val color: String = "#5B5FE3",       // 颜色
    val description: String = "",        // 描述
    val isDefault: Boolean = false,      // 是否默认账本
    val isActive: Boolean = true,        // 是否启用
    val sortOrder: Int = 0,              // 排序顺序
    val createdAt: Long = System.currentTimeMillis()  // 创建时间
)

/**
 * 账本统计信息
 */
data class NotebookWithStats(
    val notebook: Notebook,
    val totalIncome: Double = 0.0,       // 总收入
    val totalExpense: Double = 0.0,      // 总支出
    val balance: Double = 0.0,           // 结余
    val transactionCount: Int = 0        // 交易数量
)

/**
 * 预设账本
 */
object DefaultNotebooks {
    val list = listOf(
        Notebook(name = "日常账本", icon = "ic_notebook", color = "#5B5FE3", isDefault = true, sortOrder = 1),
        Notebook(name = "家庭账本", icon = "ic_family", color = "#FF6B6B", sortOrder = 2),
        Notebook(name = "工作账本", icon = "ic_work", color = "#26DE81", sortOrder = 3)
    )
}
