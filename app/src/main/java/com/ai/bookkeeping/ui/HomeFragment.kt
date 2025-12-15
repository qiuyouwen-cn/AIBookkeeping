package com.ai.bookkeeping.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.ai.bookkeeping.R
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.service.FloatingWindowService
import com.ai.bookkeeping.util.AIParser
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {

    private val viewModel: TransactionViewModel by activityViewModels()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    private lateinit var tvIncome: TextView
    private lateinit var tvExpense: TextView
    private lateinit var etAiInput: TextInputEditText
    private lateinit var btnAiRecord: MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var btnVoice: View
    private lateinit var btnPhoto: View
    private lateinit var btnExpense: View
    private lateinit var btnIncome: View
    private lateinit var btnCategoryManage: MaterialButton
    private lateinit var cardFloatingToggle: View
    private lateinit var ivFloatingIcon: ImageView
    private lateinit var btnAccount: View
    private lateinit var btnBudget: View
    private lateinit var btnNotebook: View
    private lateinit var btnTransfer: View
    private lateinit var btnImportBill: View

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
        tvIncome = view.findViewById(R.id.tv_income)
        tvExpense = view.findViewById(R.id.tv_expense)
        etAiInput = view.findViewById(R.id.et_ai_input)
        btnAiRecord = view.findViewById(R.id.btn_ai_record)
        progressBar = view.findViewById(R.id.progress_bar)
        btnVoice = view.findViewById(R.id.btn_voice_record)
        btnPhoto = view.findViewById(R.id.btn_photo_record)
        btnExpense = view.findViewById(R.id.btn_quick_expense)
        btnIncome = view.findViewById(R.id.btn_quick_income)
        btnCategoryManage = view.findViewById(R.id.btn_category_manage)
        cardFloatingToggle = view.findViewById(R.id.card_floating_toggle)
        ivFloatingIcon = view.findViewById(R.id.iv_floating_icon)
        btnAccount = view.findViewById(R.id.btn_account)
        btnBudget = view.findViewById(R.id.btn_budget)
        btnNotebook = view.findViewById(R.id.btn_notebook)
        btnTransfer = view.findViewById(R.id.btn_transfer)
        btnImportBill = view.findViewById(R.id.btn_import_bill)
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
        btnVoice.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_voice)
        }

        // 拍照记账
        btnPhoto.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_photo)
        }

        // 快捷支出
        btnExpense.setOnClickListener {
            showQuickAddDialog(TransactionType.EXPENSE)
        }

        // 快捷收入
        btnIncome.setOnClickListener {
            showQuickAddDialog(TransactionType.INCOME)
        }

        // 分类管理
        btnCategoryManage.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_category)
        }

        // 悬浮窗开关
        cardFloatingToggle.setOnClickListener {
            val mainActivity = activity as? MainActivity
            if (FloatingWindowService.isRunning) {
                mainActivity?.stopFloatingService()
                ivFloatingIcon.setImageResource(R.drawable.ic_mic)
            } else {
                mainActivity?.startFloatingService()
                ivFloatingIcon.setImageResource(R.drawable.ic_mic_off)
            }
        }

        // 账户管理
        btnAccount.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_account)
        }

        // 预算管理
        btnBudget.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_budget)
        }

        // 账本管理
        btnNotebook.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_notebook)
        }

        // 转账
        btnTransfer.setOnClickListener {
            showTransferDialog()
        }

        // 导入账单
        btnImportBill.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_import)
        }
    }

    private fun showTransferDialog() {
        val dialog = com.ai.bookkeeping.ui.dialog.TransferDialog.newInstance()
        dialog.show(parentFragmentManager, "transfer")
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
                        // 格式化解析的日期时间
                        val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA)
                        val dateStr = dateFormat.format(Date(result.date))
                        Toast.makeText(
                            requireContext(),
                            "已记录: $dateStr ${result.category} ${currencyFormat.format(result.amount)} ($typeStr)",
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
            ivFloatingIcon.setImageResource(R.drawable.ic_mic_off)
        } else {
            ivFloatingIcon.setImageResource(R.drawable.ic_mic)
        }
    }
}
