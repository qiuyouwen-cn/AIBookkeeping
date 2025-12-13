package com.ai.bookkeeping.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.viewModels
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.DialogAddBudgetBinding
import com.ai.bookkeeping.model.Budget
import com.ai.bookkeeping.model.BudgetPeriod
import com.ai.bookkeeping.model.Category
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.BudgetViewModel
import com.ai.bookkeeping.viewmodel.CategoryViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/**
 * 添加/编辑预算对话框
 */
class AddBudgetDialog : BottomSheetDialogFragment() {

    private var _binding: DialogAddBudgetBinding? = null
    private val binding get() = _binding!!

    private val budgetViewModel: BudgetViewModel by viewModels({ requireParentFragment() })
    private val categoryViewModel: CategoryViewModel by viewModels()

    private var notebookId: Long = 1
    private var year: Int = 0
    private var month: Int = 0
    private var isTotal: Boolean = false
    private var editingBudget: Budget? = null

    private var selectedCategoryId: Long? = null
    private val categories = mutableListOf<Category>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogAddBudgetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        notebookId = arguments?.getLong(ARG_NOTEBOOK_ID) ?: 1
        year = arguments?.getInt(ARG_YEAR) ?: 0
        month = arguments?.getInt(ARG_MONTH) ?: 0
        isTotal = arguments?.getBoolean(ARG_IS_TOTAL) ?: false
        editingBudget = arguments?.getParcelable(ARG_BUDGET)

        setupUI()
        setupListeners()
        observeCategories()
    }

    private fun setupUI() {
        if (isTotal || editingBudget?.categoryId == null) {
            binding.tvDialogTitle.text = if (editingBudget != null) "编辑总预算" else "设置总预算"
            binding.tilCategory.visibility = View.GONE
        } else {
            binding.tvDialogTitle.text = if (editingBudget != null) "编辑分类预算" else "添加分类预算"
            binding.tilCategory.visibility = View.VISIBLE
        }

        editingBudget?.let { budget ->
            binding.etAmount.setText(budget.amount.toString())
            selectedCategoryId = budget.categoryId
        }
    }

    private fun setupListeners() {
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        binding.btnSave.setOnClickListener {
            saveBudget()
        }
    }

    private fun observeCategories() {
        categoryViewModel.getExpenseCategories().observe(viewLifecycleOwner) { categoryList ->
            categories.clear()
            categories.addAll(categoryList.filter { it.parentId == null })

            val categoryNames = categories.map { it.name }
            val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, categoryNames)
            (binding.tilCategory.editText as? AutoCompleteTextView)?.setAdapter(adapter)

            // 如果是编辑模式，设置当前分类
            editingBudget?.categoryId?.let { categoryId ->
                val category = categories.find { it.id == categoryId }
                category?.let {
                    (binding.tilCategory.editText as? AutoCompleteTextView)?.setText(it.name, false)
                }
            }

            (binding.tilCategory.editText as? AutoCompleteTextView)?.setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId = categories[position].id
            }
        }
    }

    private fun saveBudget() {
        val amountStr = binding.etAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            binding.tilAmount.error = "请输入预算金额"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = "请输入有效金额"
            return
        }

        if (!isTotal && editingBudget == null && selectedCategoryId == null) {
            binding.tilCategory.error = "请选择分类"
            return
        }

        val categoryId = if (isTotal) null else (selectedCategoryId ?: editingBudget?.categoryId)

        val budget = editingBudget?.copy(
            amount = amount
        ) ?: Budget(
            notebookId = notebookId,
            categoryId = categoryId,
            amount = amount,
            periodType = BudgetPeriod.MONTHLY,
            year = year,
            month = month
        )

        if (editingBudget != null) {
            budgetViewModel.update(budget)
        } else {
            budgetViewModel.insert(budget)
        }

        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_NOTEBOOK_ID = "notebook_id"
        private const val ARG_YEAR = "year"
        private const val ARG_MONTH = "month"
        private const val ARG_IS_TOTAL = "is_total"
        private const val ARG_BUDGET = "budget"

        fun newInstance(
            notebookId: Long,
            year: Int,
            month: Int,
            isTotal: Boolean = false,
            budget: Budget? = null
        ): AddBudgetDialog {
            return AddBudgetDialog().apply {
                arguments = Bundle().apply {
                    putLong(ARG_NOTEBOOK_ID, notebookId)
                    putInt(ARG_YEAR, year)
                    putInt(ARG_MONTH, month)
                    putBoolean(ARG_IS_TOTAL, isTotal)
                    budget?.let { putParcelable(ARG_BUDGET, it) }
                }
            }
        }
    }
}
