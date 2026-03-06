package com.example.screentranslator.helper

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

class TranslationHelper(private val apiKey: String) {

    companion object {
        private const val API_URL = "https://api.moonshot.cn/v1/chat/completions"
        private const val MODEL = "moonshot-v1-8k"
    }

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    data class TranslationResult(
        val translatedText: String,
        val isSuccess: Boolean,
        val errorMessage: String? = null
    )

    suspend fun translate(text: String): TranslationResult = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext TranslationResult("", false, "API Key未配置")
            }

            if (text.isBlank()) {
                return@withContext TranslationResult("", true)
            }

            val requestBody = mapOf(
                "model" to MODEL,
                "messages" to listOf(
                    mapOf(
                        "role" to "system",
                        "content" to "你是一个专业的翻译助手。请将用户提供的文字翻译成中文，只返回翻译结果，不要添加任何解释。"
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to "请将以下文字翻译成中文：\n\n$text"
                    )
                ),
                "temperature" to 0.3,
                "max_tokens" to 2000
            )

            val body = gson.toJson(requestBody)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()

                if (!response.isSuccessful) {
                    return@withContext TranslationResult(
                        "", false, "API错误: ${response.code}"
                    )
                }

                val jsonResponse = gson.fromJson(responseBody, ChatResponse::class.java)
                val translatedText = jsonResponse.choices?.firstOrNull()?.message?.content?.trim() ?: ""

                TranslationResult(translatedText, true)
            }

        } catch (e: IOException) {
            TranslationResult("", false, "网络错误: ${e.message}")
        } catch (e: Exception) {
            TranslationResult("", false, "翻译失败: ${e.message}")
        }
    }

    suspend fun validateApiKey(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext false

            val requestBody = mapOf(
                "model" to MODEL,
                "messages" to listOf(mapOf("role" to "user", "content" to "Hi")),
                "max_tokens" to 5
            )

            val body = gson.toJson(requestBody)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    data class ChatResponse(
        @SerializedName("choices") val choices: List<Choice>?
    )

    data class Choice(
        @SerializedName("message") val message: Message?
    )

    data class Message(
        @SerializedName("content") val content: String?
    )
}
