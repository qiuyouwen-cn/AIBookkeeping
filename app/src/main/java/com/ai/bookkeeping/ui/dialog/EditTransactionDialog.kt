package com.ai.bookkeeping.ui.dialog

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.DialogEditTransactionBinding
import com.ai.bookkeeping.model.ExpenseCategories
import com.ai.bookkeeping.model.IncomeCategories
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * 编辑交易记录对话框
 */
class EditTransactionDialog : BottomSheetDialogFragment() {

    private var _binding: DialogEditTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()

    private var transaction: Transaction? = null
    private var onSaveListener: ((Transaction) -> Unit)? = null
    private var selectedDate: Long = System.currentTimeMillis()
    private var currentType: TransactionType = TransactionType.EXPENSE

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    private val calendar = Calendar.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogEditTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        transaction = arguments?.getParcelable(ARG_TRANSACTION)

        setupViews()
        setupListeners()
        populateData()
        setupRecentNotes()
    }

    private fun setupViews() {
        // 初始化分类下拉
        updateCategoryDropdown()
    }

    private fun setupListeners() {
        // 类型选择
        binding.chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentType = when (checkedIds[0]) {
                    R.id.chip_income -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
                updateCategoryDropdown()
            }
        }

        // 日期选择
        binding.etDate.setOnClickListener {
            showDateTimePicker()
        }

        binding.tilDate.setEndIconOnClickListener {
            showDateTimePicker()
        }

        // 取消按钮
        binding.btnCancel.setOnClickListener {
            dismiss()
        }

        // 保存按钮
        binding.btnSave.setOnClickListener {
            saveTransaction()
        }
    }

    private fun populateData() {
        transaction?.let { t ->
            // 类型
            currentType = t.type
            when (t.type) {
                TransactionType.EXPENSE -> binding.chipExpense.isChecked = true
                TransactionType.INCOME -> binding.chipIncome.isChecked = true
            }

            // 金额
            binding.etAmount.setText(t.amount.toString())

            // 分类
            updateCategoryDropdown()
            binding.dropdownCategory.setText(t.category, false)

            // 描述
            binding.etDescription.setText(t.description)

            // 备注
            binding.etNote.setText(t.note)

            // 日期
            selectedDate = t.date
            calendar.timeInMillis = selectedDate
            binding.etDate.setText(dateFormat.format(calendar.time))
        }
    }

    private fun updateCategoryDropdown() {
        val categories = if (currentType == TransactionType.EXPENSE) {
            ExpenseCategories.list
        } else {
            IncomeCategories.list
        }

        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.item_dropdown,
            categories
        )
        binding.dropdownCategory.setAdapter(adapter)

        // 如果当前选中的分类不在新列表中，清空选择
        val currentCategory = binding.dropdownCategory.text.toString()
        if (currentCategory.isNotEmpty() && currentCategory !in categories) {
            binding.dropdownCategory.setText("", false)
        }
    }

    private fun showDateTimePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        DatePickerDialog(requireContext(), { _, y, m, d ->
            calendar.set(Calendar.YEAR, y)
            calendar.set(Calendar.MONTH, m)
            calendar.set(Calendar.DAY_OF_MONTH, d)

            TimePickerDialog(requireContext(), { _, h, min ->
                calendar.set(Calendar.HOUR_OF_DAY, h)
                calendar.set(Calendar.MINUTE, min)
                selectedDate = calendar.timeInMillis
                binding.etDate.setText(dateFormat.format(calendar.time))
            }, hour, minute, true).show()

        }, year, month, day).show()
    }

    private fun saveTransaction() {
        // 验证金额
        val amountStr = binding.etAmount.text.toString().trim()
        if (amountStr.isEmpty()) {
            binding.tilAmount.error = "请输入金额"
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            binding.tilAmount.error = "请输入有效金额"
            return
        }
        binding.tilAmount.error = null

        // 验证分类
        val category = binding.dropdownCategory.text.toString().trim()
        if (category.isEmpty()) {
            binding.tilCategory.error = "请选择分类"
            return
        }
        binding.tilCategory.error = null

        // 获取描述和备注
        val description = binding.etDescription.text.toString().trim()
        val note = binding.etNote.text.toString().trim()

        // 创建更新后的交易记录
        val updatedTransaction = transaction?.copy(
            amount = amount,
            type = currentType,
            category = category,
            description = description,
            note = note,
            date = selectedDate
        ) ?: return

        onSaveListener?.invoke(updatedTransaction)
        dismiss()
    }

    fun setOnSaveListener(listener: (Transaction) -> Unit) {
        onSaveListener = listener
    }

    private fun setupRecentNotes() {
        viewModel.loadRecentNotes(8)

        viewModel.recentNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                binding.scrollRecentNotes.visibility = View.VISIBLE
                binding.chipGroupRecentNotes.removeAllViews()

                notes.forEach { note ->
                    val chip = Chip(requireContext()).apply {
                        text = if (note.length > 12) note.take(12) + "..." else note
                        tag = note
                        isCheckable = false
                        setChipBackgroundColorResource(R.color.glass_white_light)
                        setTextColor(resources.getColor(R.color.text_secondary, null))
                        chipStrokeWidth = 0f
                        setOnClickListener {
                            binding.etNote.setText(tag as String)
                        }
                    }
                    binding.chipGroupRecentNotes.addView(chip)
                }
            } else {
                binding.scrollRecentNotes.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TRANSACTION = "transaction"

        fun newInstance(transaction: Transaction): EditTransactionDialog {
            return EditTransactionDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_TRANSACTION, transaction)
                }
            }
        }
    }
}
