package com.example.skyplungerbot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.core.app.NotificationCompat
import com.example.skyplungerbot.MainActivity
import com.example.skyplungerbot.R
import com.example.skyplungerbot.ocr.OCRManager
import com.example.skyplungerbot.util.MediaProjectionHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class GameAutomatorService : AccessibilityService() {
    private lateinit var ocrManager: OCRManager
    private lateinit var controlManager: ControlManager
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var isCapturing: Boolean = false

    // 广播控制截图
    private val captureReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                "ACTION_START_CAPTURE" -> startCapturing()
                "ACTION_STOP_CAPTURE" -> stopCapturing()
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        Log.d("GameAutomatorService", "服务初始化")

        val filter = IntentFilter().apply {
            addAction("ACTION_START_CAPTURE")
            addAction("ACTION_STOP_CAPTURE")
        }
        registerReceiver(captureReceiver, filter)

        // 启动前台服务
        startForegroundService()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("GameAutomatorService", "GameAutomatorService 已连接")

        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        }

        ocrManager = OCRManager(this)
        controlManager = ControlManager(this)
    }

    private fun startForegroundService() {
        val channelId = "game_automator_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Game Automator Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Game Automator 正在运行")
            .setContentText("点击以管理服务")
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this, 0, Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
            .build()

        startForeground(1, notification)
    }

    fun startCapturing() {
        if (isCapturing) return
        isCapturing = true
        Log.d("GameAutomatorService", "开始截图")
        captureScreen()
    }

    fun stopCapturing() {
        isCapturing = false
        Log.d("GameAutomatorService", "停止截图")
        imageReader?.close()
        virtualDisplay?.release()
    }

    private fun captureScreen() {
        val mp = MediaProjectionHolder.mediaProjection
        if (mp == null) {
            Log.e("GameAutomatorService", "未获取到 MediaProjection 实例")
            return
        }

        val metrics = DisplayMetrics()
        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bounds = windowManager.currentWindowMetrics.bounds
            metrics.widthPixels = bounds.width()
            metrics.heightPixels = bounds.height()
            metrics.densityDpi = resources.displayMetrics.densityDpi
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(metrics)
        }

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mp.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            if (!isCapturing) return@setOnImageAvailableListener
            val image = reader.acquireLatestImage()
            if (image != null) {
                try {
                    val w = image.width
                    val h = image.height
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * w
                    val bitmap = Bitmap.createBitmap(w + rowPadding / pixelStride, h, Bitmap.Config.ARGB_8888)
                    bitmap.copyPixelsFromBuffer(buffer)
                    val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, w, h)
                    Log.d("GameAutomatorService", "截图成功")
                } catch (e: Exception) {
                    Log.e("GameAutomatorService", "截图出错: ${e.message}")
                } finally {
                    image.close()
                }
            }
        }, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(captureReceiver)
        serviceScope.cancel()
        imageReader?.close()
        virtualDisplay?.release()
        stopForeground(true)
        Log.d("GameAutomatorService", "服务已销毁")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.d("GameAutomatorService", "服务中断")
    }
}

