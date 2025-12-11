package com.ai.bookkeeping.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.FragmentVoiceRecordBinding
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
 * 语音记账Fragment
 */
class VoiceRecordFragment : Fragment() {

    private var _binding: FragmentVoiceRecordBinding? = null
    private val binding get() = _binding!!

    private val viewModel: TransactionViewModel by activityViewModels()
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    // 权限请求启动器
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListening()
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

        setupSpeechRecognizer()
        setupClickListeners()
    }

    private fun setupSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(requireContext())) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(requireContext())
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    binding.tvStatus.text = "请说话..."
                    binding.tvHint.text = "例如：\"早餐花了15块\" 或 \"收到工资5000\""
                }

                override fun onBeginningOfSpeech() {
                    binding.tvStatus.text = "正在聆听..."
                    binding.animationView.visibility = View.VISIBLE
                }

                override fun onRmsChanged(rmsdB: Float) {
                    // 可以根据音量调整动画
                }

                override fun onBufferReceived(buffer: ByteArray?) {}

                override fun onEndOfSpeech() {
                    binding.tvStatus.text = "正在识别..."
                    stopListeningUI()
                }

                override fun onError(error: Int) {
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH -> "未能识别，请重试"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "未检测到语音"
                        SpeechRecognizer.ERROR_NETWORK -> "网络错误，请检查网络连接"
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "网络超时"
                        else -> "识别错误，请重试"
                    }
                    binding.tvStatus.text = errorMessage
                    stopListeningUI()
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val text = matches[0]
                        binding.tvRecognizedText.text = "\"$text\""
                        binding.tvRecognizedText.visibility = View.VISIBLE
                        parseAndSave(text)
                    } else {
                        binding.tvStatus.text = "未能识别，请重试"
                    }
                    stopListeningUI()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        binding.tvRecognizedText.text = "\"${matches[0]}\""
                        binding.tvRecognizedText.visibility = View.VISIBLE
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            binding.tvStatus.text = "您的设备不支持语音识别"
            binding.btnVoice.isEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnVoice.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                checkPermissionAndStart()
            }
        }
    }

    private fun checkPermissionAndStart() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                startListening()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun startListening() {
        isListening = true
        binding.btnVoice.setIconResource(R.drawable.ic_stop)
        binding.btnVoice.text = "停止"
        binding.tvRecognizedText.visibility = View.GONE
        binding.cardResult.visibility = View.GONE

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "启动语音识别失败", Toast.LENGTH_SHORT).show()
            stopListeningUI()
        }
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        stopListeningUI()
    }

    private fun stopListeningUI() {
        isListening = false
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

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        _binding = null
    }
}
