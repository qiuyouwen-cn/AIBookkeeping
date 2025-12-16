package com.ai.bookkeeping.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ai.bookkeeping.R
import com.ai.bookkeeping.ai.WhisperService
import com.ai.bookkeeping.databinding.FragmentVoiceRecordBinding
import com.ai.bookkeeping.model.TransactionType
import com.ai.bookkeeping.util.AIParser
import com.ai.bookkeeping.viewmodel.TransactionViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 语音记账Fragment
 * 使用 Groq Whisper API 进行语音识别
 */
class VoiceRecordFragment : Fragment() {

    private var _binding: FragmentVoiceRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()
    private var isRecording = false
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecording()
        } else {
            Toast.makeText(requireContext(), "需要麦克风权限才能语音记账", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceRecordBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 Whisper 服务
        WhisperService.init(requireContext())

        setupUI()
        setupClickListeners()
        setupWhisperCallbacks()
    }

    private fun setupUI() {
        binding.tvStatus.text = "点击下方按钮开始语音记账"
        binding.cardResult.visibility = View.GONE
        binding.tvRecognizedText.visibility = View.GONE
        updateApiKeyHint()
    }

    private fun updateApiKeyHint() {
        if (!WhisperService.hasApiKey(requireContext())) {
            binding.tvStatus.text = "请先设置 API Key（长按按钮）"
        }
    }

    private fun setupClickListeners() {
        binding.btnVoice.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                // 检查是否配置了 API Key
                if (!WhisperService.hasApiKey(requireContext())) {
                    showApiKeyDialog()
                } else {
                    checkPermissionAndStart()
                }
            }
        }

        // 长按设置 API Key
        binding.btnVoice.setOnLongClickListener {
            showApiKeyDialog()
            true
        }
    }

    private fun setupWhisperCallbacks() {
        WhisperService.onStateChange = { state ->
            activity?.runOnUiThread {
                when (state) {
                    WhisperService.RecordingState.IDLE -> {
                        binding.tvStatus.text = "点击下方按钮开始语音记账"
                    }
                    WhisperService.RecordingState.RECORDING -> {
                        binding.tvStatus.text = "正在聆听...（点击停止）"
                        binding.animationView.visibility = View.VISIBLE
                    }
                    WhisperService.RecordingState.PROCESSING -> {
                        binding.tvStatus.text = "正在识别..."
                        binding.animationView.visibility = View.GONE
                    }
                    WhisperService.RecordingState.COMPLETED -> {
                        binding.tvStatus.text = "识别完成"
                    }
                    WhisperService.RecordingState.ERROR -> {
                        binding.tvStatus.text = "识别失败"
                        binding.animationView.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startRecording()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startRecording() {
        isRecording = true
        binding.btnVoice.setIconResource(R.drawable.ic_stop)
        binding.btnVoice.text = "停止"
        binding.tvRecognizedText.visibility = View.GONE
        binding.cardResult.visibility = View.GONE
        binding.animationView.visibility = View.VISIBLE
        binding.tvStatus.text = "正在聆听...（最长15秒）"

        CoroutineScope(Dispatchers.Main).launch {
            val result = WhisperService.recordAndTranscribe(
                context = requireContext(),
                maxDurationMs = 15000,
                onAmplitude = { amplitude ->
                    // 可以用于显示波形动画
                }
            )

            result.fold(
                onSuccess = { text ->
                    if (text.isNotEmpty()) {
                        binding.tvRecognizedText.text = "\"$text\""
                        binding.tvRecognizedText.visibility = View.VISIBLE
                        parseAndSave(text)
                    } else {
                        binding.tvStatus.text = "未识别到语音，请重试"
                    }
                },
                onFailure = { error ->
                    val errorMsg = error.message ?: "未知错误"
                    binding.tvStatus.text = "识别失败: $errorMsg"

                    // 如果是 API Key 问题，提示设置
                    if (errorMsg.contains("API Key") || errorMsg.contains("api_key")) {
                        showApiKeyDialog()
                    }
                }
            )

            stopRecordingUI()
        }
    }

    private fun stopRecording() {
        WhisperService.stopRecording()
        // 录音会在下一次循环检测到停止标志后自然结束
    }

    private fun stopRecordingUI() {
        isRecording = false
        binding.btnVoice.setIconResource(R.drawable.ic_mic)
        binding.btnVoice.text = "开始语音记账"
        binding.animationView.visibility = View.GONE
    }

    private fun parseAndSave(text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = AIParser.parse(text)
                withContext(Dispatchers.Main) {
                    if (result != null) {
                        // 显示解析结果
                        binding.cardResult.visibility = View.VISIBLE
                        val typeStr = if (result.type == TransactionType.EXPENSE) "支出" else "收入"
                        binding.tvResultType.text = typeStr
                        binding.tvResultCategory.text = result.category
                        binding.tvResultAmount.text = currencyFormat.format(result.amount)
                        binding.tvResultAmount.setTextColor(
                            if (result.type == TransactionType.EXPENSE)
                                ContextCompat.getColor(requireContext(), R.color.expense_red)
                            else
                                ContextCompat.getColor(requireContext(), R.color.income_green)
                        )

                        // 更新卡片背景色
                        binding.cardResult.setCardBackgroundColor(
                            if (result.type == TransactionType.EXPENSE)
                                ContextCompat.getColor(requireContext(), R.color.expense_red_bg)
                            else
                                ContextCompat.getColor(requireContext(), R.color.income_green_bg)
                        )

                        // 保存到数据库
                        viewModel.insert(result)

                        // 格式化解析的日期时间
                        val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA)
                        val dateStr = dateFormat.format(Date(result.date))
                        binding.tvStatus.text = "记账成功！($dateStr)"

                        Toast.makeText(
                            requireContext(),
                            "已记录: $dateStr ${result.category} ${currencyFormat.format(result.amount)}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        binding.tvStatus.text = "无法解析，请换一种说法"
                        binding.cardResult.visibility = View.GONE
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.tvStatus.text = "解析失败: ${e.message}"
                }
            }
        }
    }

    /**
     * 显示 API Key 配置对话框
     */
    private fun showApiKeyDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_api_key, null)

        val etApiKey = dialogView.findViewById<TextInputEditText>(R.id.etApiKey)
        val currentKey = WhisperService.getApiKey(requireContext())
        if (!currentKey.isNullOrEmpty()) {
            // 显示部分 key
            etApiKey.setText(currentKey.take(8) + "..." + currentKey.takeLast(4))
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("设置语音识别 API Key")
            .setMessage("使用硅基流动提供的语音识别服务\n\n获取地址: cloud.siliconflow.cn")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val apiKey = etApiKey.text.toString().trim()
                if (apiKey.isNotEmpty() && !apiKey.contains("...")) {
                    WhisperService.setApiKey(requireContext(), apiKey)
                    Toast.makeText(requireContext(), "API Key 已保存", Toast.LENGTH_SHORT).show()
                    binding.tvStatus.text = "点击下方按钮开始语音记账"
                }
            }
            .setNegativeButton("取消", null)
            .setNeutralButton("获取Key") { _, _ ->
                // 打开浏览器
                try {
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://cloud.siliconflow.cn/account/ak")
                    )
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "请访问 cloud.siliconflow.cn", Toast.LENGTH_LONG).show()
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        WhisperService.release()
        _binding = null
    }
}
