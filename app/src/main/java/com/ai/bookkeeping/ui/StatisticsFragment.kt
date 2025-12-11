package com.ai.bookkeeping.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.adapter.CategoryRankAdapter
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import com.google.android.material.chip.Chip
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticsFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)
    private val monthFormat = SimpleDateFormat("yyyy年MM月", Locale.CHINA)

    private lateinit var tvCurrentMonth: TextView
    private lateinit var tvTotalIncome: TextView
    private lateinit var tvTotalExpense: TextView
    private lateinit var tvBalance: TextView
    private lateinit var pieChart: PieChart
    private lateinit var chipExpense: Chip
    private lateinit var chipIncome: Chip
    private lateinit var recyclerCategoryRank: RecyclerView
    private lateinit var tvEmptyRank: TextView

    private lateinit var categoryRankAdapter: CategoryRankAdapter

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
        setupPieChart()
        setupRecyclerView()
        setupObservers()
        setupChipListeners()
    }

    private fun initViews(view: View) {
        tvCurrentMonth = view.findViewById(R.id.tvCurrentMonth)
        tvTotalIncome = view.findViewById(R.id.tvTotalIncome)
        tvTotalExpense = view.findViewById(R.id.tvTotalExpense)
        tvBalance = view.findViewById(R.id.tvBalance)
        pieChart = view.findViewById(R.id.pieChart)
        chipExpense = view.findViewById(R.id.chipExpense)
        chipIncome = view.findViewById(R.id.chipIncome)
        recyclerCategoryRank = view.findViewById(R.id.recyclerCategoryRank)
        tvEmptyRank = view.findViewById(R.id.tvEmptyRank)

        tvCurrentMonth.text = monthFormat.format(Calendar.getInstance().time)
    }

    private fun setupPieChart() {
        pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(11f)
            setEntryLabelColor(Color.WHITE)
            legend.isEnabled = false
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(0)
            holeRadius = 55f
            setDrawCenterText(true)
            setCenterTextSize(14f)
            setCenterTextColor(Color.parseColor("#333333"))
            centerText = "支出构成"
            setExtraOffsets(10f, 10f, 10f, 10f)
            animateY(800)
        }
    }

    private fun setupRecyclerView() {
        categoryRankAdapter = CategoryRankAdapter()
        recyclerCategoryRank.layoutManager = LinearLayoutManager(requireContext())
        recyclerCategoryRank.adapter = categoryRankAdapter
    }

    private fun setupObservers() {
        viewModel.currentMonthIncome.observe(viewLifecycleOwner) { income ->
            tvTotalIncome.text = currencyFormat.format(income ?: 0.0)
            updateBalance()
        }

        viewModel.currentMonthExpense.observe(viewLifecycleOwner) { expense ->
            tvTotalExpense.text = currencyFormat.format(expense ?: 0.0)
            updateBalance()
        }

        loadCategoryData(TransactionType.EXPENSE)
    }

    private fun updateBalance() {
        val income = viewModel.currentMonthIncome.value ?: 0.0
        val expense = viewModel.currentMonthExpense.value ?: 0.0
        tvBalance.text = currencyFormat.format(income - expense)
    }

    private fun setupChipListeners() {
        chipExpense.setOnClickListener {
            loadCategoryData(TransactionType.EXPENSE)
            pieChart.centerText = "支出构成"
        }

        chipIncome.setOnClickListener {
            loadCategoryData(TransactionType.INCOME)
            pieChart.centerText = "收入构成"
        }
    }

    private fun loadCategoryData(type: TransactionType) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endOfMonth = calendar.timeInMillis

        viewModel.getCategoryTotals(type, startOfMonth, endOfMonth).observe(viewLifecycleOwner) { categoryTotals ->
            if (categoryTotals.isEmpty()) {
                pieChart.clear()
                pieChart.centerText = "暂无数据"
                tvEmptyRank.visibility = View.VISIBLE
                recyclerCategoryRank.visibility = View.GONE
                categoryRankAdapter.submitList(emptyList())
                return@observe
            }

            tvEmptyRank.visibility = View.GONE
            recyclerCategoryRank.visibility = View.VISIBLE

            // Update pie chart
            val entries = categoryTotals.map { PieEntry(it.total.toFloat(), it.category) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = pieColors.take(entries.size)
                valueTextSize = 12f
                valueTextColor = Color.WHITE
                valueFormatter = PercentFormatter(pieChart)
                sliceSpace = 2f
            }

            pieChart.data = PieData(dataSet)
            pieChart.invalidate()

            // Calculate total for percentage
            val total = categoryTotals.sumOf { it.total }

            // Update category ranking list
            val rankItems = categoryTotals.mapIndexed { index, item ->
                CategoryRankAdapter.CategoryRankItem(
                    category = item.category,
                    amount = item.total,
                    percentage = if (total > 0) (item.total / total * 100).toFloat() else 0f,
                    color = pieColors.getOrElse(index) { pieColors[0] },
                    type = type
                )
            }
            categoryRankAdapter.submitList(rankItems)
        }
    }
}
