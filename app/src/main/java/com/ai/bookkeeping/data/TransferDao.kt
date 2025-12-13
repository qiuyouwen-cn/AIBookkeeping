package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ai.bookkeeping.model.Transfer
import com.ai.bookkeeping.model.TransferWithAccounts
import kotlinx.coroutines.flow.Flow

/**
 * 转账记录数据访问对象
 */
@Dao
interface TransferDao {

    @Insert
    suspend fun insert(transfer: Transfer): Long

    @Update
    suspend fun update(transfer: Transfer)

    @Delete
    suspend fun delete(transfer: Transfer)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun getAllTransfers(): Flow<List<Transfer>>

    @Query("SELECT * FROM transfers ORDER BY date DESC")
    fun getAllTransfersLiveData(): LiveData<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE notebookId = :notebookId ORDER BY date DESC")
    fun getTransfersByNotebook(notebookId: Long): Flow<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE notebookId = :notebookId ORDER BY date DESC")
    fun getTransfersByNotebookLiveData(notebookId: Long): LiveData<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    suspend fun getTransferById(id: Long): Transfer?

    @Query("SELECT * FROM transfers WHERE fromAccountId = :accountId OR toAccountId = :accountId ORDER BY date DESC")
    fun getTransfersByAccount(accountId: Long): Flow<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransfersByDateRange(startDate: Long, endDate: Long): Flow<List<Transfer>>

    @Query("SELECT * FROM transfers WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getTransfersByDateRangeSync(startDate: Long, endDate: Long): List<Transfer>

    @Query("""
        SELECT t.*,
               fa.name as fromAccountName, fa.icon as fromAccountIcon, fa.color as fromAccountColor,
               ta.name as toAccountName, ta.icon as toAccountIcon, ta.color as toAccountColor
        FROM transfers t
        INNER JOIN accounts fa ON t.fromAccountId = fa.id
        INNER JOIN accounts ta ON t.toAccountId = ta.id
        WHERE t.notebookId = :notebookId
        ORDER BY t.date DESC
    """)
    fun getTransfersWithAccounts(notebookId: Long): Flow<List<TransferAccountInfo>>

    @Query("""
        SELECT t.*,
               fa.name as fromAccountName, fa.icon as fromAccountIcon, fa.color as fromAccountColor,
               ta.name as toAccountName, ta.icon as toAccountIcon, ta.color as toAccountColor
        FROM transfers t
        INNER JOIN accounts fa ON t.fromAccountId = fa.id
        INNER JOIN accounts ta ON t.toAccountId = ta.id
        WHERE t.date BETWEEN :startDate AND :endDate
        ORDER BY t.date DESC
    """)
    suspend fun getTransfersWithAccountsByDateRange(startDate: Long, endDate: Long): List<TransferAccountInfo>

    @Query("SELECT COUNT(*) FROM transfers")
    suspend fun getTransferCount(): Int

    @Query("SELECT SUM(fee) FROM transfers WHERE date BETWEEN :startDate AND :endDate")
    suspend fun getTotalFeesByDateRange(startDate: Long, endDate: Long): Double?
}

/**
 * 转账账户信息
 */
data class TransferAccountInfo(
    val id: Long,
    val notebookId: Long,
    val fromAccountId: Long,
    val toAccountId: Long,
    val amount: Double,
    val fee: Double,
    val note: String,
    val date: Long,
    val fromAccountName: String,
    val fromAccountIcon: String,
    val fromAccountColor: String,
    val toAccountName: String,
    val toAccountIcon: String,
    val toAccountColor: String
)
