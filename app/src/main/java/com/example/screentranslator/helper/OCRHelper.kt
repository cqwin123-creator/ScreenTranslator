package com.example.screentranslator.helper

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OCRHelper {

    // 默认识别器支持拉丁语（英语）
    private val defaultRecognizer: TextRecognizer = TextRecognition.getClient()
    
    // 日语识别器
    private val japaneseRecognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build()
    )

    data class TextBlock(
        val text: String,
        val boundingBox: Rect?
    )

    data class OCRResult(
        val text: String,
        val blocks: List<TextBlock>
    )

    suspend fun recognizeText(bitmap: Bitmap): OCRResult = withContext(Dispatchers.IO) {
        val image = InputImage.fromBitmap(bitmap, 0)
        
        // 尝试日语识别
        val japaneseResult = try {
            japaneseRecognizer.process(image).await()
        } catch (e: Exception) { null }
        
        // 使用默认识别器（支持拉丁语/英语）
        val defaultResult = try {
            defaultRecognizer.process(image).await()
        } catch (e: Exception) { null }
        
        // 合并结果
        val allBlocks = mutableListOf<TextBlock>()
        
        japaneseResult?.textBlocks?.forEach { block ->
            allBlocks.add(TextBlock(block.text, block.boundingBox))
        }
        
        defaultResult?.textBlocks?.forEach { block ->
            // 避免重复添加
            if (!allBlocks.any { it.text == block.text }) {
                allBlocks.add(TextBlock(block.text, block.boundingBox))
            }
        }
        
        val fullText = allBlocks.joinToString("\n") { it.text }
        
        OCRResult(fullText, allBlocks)
    }

    fun release() {
        defaultRecognizer.close()
        japaneseRecognizer.close()
    }
}
