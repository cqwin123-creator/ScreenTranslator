package com.example.screentranslator.helper

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.LatinTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OCRHelper {

    // 拉丁语识别器（支持英语）
    private val latinRecognizer: TextRecognizer = TextRecognition.getClient(
        LatinTextRecognizerOptions.Builder().build()
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

        // 拉丁语识别（英语）
        val latinResult = try {
            suspendCancellableCoroutine<Text> { continuation ->
                latinRecognizer.process(image)
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
        } catch (e: Exception) { null }

        // 合并结果
        val allBlocks = mutableListOf<TextBlock>()

        japaneseResult?.textBlocks?.forEach { block ->
            allBlocks.add(TextBlock(block.text, block.boundingBox))
        }

        latinResult?.textBlocks?.forEach { block ->
            if (!allBlocks.any { it.text == block.text }) {
                allBlocks.add(TextBlock(block.text, block.boundingBox))
            }
        }

        val fullText = allBlocks.joinToString("\n") { it.text }

        OCRResult(fullText, allBlocks)
    }

    fun release() {
        latinRecognizer.close()
        japaneseRecognizer.close()
    }
}
