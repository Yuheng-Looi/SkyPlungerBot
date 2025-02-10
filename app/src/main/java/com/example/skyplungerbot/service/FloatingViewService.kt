package com.example.skyplungerbot.service

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.Button
import androidx.annotation.RequiresApi
import com.example.skyplungerbot.R

class FloatingViewService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var isBotRunning: Boolean = false

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 加载悬浮布局
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating, null)

        // 设置悬浮窗口参数
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // 设置窗口初始位置
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 100

        // 添加悬浮窗口
        windowManager.addView(floatingView, params)

        // 设置拖动监听（可选）
        floatingView?.setOnTouchListener(FloatingOnTouchListener(params))

        // 设置按钮点击事件
        val btnToggle = floatingView?.findViewById<Button>(R.id.btnToggle)
        btnToggle?.setOnClickListener {
            isBotRunning = !isBotRunning
            if (isBotRunning) {
                btnToggle.text = "Stop Bot"
                // 启动自动化操作服务（例如启动你的 GameAutomatorService）
//                startService(Intent(this, com.example.skyplungerbot.service.GameAutomatorService::class.java))
            } else {
                btnToggle.text = "Start Bot"
                // 停止自动化服务
//                stopService(Intent(this, com.example.skyplungerbot.service.GameAutomatorService::class.java))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (floatingView != null) windowManager.removeView(floatingView)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}

// 可选的拖动监听
class FloatingOnTouchListener(private val params: WindowManager.LayoutParams) : View.OnTouchListener {
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = params.x
                initialY = params.y
                initialTouchX = motionEvent.rawX
                initialTouchY = motionEvent.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                params.x = initialX + (motionEvent.rawX - initialTouchX).toInt()
                params.y = initialY + (motionEvent.rawY - initialTouchY).toInt()
                view.context.getSystemService(Service.WINDOW_SERVICE)?.let {
                    (it as WindowManager).updateViewLayout(view, params)
                }
                return true
            }
        }
        return false
    }
}