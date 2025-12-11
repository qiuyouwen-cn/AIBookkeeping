package com.ai.bookkeeping.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 分类实体类 - 支持二级分类
 */
@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = Category::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("parentId")]
)
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,                    // 分类名称
    val icon: String,                    // 图标名称
    val color: String,                   // 颜色代码
    val type: TransactionType,           // 收入/支出
    val parentId: Long? = null,          // 父分类ID，null表示一级分类
    val sortOrder: Int = 0,              // 排序顺序
    val isSystem: Boolean = false,       // 是否系统预设
    val isActive: Boolean = true         // 是否启用
)

/**
 * 带子分类的分类数据
 */
data class CategoryWithChildren(
    val category: Category,
    val children: List<Category> = emptyList()
)

/**
 * 预设的支出分类（带图标和颜色）
 */
object DefaultExpenseCategories {
    val list = listOf(
        Category(name = "餐饮", icon = "ic_food", color = "#FF6B6B", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 1),
        Category(name = "交通", icon = "ic_transport", color = "#4ECDC4", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 2),
        Category(name = "购物", icon = "ic_shopping", color = "#FFE66D", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 3),
        Category(name = "娱乐", icon = "ic_entertainment", color = "#95E1D3", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 4),
        Category(name = "医疗", icon = "ic_medical", color = "#F38181", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 5),
        Category(name = "教育", icon = "ic_education", color = "#AA96DA", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 6),
        Category(name = "居住", icon = "ic_home_category", color = "#FCBAD3", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 7),
        Category(name = "通讯", icon = "ic_communication", color = "#A8D8EA", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 8),
        Category(name = "服饰", icon = "ic_clothing", color = "#D4A5A5", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 9),
        Category(name = "其他", icon = "ic_other", color = "#C9CCD5", type = TransactionType.EXPENSE, isSystem = true, sortOrder = 100)
    )

    // 二级分类
    val subCategories = mapOf(
        "餐饮" to listOf("早餐", "午餐", "晚餐", "零食", "饮料", "水果"),
        "交通" to listOf("公交地铁", "打车", "加油", "停车费", "火车票", "机票"),
        "购物" to listOf("日用品", "数码产品", "家电", "美妆护肤", "礼物"),
        "娱乐" to listOf("电影", "游戏", "旅游", "运动健身", "KTV")
    )
}

/**
 * 预设的收入分类
 */
object DefaultIncomeCategories {
    val list = listOf(
        Category(name = "工资", icon = "ic_salary", color = "#26DE81", type = TransactionType.INCOME, isSystem = true, sortOrder = 1),
        Category(name = "奖金", icon = "ic_bonus", color = "#FD9644", type = TransactionType.INCOME, isSystem = true, sortOrder = 2),
        Category(name = "投资", icon = "ic_investment", color = "#45AAF2", type = TransactionType.INCOME, isSystem = true, sortOrder = 3),
        Category(name = "兼职", icon = "ic_parttime", color = "#A55EEA", type = TransactionType.INCOME, isSystem = true, sortOrder = 4),
        Category(name = "红包", icon = "ic_redpacket", color = "#FC5C65", type = TransactionType.INCOME, isSystem = true, sortOrder = 5),
        Category(name = "其他", icon = "ic_other", color = "#C9CCD5", type = TransactionType.INCOME, isSystem = true, sortOrder = 100)
    )
}
