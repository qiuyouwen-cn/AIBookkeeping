package com.ai.bookkeeping.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ai.bookkeeping.AIBookkeepingApp
import com.ai.bookkeeping.data.TransferAccountInfo
import com.ai.bookkeeping.data.TransferRepository
import com.ai.bookkeeping.model.Transfer
import kotlinx.coroutines.launch

/**
 * 转账记录ViewModel
 */
class TransferViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransferRepository = (application as AIBookkeepingApp).transferRepository

    val allTransfers: LiveData<List<Transfer>> = repository.allTransfers.asLiveData()

    private val _transfersWithAccounts = MutableLiveData<List<TransferAccountInfo>>()
    val transfersWithAccounts: LiveData<List<TransferAccountInfo>> = _transfersWithAccounts

    private val _operationResult = MutableLiveData<OperationResult>()
    val operationResult: LiveData<OperationResult> = _operationResult

    /**
     * 执行转账
     */
    fun executeTransfer(
        notebookId: Long,
        fromAccountId: Long,
        toAccountId: Long,
        amount: Double,
        fee: Double = 0.0,
        note: String = "",
        date: Long = System.currentTimeMillis()
    ) = viewModelScope.launch {
        try {
            if (fromAccountId == toAccountId) {
                _operationResult.value = OperationResult.Error("转出和转入账户不能相同")
                return@launch
            }
            if (amount <= 0) {
                _operationResult.value = OperationResult.Error("转账金额必须大于0")
                return@launch
            }

            val transfer = Transfer(
                notebookId = notebookId,
                fromAccountId = fromAccountId,
                toAccountId = toAccountId,
                amount = amount,
                fee = fee,
                note = note,
                date = date
            )
            repository.executeTransfer(transfer)
            _operationResult.value = OperationResult.Success("转账成功")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("转账失败: ${e.message}")
        }
    }

    fun delete(transfer: Transfer) = viewModelScope.launch {
        try {
            repository.delete(transfer)
            _operationResult.value = OperationResult.Success("转账记录已删除")
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("删除失败: ${e.message}")
        }
    }

    fun loadTransfersWithAccounts(notebookId: Long) = viewModelScope.launch {
        repository.getTransfersWithAccounts(notebookId).collect { transfers ->
            _transfersWithAccounts.value = transfers
        }
    }

    fun getTransfersByNotebook(notebookId: Long): LiveData<List<Transfer>> {
        return repository.getTransfersByNotebookLiveData(notebookId)
    }

    suspend fun getTransferById(id: Long): Transfer? {
        return repository.getTransferById(id)
    }

    suspend fun getTransfersByDateRange(startDate: Long, endDate: Long): List<Transfer> {
        return repository.getTransfersByDateRangeSync(startDate, endDate)
    }

    suspend fun getTotalFeesByDateRange(startDate: Long, endDate: Long): Double {
        return repository.getTotalFeesByDateRange(startDate, endDate)
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}
