package com.jarvis.assistant.network

import com.google.gson.Gson
import com.jarvis.assistant.entitlement.SubscriptionState
import com.jarvis.assistant.entitlement.UserEntitlement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class LicenseActivateRequest(
    val licenseKey: String,
    val deviceId: String
)

data class LicenseActivateResponse(
    val success: Boolean,
    val message: String,
    val entitlement: UserEntitlement?
)

object NusratApiService {
    private const val BASE_URL = "https://api.nusrat.ai/v1"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()
    private val gson = Gson()

    suspend fun fetchEntitlement(authHeader: String): UserEntitlement? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/me/entitlement")
                .header("Authorization", authHeader)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = response.body?.string() ?: return@withContext null
                gson.fromJson(json, UserEntitlement::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun activateLicense(authHeader: String, licenseKey: String, deviceId: String): LicenseActivateResponse? = withContext(Dispatchers.IO) {
        try {
            val payload = gson.toJson(LicenseActivateRequest(licenseKey, deviceId))
            val body = payload.toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL/api/licenses/activate")
                .header("Authorization", authHeader)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                val json = response.body?.string() ?: return@withContext null
                gson.fromJson(json, LicenseActivateResponse::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun refreshEntitlement(authHeader: String): UserEntitlement? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL/api/entitlement/refresh")
                .header("Authorization", authHeader)
                .post("{}".toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val json = response.body?.string() ?: return@withContext null
                gson.fromJson(json, UserEntitlement::class.java)
            }
        } catch (_: Exception) {
            null
        }
    }
}
