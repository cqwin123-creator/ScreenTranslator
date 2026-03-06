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
import java.net.HttpURLConnection
import java.net.URL

class OCRHelper(private val context: Context) {

    companion object {
        private const val TAG = "OCRHelper"
        // Tesseract 语言数据文件
        private const val LANG_ENG = "eng"      // 英语
        private const val LANG_JPN = "jpn"      // 日语
        private const val LANG_JPN_VERT = "jpn_vert" // 日语竖排
        
        // 语言数据下载地址（GitHub 镜像）
        private const val TESSDATA_BASE_URL = "https://github.com/tesseract-ocr/tessdata/raw/main/"
    }

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?
    )

    data class OCRResult(
        val text: String,
        val blocks: List<TextBlock>
    )

    data class LanguageInfo(
        val code: String,
        val name: String,
        val size: String
    )

    // 可用语言列表
    val availableLanguages = listOf(
        LanguageInfo(LANG_ENG, "英语", "4MB"),
        LanguageInfo(LANG_JPN, "日语", "35MB"),
        LanguageInfo(LANG_JPN_VERT, "日语竖排", "35MB")
    )

    private var tessBaseAPI: TessBaseAPI? = null
    private val dataPath: String = context.filesDir.absolutePath + "/tesseract/"

    init {
        File(dataPath + "tessdata/").mkdirs()
        tessBaseAPI = TessBaseAPI()
    }

    /**
     * 检查语言数据是否已下载
     */
    fun isLanguageDownloaded(languageCode: String): Boolean {
        return File(dataPath + "tessdata/$languageCode.traineddata").exists()
    }

    /**
     * 下载语言数据文件
     */
    suspend fun downloadLanguage(
        languageCode: String,
        onProgress: (progress: Int) -> Unit = {}
    ): Boolean = withContext(Dispatchers.IO) {
        val url = URL("$TESSDATA_BASE_URL$languageCode.traineddata")
        val destFile = File(dataPath + "tessdata/$languageCode.traineddata")
        
        try {
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 120000
            connection.connect()
            
            val totalSize = connection.contentLength
            val input = connection.inputStream
            val output = FileOutputStream(destFile)
            
            val buffer = ByteArray(8192)
            var downloaded = 0
            var read: Int
            
            while (input.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                downloaded += read
                
                if (totalSize > 0) {
                    val progress = (downloaded * 100 / totalSize)
                    onProgress(progress)
                }
            }
            
            output.flush()
            output.close()
            input.close()
            
            Log.d(TAG, "Downloaded language: $languageCode")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download language: $languageCode", e)
            destFile.delete() // 清理失败的文件
            false
        }
    }

    /**
     * 删除语言数据文件
     */
    fun deleteLanguage(languageCode: String): Boolean {
        val file = File(dataPath + "tessdata/$languageCode.traineddata")
        return file.delete()
    }

    /**
     * 获取已下载语言总大小
     */
    fun getDownloadedLanguagesSize(): Long {
        val tessdataDir = File(dataPath + "tessdata/")
        return tessdataDir.listFiles()?.sumOf { it.length() } ?: 0
    }

    suspend fun recognizeText(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        val allBlocks = mutableListOf<TextBlock>()
        
        // 按优先级识别：日语竖排 -> 日语 -> 英语
        val languages = listOf(LANG_JPN_VERT, LANG_JPN, LANG_ENG)
        
        for (lang in languages) {
            if (!isLanguageDownloaded(lang)) continue
            
            val text = recognizeWithLanguage(bitmap, lang)
            if (text.isNotBlank()) {
                // 避免重复添加相同内容
                if (!allBlocks.any { it.text == text }) {
                    allBlocks.add(TextBlock(text, null))
                }
            }
        }
        
        val fullText = allBlocks.joinToString("\n") { it.text }
        OCRResult(fullText, allBlocks)
    }

    private fun recognizeWithLanguage(bitmap: Bitmap, language: String): String {
        return try {
            tessBaseAPI?.let { tess ->
                if (tess.init(dataPath, language)) {
                    tess.setImage(bitmap)
                    val text = tess.utF8Text ?: ""
                    tess.clear()
                    text.trim()
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
