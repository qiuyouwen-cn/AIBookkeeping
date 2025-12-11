package com.ai.bookkeeping.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ai.bookkeeping.ai.AIService
import com.ai.bookkeeping.data.AppDatabase
import com.ai.bookkeeping.data.CategoryTotal
import com.ai.bookkeeping.data.DailyTotal
import com.ai.bookkeeping.data.MonthlyTotal
import com.ai.bookkeeping.data.TransactionRepository
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    // 统计周期
    enum class StatsPeriod {
        TODAY, THIS_WEEK, THIS_MONTH, THIS_YEAR, CUSTOM
    }

    // 当前选中的周期
    private val _currentPeriod = MutableLiveData(StatsPeriod.THIS_MONTH)
    val currentPeriod: LiveData<StatsPeriod> = _currentPeriod

    // 自定义日期范围
    private val _startDate = MutableLiveData<Long>()
    val startDate: LiveData<Long> = _startDate

    private val _endDate = MutableLiveData<Long>()
    val endDate: LiveData<Long> = _endDate

    // 统计数据
    private val _totalIncome = MutableLiveData<Double>()
    val totalIncome: LiveData<Double> = _totalIncome

    private val _totalExpense = MutableLiveData<Double>()
    val totalExpense: LiveData<Double> = _totalExpense

    private val _transactionCount = MutableLiveData<Int>()
    val transactionCount: LiveData<Int> = _transactionCount

    private val _expenseCategories = MutableLiveData<List<CategoryTotal>>()
    val expenseCategories: LiveData<List<CategoryTotal>> = _expenseCategories

    private val _incomeCategories = MutableLiveData<List<CategoryTotal>>()
    val incomeCategories: LiveData<List<CategoryTotal>> = _incomeCategories

    private val _dailyExpenses = MutableLiveData<List<DailyTotal>>()
    val dailyExpenses: LiveData<List<DailyTotal>> = _dailyExpenses

    private val _dailyIncomes = MutableLiveData<List<DailyTotal>>()
    val dailyIncomes: LiveData<List<DailyTotal>> = _dailyIncomes

    private val _monthlyExpenses = MutableLiveData<List<MonthlyTotal>>()
    val monthlyExpenses: LiveData<List<MonthlyTotal>> = _monthlyExpenses

    private val _monthlyIncomes = MutableLiveData<List<MonthlyTotal>>()
    val monthlyIncomes: LiveData<List<MonthlyTotal>> = _monthlyIncomes

    private val _transactions = MutableLiveData<List<Transaction>>()
    val transactions: LiveData<List<Transaction>> = _transactions

    // AI分析结果
    private val _aiAnalysis = MutableLiveData<String?>()
    val aiAnalysis: LiveData<String?> = _aiAnalysis

    private val _isAnalyzing = MutableLiveData(false)
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing

    private val _analysisError = MutableLiveData<String?>()
    val analysisError: LiveData<String?> = _analysisError

    init {
        val transactionDao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository.getInstance(transactionDao)

        // 初始化为本月
        setPeriod(StatsPeriod.THIS_MONTH)
    }

    fun setPeriod(period: StatsPeriod) {
        _currentPeriod.value = period

        val calendar = Calendar.getInstance()
        val end = System.currentTimeMillis()
        val start: Long

        when (period) {
            StatsPeriod.TODAY -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
            }
            StatsPeriod.THIS_WEEK -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
            }
            StatsPeriod.THIS_MONTH -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
            }
            StatsPeriod.THIS_YEAR -> {
                calendar.set(Calendar.DAY_OF_YEAR, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                start = calendar.timeInMillis
            }
            StatsPeriod.CUSTOM -> {
                // 自定义周期，不自动设置日期
                return
            }
        }

        setDateRange(start, end)
    }

    fun setDateRange(start: Long, end: Long) {
        _startDate.value = start
        _endDate.value = end
        loadStatistics(start, end)
    }

    private fun loadStatistics(start: Long, end: Long) {
        viewModelScope.launch {
            // 总收入
            val income = repository.getTotalByTypeAndDateRangeSync(TransactionType.INCOME, start, end) ?: 0.0
            _totalIncome.postValue(income)

            // 总支出
            val expense = repository.getTotalByTypeAndDateRangeSync(TransactionType.EXPENSE, start, end) ?: 0.0
            _totalExpense.postValue(expense)

            // 交易笔数
            val count = repository.getTransactionCountByDateRange(start, end)
            _transactionCount.postValue(count)

            // 支出分类
            val expenseCats = repository.getCategoryTotalsSync(TransactionType.EXPENSE, start, end)
            _expenseCategories.postValue(expenseCats)

            // 收入分类
            val incomeCats = repository.getCategoryTotalsSync(TransactionType.INCOME, start, end)
            _incomeCategories.postValue(incomeCats)

            // 每日支出趋势
            val dailyExp = repository.getDailyTotals(TransactionType.EXPENSE, start, end)
            _dailyExpenses.postValue(dailyExp)

            // 每日收入趋势
            val dailyInc = repository.getDailyTotals(TransactionType.INCOME, start, end)
            _dailyIncomes.postValue(dailyInc)

            // 月度统计
            val monthlyExp = repository.getMonthlyTotals(TransactionType.EXPENSE, start, end)
            _monthlyExpenses.postValue(monthlyExp)

            val monthlyInc = repository.getMonthlyTotals(TransactionType.INCOME, start, end)
            _monthlyIncomes.postValue(monthlyInc)

            // 交易列表
            val trans = repository.getTransactionsByDateRangeSync(start, end)
            _transactions.postValue(trans)
        }
    }

    fun refreshData() {
        val start = _startDate.value ?: return
        val end = _endDate.value ?: return
        loadStatistics(start, end)
    }

    fun analyzeWithAI(context: Context) {
        if (_isAnalyzing.value == true) return

        _isAnalyzing.value = true
        _analysisError.value = null
        _aiAnalysis.value = null

        viewModelScope.launch {
            val income = _totalIncome.value ?: 0.0
            val expense = _totalExpense.value ?: 0.0
            val categories = _expenseCategories.value ?: emptyList()
            val count = _transactionCount.value ?: 0

            val periodName = when (_currentPeriod.value) {
                StatsPeriod.TODAY -> "今日"
                StatsPeriod.THIS_WEEK -> "本周"
                StatsPeriod.THIS_MONTH -> "本月"
                StatsPeriod.THIS_YEAR -> "今年"
                StatsPeriod.CUSTOM -> "自定义周期"
                else -> "选定周期"
            }

            val categoryBreakdown = categories.map { it.category to it.total }

            val result = AIService.analyzeFinancialData(
                context = context,
                totalIncome = income,
                totalExpense = expense,
                categoryBreakdown = categoryBreakdown,
                period = periodName,
                transactionCount = count
            )

            result.fold(
                onSuccess = { analysis ->
                    _aiAnalysis.postValue(analysis)
                    _isAnalyzing.postValue(false)
                },
                onFailure = { error ->
                    _analysisError.postValue(error.message ?: "分析失败")
                    _isAnalyzing.postValue(false)
                }
            )
        }
    }

    fun clearAnalysis() {
        _aiAnalysis.value = null
        _analysisError.value = null
    }

    fun getPeriodName(): String {
        return when (_currentPeriod.value) {
            StatsPeriod.TODAY -> "今日"
            StatsPeriod.THIS_WEEK -> "本周"
            StatsPeriod.THIS_MONTH -> "本月"
            StatsPeriod.THIS_YEAR -> "今年"
            StatsPeriod.CUSTOM -> "自定义"
            else -> ""
        }
    }

    // 导出数据
    fun getExportData(): ExportData {
        return ExportData(
            periodName = getPeriodName(),
            startDate = _startDate.value ?: 0L,
            endDate = _endDate.value ?: 0L,
            totalIncome = _totalIncome.value ?: 0.0,
            totalExpense = _totalExpense.value ?: 0.0,
            transactionCount = _transactionCount.value ?: 0,
            expenseCategories = _expenseCategories.value ?: emptyList(),
            incomeCategories = _incomeCategories.value ?: emptyList(),
            transactions = _transactions.value ?: emptyList()
        )
    }

    data class ExportData(
        val periodName: String,
        val startDate: Long,
        val endDate: Long,
        val totalIncome: Double,
        val totalExpense: Double,
        val transactionCount: Int,
        val expenseCategories: List<CategoryTotal>,
        val incomeCategories: List<CategoryTotal>,
        val transactions: List<Transaction>
    )
}
