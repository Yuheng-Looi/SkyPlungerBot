package com.example.skyplungerbot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
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
import com.example.skyplungerbot.logic.GameLogicManager
import com.example.skyplungerbot.ocr.OCRManager
import com.example.skyplungerbot.util.MediaProjectionHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GameAutomatorService : AccessibilityService() {
    private lateinit var ocrManager: OCRManager
    private lateinit var controlManager: ControlManager
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("GameAutomatorService", "onCreate() 被调用，服务初始化中...")
        startForegroundService() // 确保前台服务启动
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("GameAutomatorService", "onServiceConnected() 被触发")
        startForegroundService()  // 先启动前台服务


        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
        }
        ocrManager = OCRManager(this)
        controlManager = ControlManager(this)
        Log.d("GameAutomatorService", "服务已启动，开始自动化流程")

        val mp = MediaProjectionHolder.mediaProjection
        if (mp == null) {
            Log.e("GameAutomatorService", "未获取到 MediaProjection 实例，无法截屏")
        } else {
            // 获取屏幕宽度、高度和密度，兼容 API 30+
            val metrics = DisplayMetrics()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                val bounds = windowManager.currentWindowMetrics.bounds
                metrics.widthPixels = bounds.width()
                metrics.heightPixels = bounds.height()
                metrics.densityDpi = resources.displayMetrics.densityDpi
            } else {
                val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
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
        }

        // 启动定时截屏任务（每秒截屏一次，并对目标区域进行处理）
        serviceScope.launch {
            // 定义目标区域，覆盖所有 OCR 检测区域（例如：(58,520) 到 (1018,1917)）
            val unionRegion = Rect(58, 520, 1018, 1917)
            while (true) {
                val fullBitmap = captureScreen()
                if (fullBitmap != null) {
                    val targetedBitmap = cropBitmap(fullBitmap, unionRegion)
                    if (targetedBitmap != null) {
                        GameLogicManager.processState(targetedBitmap, ocrManager, controlManager)
                    } else {
                        Log.d("GameAutomatorService", "目标区域裁剪失败")
                    }
                } else {
                    Log.d("GameAutomatorService", "屏幕截图失败")
                }
                delay(1000L)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 如有需要可处理 Accessibility 事件
    }

    override fun onInterrupt() {
        Log.d("GameAutomatorService", "服务中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        ocrManager.release()
        Log.d("GameAutomatorService", "服务已销毁")
    }

    /**
     * 利用 ImageReader 获取最新屏幕截图并转换为 Bitmap
     */
    private fun captureScreen(): Bitmap? {
        Log.d("GameAutomatorService", "captureScreen() 被调用")
        val image = imageReader?.acquireLatestImage()
        if (image != null) {
            Log.d("GameAutomatorService", "成功获取屏幕截图")
            try {
                val width = image.width
                val height = image.height
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width
                val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                Log.d("GameAutomatorService", "截图成功")
                // 调用 OCR 进行状态检测
                val detectedText = ocrManager.detectText(bitmap, Rect(0,0,1000,1500))
                Log.d("GameAutomatorService", "OCR 识别结果: $detectedText")

                // 这里可以加入状态判断逻辑
//                when {
//                    detectedText.contains("SETTINGS", ignoreCase = true) -> Log.d("GameAutomatorService", "检测到主菜单")
//                    detectedText.contains("HEIGHT", ignoreCase = true) -> Log.d("GameAutomatorService", "检测到游戏进行中")
//                    detectedText.contains("§@@RE", ignoreCase = true) -> Log.d("GameAutomatorService", "检测到游戏结束")
//                    else -> Log.d("GameAutomatorService", "未知状态")
//                }
                return croppedBitmap
            } catch (e: Exception) {
                Log.e("GameAutomatorService", "截图转换出错: ${e.message}")
            } finally {
                image.close()
            }
        } else {
            Log.e("GameAutomatorService", "imageReader.acquireLatestImage() 返回 null")
        }
        return null
    }

    /**
     * 裁剪传入的 Bitmap 到指定区域 [region]，返回裁剪后的 Bitmap
     */
    private fun cropBitmap(bitmap: Bitmap, region: Rect): Bitmap? {
        return try {
            Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height())
        } catch (e: Exception) {
            Log.e("GameAutomatorService", "裁剪 Bitmap 出错: ${e.message}")
            null
        }
    }

    /**
     * 启动前台服务，创建通知渠道并调用 startForeground()
     */
    private fun startForegroundService() {
        val channelId = "game_automation_service"
        val channelName = "Game Automation Service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Sky Plunger Bot Running")
            .setContentText("自动化服务正在运行...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification)
    }
}


