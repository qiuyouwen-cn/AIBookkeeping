package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType

/**
 * 交易记录仓库类
 */
class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: LiveData<List<Transaction>> = transactionDao.getAllTransactions()

    suspend fun insert(transaction: Transaction): Long {
        return transactionDao.insert(transaction)
    }

    suspend fun update(transaction: Transaction) {
        transactionDao.update(transaction)
    }

    suspend fun delete(transaction: Transaction) {
        transactionDao.delete(transaction)
    }

    suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    fun getTransactionsByType(type: TransactionType): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByType(type)
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return transactionDao.getTransactionsByDateRange(startDate, endDate)
    }

    fun getTotalIncome(): LiveData<Double?> {
        return transactionDao.getTotalByType(TransactionType.INCOME)
    }

    fun getTotalExpense(): LiveData<Double?> {
        return transactionDao.getTotalByType(TransactionType.EXPENSE)
    }

    fun getTotalByTypeAndDateRange(type: TransactionType, startDate: Long, endDate: Long): LiveData<Double?> {
        return transactionDao.getTotalByTypeAndDateRange(type, startDate, endDate)
    }

    fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): LiveData<List<CategoryTotal>> {
        return transactionDao.getCategoryTotals(type, startDate, endDate)
    }

    suspend fun getTransactionsByDateRangeSync(startDate: Long, endDate: Long): List<Transaction> {
        return transactionDao.getTransactionsByDateRangeSync(startDate, endDate)
    }

    suspend fun getTransactionsByTypeAndDateRangeSync(type: TransactionType, startDate: Long, endDate: Long): List<Transaction> {
        return transactionDao.getTransactionsByTypeAndDateRangeSync(type, startDate, endDate)
    }

    suspend fun getTransactionCountByDateRange(startDate: Long, endDate: Long): Int {
        return transactionDao.getTransactionCountByDateRange(startDate, endDate)
    }

    suspend fun getTotalByTypeAndDateRangeSync(type: TransactionType, startDate: Long, endDate: Long): Double? {
        return transactionDao.getTotalByTypeAndDateRangeSync(type, startDate, endDate)
    }

    suspend fun getCategoryTotalsSync(type: TransactionType, startDate: Long, endDate: Long): List<CategoryTotal> {
        return transactionDao.getCategoryTotalsSync(type, startDate, endDate)
    }

    suspend fun getDailyTotals(type: TransactionType, startDate: Long, endDate: Long): List<DailyTotal> {
        return transactionDao.getDailyTotals(type, startDate, endDate)
    }

    suspend fun getMonthlyTotals(type: TransactionType, startDate: Long, endDate: Long): List<MonthlyTotal> {
        return transactionDao.getMonthlyTotals(type, startDate, endDate)
    }

    suspend fun getRecentNotes(limit: Int = 20): List<String> {
        return transactionDao.getRecentNotes(limit)
    }

    suspend fun getRecentNotesByCategory(category: String, limit: Int = 10): List<String> {
        return transactionDao.getRecentNotesByCategory(category, limit)
    }

    companion object {
        @Volatile
        private var INSTANCE: TransactionRepository? = null

        fun getInstance(transactionDao: TransactionDao): TransactionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TransactionRepository(transactionDao)
                INSTANCE = instance
                instance
            }
        }
    }
}
