package com.jarvis.assistant.network

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.vertexai.vertexAI
import com.google.firebase.vertexai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val DEFAULT_MODEL = "gemini-1.5-flash"

    private val model by lazy {
        Firebase.vertexAI.generativeModel(DEFAULT_MODEL)
    }

    suspend fun getChatResponse(prompt: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = withTimeout(60_000L) {
                model.generateContent(prompt)
            }
            response.text?.trim()
        } catch (e: Exception) {
            Log.e(TAG, "Gemini generation error via Vertex AI", e)
            null
        }
    }
}


