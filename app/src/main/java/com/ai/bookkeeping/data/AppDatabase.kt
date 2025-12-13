package com.ai.bookkeeping.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ai.bookkeeping.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room数据库
 */
@Database(
    entities = [
        Transaction::class,
        Category::class,
        Account::class,
        Budget::class,
        Notebook::class,
        Transfer::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun notebookDao(): NotebookDao
    abstract fun transferDao(): TransferDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ai_bookkeeping_database"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback())
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback : Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(database: AppDatabase) {
            val categoryDao = database.categoryDao()
            val notebookDao = database.notebookDao()
            val accountDao = database.accountDao()

            // 初始化分类
            if (categoryDao.getCategoryCount() == 0) {
                categoryDao.insertAll(DefaultExpenseCategories.list)
                categoryDao.insertAll(DefaultIncomeCategories.list)
            }

            // 初始化默认账本
            if (notebookDao.getNotebookCount() == 0) {
                val defaultNotebook = Notebook(
                    name = "日常账本",
                    icon = "ic_notebook",
                    color = "#5B5FE3",
                    isDefault = true,
                    sortOrder = 1
                )
                notebookDao.insert(defaultNotebook)
            }

            // 初始化默认账户
            if (accountDao.getAccountCount() == 0) {
                val defaultAccounts = listOf(
                    Account(name = "现金", type = AccountType.CASH, icon = "ic_cash", color = "#26DE81", isDefault = true, sortOrder = 1),
                    Account(name = "储蓄卡", type = AccountType.BANK_CARD, icon = "ic_bank", color = "#45AAF2", sortOrder = 2),
                    Account(name = "支付宝", type = AccountType.ALIPAY, icon = "ic_alipay", color = "#00AAEE", sortOrder = 3),
                    Account(name = "微信", type = AccountType.WECHAT, icon = "ic_wechat", color = "#07C160", sortOrder = 4)
                )
                accountDao.insertAll(defaultAccounts)
            }
        }
    }
}

/**
 * 类型转换器
 */
class Converters {
    @androidx.room.TypeConverter
    fun fromAccountType(value: AccountType): String = value.name

    @androidx.room.TypeConverter
    fun toAccountType(value: String): AccountType = AccountType.valueOf(value)

    @androidx.room.TypeConverter
    fun fromBudgetPeriod(value: BudgetPeriod): String = value.name

    @androidx.room.TypeConverter
    fun toBudgetPeriod(value: String): BudgetPeriod = BudgetPeriod.valueOf(value)

    @androidx.room.TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @androidx.room.TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}
