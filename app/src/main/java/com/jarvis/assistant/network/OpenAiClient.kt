package com.jarvis.assistant.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OpenAiClient {
    private const val TAG = "OpenAiClient"
    const val PLATFORM_URL = "https://platform.openai.com/"
    const val DESCRIPTION_BN = "এটি চ্যাট, কোড জেনারেশন এবং অন্যান্য টেক্সট-ভিত্তিক কাজের জন্য দারুণ সাহায্য করে।"

    val SUPPORTED_MODELS = listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    suspend fun testApiKey(apiKey: String): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext Pair(false, -1L)

        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("https://api.openai.com/v1/models")
                .header("Authorization", "Bearer $cleanKey")
                .build()

            httpClient.newCall(request).execute().use { response ->
                val duration = System.currentTimeMillis() - startTime
                if (response.isSuccessful) {
                    Pair(true, duration)
                } else {
                    Pair(false, response.code.toLong())
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI test connection failed", e)
            Pair(false, -1L)
        }
    }

    suspend fun getChatCompletion(
        apiKey: String,
        prompt: String,
        model: String = "gpt-4o-mini",
        systemInstruction: String = "You are NUSRAT AI, a helpful and respectful voice assistant developed by MD APON."
    ): String? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext null

        try {
            val root = JSONObject().apply {
                put("model", model)
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstruction)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                }
                put("messages", messages)
                put("max_tokens", 350)
                put("temperature", 0.7)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $cleanKey")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val first = choices.getJSONObject(0)
                    val msg = first.optJSONObject("message")
                    return@withContext msg?.optString("content")?.trim()
                }
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "OpenAI chat completion error", e)
            null
        }
    }
}
