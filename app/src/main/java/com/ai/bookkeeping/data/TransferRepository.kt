package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import com.ai.bookkeeping.model.Transfer
import kotlinx.coroutines.flow.Flow

/**
 * 转账记录仓库类
 */
class TransferRepository(
    private val transferDao: TransferDao,
    private val accountDao: AccountDao
) {

    val allTransfers: Flow<List<Transfer>> = transferDao.getAllTransfers()
    val allTransfersLiveData: LiveData<List<Transfer>> = transferDao.getAllTransfersLiveData()

    /**
     * 执行转账操作
     * 同时更新两个账户的余额
     */
    suspend fun executeTransfer(transfer: Transfer): Long {
        // 插入转账记录
        val transferId = transferDao.insert(transfer)

        // 从转出账户扣款（包含手续费）
        accountDao.updateBalance(transfer.fromAccountId, -(transfer.amount + transfer.fee))

        // 向转入账户加款
        accountDao.updateBalance(transfer.toAccountId, transfer.amount)

        return transferId
    }

    suspend fun insert(transfer: Transfer): Long {
        return transferDao.insert(transfer)
    }

    suspend fun update(transfer: Transfer) {
        transferDao.update(transfer)
    }

    suspend fun delete(transfer: Transfer) {
        // 回滚余额变化
        accountDao.updateBalance(transfer.fromAccountId, transfer.amount + transfer.fee)
        accountDao.updateBalance(transfer.toAccountId, -transfer.amount)

        // 删除转账记录
        transferDao.delete(transfer)
    }

    suspend fun deleteById(id: Long) {
        val transfer = transferDao.getTransferById(id)
        if (transfer != null) {
            delete(transfer)
        }
    }

    fun getTransfersByNotebook(notebookId: Long): Flow<List<Transfer>> {
        return transferDao.getTransfersByNotebook(notebookId)
    }

    fun getTransfersByNotebookLiveData(notebookId: Long): LiveData<List<Transfer>> {
        return transferDao.getTransfersByNotebookLiveData(notebookId)
    }

    suspend fun getTransferById(id: Long): Transfer? {
        return transferDao.getTransferById(id)
    }

    fun getTransfersByAccount(accountId: Long): Flow<List<Transfer>> {
        return transferDao.getTransfersByAccount(accountId)
    }

    fun getTransfersByDateRange(startDate: Long, endDate: Long): Flow<List<Transfer>> {
        return transferDao.getTransfersByDateRange(startDate, endDate)
    }

    suspend fun getTransfersByDateRangeSync(startDate: Long, endDate: Long): List<Transfer> {
        return transferDao.getTransfersByDateRangeSync(startDate, endDate)
    }

    fun getTransfersWithAccounts(notebookId: Long): Flow<List<TransferAccountInfo>> {
        return transferDao.getTransfersWithAccounts(notebookId)
    }

    suspend fun getTransfersWithAccountsByDateRange(startDate: Long, endDate: Long): List<TransferAccountInfo> {
        return transferDao.getTransfersWithAccountsByDateRange(startDate, endDate)
    }

    suspend fun getTransferCount(): Int {
        return transferDao.getTransferCount()
    }

    suspend fun getTotalFeesByDateRange(startDate: Long, endDate: Long): Double {
        return transferDao.getTotalFeesByDateRange(startDate, endDate) ?: 0.0
    }

    companion object {
        @Volatile
        private var INSTANCE: TransferRepository? = null

        fun getInstance(transferDao: TransferDao, accountDao: AccountDao): TransferRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = TransferRepository(transferDao, accountDao)
                INSTANCE = instance
                instance
            }
        }
    }
}
