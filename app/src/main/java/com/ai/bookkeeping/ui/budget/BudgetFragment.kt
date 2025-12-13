package com.ai.bookkeeping.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ai.bookkeeping.databinding.FragmentBudgetBinding
import com.ai.bookkeeping.model.BudgetWithUsage
import com.ai.bookkeeping.viewmodel.BudgetViewModel
import com.ai.bookkeeping.viewmodel.NotebookViewModel
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

/**
 * 预算管理Fragment
 */
class BudgetFragment : Fragment() {

    private var _binding: FragmentBudgetBinding? = null
    private val binding get() = _binding!!

    private val budgetViewModel: BudgetViewModel by viewModels()
    private val notebookViewModel: NotebookViewModel by viewModels()
    private lateinit var adapter: BudgetAdapter

    private var currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
    private var currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var currentNotebookId: Long = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupMonthSelector()
        setupRecyclerView()
        setupFab()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupMonthSelector() {
        updateMonthDisplay()

        binding.btnPrevMonth.setOnClickListener {
            if (currentMonth == 1) {
                currentMonth = 12
                currentYear--
            } else {
                currentMonth--
            }
            updateMonthDisplay()
            loadBudgets()
        }

        binding.btnNextMonth.setOnClickListener {
            if (currentMonth == 12) {
                currentMonth = 1
                currentYear++
            } else {
                currentMonth++
            }
            updateMonthDisplay()
            loadBudgets()
        }
    }

    private fun updateMonthDisplay() {
        binding.tvCurrentMonth.text = "${currentYear}年${currentMonth}月"
    }

    private fun setupRecyclerView() {
        adapter = BudgetAdapter(
            onItemClick = { budgetWithUsage ->
                showEditBudgetDialog(budgetWithUsage)
            },
            onItemLongClick = { budgetWithUsage ->
                confirmDeleteBudget(budgetWithUsage)
            }
        )
        binding.rvCategoryBudgets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCategoryBudgets.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddBudget.setOnClickListener {
            showAddBudgetDialog()
        }

        binding.cardTotalBudget.setOnClickListener {
            showAddTotalBudgetDialog()
        }
    }

    private fun observeData() {
        notebookViewModel.currentNotebook.observe(viewLifecycleOwner) { notebook ->
            notebook?.let {
                currentNotebookId = it.id
                loadBudgets()
            }
        }

        budgetViewModel.totalBudget.observe(viewLifecycleOwner) { totalBudget ->
            updateTotalBudgetUI(totalBudget)
        }

        budgetViewModel.currentMonthBudgets.observe(viewLifecycleOwner) { budgets ->
            adapter.submitList(budgets)
            binding.tvEmptyHint.visibility = if (budgets.isEmpty()) View.VISIBLE else View.GONE
        }

        budgetViewModel.operationResult.observe(viewLifecycleOwner) { result ->
            when (result) {
                is BudgetViewModel.OperationResult.Success -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
                is BudgetViewModel.OperationResult.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadBudgets() {
        budgetViewModel.loadBudgetsForMonth(currentNotebookId, currentYear, currentMonth)
    }

    private fun updateTotalBudgetUI(totalBudget: BudgetWithUsage?) {
        val formatter = NumberFormat.getCurrencyInstance(Locale.CHINA)

        if (totalBudget != null) {
            binding.tvTotalBudgetAmount.text = formatter.format(totalBudget.budget.amount)
            binding.tvBudgetStatus.text = "剩余 ${formatter.format(totalBudget.remainingAmount)}"
            binding.tvUsedAmount.text = "已用 ${formatter.format(totalBudget.usedAmount)}"
            binding.tvPercentage.text = "${totalBudget.usagePercentage.toInt()}%"
            binding.progressTotalBudget.progress = totalBudget.usagePercentage.toInt()
        } else {
            binding.tvTotalBudgetAmount.text = "未设置"
            binding.tvBudgetStatus.text = "点击设置总预算"
            binding.tvUsedAmount.text = ""
            binding.tvPercentage.text = ""
            binding.progressTotalBudget.progress = 0
        }
    }

    private fun showAddBudgetDialog() {
        AddBudgetDialog.newInstance(currentNotebookId, currentYear, currentMonth)
            .show(childFragmentManager, "add_budget")
    }

    private fun showAddTotalBudgetDialog() {
        AddBudgetDialog.newInstance(currentNotebookId, currentYear, currentMonth, isTotal = true)
            .show(childFragmentManager, "add_total_budget")
    }

    private fun showEditBudgetDialog(budgetWithUsage: BudgetWithUsage) {
        AddBudgetDialog.newInstance(
            currentNotebookId,
            currentYear,
            currentMonth,
            budget = budgetWithUsage.budget
        ).show(childFragmentManager, "edit_budget")
    }

    private fun confirmDeleteBudget(budgetWithUsage: BudgetWithUsage) {
        val name = budgetWithUsage.categoryName ?: "总预算"
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("删除预算")
            .setMessage("确定要删除「$name」的预算吗？")
            .setPositiveButton("删除") { _, _ ->
                budgetViewModel.delete(budgetWithUsage.budget)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
