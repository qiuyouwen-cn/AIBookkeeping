package com.ai.bookkeeping.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ai.bookkeeping.AIBookkeepingApp
import com.ai.bookkeeping.data.CategoryTotal
import com.ai.bookkeeping.data.TransactionRepository
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 交易记录ViewModel
 */
class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository = (application as AIBookkeepingApp).repository

    val allTransactions: LiveData<List<Transaction>> = repository.allTransactions

    private val _currentMonthIncome = MutableLiveData<Double>()
    val currentMonthIncome: LiveData<Double> = _currentMonthIncome

    private val _currentMonthExpense = MutableLiveData<Double>()
    val currentMonthExpense: LiveData<Double> = _currentMonthExpense

    init {
        loadCurrentMonthSummary()
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
        loadCurrentMonthSummary()
    }

    fun update(transaction: Transaction) = viewModelScope.launch {
        repository.update(transaction)
        loadCurrentMonthSummary()
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
        loadCurrentMonthSummary()
    }

    private fun loadCurrentMonthSummary() {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        repository.getTotalByTypeAndDateRange(TransactionType.INCOME, startOfMonth, endOfMonth)
            .observeForever { income ->
                _currentMonthIncome.value = income ?: 0.0
            }

        repository.getTotalByTypeAndDateRange(TransactionType.EXPENSE, startOfMonth, endOfMonth)
            .observeForever { expense ->
                _currentMonthExpense.value = expense ?: 0.0
            }
    }

    fun getTransactionsByDateRange(startDate: Long, endDate: Long): LiveData<List<Transaction>> {
        return repository.getTransactionsByDateRange(startDate, endDate)
    }

    fun getCategoryTotals(type: TransactionType, startDate: Long, endDate: Long): LiveData<List<CategoryTotal>> {
        return repository.getCategoryTotals(type, startDate, endDate)
    }

    private val _recentNotes = MutableLiveData<List<String>>()
    val recentNotes: LiveData<List<String>> = _recentNotes

    fun loadRecentNotes(limit: Int = 20) {
        viewModelScope.launch {
            _recentNotes.value = repository.getRecentNotes(limit)
        }
    }

    fun loadRecentNotesByCategory(category: String, limit: Int = 10) {
        viewModelScope.launch {
            _recentNotes.value = repository.getRecentNotesByCategory(category, limit)
        }
    }
}
