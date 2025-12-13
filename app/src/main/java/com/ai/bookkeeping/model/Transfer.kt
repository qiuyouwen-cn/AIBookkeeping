package com.ai.bookkeeping.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 转账记录实体类 - 记录账户间转账
 */
@Entity(
    tableName = "transfers",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["fromAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("fromAccountId"),
        Index("toAccountId"),
        Index("notebookId")
    ]
)
data class Transfer(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notebookId: Long = 1,            // 所属账本ID
    val fromAccountId: Long,             // 转出账户ID
    val toAccountId: Long,               // 转入账户ID
    val amount: Double,                   // 转账金额
    val fee: Double = 0.0,               // 手续费
    val note: String = "",               // 备注
    val date: Long = System.currentTimeMillis()  // 转账时间
)

/**
 * 转账记录详情（包含账户名称）
 */
data class TransferWithAccounts(
    val transfer: Transfer,
    val fromAccountName: String,         // 转出账户名称
    val fromAccountIcon: String,         // 转出账户图标
    val fromAccountColor: String,        // 转出账户颜色
    val toAccountName: String,           // 转入账户名称
    val toAccountIcon: String,           // 转入账户图标
    val toAccountColor: String           // 转入账户颜色
)
