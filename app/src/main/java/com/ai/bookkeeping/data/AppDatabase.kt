package com.ai.bookkeeping.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ai.bookkeeping.model.Category
import com.ai.bookkeeping.model.DefaultExpenseCategories
import com.ai.bookkeeping.model.DefaultIncomeCategories
import com.ai.bookkeeping.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Room数据库
 */
@Database(entities = [Transaction::class, Category::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao

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
                    populateDatabase(database.categoryDao())
                }
            }
        }

        suspend fun populateDatabase(categoryDao: CategoryDao) {
            // 检查是否已有数据
            if (categoryDao.getCategoryCount() > 0) return

            // 插入预设的支出分类
            categoryDao.insertAll(DefaultExpenseCategories.list)

            // 插入预设的收入分类
            categoryDao.insertAll(DefaultIncomeCategories.list)
        }
    }
}
