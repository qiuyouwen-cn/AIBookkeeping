package com.ai.bookkeeping.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ai.bookkeeping.databinding.FragmentStatisticsBinding
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.PercentFormatter
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * 统计页面Fragment
 */
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    private val pieColors = listOf(
        Color.parseColor("#FF6384"),
        Color.parseColor("#36A2EB"),
        Color.parseColor("#FFCE56"),
        Color.parseColor("#4BC0C0"),
        Color.parseColor("#9966FF"),
        Color.parseColor("#FF9F40"),
        Color.parseColor("#E7E9ED"),
        Color.parseColor("#7CB342"),
        Color.parseColor("#D81B60"),
        Color.parseColor("#1E88E5")
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupPieChart()
        setupObservers()
        setupTabListener()
    }

    private fun setupPieChart() {
        binding.pieChart.apply {
            description.isEnabled = false
            setUsePercentValues(true)
            setEntryLabelTextSize(12f)
            setEntryLabelColor(Color.BLACK)
            legend.isEnabled = true
            setHoleColor(Color.WHITE)
            setTransparentCircleAlpha(0)
            holeRadius = 40f
            setDrawCenterText(true)
            centerText = "支出分布"
        }
    }

    private fun setupObservers() {
        viewModel.currentMonthIncome.observe(viewLifecycleOwner) { income ->
            binding.tvTotalIncome.text = currencyFormat.format(income ?: 0.0)
            updateBalance()
        }

        viewModel.currentMonthExpense.observe(viewLifecycleOwner) { expense ->
            binding.tvTotalExpense.text = currencyFormat.format(expense ?: 0.0)
            updateBalance()
        }

        loadCategoryData(TransactionType.EXPENSE)
    }

    private fun updateBalance() {
        val income = viewModel.currentMonthIncome.value ?: 0.0
        val expense = viewModel.currentMonthExpense.value ?: 0.0
        binding.tvBalance.text = currencyFormat.format(income - expense)
    }

    private fun setupTabListener() {
        binding.tabLayout.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        loadCategoryData(TransactionType.EXPENSE)
                        binding.pieChart.centerText = "支出分布"
                    }
                    1 -> {
                        loadCategoryData(TransactionType.INCOME)
                        binding.pieChart.centerText = "收入分布"
                    }
                }
            }

            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
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
                binding.pieChart.clear()
                binding.pieChart.centerText = "暂无数据"
                return@observe
            }

            val entries = categoryTotals.map { PieEntry(it.total.toFloat(), it.category) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = pieColors.take(entries.size)
                valueTextSize = 14f
                valueTextColor = Color.WHITE
                valueFormatter = PercentFormatter(binding.pieChart)
            }

            binding.pieChart.data = PieData(dataSet)
            binding.pieChart.invalidate()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
