package com.ai.bookkeeping.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.ai.bookkeeping.AIBookkeepingApp
import com.ai.bookkeeping.data.BudgetCategoryInfo
import com.ai.bookkeeping.data.BudgetRepository
import com.ai.bookkeeping.data.TransactionRepository
import com.ai.bookkeeping.model.Budget
import com.ai.bookkeeping.model.BudgetPeriod
import com.ai.bookkeeping.model.BudgetWithUsage
import com.ai.bookkeeping.model.TransactionType
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * 预算管理ViewModel
 */
class BudgetViewModel(application: Application) : AndroidViewModel(application) {

    private val budgetRepository: BudgetRepository = (application as AIBookkeepingApp).budgetRepository
    private val transactionRepository: TransactionRepository = (application as AIBookkeepingApp).repository

    val allBudgets: LiveData<List<Budget>> = budgetRepository.allBudgets.asLiveData()

    private val _currentMonthBudgets = MutableLiveData<List<BudgetWithUsage>>()
    val currentMonthBudgets: LiveData<List<BudgetWithUsage>> = _currentMonthBudgets

    private val _totalBudget = MutableLiveData<BudgetWithUsage?>()
    val totalBudget: LiveData<BudgetWithUsage?> = _totalBudget

    private val _selectedYear = MutableLiveData<Int>()
    val selectedYear: LiveData<Int> = _selectedYear

    private val _selectedMonth = MutableLiveData<Int>()
    val selectedMonth: LiveData<Int> = _selectedMonth

    private val _operationResult = MutableLiveData<OperationResult>()
    val operationResult: LiveData<OperationResult> = _operationResult

    init {
        val calendar = Calendar.getInstance()
        _selectedYear.value = calendar.get(Calendar.YEAR)
        _selectedMonth.value = calendar.get(Calendar.MONTH) + 1
    }

    fun loadBudgetsForMonth(notebookId: Long, year: Int, month: Int) = viewModelScope.launch {
        _selectedYear.value = year
        _selectedMonth.value = month

        // 获取月份的开始和结束时间
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1, 0, 0, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        // 获取预算列表
        val budgets = budgetRepository.getMonthlyBudgetsSync(year, month, notebookId)
        val budgetWithUsageList = mutableListOf<BudgetWithUsage>()

        for (budget in budgets) {
            val usedAmount = if (budget.categoryId == null) {
                // 总预算：计算所有支出
                transactionRepository.getTotalByTypeAndDateRangeSync(
                    TransactionType.EXPENSE, startOfMonth, endOfMonth
                ) ?: 0.0
            } else {
                // 分类预算：计算该分类支出
                // 需要先获取分类名称
                val categoryInfo = budgetRepository.getBudgetsWithCategory(notebookId, year, month)
                    .find { it.id == budget.id }
                if (categoryInfo != null) {
                    val categoryTotals = transactionRepository.getCategoryTotalsSync(
                        TransactionType.EXPENSE, startOfMonth, endOfMonth
                    )
                    categoryTotals.find { it.category == categoryInfo.categoryName }?.total ?: 0.0
                } else {
                    0.0
                }
            }

            val remaining = budget.amount - usedAmount
            val percentage = if (budget.amount > 0) (usedAmount / budget.amount * 100).toFloat() else 0f

            // 获取分类信息
            val categoryInfo = if (budget.categoryId != null) {
                budgetRepository.getBudgetsWithCategory(notebookId, year, month)
                    .find { it.id == budget.id }
            } else null

            budgetWithUsageList.add(
                BudgetWithUsage(
                    budget = budget,
                    categoryName = categoryInfo?.categoryName,
                    categoryIcon = categoryInfo?.categoryIcon,
                    categoryColor = categoryInfo?.categoryColor,
                    usedAmount = usedAmount,
                    remainingAmount = remaining,
                    usagePercentage = percentage
                )
            )
        }

        // 分离总预算和分类预算
        val totalBudgetItem = budgetWithUsageList.find { it.budget.categoryId == null }
        val categoryBudgets = budgetWithUsageList.filter { it.budget.categoryId != null }

        _totalBudget.value = totalBudgetItem
        _currentMonthBudgets.value = categoryBudgets
    }

    fun insert(budget: Budget) = viewModelScope.launch {
        try {
            budgetRepository.insert(budget)
            _operationResult.value = OperationResult.Success("预算添加成功")
            // 重新加载
            loadBudgetsForMonth(
                budget.notebookId,
                _selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR),
                _selectedMonth.value ?: Calendar.getInstance().get(Calendar.MONTH) + 1
            )
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("添加失败: ${e.message}")
        }
    }

    fun update(budget: Budget) = viewModelScope.launch {
        try {
            budgetRepository.update(budget)
            _operationResult.value = OperationResult.Success("预算更新成功")
            loadBudgetsForMonth(
                budget.notebookId,
                _selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR),
                _selectedMonth.value ?: Calendar.getInstance().get(Calendar.MONTH) + 1
            )
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("更新失败: ${e.message}")
        }
    }

    fun delete(budget: Budget) = viewModelScope.launch {
        try {
            budgetRepository.deactivateBudget(budget.id)
            _operationResult.value = OperationResult.Success("预算删除成功")
            loadBudgetsForMonth(
                budget.notebookId,
                _selectedYear.value ?: Calendar.getInstance().get(Calendar.YEAR),
                _selectedMonth.value ?: Calendar.getInstance().get(Calendar.MONTH) + 1
            )
        } catch (e: Exception) {
            _operationResult.value = OperationResult.Error("删除失败: ${e.message}")
        }
    }

    suspend fun getBudgetById(id: Long): Budget? {
        return budgetRepository.getBudgetById(id)
    }

    sealed class OperationResult {
        data class Success(val message: String) : OperationResult()
        data class Error(val message: String) : OperationResult()
    }
}
