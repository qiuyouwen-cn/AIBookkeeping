package com.ai.bookkeeping.data

import androidx.lifecycle.LiveData
import androidx.room.*
import com.ai.bookkeeping.model.Budget
import com.ai.bookkeeping.model.BudgetPeriod
import com.ai.bookkeeping.model.BudgetWithUsage
import kotlinx.coroutines.flow.Flow

/**
 * 预算数据访问对象
 */
@Dao
interface BudgetDao {

    @Insert
    suspend fun insert(budget: Budget): Long

    @Update
    suspend fun update(budget: Budget)

    @Delete
    suspend fun delete(budget: Budget)

    @Query("DELETE FROM budgets WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY year DESC, month DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE isActive = 1 ORDER BY year DESC, month DESC")
    fun getAllBudgetsLiveData(): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE notebookId = :notebookId AND isActive = 1 ORDER BY year DESC, month DESC")
    fun getBudgetsByNotebook(notebookId: Long): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE notebookId = :notebookId AND isActive = 1 ORDER BY year DESC, month DESC")
    fun getBudgetsByNotebookLiveData(notebookId: Long): LiveData<List<Budget>>

    @Query("SELECT * FROM budgets WHERE id = :id")
    suspend fun getBudgetById(id: Long): Budget?

    @Query("SELECT * FROM budgets WHERE periodType = :periodType AND year = :year AND isActive = 1")
    fun getBudgetsByPeriod(periodType: BudgetPeriod, year: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE periodType = :periodType AND year = :year AND month = :month AND isActive = 1")
    fun getMonthlyBudgets(periodType: BudgetPeriod, year: Int, month: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budgets WHERE periodType = 'MONTHLY' AND year = :year AND month = :month AND notebookId = :notebookId AND isActive = 1")
    suspend fun getMonthlyBudgetsSync(year: Int, month: Int, notebookId: Long): List<Budget>

    @Query("SELECT * FROM budgets WHERE categoryId IS NULL AND periodType = 'MONTHLY' AND year = :year AND month = :month AND notebookId = :notebookId AND isActive = 1 LIMIT 1")
    suspend fun getTotalMonthlyBudget(year: Int, month: Int, notebookId: Long): Budget?

    @Query("SELECT * FROM budgets WHERE categoryId = :categoryId AND periodType = 'MONTHLY' AND year = :year AND month = :month AND isActive = 1 LIMIT 1")
    suspend fun getCategoryBudget(categoryId: Long, year: Int, month: Int): Budget?

    @Query("UPDATE budgets SET isActive = 0 WHERE id = :id")
    suspend fun deactivateBudget(id: Long)

    @Query("SELECT COUNT(*) FROM budgets WHERE isActive = 1")
    suspend fun getBudgetCount(): Int

    @Query("""
        SELECT b.*, c.name as categoryName, c.icon as categoryIcon, c.color as categoryColor
        FROM budgets b
        LEFT JOIN categories c ON b.categoryId = c.id
        WHERE b.notebookId = :notebookId AND b.periodType = 'MONTHLY' AND b.year = :year AND b.month = :month AND b.isActive = 1
    """)
    suspend fun getBudgetsWithCategory(notebookId: Long, year: Int, month: Int): List<BudgetCategoryInfo>
}

/**
 * 预算分类信息
 */
data class BudgetCategoryInfo(
    val id: Long,
    val notebookId: Long,
    val categoryId: Long?,
    val amount: Double,
    val periodType: BudgetPeriod,
    val year: Int,
    val month: Int?,
    val week: Int?,
    val quarter: Int?,
    val isActive: Boolean,
    val createdAt: Long,
    val categoryName: String?,
    val categoryIcon: String?,
    val categoryColor: String?
)
