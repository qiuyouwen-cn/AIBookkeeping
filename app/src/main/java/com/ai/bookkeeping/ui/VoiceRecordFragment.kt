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
import com.ai.bookkeeping.ai.ASRService
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
import java.util.Locale

/**
 * 语音记账Fragment
 * 使用自定义 ASR 服务进行语音识别
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

        // 初始化 ASR 服务
        ASRService.init(requireContext())

        setupUI()
        setupClickListeners()
        setupASRCallbacks()
    }

    private fun setupUI() {
        binding.tvStatus.text = "点击下方按钮开始"
        binding.cardResult.visibility = View.GONE
        binding.tvRecognizedText.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.btnVoice.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                // 检查是否配置了服务器
                if (!ASRService.hasServerConfig(requireContext())) {
                    showServerConfigDialog()
                } else {
                    checkPermissionAndStart()
                }
            }
        }

        // 长按设置服务器
        binding.btnVoice.setOnLongClickListener {
            showServerConfigDialog()
            true
        }
    }

    private fun setupASRCallbacks() {
        ASRService.onStateChange = { state ->
            activity?.runOnUiThread {
                when (state) {
                    ASRService.RecordingState.IDLE -> {
                        binding.tvStatus.text = "点击下方按钮开始"
                    }
                    ASRService.RecordingState.CONNECTING -> {
                        binding.tvStatus.text = "正在连接..."
                    }
                    ASRService.RecordingState.RECORDING -> {
                        binding.tvStatus.text = "正在聆听..."
                        binding.animationView.visibility = View.VISIBLE
                    }
                    ASRService.RecordingState.PROCESSING -> {
                        binding.tvStatus.text = "正在识别..."
                        binding.animationView.visibility = View.GONE
                    }
                    ASRService.RecordingState.COMPLETED -> {
                        binding.tvStatus.text = "识别完成"
                    }
                    ASRService.RecordingState.ERROR -> {
                        binding.tvStatus.text = "识别失败"
                        binding.animationView.visibility = View.GONE
                    }
                }
            }
        }

        ASRService.onError = { error ->
            activity?.runOnUiThread {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                stopRecordingUI()
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
        binding.tvStatus.text = "正在聆听..."

        // 使用流式识别
        ASRService.startStreaming(
            onPartialResult = { text ->
                activity?.runOnUiThread {
                    if (text.isNotEmpty()) {
                        binding.tvRecognizedText.text = "\"$text\""
                        binding.tvRecognizedText.visibility = View.VISIBLE
                    }
                }
            },
            onFinalResult = { text ->
                activity?.runOnUiThread {
                    if (text.isNotEmpty()) {
                        binding.tvRecognizedText.text = "\"$text\""
                        binding.tvRecognizedText.visibility = View.VISIBLE
                        parseAndSave(text)
                    } else {
                        binding.tvStatus.text = "未能识别，请重试"
                    }
                    stopRecordingUI()
                }
            },
            onStreamError = { error ->
                activity?.runOnUiThread {
                    binding.tvStatus.text = "识别错误: $error"
                    stopRecordingUI()

                    // 如果流式识别失败，尝试使用文件转录方式
                    tryFileTranscribe()
                }
            }
        )
    }

    /**
     * 尝试使用文件转录方式（作为流式识别的备用方案）
     */
    private fun tryFileTranscribe() {
        isRecording = true
        binding.btnVoice.setIconResource(R.drawable.ic_stop)
        binding.btnVoice.text = "录音中..."
        binding.tvStatus.text = "正在录音（最长10秒）..."
        binding.animationView.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.Main).launch {
            val result = ASRService.recordAndTranscribe(requireContext(), 10000)

            result.fold(
                onSuccess = { text ->
                    if (text.isNotEmpty()) {
                        binding.tvRecognizedText.text = "\"$text\""
                        binding.tvRecognizedText.visibility = View.VISIBLE
                        parseAndSave(text)
                    } else {
                        binding.tvStatus.text = "未能识别，请重试"
                    }
                },
                onFailure = { error ->
                    binding.tvStatus.text = "识别失败: ${error.message}"
                }
            )

            stopRecordingUI()
        }
    }

    private fun stopRecording() {
        ASRService.finishRecording()
        stopRecordingUI()
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
                        binding.tvStatus.text = "记账成功！"

                        Toast.makeText(
                            requireContext(),
                            "已记录: ${result.category} ${currencyFormat.format(result.amount)}",
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
     * 显示服务器配置对话框
     */
    private fun showServerConfigDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_asr_server, null)

        val etServerUrl = dialogView.findViewById<TextInputEditText>(R.id.etServerUrl)
        val currentUrl = ASRService.getServerUrl(requireContext())
        if (currentUrl != "YOUR_SERVER:5678") {
            etServerUrl.setText(currentUrl)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("设置语音识别服务器")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                val serverUrl = etServerUrl.text.toString().trim()
                if (serverUrl.isNotEmpty()) {
                    ASRService.setServerUrl(requireContext(), serverUrl)
                    Toast.makeText(requireContext(), "服务器地址已保存", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ASRService.release()
        _binding = null
    }
}
