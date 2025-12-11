package com.ai.bookkeeping.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.ai.bookkeeping.R
import com.ai.bookkeeping.ui.MainActivity

/**
 * 悬浮窗服务 - 快速唤起语音记账
 */
class FloatingWindowService : Service() {

    companion object {
        var isRunning = false
            private set
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var params: WindowManager.LayoutParams? = null

    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
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
        setupClickListener()
    }

    private fun setupTouchListener() {
        floatingView?.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params?.x ?: 0
                    initialY = params?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params?.x = initialX + (event.rawX - initialTouchX).toInt()
                    params?.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    private fun setupClickListener() {
        val btnFloat = floatingView?.findViewById<ImageView>(R.id.btn_floating)
        btnFloat?.setOnClickListener {
            // 计算移动距离，如果移动很小则视为点击
            val moveThreshold = 10
            if (Math.abs(initialX - (params?.x ?: 0)) < moveThreshold &&
                Math.abs(initialY - (params?.y ?: 0)) < moveThreshold) {
                openVoiceRecord()
            }
        }
    }

    private fun openVoiceRecord() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_voice", true)
        }
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        floatingView?.let {
            windowManager?.removeView(it)
        }
    }
}
