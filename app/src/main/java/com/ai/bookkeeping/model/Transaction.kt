package com.ai.bookkeeping.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 交易记录实体类
 */
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val amount: Double,              // 金额
    val type: TransactionType,       // 类型：收入/支出
    val category: String,            // 分类名称（兼容旧数据）
    val categoryId: Long? = null,    // 分类ID
    val subCategoryId: Long? = null, // 子分类ID
    val description: String,         // 描述
    val date: Long = System.currentTimeMillis(),  // 日期时间戳
    val note: String = "",           // 备注
    val aiParsed: Boolean = false,   // 是否由AI解析生成
    val imagePath: String? = null    // 照片路径
)

/**
 * 交易类型枚举
 */
enum class TransactionType {
    INCOME,   // 收入
    EXPENSE   // 支出
}

/**
 * 预定义的支出分类（保留用于AI解析兼容）
 */
object ExpenseCategories {
    val list = listOf(
        "餐饮", "交通", "购物", "娱乐", "医疗",
        "教育", "居住", "通讯", "服饰", "其他"
    )
}

/**
 * 预定义的收入分类（保留用于AI解析兼容）
 */
object IncomeCategories {
    val list = listOf(
        "工资", "奖金", "投资", "兼职", "红包", "其他"
    )
}
