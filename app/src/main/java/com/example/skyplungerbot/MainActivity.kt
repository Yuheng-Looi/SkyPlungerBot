package com.example.skyplungerbot

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.skyplungerbot.service.FloatingViewService
import com.example.skyplungerbot.service.GameAutomatorService
import com.example.skyplungerbot.util.MediaProjectionHolder

class MainActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager

    // 请求悬浮窗权限的 ActivityResultLauncher
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (Settings.canDrawOverlays(this)) {
            Log.d("MainActivity", "悬浮窗权限已授权")
        }
    }

    // 请求屏幕捕获权限的 ActivityResultLauncher
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val mp = mediaProjectionManager.getMediaProjection(result.resultCode, result.data!!)
            MediaProjectionHolder.mediaProjection = mp
            Log.d("MainActivity", "MediaProjection 获取成功")
        } else {
            Log.e("MainActivity", "屏幕捕获权限未获得")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 请求悬浮窗权限
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }

        // 2. 启动 FloatingViewService 用于显示悬浮窗口（用于控制启动/停止 GameAutomatorService）
        startService(Intent(this, FloatingViewService::class.java))

        // 3. 启动 GameAutomatorService（注意：GameAutomatorService 必须在 onServiceConnected() 中调用 startForeground()）
        startService(Intent(this, GameAutomatorService::class.java))

        // 4. 请求屏幕捕获权限（此时 GameAutomatorService 已经启动并作为前台服务运行）
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }
}


