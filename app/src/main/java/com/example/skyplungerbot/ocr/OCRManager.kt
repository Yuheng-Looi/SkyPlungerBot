package com.example.skyplungerbot.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import java.io.File
import java.io.FileOutputStream

class OCRManager(private val context: Context) {
    private val tessBaseAPI: TessBaseAPI = TessBaseAPI()

    init {
        val datapath = context.filesDir.absolutePath + "/tesseract/"
        copyTessDataFiles(datapath)
        if (tessBaseAPI.init(datapath, "eng")) {
            Log.d("OCRManager", "Tesseract initialized successfully.")
        } else {
            Log.e("OCRManager", "Could not initialize Tesseract.")
        }
    }

    private fun copyTessDataFiles(datapath: String) {
        val tessDataDir = File(datapath + "tessdata/")
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }
        try {
            val filePath = tessDataDir.absolutePath + "/eng.traineddata"
            val file = File(filePath)
            if (!file.exists()) {
                context.assets.open("tessdata/eng.traineddata").use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        val buffer = ByteArray(1024)
                        var read: Int
                        while (inputStream.read(buffer).also { read = it } != -1) {
                            outputStream.write(buffer, 0, read)
                        }
                        outputStream.flush()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("OCRManager", "Error copying tessdata: ${e.message}")
        }
    }

    /**
     * 检测 bitmap 中指定区域 [region] 的文字
     */
    fun detectText(bitmap: Bitmap, region: Rect): String {
        val cropped = Bitmap.createBitmap(bitmap, region.left, region.top, region.width(), region.height())
        tessBaseAPI.setImage(cropped)
        val result = tessBaseAPI.utF8Text ?: ""
        tessBaseAPI.clear()
        return result.trim()
    }

    fun release() {
        tessBaseAPI.end()
    }
}
