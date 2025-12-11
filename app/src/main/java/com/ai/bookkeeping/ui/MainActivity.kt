package com.ai.bookkeeping.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.ai.bookkeeping.R
import com.ai.bookkeeping.databinding.ActivityMainBinding
import com.ai.bookkeeping.service.FloatingWindowService

/**
 * 主Activity，承载底部导航和Fragment容器
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var navHostFragment: NavHostFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let { handleIntent(it) }
    }

    private fun setupNavigation() {
        navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment!!.navController
        binding.bottomNavigation.setupWithNavController(navController)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.getBooleanExtra("open_voice", false)) {
            // 从悬浮窗点击进入，直接跳转到语音记账
            navHostFragment?.navController?.navigate(R.id.voiceRecordFragment)
        }
    }

    /**
     * 启动悬浮窗服务
     */
    fun startFloatingService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // 请求悬浮窗权限
                AlertDialog.Builder(this)
                    .setTitle("需要悬浮窗权限")
                    .setMessage("开启悬浮窗后，可以在任意界面快速唤起语音记账")
                    .setPositiveButton("去开启") { _, _ ->
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
                    }
                    .setNegativeButton("取消", null)
                    .show()
                return
            }
        }
        startService(Intent(this, FloatingWindowService::class.java))
        Toast.makeText(this, "悬浮窗已开启", Toast.LENGTH_SHORT).show()
    }

    /**
     * 停止悬浮窗服务
     */
    fun stopFloatingService() {
        stopService(Intent(this, FloatingWindowService::class.java))
        Toast.makeText(this, "悬浮窗已关闭", Toast.LENGTH_SHORT).show()
    }

    /**
     * 检查悬浮窗是否运行中
     */
    fun isFloatingServiceRunning(): Boolean {
        // 简单实现，实际可以通过ActivityManager检查
        return FloatingWindowService.isRunning
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, FloatingWindowService::class.java))
                    Toast.makeText(this, "悬浮窗已开启", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "未授予悬浮窗权限", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
}
