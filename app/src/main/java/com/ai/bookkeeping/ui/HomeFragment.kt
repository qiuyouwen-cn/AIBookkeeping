package com.ai.bookkeeping.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ai.bookkeeping.R
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.service.FloatingWindowService
import com.ai.bookkeeping.util.AIParser
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var etAiInput: EditText
    private lateinit var btnAiRecord: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var cardVoice: CardView
    private lateinit var cardPhoto: CardView
    private lateinit var cardExpense: CardView
    private lateinit var cardIncome: CardView
    private lateinit var btnCategoryManage: Button
    private lateinit var fabVoice: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupObservers()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        tvIncome = view.findViewById(R.id.tvIncome)
        tvExpense = view.findViewById(R.id.tvExpense)
        etAiInput = view.findViewById(R.id.etAiInput)
        btnAiRecord = view.findViewById(R.id.btnAiRecord)
        progressBar = view.findViewById(R.id.progressBar)
        cardVoice = view.findViewById(R.id.cardVoice)
        cardPhoto = view.findViewById(R.id.cardPhoto)
        cardExpense = view.findViewById(R.id.cardExpense)
        cardIncome = view.findViewById(R.id.cardIncome)
        btnCategoryManage = view.findViewById(R.id.btnCategoryManage)
        fabVoice = view.findViewById(R.id.fabVoice)
    }

    private fun setupObservers() {
        viewModel.currentMonthIncome.observe(viewLifecycleOwner) { income ->
            tvIncome.text = currencyFormat.format(income ?: 0.0)
        }

        viewModel.currentMonthExpense.observe(viewLifecycleOwner) { expense ->
            tvExpense.text = currencyFormat.format(expense ?: 0.0)
        }
    }

    private fun setupClickListeners() {
        // AI记账按钮
        btnAiRecord.setOnClickListener {
            val input = etAiInput.text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(requireContext(), "请输入记账内容", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            parseAndSave(input)
        }

        // 语音记账
        cardVoice.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_voice)
        }

        // 拍照记账
        cardPhoto.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_photo)
        }

        // 快捷支出
        cardExpense.setOnClickListener {
            showQuickAddDialog(TransactionType.EXPENSE)
        }

        // 快捷收入
        cardIncome.setOnClickListener {
            showQuickAddDialog(TransactionType.INCOME)
        }

        // 分类管理
        btnCategoryManage.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_category)
        }

        // 悬浮窗语音按钮
        fabVoice.setOnClickListener {
            val mainActivity = activity as? MainActivity
            if (FloatingWindowService.isRunning) {
                mainActivity?.stopFloatingService()
                fabVoice.setImageResource(R.drawable.ic_mic)
            } else {
                mainActivity?.startFloatingService()
                fabVoice.setImageResource(R.drawable.ic_mic_off)
            }
        }
    }

    private fun parseAndSave(input: String) {
        progressBar.visibility = View.VISIBLE
        btnAiRecord.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = AIParser.parse(input)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnAiRecord.isEnabled = true

                    if (result != null) {
                        viewModel.insert(result)
                        etAiInput.text?.clear()
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
                    progressBar.visibility = View.GONE
                    btnAiRecord.isEnabled = true
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

    override fun onResume() {
        super.onResume()
        // 更新悬浮窗按钮状态
        if (FloatingWindowService.isRunning) {
            fabVoice.setImageResource(R.drawable.ic_mic_off)
        } else {
            fabVoice.setImageResource(R.drawable.ic_mic)
        }
    }
}
