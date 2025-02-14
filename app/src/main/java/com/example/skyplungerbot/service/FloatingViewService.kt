package com.example.skyplungerbot.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import com.example.skyplungerbot.R

class FloatingViewService : Service() {
    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var isBotRunning = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating, null)

        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 100
        layoutParams.y = 100

        windowManager.addView(floatingView, layoutParams)

        val btnToggle = floatingView?.findViewById<Button>(R.id.btnToggle)
        btnToggle?.setOnClickListener {
            val intent = Intent(if (isBotRunning) "ACTION_STOP_CAPTURE" else "ACTION_START_CAPTURE")
            sendBroadcast(intent)
            isBotRunning = !isBotRunning
            btnToggle.text = if (isBotRunning) "Stop Bot" else "Start Bot"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) {
            windowManager.removeView(floatingView)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

