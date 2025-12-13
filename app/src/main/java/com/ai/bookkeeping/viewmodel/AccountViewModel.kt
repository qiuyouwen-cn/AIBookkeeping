package com.ai.bookkeeping.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ai.bookkeeping.AIBookkeepingApp
import com.ai.bookkeeping.data.AccountRepository
import com.ai.bookkeeping.model.Account
import com.ai.bookkeeping.model.AccountType
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 账户管理ViewModel
 */
class AccountViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AccountRepository = (application as AIBookkeepingApp).accountRepository

    val allAccounts: LiveData<List<Account>> = repository.allAccounts.asLiveData()

    val totalBalance: LiveData<Double> = repository.totalBalance
        .map { it ?: 0.0 }
        .asLiveData()

    private val _selectedAccount = MutableLiveData<Account?>()
    val selectedAccount: LiveData<Account?> = _selectedAccount

    private val _operationResult = MutableLiveData<OperationResult>()
    val operationResult: LiveData<OperationResult> = _operationResult

    fun insert(account: Account) = viewModelScope.launch {
        try {
            val sortOrder = repository.getMaxSortOrder() + 1
            repository.insert(account.copy(sortOrder = sortOrder))
            _operationResult.value = OperationResult.Success("账户添加成功")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("添加失败: ${e.message}")
        }
    }

    fun update(account: Account) = viewModelScope.launch {
        try {
            repository.update(account)
            _operationResult.value = OperationResult.Success("账户更新成功")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("更新失败: ${e.message}")
        }
    }

    fun delete(account: Account) = viewModelScope.launch {
        try {
            repository.deactivateAccount(account.id)
            _operationResult.value = OperationResult.Success("账户删除成功")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("删除失败: ${e.message}")
        }
    }

    fun setAsDefault(account: Account) = viewModelScope.launch {
        try {
            repository.setAsDefault(account.id)
            _operationResult.value = OperationResult.Success("已设为默认账户")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("设置失败: ${e.message}")
        }
    }

    fun adjustBalance(accountId: Long, amount: Double) = viewModelScope.launch {
        try {
            repository.updateBalance(accountId, amount)
            _operationResult.value = OperationResult.Success("余额调整成功")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("调整失败: ${e.message}")
        }
    }

    fun setBalance(accountId: Long, balance: Double) = viewModelScope.launch {
        try {
            repository.setBalance(accountId, balance)
            _operationResult.value = OperationResult.Success("余额设置成功")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("设置失败: ${e.message}")
        }
    }

    fun selectAccount(account: Account?) {
        _selectedAccount.value = account
    }

    suspend fun getAccountById(id: Long): Account? {
        return repository.getAccountById(id)
    }

    suspend fun getDefaultAccount(): Account? {
        return repository.getDefaultAccount()
    }

    fun getAccountsByNotebook(notebookId: Long): LiveData<List<Account>> {
        return repository.getAccountsByNotebookLiveData(notebookId)
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}
