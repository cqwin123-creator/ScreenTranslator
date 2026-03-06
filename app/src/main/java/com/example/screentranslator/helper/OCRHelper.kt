package com.example.screentranslator.helper

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OCRHelper {

    // 基础识别器（支持拉丁语/英语）
    private val defaultRecognizer: TextRecognizer = TextRecognition.getClient(
        com.google.mlkit.vision.text.TextRecognizerOptions.DEFAULT_OPTIONS
    )

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

        // 日语识别
        val japaneseResult = try {
            suspendCancellableCoroutine<Text> { continuation ->
                japaneseRecognizer.process(image)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        } catch (e: Exception) { null }

        // 基础识别（英语/拉丁语）
        val defaultResult = try {
            suspendCancellableCoroutine<Text> { continuation ->
                defaultRecognizer.process(image)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        } catch (e: Exception) { null }

        // 合并结果
        val allBlocks = mutableListOf<TextBlock>()

        japaneseResult?.textBlocks?.forEach { block ->
            allBlocks.add(TextBlock(block.text, block.boundingBox))
        }

        defaultResult?.textBlocks?.forEach { block ->
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
