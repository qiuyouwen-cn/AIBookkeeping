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

    // 按账本查询
    @Query("SELECT * FROM transactions WHERE notebookId = :notebookId ORDER BY date DESC")
    fun getTransactionsByNotebook(notebookId: Long): kotlinx.coroutines.flow.Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE notebookId = :notebookId ORDER BY date DESC")
    fun getTransactionsByNotebookLiveData(notebookId: Long): LiveData<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE notebookId = :notebookId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByNotebookAndDateRange(notebookId: Long, startDate: Long, endDate: Long): List<Transaction>

    // 按账户查询
    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsByAccount(accountId: Long): kotlinx.coroutines.flow.Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransactionsByAccountAndDateRange(accountId: Long, startDate: Long, endDate: Long): List<Transaction>

    // 按账本和类型统计
    @Query("SELECT SUM(amount) FROM transactions WHERE notebookId = :notebookId AND type = :type AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalByNotebookAndType(notebookId: Long, type: TransactionType, startDate: Long, endDate: Long): Double?

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE notebookId = :notebookId AND type = :type AND date BETWEEN :startDate AND :endDate GROUP BY category ORDER BY total DESC")
    suspend fun getCategoryTotalsByNotebook(notebookId: Long, type: TransactionType, startDate: Long, endDate: Long): List<CategoryTotal>

    // 获取最近备注（用于历史备注功能）
    @Query("SELECT DISTINCT note FROM transactions WHERE note != '' ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentNotes(limit: Int = 20): List<String>

    @Query("SELECT DISTINCT note FROM transactions WHERE category = :category AND note != '' ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentNotesByCategory(category: String, limit: Int = 10): List<String>

    // 批量删除
    @Query("DELETE FROM transactions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
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
