package com.example.skyplungerbot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Bitmap
import android.graphics.Path
import android.util.Log
import kotlinx.coroutines.delay

class ControlManager(private val service: AccessibilityService) {

    fun performTap(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()
        service.dispatchGesture(gesture, null, null)
        log("Performed tap at ($x, $y)")
    }

    fun performSwipe(startX: Int, startY: Int, endX: Int, endY: Int) {
        val path = Path().apply {
            moveTo(startX.toFloat(), startY.toFloat())
            lineTo(endX.toFloat(), endY.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 500))
            .build()
        service.dispatchGesture(gesture, null, null)
        log("Performed swipe from ($startX, $startY) to ($endX, $endY)")
    }

    // 将 delayMs 改为 suspend 函数，利用协程实现延时而不阻塞线程
    suspend fun delayMs(timeMillis: Long) {
        delay(timeMillis)
    }

    fun log(message: String) {
        Log.d("ControlManager", message)
    }

    fun getPixelColor(bitmap: Bitmap, x: Int, y: Int): Int {
        return bitmap.getPixel(x, y)
    }
}
