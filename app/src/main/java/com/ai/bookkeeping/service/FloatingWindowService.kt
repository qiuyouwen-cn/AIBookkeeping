package com.ai.bookkeeping.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.ai.bookkeeping.R
import com.ai.bookkeeping.ai.WhisperService
import com.ai.bookkeeping.data.AppDatabase
import com.ai.bookkeeping.util.AIParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 悬浮窗服务 - 快速语音记账
 * 点击悬浮按钮直接开始录音，无需跳转到APP
 */
class FloatingWindowService : Service() {

    companion object {
        var isRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var recordingView: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var recordingParams: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var hasMoved = false
    private var touchStartTime = 0L

    private var isRecording = false
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale.CHINA)

    // 录音界面控件
    private var tvStatus: TextView? = null
    private var tvResult: TextView? = null
    private var tvHint: TextView? = null
    private var btnRecord: ImageView? = null
    private var btnClose: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        WhisperService.init(this)
        createFloatingWindow()
    }

    private fun createFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_button, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 300
        }

        windowManager?.addView(floatingView, params)

        setupTouchListener()
    }

    private fun setupTouchListener() {
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    hasMoved = false
                    touchStartTime = System.currentTimeMillis()
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                        hasMoved = true
                        params?.x = initialX + deltaX.toInt()
                        params?.y = initialY + deltaY.toInt()
                        windowManager?.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val touchDuration = System.currentTimeMillis() - touchStartTime
                    if (!hasMoved && touchDuration < 300) {
                        showRecordingPanel()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * 显示录音面板
     */
    private fun showRecordingPanel() {
        if (recordingView != null) return

        // 检查 API Key
        if (!WhisperService.hasApiKey(this)) {
            Toast.makeText(this, "请先在APP中设置语音识别 API Key", Toast.LENGTH_LONG).show()
            return
        }

        recordingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_recording, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        recordingParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        windowManager?.addView(recordingView, recordingParams)

        // 初始化控件
        tvStatus = recordingView?.findViewById(R.id.tvStatus)
        tvResult = recordingView?.findViewById(R.id.tvResult)
        tvHint = recordingView?.findViewById(R.id.tvHint)
        btnRecord = recordingView?.findViewById(R.id.btnRecord)
        btnClose = recordingView?.findViewById(R.id.btnClose)

        // 设置点击事件
        btnRecord?.setOnClickListener {
            if (isRecording) {
                stopRecording()
            } else {
                startRecording()
            }
        }

        btnClose?.setOnClickListener {
            hideRecordingPanel()
        }

        // 自动开始录音
        handler.postDelayed({ startRecording() }, 300)
    }

    /**
     * 隐藏录音面板
     */
    private fun hideRecordingPanel() {
        if (isRecording) {
            WhisperService.stopRecording()
            isRecording = false
        }
        recordingView?.let {
            windowManager?.removeView(it)
        }
        recordingView = null
        tvStatus = null
        tvResult = null
        tvHint = null
        btnRecord = null
        btnClose = null
    }

    /**
     * 开始录音
     */
    private fun startRecording() {
        isRecording = true
        btnRecord?.isSelected = true
        tvStatus?.text = "正在聆听..."
        tvResult?.visibility = View.GONE
        tvHint?.text = "点击停止录音"

        serviceScope.launch {
            val result = WhisperService.recordAndTranscribe(
                context = this@FloatingWindowService,
                maxDurationMs = 15000
            )

            result.fold(
                onSuccess = { text ->
                    if (text.isNotEmpty()) {
                        tvStatus?.text = "识别成功"
                        tvResult?.text = "\"$text\""
                        tvResult?.visibility = View.VISIBLE
                        parseAndSave(text)
                    } else {
                        tvStatus?.text = "未识别到语音"
                        tvHint?.text = "点击重新录音"
                    }
                },
                onFailure = { error ->
                    tvStatus?.text = "识别失败"
                    tvResult?.text = error.message
                    tvResult?.visibility = View.VISIBLE
                    tvHint?.text = "点击重新录音"
                }
            )

            isRecording = false
            btnRecord?.isSelected = false
        }
    }

    /**
     * 停止录音
     */
    private fun stopRecording() {
        WhisperService.stopRecording()
        tvStatus?.text = "正在识别..."
        tvHint?.text = "请稍候..."
    }

    /**
     * 解析并保存记账
     */
    private fun parseAndSave(text: String) {
        serviceScope.launch {
            try {
                val transaction = withContext(Dispatchers.IO) {
                    AIParser.parse(text)
                }

                if (transaction != null) {
                    // 保存到数据库
                    withContext(Dispatchers.IO) {
                        val db = AppDatabase.getDatabase(this@FloatingWindowService)
                        db.transactionDao().insert(transaction)
                    }

                    // 显示成功信息
                    val typeStr = if (transaction.type == com.ai.bookkeeping.model.TransactionType.EXPENSE) "支出" else "收入"
                    val dateFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA)
                    val dateStr = dateFormat.format(Date(transaction.date))

                    tvStatus?.text = "记账成功！"
                    tvResult?.text = "$dateStr ${transaction.category} ${currencyFormat.format(transaction.amount)} ($typeStr)"
                    tvResult?.visibility = View.VISIBLE
                    tvHint?.text = "3秒后自动关闭"

                    // 显示Toast
                    Toast.makeText(
                        this@FloatingWindowService,
                        "已记录: ${transaction.category} ${currencyFormat.format(transaction.amount)}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // 3秒后自动关闭
                    handler.postDelayed({ hideRecordingPanel() }, 3000)
                } else {
                    tvStatus?.text = "无法解析"
                    tvHint?.text = "请换一种说法，点击重试"
                }
            } catch (e: Exception) {
                tvStatus?.text = "保存失败"
                tvResult?.text = e.message
                tvResult?.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        hideRecordingPanel()
        floatingView?.let {
            windowManager?.removeView(it)
        }
    }
}
