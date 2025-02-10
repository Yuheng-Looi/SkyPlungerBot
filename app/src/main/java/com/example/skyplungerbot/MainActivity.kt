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

class MainActivity : AppCompatActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager

    // 请求悬浮窗权限（使用 ActivityResultContracts）
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // 当用户授权悬浮窗权限后启动 FloatingViewService
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
            startService(Intent(this, FloatingViewService::class.java))
        }
    }

    // 请求屏幕捕获权限，注意不在这里调用 getMediaProjection()，而是将返回数据传给服务
    private val screenCaptureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // 将屏幕捕获结果通过 Intent Extra 传递给 GameAutomatorService
            val intent = Intent(this, GameAutomatorService::class.java)
            intent.putExtra("screen_capture_result_code", result.resultCode)
            intent.putExtra("screen_capture_data", result.data)
            startService(intent)
        } else {
            Log.e("MainActivity", "屏幕捕获权限未获得")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 请求悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startService(Intent(this, FloatingViewService::class.java))
        }

        // 初始化 MediaProjectionManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        // 请求屏幕捕获权限
        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
        screenCaptureLauncher.launch(captureIntent)
    }
}

