package com.ai.bookkeeping.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ai.bookkeeping.AIBookkeepingApp
import com.ai.bookkeeping.data.NotebookRepository
import com.ai.bookkeeping.data.TransactionRepository
import com.ai.bookkeeping.model.Notebook
import com.ai.bookkeeping.model.NotebookWithStats
import com.ai.bookkeeping.model.TransactionType
import kotlinx.coroutines.launch

/**
 * 账本管理ViewModel
 */
class NotebookViewModel(application: Application) : AndroidViewModel(application) {

    private val notebookRepository: NotebookRepository = (application as AIBookkeepingApp).notebookRepository
    private val transactionRepository: TransactionRepository = (application as AIBookkeepingApp).repository

    val allNotebooks: LiveData<List<Notebook>> = notebookRepository.allNotebooks.asLiveData()

    private val _currentNotebook = MutableLiveData<Notebook?>()
    val currentNotebook: LiveData<Notebook?> = _currentNotebook

    private val _notebooksWithStats = MutableLiveData<List<NotebookWithStats>>()
    val notebooksWithStats: LiveData<List<NotebookWithStats>> = _notebooksWithStats

    private val _operationResult = MutableLiveData<OperationResult>()
    val operationResult: LiveData<OperationResult> = _operationResult

    init {
        loadDefaultNotebook()
    }

    private fun loadDefaultNotebook() = viewModelScope.launch {
        val defaultNotebook = notebookRepository.getDefaultNotebookSync()
        _currentNotebook.value = defaultNotebook
    }

    fun loadNotebooksWithStats() = viewModelScope.launch {
        val notebooks = notebookRepository.getAllNotebooksSync()
        val notebooksWithStatsList = notebooks.map { notebook ->
            // 获取该账本的统计数据
            val transactions = transactionRepository.getTransactionsByDateRangeSync(0, Long.MAX_VALUE)
                .filter { it.notebookId == notebook.id }

            val totalIncome = transactions
                .filter { it.type == TransactionType.INCOME }
                .sumOf { it.amount }

            val totalExpense = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            NotebookWithStats(
                notebook = notebook,
                totalIncome = totalIncome,
                totalExpense = totalExpense,
                balance = totalIncome - totalExpense,
                transactionCount = transactions.size
            )
        }
        _notebooksWithStats.value = notebooksWithStatsList
    }

    fun insert(notebook: Notebook) = viewModelScope.launch {
        try {
            val sortOrder = notebookRepository.getMaxSortOrder() + 1
            notebookRepository.insert(notebook.copy(sortOrder = sortOrder))
            _operationResult.value = OperationResult.Success("账本创建成功")
            loadNotebooksWithStats()
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("创建失败: ${e.message}")
        }
    }

    fun update(notebook: Notebook) = viewModelScope.launch {
        try {
            notebookRepository.update(notebook)
            _operationResult.value = OperationResult.Success("账本更新成功")
            loadNotebooksWithStats()
            // 如果更新的是当前账本，同步更新
            if (_currentNotebook.value?.id == notebook.id) {
                _currentNotebook.value = notebook
            }
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("更新失败: ${e.message}")
        }
    }

    fun delete(notebook: Notebook) = viewModelScope.launch {
        try {
            // 不能删除默认账本
            if (notebook.isDefault) {
                _operationResult.value = OperationResult.Error("默认账本不能删除")
                return@launch
            }
            notebookRepository.deactivateNotebook(notebook.id)
            _operationResult.value = OperationResult.Success("账本删除成功")
            loadNotebooksWithStats()
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("删除失败: ${e.message}")
        }
    }

    fun setAsDefault(notebook: Notebook) = viewModelScope.launch {
        try {
            notebookRepository.setAsDefault(notebook.id)
            _operationResult.value = OperationResult.Success("已设为默认账本")
            loadNotebooksWithStats()
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("设置失败: ${e.message}")
        }
    }

    fun switchNotebook(notebook: Notebook) {
        _currentNotebook.value = notebook
    }

    suspend fun getNotebookById(id: Long): Notebook? {
        return notebookRepository.getNotebookById(id)
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}
