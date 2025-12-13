package com.ai.bookkeeping.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.FragmentAddTransactionBinding
import com.ai.bookkeeping.model.ExpenseCategories
import com.ai.bookkeeping.model.IncomeCategories
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.google.android.material.chip.Chip

/**
 * 手动添加记账Fragment
 */
class AddTransactionFragment : Fragment() {

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()
    private var currentType = TransactionType.EXPENSE

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTypeToggle()
        setupCategorySpinner()
        setupSaveButton()
        setupRecentNotes()
    }

    private fun setupTypeToggle() {
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentType = when (checkedId) {
                    binding.btnExpense.id -> TransactionType.EXPENSE
                    binding.btnIncome.id -> TransactionType.INCOME
                    else -> TransactionType.EXPENSE
                }
                updateCategorySpinner()
            }
        }
        binding.btnExpense.isChecked = true
    }

    private fun setupCategorySpinner() {
        updateCategorySpinner()
    }

    private fun updateCategorySpinner() {
        val categories = if (currentType == TransactionType.EXPENSE) {
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

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            val amountStr = binding.etAmount.text.toString()
            if (amountStr.isEmpty()) {
                Toast.makeText(requireContext(), "请输入金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(requireContext(), "请输入有效金额", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val category = binding.spinnerCategory.selectedItem.toString()
            val description = binding.etDescription.text.toString().ifEmpty { category }
            val note = binding.etNote.text.toString()

            val transaction = Transaction(
                amount = amount,
                type = currentType,
                category = category,
                description = description,
                note = note,
                aiParsed = false
            )

            viewModel.insert(transaction)
            Toast.makeText(requireContext(), "保存成功", Toast.LENGTH_SHORT).show()
            findNavController().popBackStack()
        }
    }

    private fun setupRecentNotes() {
        // Load recent notes
        viewModel.loadRecentNotes(10)

        viewModel.recentNotes.observe(viewLifecycleOwner) { notes ->
            if (notes.isNotEmpty()) {
                binding.tvRecentNotesLabel.visibility = View.VISIBLE
                binding.scrollRecentNotes.visibility = View.VISIBLE

                binding.chipGroupRecentNotes.removeAllViews()
                notes.forEach { note ->
                    val chip = Chip(requireContext()).apply {
                        text = if (note.length > 15) note.take(15) + "..." else note
                        tag = note  // Store full note in tag
                        isCheckable = false
                        setChipBackgroundColorResource(R.color.surface_variant)
                        setTextColor(resources.getColor(R.color.text_secondary, null))
                        chipStrokeWidth = 0f
                        setOnClickListener {
                            binding.etNote.setText(tag as String)
                        }
                    }
                    binding.chipGroupRecentNotes.addView(chip)
                }
            } else {
                binding.tvRecentNotesLabel.visibility = View.GONE
                binding.scrollRecentNotes.visibility = View.GONE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
