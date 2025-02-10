package com.example.skyplungerbot.logic

import android.graphics.Bitmap
import android.graphics.Rect
import com.example.skyplungerbot.ocr.OCRManager
import com.example.skyplungerbot.service.ControlManager

object GameLogicManager {
    // 定义各状态检测区域（根据你提供的数据）
    private val SETTINGS_REGION = Rect(825, 520, 1018, 617)   // SETTINGS
    private val HEIGHT_REGION = Rect(58, 685, 185, 730)         // HEIGHT
    private val SCORE_REGION = Rect(350, 1860, 558, 1917)       // §@@RE

    // 固定操作坐标
    private val TAP_TO_LAUNCH_COORD = Pair(550, 1388)
    private val CONTINUE_COORD = Pair(567, 2004)
    // 广告状态中：后续用广告检测模块，这里仅作占位

    suspend fun processState(bitmap: Bitmap, ocrManager: OCRManager, controlManager: ControlManager) {
        // 使用 OCR 分别检测三个区域的文字
        val detectedSettings = ocrManager.detectText(bitmap, SETTINGS_REGION)
        val detectedHeight = ocrManager.detectText(bitmap, HEIGHT_REGION)
        val detectedScore = ocrManager.detectText(bitmap, SCORE_REGION)

        controlManager.log("OCR结果：SETTINGS='$detectedSettings', HEIGHT='$detectedHeight', SCORE='$detectedScore'")

        when {
            detectedSettings.contains("SETTINGS", ignoreCase = true) -> {
                controlManager.log("状态：Main Menu -> 执行双击启动游戏")
                controlManager.performTap(TAP_TO_LAUNCH_COORD.first, TAP_TO_LAUNCH_COORD.second)
                controlManager.performTap(TAP_TO_LAUNCH_COORD.first, TAP_TO_LAUNCH_COORD.second)
            }
            detectedHeight.contains("HEIGHT", ignoreCase = true) -> {
                controlManager.log("状态：Game Playing -> 游戏进行中")
                // 此处可以添加左右滑动操作逻辑（后续扩展）
            }
            detectedScore.contains("§@@RE") -> {
                controlManager.log("状态：End Game -> 等待2秒后检测奖励按钮")
                controlManager.delayMs(2000)
                // 检查 (750,1074) 的颜色是否为白色
                val color = controlManager.getPixelColor(bitmap, 750, 1074)
                if (isWhite(color)) {
                    controlManager.log("检测到白色 -> 点击 x3 奖励按钮并执行广告跳过")
                    controlManager.performTap(750, 1074)
                    // 这里可以调用广告检测模块的逻辑
                } else {
                    controlManager.log("未检测到白色 -> 点击 Continue 返回主菜单")
                    controlManager.performTap(CONTINUE_COORD.first, CONTINUE_COORD.second)
                }
            }
            else -> {
                controlManager.log("状态：Ads 或未知 -> 待处理广告跳过逻辑")
                // 这里可调用广告检测模块，如果需要
            }
        }
    }

    private fun isWhite(color: Int): Boolean {
        // 检查颜色是否为纯白色 (RGB 255,255,255)
        return (color shr 16 and 0xFF) == 255 &&
                (color shr 8 and 0xFF) == 255 &&
                (color and 0xFF) == 255
    }
}
