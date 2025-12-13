package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import com.ai.bookkeeping.model.Account
import com.ai.bookkeeping.model.AccountType
import kotlinx.coroutines.flow.Flow

/**
 * 账户仓库类
 */
class AccountRepository(private val accountDao: AccountDao) {

    val allAccounts: Flow<List<Account>> = accountDao.getAllAccounts()
    val allAccountsLiveData: LiveData<List<Account>> = accountDao.getAllAccountsLiveData()
    val totalBalance: Flow<Double?> = accountDao.getTotalBalance()

    suspend fun insert(account: Account): Long {
        return accountDao.insert(account)
    }

    suspend fun insertAll(accounts: List<Account>) {
        accountDao.insertAll(accounts)
    }

    suspend fun update(account: Account) {
        accountDao.update(account)
    }

    suspend fun delete(account: Account) {
        accountDao.delete(account)
    }

    suspend fun deleteById(id: Long) {
        accountDao.deleteById(id)
    }

    suspend fun getAllAccountsSync(): List<Account> {
        return accountDao.getAllAccountsSync()
    }

    fun getAccountsByNotebook(notebookId: Long): Flow<List<Account>> {
        return accountDao.getAccountsByNotebook(notebookId)
    }

    fun getAccountsByNotebookLiveData(notebookId: Long): LiveData<List<Account>> {
        return accountDao.getAccountsByNotebookLiveData(notebookId)
    }

    suspend fun getAccountsByNotebookSync(notebookId: Long): List<Account> {
        return accountDao.getAccountsByNotebookSync(notebookId)
    }

    suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)
    }

    fun getAccountsByType(type: AccountType): Flow<List<Account>> {
        return accountDao.getAccountsByType(type)
    }

    suspend fun getDefaultAccount(): Account? {
        return accountDao.getDefaultAccount()
    }

    suspend fun setAsDefault(id: Long) {
        accountDao.setAsDefault(id)
    }

    suspend fun updateBalance(id: Long, amount: Double) {
        accountDao.updateBalance(id, amount)
    }

    suspend fun setBalance(id: Long, balance: Double) {
        accountDao.setBalance(id, balance)
    }

    fun getTotalBalanceByNotebook(notebookId: Long): Flow<Double?> {
        return accountDao.getTotalBalanceByNotebook(notebookId)
    }

    suspend fun getAccountCount(): Int {
        return accountDao.getAccountCount()
    }

    suspend fun getMaxSortOrder(): Int {
        return accountDao.getMaxSortOrder() ?: 0
    }

    suspend fun deactivateAccount(id: Long) {
        accountDao.deactivateAccount(id)
    }

    companion object {
        @Volatile
        private var INSTANCE: AccountRepository? = null

        fun getInstance(accountDao: AccountDao): AccountRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = AccountRepository(accountDao)
                INSTANCE = instance
                instance
            }
        }
    }
}
