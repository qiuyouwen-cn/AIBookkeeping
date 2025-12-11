package com.ai.bookkeeping.ui

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.adapter.CategoryRankAdapter
import com.ai.bookkeeping.ai.AIService
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.util.ExportUtil
import com.ai.bookkeeping.viewmodel.StatisticsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class StatisticsFragment : Fragment() {

    private val viewModel: StatisticsViewModel by viewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)

    // Views
    private lateinit var chipToday: Chip
    private lateinit var chipWeek: Chip
    private lateinit var chipMonth: Chip
    private lateinit var chipYear: Chip
    private lateinit var chipCustom: Chip
    private lateinit var tvDateRange: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvBalance: TextView
    private lateinit var tvTransactionCount: TextView
    private lateinit var btnAiAnalyze: MaterialButton
    private lateinit var cardAiAnalysis: MaterialCardView
    private lateinit var progressAiAnalysis: ProgressBar
    private lateinit var tvAiAnalysis: TextView
    private lateinit var btnCloseAnalysis: ImageButton
    private lateinit var btnExport: ImageButton
    private lateinit var btnAiSettings: ImageButton
    private lateinit var tabChartType: TabLayout
    private lateinit var chipExpense: Chip
    private lateinit var chipIncome: Chip
    private lateinit var pieChart: PieChart
    private lateinit var barChart: BarChart
    private lateinit var lineChart: LineChart
    private lateinit var recyclerCategoryRank: RecyclerView
    private lateinit var tvEmptyRank: TextView

    private lateinit var categoryRankAdapter: CategoryRankAdapter
    private var currentChartType = 0 // 0: Pie, 1: Bar, 2: Line
    private var showExpense = true

    private val pieColors = listOf(
        Color.parseColor("#6C63FF"),
        Color.parseColor("#FF6B6B"),
        Color.parseColor("#4D96FF"),
        Color.parseColor("#6BCB77"),
        Color.parseColor("#C850C0"),
        Color.parseColor("#FF8E53"),
        Color.parseColor("#00D9FF"),
        Color.parseColor("#A66CFF"),
        Color.parseColor("#2EC4B6"),
        Color.parseColor("#FFD93D")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_statistics, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupCharts()
        setupRecyclerView()
        setupListeners()
        setupObservers()
    }

    private fun initViews(view: View) {
        chipToday = view.findViewById(R.id.chipToday)
        chipWeek = view.findViewById(R.id.chipWeek)
        chipMonth = view.findViewById(R.id.chipMonth)
        chipYear = view.findViewById(R.id.chipYear)
        chipCustom = view.findViewById(R.id.chipCustom)
        tvDateRange = view.findViewById(R.id.tvDateRange)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvBalance = view.findViewById(R.id.tvBalance)
        tvTransactionCount = view.findViewById(R.id.tvTransactionCount)
        btnAiAnalyze = view.findViewById(R.id.btnAiAnalyze)
        cardAiAnalysis = view.findViewById(R.id.cardAiAnalysis)
        progressAiAnalysis = view.findViewById(R.id.progressAiAnalysis)
        tvAiAnalysis = view.findViewById(R.id.tvAiAnalysis)
        btnCloseAnalysis = view.findViewById(R.id.btnCloseAnalysis)
        btnExport = view.findViewById(R.id.btnExport)
        btnAiSettings = view.findViewById(R.id.btnAiSettings)
        tabChartType = view.findViewById(R.id.tabChartType)
        chipExpense = view.findViewById(R.id.chipExpense)
        chipIncome = view.findViewById(R.id.chipIncome)
        pieChart = view.findViewById(R.id.pieChart)
        barChart = view.findViewById(R.id.barChart)
        lineChart = view.findViewById(R.id.lineChart)
        recyclerCategoryRank = view.findViewById(R.id.recyclerCategoryRank)
        tvEmptyRank = view.findViewById(R.id.tvEmptyRank)
    }

    private fun setupCharts() {
        // Pie Chart
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(10f)
            setEntryLabelColor(Color.WHITE)
            legend.isEnabled = false
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(0)
            holeRadius = 55f
            setDrawCenterText(true)
            setCenterTextSize(14f)
            setCenterTextColor(Color.parseColor("#333333"))
            setExtraOffsets(10f, 10f, 10f, 10f)
            animateY(800)
        }

        // Bar Chart
        barChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            setDrawBarShadow(false)
            setFitBars(true)
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                textColor = Color.parseColor("#6B7280")
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#6B7280")
                granularity = 1f
            }
            animateY(800)
        }

        // Line Chart
        lineChart.apply {
            description.isEnabled = false
            setDrawGridBackground(false)
            legend.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E5E7EB")
                textColor = Color.parseColor("#6B7280")
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                textColor = Color.parseColor("#6B7280")
                granularity = 1f
            }
            animateX(800)
        }
    }

    private fun setupRecyclerView() {
        categoryRankAdapter = CategoryRankAdapter()
        recyclerCategoryRank.layoutManager = LinearLayoutManager(requireContext())
        recyclerCategoryRank.adapter = categoryRankAdapter
    }

    private fun setupListeners() {
        // Period selection
        chipToday.setOnClickListener { viewModel.setPeriod(StatisticsViewModel.StatsPeriod.TODAY) }
        chipWeek.setOnClickListener { viewModel.setPeriod(StatisticsViewModel.StatsPeriod.THIS_WEEK) }
        chipMonth.setOnClickListener { viewModel.setPeriod(StatisticsViewModel.StatsPeriod.THIS_MONTH) }
        chipYear.setOnClickListener { viewModel.setPeriod(StatisticsViewModel.StatsPeriod.THIS_YEAR) }
        chipCustom.setOnClickListener { showDateRangePicker() }

        // Type selection
        chipExpense.setOnClickListener {
            showExpense = true
            updateCharts()
        }
        chipIncome.setOnClickListener {
            showExpense = false
            updateCharts()
        }

        // Chart type tab
        tabChartType.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentChartType = tab?.position ?: 0
                switchChartView()
                updateCharts()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // AI Analyze
        btnAiAnalyze.setOnClickListener {
            if (!AIService.hasApiKey(requireContext())) {
                showApiKeyDialog()
            } else {
                viewModel.analyzeWithAI(requireContext())
            }
        }

        btnCloseAnalysis.setOnClickListener {
            viewModel.clearAnalysis()
            cardAiAnalysis.visibility = View.GONE
        }

        // AI Settings
        btnAiSettings.setOnClickListener { showApiKeyDialog() }

        // Export
        btnExport.setOnClickListener { showExportDialog() }
    }

    private fun setupObservers() {
        viewModel.startDate.observe(viewLifecycleOwner) { updateDateRangeText() }
        viewModel.endDate.observe(viewLifecycleOwner) { updateDateRangeText() }

        viewModel.totalIncome.observe(viewLifecycleOwner) { income ->
            tvTotalIncome.text = currencyFormat.format(income ?: 0.0)
            updateBalance()
        }

        viewModel.totalExpense.observe(viewLifecycleOwner) { expense ->
            tvTotalExpense.text = currencyFormat.format(expense ?: 0.0)
            updateBalance()
        }

        viewModel.transactionCount.observe(viewLifecycleOwner) { count ->
            tvTransactionCount.text = count.toString()
        }

        viewModel.expenseCategories.observe(viewLifecycleOwner) {
            if (showExpense) updateCharts()
        }

        viewModel.incomeCategories.observe(viewLifecycleOwner) {
            if (!showExpense) updateCharts()
        }

        viewModel.dailyExpenses.observe(viewLifecycleOwner) {
            if (showExpense && currentChartType == 2) updateLineChart()
        }

        viewModel.dailyIncomes.observe(viewLifecycleOwner) {
            if (!showExpense && currentChartType == 2) updateLineChart()
        }

        viewModel.isAnalyzing.observe(viewLifecycleOwner) { isAnalyzing ->
            if (isAnalyzing) {
                cardAiAnalysis.visibility = View.VISIBLE
                progressAiAnalysis.visibility = View.VISIBLE
                tvAiAnalysis.text = "AI正在分析中，请稍候..."
            } else {
                progressAiAnalysis.visibility = View.GONE
            }
        }

        viewModel.aiAnalysis.observe(viewLifecycleOwner) { analysis ->
            if (analysis != null) {
                cardAiAnalysis.visibility = View.VISIBLE
                tvAiAnalysis.text = analysis
            }
        }

        viewModel.analysisError.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                cardAiAnalysis.visibility = View.VISIBLE
                tvAiAnalysis.text = "分析失败: $error"
            }
        }
    }

    private fun updateDateRangeText() {
        val start = viewModel.startDate.value ?: return
        val end = viewModel.endDate.value ?: return
        tvDateRange.text = "${dateFormat.format(Date(start))} - ${dateFormat.format(Date(end))}"
    }

    private fun updateBalance() {
        val income = viewModel.totalIncome.value ?: 0.0
        val expense = viewModel.totalExpense.value ?: 0.0
        tvBalance.text = currencyFormat.format(income - expense)
    }

    private fun switchChartView() {
        pieChart.visibility = if (currentChartType == 0) View.VISIBLE else View.GONE
        barChart.visibility = if (currentChartType == 1) View.VISIBLE else View.GONE
        lineChart.visibility = if (currentChartType == 2) View.VISIBLE else View.GONE
    }

    private fun updateCharts() {
        val categories = if (showExpense) {
            viewModel.expenseCategories.value
        } else {
            viewModel.incomeCategories.value
        } ?: emptyList()

        // Update rank list
        if (categories.isEmpty()) {
            tvEmptyRank.visibility = View.VISIBLE
            recyclerCategoryRank.visibility = View.GONE
            pieChart.clear()
            barChart.clear()
            lineChart.clear()
            pieChart.centerText = "暂无数据"
            return
        }

        tvEmptyRank.visibility = View.GONE
        recyclerCategoryRank.visibility = View.VISIBLE

        val total = categories.sumOf { it.total }
        val type = if (showExpense) TransactionType.EXPENSE else TransactionType.INCOME

        val rankItems = categories.mapIndexed { index, item ->
            CategoryRankAdapter.CategoryRankItem(
                category = item.category,
                amount = item.total,
                percentage = if (total > 0) (item.total / total * 100).toFloat() else 0f,
                color = pieColors.getOrElse(index) { pieColors[0] },
                type = type
            )
        }
        categoryRankAdapter.submitList(rankItems)

        when (currentChartType) {
            0 -> updatePieChart(categories)
            1 -> updateBarChart(categories)
            2 -> updateLineChart()
        }
    }

    private fun updatePieChart(categories: List<com.ai.bookkeeping.data.CategoryTotal>) {
        val entries = categories.map { PieEntry(it.total.toFloat(), it.category) }
        val dataSet = PieDataSet(entries, "").apply {
            colors = pieColors.take(entries.size)
            valueTextSize = 11f
            valueTextColor = Color.WHITE
            valueFormatter = PercentFormatter(pieChart)
            sliceSpace = 2f
        }
        pieChart.data = PieData(dataSet)
        pieChart.centerText = if (showExpense) "支出构成" else "收入构成"
        pieChart.invalidate()
    }

    private fun updateBarChart(categories: List<com.ai.bookkeeping.data.CategoryTotal>) {
        val entries = categories.mapIndexed { index, cat ->
            BarEntry(index.toFloat(), cat.total.toFloat())
        }
        val dataSet = BarDataSet(entries, "").apply {
            colors = pieColors.take(entries.size)
            setDrawValues(true)
            valueTextSize = 10f
        }
        barChart.data = BarData(dataSet).apply { barWidth = 0.6f }
        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(categories.map { it.category })
        barChart.xAxis.labelCount = categories.size
        barChart.invalidate()
    }

    private fun updateLineChart() {
        val dailyData = if (showExpense) {
            viewModel.dailyExpenses.value
        } else {
            viewModel.dailyIncomes.value
        } ?: return

        if (dailyData.isEmpty()) {
            lineChart.clear()
            return
        }

        val dateFormatter = SimpleDateFormat("MM/dd", Locale.getDefault())
        val entries = dailyData.mapIndexed { index, daily ->
            Entry(index.toFloat(), daily.total.toFloat())
        }

        val dataSet = LineDataSet(entries, "").apply {
            color = if (showExpense) Color.parseColor("#FF6B6B") else Color.parseColor("#6BCB77")
            setCircleColor(color)
            lineWidth = 2f
            circleRadius = 4f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            setDrawFilled(true)
            fillColor = color
            fillAlpha = 30
        }

        lineChart.data = LineData(dataSet)
        lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(dailyData.map {
            dateFormatter.format(Date(it.date))
        })
        lineChart.xAxis.labelCount = minOf(dailyData.size, 7)
        lineChart.invalidate()
    }

    private fun showDateRangePicker() {
        val calendar = Calendar.getInstance()

        DatePickerDialog(requireContext(), { _, year, month, day ->
            val startCal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startDate = startCal.timeInMillis

            DatePickerDialog(requireContext(), { _, endYear, endMonth, endDay ->
                val endCal = Calendar.getInstance().apply {
                    set(endYear, endMonth, endDay, 23, 59, 59)
                    set(Calendar.MILLISECOND, 999)
                }
                val endDate = endCal.timeInMillis

                if (endDate >= startDate) {
                    viewModel.setDateRange(startDate, endDate)
                } else {
                    Toast.makeText(requireContext(), "结束日期不能早于开始日期", Toast.LENGTH_SHORT).show()
                }
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
                setTitle("选择结束日期")
                show()
            }
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("选择开始日期")
            show()
        }
    }

    private fun showApiKeyDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_api_key, null)

        val etApiKey = dialogView.findViewById<TextInputEditText>(R.id.etApiKey)
        val currentKey = AIService.getApiKey(requireContext())
        if (!currentKey.isNullOrEmpty()) {
            etApiKey.setText(currentKey)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("设置AI API密钥")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val apiKey = etApiKey.text.toString().trim()
                if (apiKey.isNotEmpty()) {
                    AIService.setApiKey(requireContext(), apiKey)
                    Toast.makeText(requireContext(), "API密钥已保存", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showExportDialog() {
        val options = arrayOf("导出为CSV文件", "导出为TXT文件")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("导出统计报告")
            .setItems(options) { _, which ->
                val exportData = viewModel.getExportData()
                val result = when (which) {
                    0 -> ExportUtil.exportToCsv(requireContext(), exportData)
                    1 -> ExportUtil.exportToTxt(requireContext(), exportData)
                    else -> return@setItems
                }

                result.fold(
                    onSuccess = { path ->
                        Toast.makeText(requireContext(), "导出成功: $path", Toast.LENGTH_LONG).show()
                    },
                    onFailure = { error ->
                        Toast.makeText(requireContext(), "导出失败: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            .show()
    }
}
