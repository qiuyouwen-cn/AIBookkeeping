package com.ai.bookkeeping.ui

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.ai.bookkeeping.databinding.DialogQuickAddBinding
import com.ai.bookkeeping.model.ExpenseCategories
import com.ai.bookkeeping.model.IncomeCategories
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType

/**
 * 快捷添加记账对话框
 */
class QuickAddDialogFragment : DialogFragment() {

    private var _binding: DialogQuickAddBinding? = null
    private val binding get() = _binding!!

    private var transactionType: TransactionType = TransactionType.EXPENSE
    private var onSaveListener: ((Transaction) -> Unit)? = null

    fun setOnSaveListener(listener: (Transaction) -> Unit) {
        onSaveListener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogQuickAddBinding.inflate(LayoutInflater.from(context))

        transactionType = arguments?.getString(ARG_TYPE)?.let {
            TransactionType.valueOf(it)
        } ?: TransactionType.EXPENSE

        setupCategorySpinner()

        val title = if (transactionType == TransactionType.EXPENSE) "快捷支出" else "快捷收入"

        return AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton("保存") { _, _ ->
                saveTransaction()
            }
            .setNegativeButton("取消", null)
            .create()
    }

    private fun setupCategorySpinner() {
        val categories = if (transactionType == TransactionType.EXPENSE) {
            ExpenseCategories.list
        } else {
            IncomeCategories.list
        }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            categories
        )
        binding.spinnerCategory.adapter = adapter
    }

    private fun saveTransaction() {
        val amountStr = binding.etAmount.text.toString()
        if (amountStr.isEmpty()) {
            Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(requireContext(), "请输入有效金额", Toast.LENGTH_SHORT).show()
            return
        }

        val category = binding.spinnerCategory.selectedItem.toString()
        val description = binding.etDescription.text.toString().ifEmpty { category }

        val transaction = Transaction(
            amount = amount,
            type = transactionType,
            category = category,
            description = description,
            aiParsed = false
        )

        onSaveListener?.invoke(transaction)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TYPE = "transaction_type"

        fun newInstance(type: TransactionType): QuickAddDialogFragment {
            return QuickAddDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TYPE, type.name)
                }
            }
        }
    }
}
