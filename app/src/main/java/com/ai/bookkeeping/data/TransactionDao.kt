package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType

/**
 * 交易记录数据访问对象
 */
@Dao
interface TransactionDao {

    @Insert
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): Transaction?

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type")
    fun getTotalByType(type: TransactionType): LiveData<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): LiveData<Double?>

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY category")
    fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): LiveData<List<CategoryTotal>>

    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByDateRangeSync(startDate: Long, endDate: Long): List<Transaction>

    @Query("SELECT * FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByTypeAndDateRangeSync(type: TransactionType, startDate: Long, endDate: Long): List<Transaction>

    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTransactionCountByDateRange(startDate: Long, endDate: Long): Int

    @Query("SELECT SUM(amount) FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalByTypeAndDateRangeSync(type: TransactionType, startDate: Long, endDate: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryTotalsSync(type: TransactionType, startDate: Long, endDate: Long): List<CategoryTotal>

    @Query("SELECT date, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY date / 86400000 ORDER BY date ASC")
    suspend fun getDailyTotals(type: TransactionType, startDate: Long, endDate: Long): List<DailyTotal>

    @Query("SELECT strftime('%Y-%m', datetime(date/1000, 'unixepoch')) as month, SUM(amount) as total FROM transactions WHERE type = :type AND date BETWEEN :startDate AND :endDate GROUP BY month ORDER BY month ASC")
    suspend fun getMonthlyTotals(type: TransactionType, startDate: Long, endDate: Long): List<MonthlyTotal>
}

/**
 * 分类统计数据类
 */
data class CategoryTotal(
    val category: String,
    val total: Double
)

/**
 * 每日统计数据类
 */
data class DailyTotal(
    val date: Long,
    val total: Double
)

/**
 * 月度统计数据类
 */
data class MonthlyTotal(
    val month: String,
    val total: Double
)
