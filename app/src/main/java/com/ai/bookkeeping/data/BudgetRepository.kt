package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import com.ai.bookkeeping.model.Budget
import com.ai.bookkeeping.model.BudgetPeriod
import kotlinx.coroutines.flow.Flow

/**
 * 预算仓库类
 */
class BudgetRepository(private val budgetDao: BudgetDao) {

    val allBudgets: Flow<List<Budget>> = budgetDao.getAllBudgets()
    val allBudgetsLiveData: LiveData<List<Budget>> = budgetDao.getAllBudgetsLiveData()

    suspend fun insert(budget: Budget): Long {
        return budgetDao.insert(budget)
    }

    suspend fun update(budget: Budget) {
        budgetDao.update(budget)
    }

    suspend fun delete(budget: Budget) {
        budgetDao.delete(budget)
    }

    suspend fun deleteById(id: Long) {
        budgetDao.deleteById(id)
    }

    fun getBudgetsByNotebook(notebookId: Long): Flow<List<Budget>> {
        return budgetDao.getBudgetsByNotebook(notebookId)
    }

    fun getBudgetsByNotebookLiveData(notebookId: Long): LiveData<List<Budget>> {
        return budgetDao.getBudgetsByNotebookLiveData(notebookId)
    }

    suspend fun getBudgetById(id: Long): Budget? {
        return budgetDao.getBudgetById(id)
    }

    fun getBudgetsByPeriod(periodType: BudgetPeriod, year: Int): Flow<List<Budget>> {
        return budgetDao.getBudgetsByPeriod(periodType, year)
    }

    fun getMonthlyBudgets(periodType: BudgetPeriod, year: Int, month: Int): Flow<List<Budget>> {
        return budgetDao.getMonthlyBudgets(periodType, year, month)
    }

    suspend fun getMonthlyBudgetsSync(year: Int, month: Int, notebookId: Long): List<Budget> {
        return budgetDao.getMonthlyBudgetsSync(year, month, notebookId)
    }

    suspend fun getTotalMonthlyBudget(year: Int, month: Int, notebookId: Long): Budget? {
        return budgetDao.getTotalMonthlyBudget(year, month, notebookId)
    }

    suspend fun getCategoryBudget(categoryId: Long, year: Int, month: Int): Budget? {
        return budgetDao.getCategoryBudget(categoryId, year, month)
    }

    suspend fun getBudgetsWithCategory(notebookId: Long, year: Int, month: Int): List<BudgetCategoryInfo> {
        return budgetDao.getBudgetsWithCategory(notebookId, year, month)
    }

    suspend fun deactivateBudget(id: Long) {
        budgetDao.deactivateBudget(id)
    }

    suspend fun getBudgetCount(): Int {
        return budgetDao.getBudgetCount()
    }

    companion object {
        @Volatile
        private var INSTANCE: BudgetRepository? = null

        fun getInstance(budgetDao: BudgetDao): BudgetRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = BudgetRepository(budgetDao)
                INSTANCE = instance
                instance
            }
        }
    }
}
