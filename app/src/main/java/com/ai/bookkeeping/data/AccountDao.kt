package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ai.bookkeeping.model.Account
import com.ai.bookkeeping.model.AccountType
import kotlinx.coroutines.flow.Flow

/**
 * 账户数据访问对象
 */
@Dao
interface AccountDao {

    @Insert
    suspend fun insert(account: Account): Long

    @Insert
    suspend fun insertAll(accounts: List<Account>)

    @Update
    suspend fun update(account: Account)

    @Delete
    suspend fun delete(account: Account)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY sortOrder")
    fun getAllAccounts(): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY sortOrder")
    fun getAllAccountsLiveData(): LiveData<List<Account>>

    @Query("SELECT * FROM accounts WHERE isActive = 1 ORDER BY sortOrder")
    suspend fun getAllAccountsSync(): List<Account>

    @Query("SELECT * FROM accounts WHERE notebookId = :notebookId AND isActive = 1 ORDER BY sortOrder")
    fun getAccountsByNotebook(notebookId: Long): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE notebookId = :notebookId AND isActive = 1 ORDER BY sortOrder")
    fun getAccountsByNotebookLiveData(notebookId: Long): LiveData<List<Account>>

    @Query("SELECT * FROM accounts WHERE notebookId = :notebookId AND isActive = 1 ORDER BY sortOrder")
    suspend fun getAccountsByNotebookSync(notebookId: Long): List<Account>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): Account?

    @Query("SELECT * FROM accounts WHERE type = :type AND isActive = 1 ORDER BY sortOrder")
    fun getAccountsByType(type: AccountType): Flow<List<Account>>

    @Query("SELECT * FROM accounts WHERE isDefault = 1 AND isActive = 1 LIMIT 1")
    suspend fun getDefaultAccount(): Account?

    @Query("UPDATE accounts SET isDefault = 0")
    suspend fun clearDefaultAccounts()

    @Query("UPDATE accounts SET isDefault = 1 WHERE id = :id")
    suspend fun setDefaultAccount(id: Long)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE id = :id")
    suspend fun updateBalance(id: Long, amount: Double)

    @Query("UPDATE accounts SET balance = :balance WHERE id = :id")
    suspend fun setBalance(id: Long, balance: Double)

    @Query("SELECT SUM(balance) FROM accounts WHERE isActive = 1")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(balance) FROM accounts WHERE notebookId = :notebookId AND isActive = 1")
    fun getTotalBalanceByNotebook(notebookId: Long): Flow<Double?>

    @Query("SELECT COUNT(*) FROM accounts WHERE isActive = 1")
    suspend fun getAccountCount(): Int

    @Query("SELECT MAX(sortOrder) FROM accounts")
    suspend fun getMaxSortOrder(): Int?

    @Query("UPDATE accounts SET isActive = 0 WHERE id = :id")
    suspend fun deactivateAccount(id: Long)

    @Transaction
    suspend fun setAsDefault(id: Long) {
        clearDefaultAccounts()
        setDefaultAccount(id)
    }
}
