package com.ai.bookkeeping.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

/**
 * 预算周期类型
 */
enum class BudgetPeriod {
    WEEKLY,     // 周预算
    MONTHLY,    // 月预算
    QUARTERLY,  // 季度预算
    YEARLY      // 年预算
}

/**
 * 预算实体类 - 管理消费预算
 */
@Parcelize
@Entity(
    tableName = "budgets",
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notebookId"), Index("categoryId")]
)
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long = 1,            // 所属账本ID
    val categoryId: Long? = null,        // 分类ID，null表示总预算
    val amount: Double,                   // 预算金额
    val periodType: BudgetPeriod,        // 预算周期类型
    val year: Int,                        // 年份
    val month: Int? = null,              // 月份（月预算时使用）
    val week: Int? = null,               // 周数（周预算时使用）
    val quarter: Int? = null,            // 季度（季度预算时使用）
    val isActive: Boolean = true,        // 是否启用
    val createdAt: Long = System.currentTimeMillis()  // 创建时间
) : Parcelable

/**
 * 预算使用情况
 */
data class BudgetWithUsage(
    val budget: Budget,
    val categoryName: String? = null,    // 分类名称
    val categoryIcon: String? = null,    // 分类图标
    val categoryColor: String? = null,   // 分类颜色
    val usedAmount: Double = 0.0,        // 已使用金额
    val remainingAmount: Double = 0.0,   // 剩余金额
    val usagePercentage: Float = 0f      // 使用百分比
)
