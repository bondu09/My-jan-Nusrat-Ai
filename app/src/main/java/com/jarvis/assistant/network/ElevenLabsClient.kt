package com.jarvis.assistant.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ElevenLabsClient {
    private const val TAG = "ElevenLabsClient"
    const val PLATFORM_URL = "https://elevenlabs.io/"
    const val DESCRIPTION_BN = "এটি মানুষের মতো ভয়েস তৈরি করতে সাহায্য করে।"

    // Popular default ElevenLabs voices
    val PRESET_VOICES = mapOf(
        "Rachel (Calm & Natural)" to "21m00Tcm4TlvDq8ikWAM",
        "Domi (Emphatic & Clear)" to "AZnzlk1XvdvUeBnXmlld",
        "Bella (Expressive Female)" to "EXAVITQu4vr4xnSDxMaL",
        "Antoni (Energetic Male)" to "ErXwobaYiN019PkySvjV",
        "Adam (Deep Male Narrator)" to "pNInz6obpgDQGcFmaJgB"
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun testApiKey(apiKey: String): Pair<Boolean, Long> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) return@withContext Pair(false, -1L)

        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/user")
                .header("xi-api-key", cleanKey)
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
            Log.e(TAG, "ElevenLabs test connection failed", e)
            Pair(false, -1L)
        }
    }

    suspend fun synthesizeSpeech(
        apiKey: String,
        text: String,
        voiceId: String = "21m00Tcm4TlvDq8ikWAM"
    ): ByteArray? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || text.isBlank()) return@withContext null

        try {
            val root = JSONObject().apply {
                put("text", text)
                put("model_id", "eleven_multilingual_v2")
                val voiceSettings = JSONObject().apply {
                    put("stability", 0.5)
                    put("similarity_boost", 0.75)
                }
                put("voice_settings", voiceSettings)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = root.toString().toRequestBody(mediaType)

            val request = Request.Builder()
                .url("https://api.elevenlabs.io/v1/text-to-speech/$voiceId")
                .header("xi-api-key", cleanKey)
                .header("Accept", "audio/mpeg")
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.bytes()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs TTS error", e)
            null
        }
    }
}
