package com.example.screentranslator.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class OCRHelper(private val context: Context) {

    companion object {
        private const val TAG = "OCRHelper"
        // Tesseract 语言数据文件
        private const val LANG_ENG = "eng"      // 英语
        private const val LANG_JPN = "jpn"      // 日语
        private const val LANG_JPN_VERT = "jpn_vert" // 日语竖排
    }

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?
    )

    data class OCRResult(
        val text: String,
        val blocks: List<TextBlock>
    )

    private var tessBaseAPI: TessBaseAPI? = null
    private var dataPath: String = ""

    init {
        initTesseract()
    }

    private fun initTesseract() {
        dataPath = context.filesDir.absolutePath + "/tesseract/"
        val tessdataPath = dataPath + "tessdata/"
        
        // 创建目录
        File(tessdataPath).mkdirs()
        
        // 复制语言数据文件（如果不存在）
        copyLanguageDataIfNeeded("eng.traineddata")
        copyLanguageDataIfNeeded("jpn.traineddata")
        copyLanguageDataIfNeeded("jpn_vert.traineddata")
        
        // 初始化 Tesseract
        tessBaseAPI = TessBaseAPI()
    }

    private fun copyLanguageDataIfNeeded(filename: String) {
        val destFile = File(dataPath + "tessdata/" + filename)
        if (destFile.exists()) return
        
        try {
            // 从 assets 复制（需要提前将 traineddata 文件放入 assets）
            context.assets.open("tessdata/$filename").use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            Log.d(TAG, "Copied language data: $filename")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy language data: $filename", e)
        }
    }

    suspend fun recognizeText(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        val allBlocks = mutableListOf<TextBlock>()
        
        // 尝试日语识别
        val japaneseText = recognizeWithLanguage(bitmap, LANG_JPN)
        if (japaneseText.isNotBlank()) {
            allBlocks.add(TextBlock(japaneseText, null))
        }
        
        // 尝试日语竖排识别
        val japaneseVertText = recognizeWithLanguage(bitmap, LANG_JPN_VERT)
        if (japaneseVertText.isNotBlank() && japaneseVertText != japaneseText) {
            allBlocks.add(TextBlock(japaneseVertText, null))
        }
        
        // 尝试英语识别
        val englishText = recognizeWithLanguage(bitmap, LANG_ENG)
        if (englishText.isNotBlank()) {
            allBlocks.add(TextBlock(englishText, null))
        }
        
        // 合并所有结果
        val fullText = allBlocks.joinToString("\n") { it.text }
        
        OCRResult(fullText, allBlocks)
    }

    private fun recognizeWithLanguage(bitmap: Bitmap, language: String): String {
        return try {
            tessBaseAPI?.let { tess ->
                // 初始化指定语言
                if (tess.init(dataPath, language)) {
                    tess.setImage(bitmap)
                    val text = tess.utF8Text ?: ""
                    tess.clear()
                    text
                } else {
                    Log.e(TAG, "Failed to initialize Tesseract with language: $language")
                    ""
                }
            } ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "OCR error with language $language", e)
            ""
        }
    }

    fun release() {
        tessBaseAPI?.end()
    }
}
