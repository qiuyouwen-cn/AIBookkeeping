package com.ai.bookkeeping.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.FragmentHomeBinding
import com.ai.bookkeeping.model.Transaction
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.util.AIParser
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

/**
 * 首页Fragment - AI记账入口
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.currentMonthIncome.observe(viewLifecycleOwner) { income ->
            binding.tvIncome.text = currencyFormat.format(income ?: 0.0)
        }

        viewModel.currentMonthExpense.observe(viewLifecycleOwner) { expense ->
            binding.tvExpense.text = currencyFormat.format(expense ?: 0.0)
        }
    }

    private fun setupClickListeners() {
        // AI记账按钮
        binding.btnAiRecord.setOnClickListener {
            val input = binding.etAiInput.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "请输入记账内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            parseAndSave(input)
        }

        // 手动记账按钮
        binding.btnManualRecord.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_add)
        }

        // 快捷记账按钮
        binding.btnQuickExpense.setOnClickListener {
            showQuickAddDialog(TransactionType.EXPENSE)
        }

        binding.btnQuickIncome.setOnClickListener {
            showQuickAddDialog(TransactionType.INCOME)
        }
    }

    private fun parseAndSave(input: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnAiRecord.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = AIParser.parse(input)
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnAiRecord.isEnabled = true

                    if (result != null) {
                        viewModel.insert(result)
                        binding.etAiInput.text?.clear()
                        val typeStr = if (result.type == TransactionType.EXPENSE) "支出" else "收入"
                        Toast.makeText(
                            requireContext(),
                            "已记录: ${result.category} ${currencyFormat.format(result.amount)} ($typeStr)",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(requireContext(), "无法解析，请重新描述", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                    binding.btnAiRecord.isEnabled = true
                    Toast.makeText(requireContext(), "解析失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showQuickAddDialog(type: TransactionType) {
        val dialog = QuickAddDialogFragment.newInstance(type)
        dialog.setOnSaveListener { transaction ->
            viewModel.insert(transaction)
        }
        dialog.show(parentFragmentManager, "quick_add")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
