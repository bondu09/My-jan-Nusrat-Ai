package com.jarvis.assistant.ui.settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.JarvisApp
import com.jarvis.assistant.data.model.GeminiConstants
import com.jarvis.assistant.databinding.ActivityApiConnectionBinding
import com.jarvis.assistant.network.ElevenLabsClient
import com.jarvis.assistant.network.OpenAiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ApiConnectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApiConnectionBinding
    private val preferences by lazy { JarvisApp.instance.preferences }
    private val testHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        com.jarvis.assistant.util.ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivityApiConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        com.jarvis.assistant.util.ThemeManager.applyDynamicBackground(binding.root)

        setupUI()
        loadCurrentPreferences()
        setupListeners()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentPreferences() {
        // 1. Gemini
        binding.etApiKey.setText(preferences.apiKey)
        binding.etCustomEndpoint.setText(preferences.customEndpoint)

        when (preferences.aiModel) {
            GeminiConstants.MODEL_3_8_LIVE -> binding.rbModelFlash20.isChecked = true
            GeminiConstants.MODEL_3_1_FLASH_AGENT -> binding.rbModelFlash15.isChecked = true
            else -> binding.rbModelFlashThinking.isChecked = true
        }

        if (preferences.apiKey.isNotBlank()) {
            binding.tvLiveStatus.text = "GEMINI CONFIGURED • READY"
            binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
        } else {
            binding.tvLiveStatus.text = "NO API KEY • ENTER KEY BELOW"
            binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EAB308"))
        }

        if (preferences.customEndpoint.isNotBlank()) {
            binding.tvEndpointSummary.text = "Active Endpoint: ${preferences.customEndpoint}"
        }

        // 2. OpenAI
        binding.etOpenAiApiKey.setText(preferences.openAiApiKey)
        if (preferences.openAiModel == "gpt-4o") {
            binding.rbOpenAiGpt4o.isChecked = true
        } else {
            binding.rbOpenAiGpt4oMini.isChecked = true
        }

        if (preferences.openAiApiKey.isNotBlank()) {
            binding.tvOpenAiLiveStatus.text = "OPENAI CONFIGURED • READY"
            binding.viewOpenAiStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
        } else {
            binding.tvOpenAiLiveStatus.text = "OPTIONAL • KEY NOT SET"
            binding.viewOpenAiStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        }

        // 3. ElevenLabs
        binding.etElevenLabsApiKey.setText(preferences.elevenLabsApiKey)
        binding.etElevenLabsVoiceId.setText(preferences.elevenLabsVoiceId)

        if (preferences.elevenLabsApiKey.isNotBlank()) {
            binding.tvElevenLabsLiveStatus.text = "ELEVENLABS CONFIGURED • READY"
            binding.viewElevenLabsStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
        } else {
            binding.tvElevenLabsLiveStatus.text = "OPTIONAL • KEY NOT SET"
            binding.viewElevenLabsStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        }

        // 4. Active Engines
        if (preferences.activeAiEngine == "OPENAI") {
            binding.rbEngineOpenAi.isChecked = true
        } else {
            binding.rbEngineGemini.isChecked = true
        }

        if (preferences.activeTtsEngine == "ELEVENLABS") {
            binding.rbTtsElevenLabs.isChecked = true
        } else {
            binding.rbTtsGeminiLive.isChecked = true
        }
    }

    private fun setupListeners() {
        // --- Direct External Links ---
        binding.btnOpenGeminiStudioLink.setOnClickListener {
            openExternalUrl("https://aistudio.google.com/")
        }
        binding.btnOpenOpenAiPlatformLink.setOnClickListener {
            openExternalUrl("https://platform.openai.com/")
        }
        binding.btnOpenElevenLabsLink.setOnClickListener {
            openExternalUrl("https://elevenlabs.io/")
        }

        // --- Gemini Actions ---
        binding.btnPasteKey.setOnClickListener {
            val pasted = getClipboardText()
            if (pasted.isNotBlank()) {
                binding.etApiKey.setText(pasted)
                Toast.makeText(this, "Gemini API Key pasted from clipboard", Toast.LENGTH_SHORT).show()
                testGeminiConnection(pasted)
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearKey.setOnClickListener {
            binding.etApiKey.setText("")
            binding.tvLiveStatus.text = "NO API KEY • ENTER KEY BELOW"
            binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EAB308"))
        }

        binding.btnTestConnection.setOnClickListener {
            val key = binding.etApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Snackbar.make(binding.root, "Please enter a Gemini API key first", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            testGeminiConnection(key)
        }

        // --- OpenAI Actions ---
        binding.btnPasteOpenAiKey.setOnClickListener {
            val pasted = getClipboardText()
            if (pasted.isNotBlank()) {
                binding.etOpenAiApiKey.setText(pasted)
                Toast.makeText(this, "OpenAI API Key pasted from clipboard", Toast.LENGTH_SHORT).show()
                testOpenAiConnection(pasted)
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearOpenAiKey.setOnClickListener {
            binding.etOpenAiApiKey.setText("")
            binding.tvOpenAiLiveStatus.text = "OPTIONAL • KEY NOT SET"
            binding.viewOpenAiStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        }

        binding.btnTestOpenAiConnection.setOnClickListener {
            val key = binding.etOpenAiApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Snackbar.make(binding.root, "Please enter an OpenAI API key first", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            testOpenAiConnection(key)
        }

        // --- ElevenLabs Actions ---
        binding.btnPasteElevenLabsKey.setOnClickListener {
            val pasted = getClipboardText()
            if (pasted.isNotBlank()) {
                binding.etElevenLabsApiKey.setText(pasted)
                Toast.makeText(this, "ElevenLabs API Key pasted from clipboard", Toast.LENGTH_SHORT).show()
                testElevenLabsConnection(pasted)
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearElevenLabsKey.setOnClickListener {
            binding.etElevenLabsApiKey.setText("")
            binding.tvElevenLabsLiveStatus.text = "OPTIONAL • KEY NOT SET"
            binding.viewElevenLabsStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        }

        binding.btnTestElevenLabsConnection.setOnClickListener {
            val key = binding.etElevenLabsApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Snackbar.make(binding.root, "Please enter an ElevenLabs API key first", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            testElevenLabsConnection(key)
        }

        // --- Save and Apply ---
        binding.btnSaveConnection.setOnClickListener {
            saveAndApply()
        }
    }

    private fun getClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val item = clipboard.primaryClip?.getItemAt(0)
        return item?.text?.toString()?.trim() ?: ""
    }

    private fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testGeminiConnection(apiKey: String) {
        binding.btnTestConnection.isEnabled = false
        binding.tvPingResult.text = "Testing ping..."
        binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                    val request = Request.Builder().url(url).build()
                    testHttpClient.newCall(request).execute().use { response ->
                        val duration = System.currentTimeMillis() - startTime
                        if (response.isSuccessful) {
                            Pair(true, duration)
                        } else {
                            Pair(false, response.code.toLong())
                        }
                    }
                } catch (e: Exception) {
                    Pair(false, -1L)
                }
            }

            binding.btnTestConnection.isEnabled = true
            if (result.first) {
                val ping = result.second
                binding.tvPingResult.text = "Ping: ${ping}ms • Valid"
                binding.tvLiveStatus.text = "CONNECTED • HANDSHAKE OK"
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
                Snackbar.make(binding.root, "Gemini connection verified! Ping: ${ping}ms", Snackbar.LENGTH_LONG).show()
            } else {
                val code = result.second
                binding.tvPingResult.text = if (code == -1L) "Network Timeout" else "HTTP $code Error"
                binding.tvLiveStatus.text = "FAILED • INVALID KEY OR NETWORK"
                binding.viewStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                Snackbar.make(binding.root, "Gemini connection failed. Please verify API key.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun testOpenAiConnection(apiKey: String) {
        binding.btnTestOpenAiConnection.isEnabled = false
        binding.tvOpenAiPingResult.text = "Testing ping..."
        binding.viewOpenAiStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))

        lifecycleScope.launch {
            val result = OpenAiClient.testApiKey(apiKey)
            binding.btnTestOpenAiConnection.isEnabled = true
            if (result.first) {
                val ping = result.second
                binding.tvOpenAiPingResult.text = "Ping: ${ping}ms • Valid"
                binding.tvOpenAiLiveStatus.text = "CONNECTED • HANDSHAKE OK"
                binding.viewOpenAiStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
                Snackbar.make(binding.root, "OpenAI connection verified! Ping: ${ping}ms", Snackbar.LENGTH_LONG).show()
            } else {
                val code = result.second
                binding.tvOpenAiPingResult.text = if (code == -1L) "Network Timeout" else "HTTP $code Error"
                binding.tvOpenAiLiveStatus.text = "FAILED • INVALID KEY OR NETWORK"
                binding.viewOpenAiStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                Snackbar.make(binding.root, "OpenAI connection failed. Please verify API key.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun testElevenLabsConnection(apiKey: String) {
        binding.btnTestElevenLabsConnection.isEnabled = false
        binding.tvElevenLabsPingResult.text = "Testing ping..."
        binding.viewElevenLabsStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))

        lifecycleScope.launch {
            val result = ElevenLabsClient.testApiKey(apiKey)
            binding.btnTestElevenLabsConnection.isEnabled = true
            if (result.first) {
                val ping = result.second
                binding.tvElevenLabsPingResult.text = "Ping: ${ping}ms • Valid"
                binding.tvElevenLabsLiveStatus.text = "CONNECTED • HANDSHAKE OK"
                binding.viewElevenLabsStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
                Snackbar.make(binding.root, "ElevenLabs connection verified! Ping: ${ping}ms", Snackbar.LENGTH_LONG).show()
            } else {
                val code = result.second
                binding.tvElevenLabsPingResult.text = if (code == -1L) "Network Timeout" else "HTTP $code Error"
                binding.tvElevenLabsLiveStatus.text = "FAILED • INVALID KEY OR NETWORK"
                binding.viewElevenLabsStatusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                Snackbar.make(binding.root, "ElevenLabs connection failed. Please verify API key.", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun saveAndApply() {
        val geminiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
        val customEp = binding.etCustomEndpoint.text?.toString()?.trim() ?: ""
        val selectedModel = when (binding.rgModelGroup.checkedRadioButtonId) {
            binding.rbModelFlash20.id -> GeminiConstants.MODEL_3_8_LIVE
            binding.rbModelFlash15.id -> GeminiConstants.MODEL_3_1_FLASH_AGENT
            else -> GeminiConstants.MODEL_3_1_FLASH
        }

        val openAiKey = binding.etOpenAiApiKey.text?.toString()?.trim() ?: ""
        val openAiModel = if (binding.rbOpenAiGpt4o.isChecked) "gpt-4o" else "gpt-4o-mini"

        val elevenLabsKey = binding.etElevenLabsApiKey.text?.toString()?.trim() ?: ""
        val elevenLabsVoiceId = binding.etElevenLabsVoiceId.text?.toString()?.trim()
            ?.ifBlank { "21m00Tcm4TlvDq8ikWAM" } ?: "21m00Tcm4TlvDq8ikWAM"

        val activeAi = if (binding.rbEngineOpenAi.isChecked) "OPENAI" else "GEMINI"
        val activeTts = if (binding.rbTtsElevenLabs.isChecked) "ELEVENLABS" else "GEMINI_LIVE"

        preferences.apiKey = geminiKey
        preferences.aiModel = selectedModel
        preferences.customEndpoint = customEp

        preferences.openAiApiKey = openAiKey
        preferences.openAiModel = openAiModel

        preferences.elevenLabsApiKey = elevenLabsKey
        preferences.elevenLabsVoiceId = elevenLabsVoiceId

        preferences.activeAiEngine = activeAi
        preferences.activeTtsEngine = activeTts

        com.jarvis.assistant.background.JarvisConversationService.clearLiveError()
        com.jarvis.assistant.background.JarvisConversationService.restartSession()

        Toast.makeText(this, "AI APIs ও মডেল কনফিগারেশন সফলভাবে সংরক্ষিত হয়েছে!", Toast.LENGTH_SHORT).show()
        finish()
    }
}
