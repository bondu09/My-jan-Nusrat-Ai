package com.jarvis.assistant.ui.setup

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
import com.jarvis.assistant.databinding.ActivitySetupBinding
import com.jarvis.assistant.network.ElevenLabsClient
import com.jarvis.assistant.network.OpenAiClient
import com.jarvis.assistant.ui.home.MainActivity
import com.jarvis.assistant.ui.permission.PermissionActivity
import com.jarvis.assistant.util.ThemeManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private val preferences by lazy { JarvisApp.instance.preferences }
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.applyTheme(this)
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ThemeManager.applyDynamicBackground(binding.root)

        setupUI()
        loadSavedState()
        setupListeners()
    }

    private fun setupUI() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadSavedState() {
        binding.cbAgreement.isChecked = preferences.isAgreementAccepted

        // Gemini State
        val currentGeminiKey = preferences.apiKey
        if (currentGeminiKey.isNotBlank()) {
            binding.etApiKey.setText(currentGeminiKey)
            markGeminiStatusAsLive(pingMs = null)
        } else {
            markGeminiStatusAsPending()
        }

        // OpenAI State
        val currentOpenAiKey = preferences.openAiApiKey
        if (currentOpenAiKey.isNotBlank()) {
            binding.etOpenAiKey.setText(currentOpenAiKey)
            markOpenAiStatusAsConfigured(pingMs = null)
        } else {
            markOpenAiStatusAsOptional()
        }

        // ElevenLabs State
        val currentElevenLabsKey = preferences.elevenLabsApiKey
        if (currentElevenLabsKey.isNotBlank()) {
            binding.etElevenLabsKey.setText(currentElevenLabsKey)
            markElevenLabsStatusAsConfigured(pingMs = null)
        } else {
            markElevenLabsStatusAsOptional()
        }
    }

    private fun setupListeners() {
        // --- 1. Google Gemini Listeners ---
        binding.btnPasteKey.setOnClickListener {
            val text = getClipboardText()
            if (text.isNotBlank()) {
                binding.etApiKey.setText(text)
                Toast.makeText(this, "Gemini API Key পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
                testGeminiApiKey(text)
            } else {
                Toast.makeText(this, "ক্লিপবোর্ডে কোনো লেখা নেই", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearKey.setOnClickListener {
            binding.etApiKey.setText("")
            markGeminiStatusAsPending()
        }

        binding.btnGetApiKeyLink.setOnClickListener {
            openExternalUrl("https://aistudio.google.com/")
        }

        binding.btnTestKey.setOnClickListener {
            val key = binding.etApiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Snackbar.make(binding.root, "অনুগ্রহ করে প্রথমে Gemini API Key প্রবেশ করান", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            testGeminiApiKey(key)
        }

        // --- 2. OpenAI Listeners ---
        binding.btnPasteOpenAiKey.setOnClickListener {
            val text = getClipboardText()
            if (text.isNotBlank()) {
                binding.etOpenAiKey.setText(text)
                Toast.makeText(this, "OpenAI API Key পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
                testOpenAiKey(text)
            } else {
                Toast.makeText(this, "ক্লিপবোর্ডে কোনো লেখা নেই", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearOpenAiKey.setOnClickListener {
            binding.etOpenAiKey.setText("")
            markOpenAiStatusAsOptional()
        }

        binding.btnGetOpenAiLink.setOnClickListener {
            openExternalUrl("https://platform.openai.com/")
        }

        binding.btnTestOpenAiKey.setOnClickListener {
            val key = binding.etOpenAiKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Snackbar.make(binding.root, "অনুগ্রহ করে প্রথমে OpenAI API Key প্রবেশ করান", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            testOpenAiKey(key)
        }

        // --- 3. ElevenLabs Listeners ---
        binding.btnPasteElevenLabsKey.setOnClickListener {
            val text = getClipboardText()
            if (text.isNotBlank()) {
                binding.etElevenLabsKey.setText(text)
                Toast.makeText(this, "ElevenLabs API Key পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
                testElevenLabsKey(text)
            } else {
                Toast.makeText(this, "ক্লিপবোর্ডে কোনো লেখা নেই", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnClearElevenLabsKey.setOnClickListener {
            binding.etElevenLabsKey.setText("")
            markElevenLabsStatusAsOptional()
        }

        binding.btnGetElevenLabsLink.setOnClickListener {
            openExternalUrl("https://elevenlabs.io/")
        }

        binding.btnTestElevenLabsKey.setOnClickListener {
            val key = binding.etElevenLabsKey.text?.toString()?.trim() ?: ""
            if (key.isBlank()) {
                Snackbar.make(binding.root, "অনুগ্রহ করে প্রথমে ElevenLabs API Key প্রবেশ করান", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            testElevenLabsKey(key)
        }

        // --- Bottom Actions ---
        binding.btnNextPermission.setOnClickListener {
            proceedToPermissionPanel()
        }

        binding.btnSkipToHome.setOnClickListener {
            saveAllKeys()
            if (binding.cbAgreement.isChecked) {
                preferences.isAgreementAccepted = true
            }
            preferences.isSetupCompleted = true
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun getClipboardText(): String {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipboard.primaryClip
        return clip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
    }

    private fun openExternalUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(this, "লিংক ওপেন করা সম্ভব হয়নি", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Gemini Verification ---
    private fun testGeminiApiKey(apiKey: String) {
        binding.btnTestKey.isEnabled = false
        binding.tvLiveStatus.text = "Gemini যাচাই করা হচ্ছে..."
        binding.tvLiveStatus.setTextColor(Color.parseColor("#3B82F6"))
        binding.viewLiveDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))
        binding.tvPingSummary.text = "Google AI সার্ভারের সাথে হ্যান্ডশেক চলছে..."

        lifecycleScope.launch {
            val startTime = System.currentTimeMillis()
            val result = withContext(Dispatchers.IO) {
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                    val request = Request.Builder().url(url).build()
                    httpClient.newCall(request).execute().use { response ->
                        val duration = System.currentTimeMillis() - startTime
                        if (response.isSuccessful) {
                            Pair(true, duration)
                        } else {
                            Pair(false, response.code.toLong())
                        }
                    }
                } catch (_: Exception) {
                    Pair(false, -1L)
                }
            }

            binding.btnTestKey.isEnabled = true
            if (result.first) {
                val ping = result.second
                markGeminiStatusAsLive(ping)
                Snackbar.make(binding.root, "Gemini Key কার্যকর ও সক্রিয়! (Ping: ${ping}ms)", Snackbar.LENGTH_LONG).show()
            } else {
                markGeminiStatusAsFailed(result.second)
                Snackbar.make(binding.root, "Gemini Key সংযোগ ব্যর্থ। দয়া করে সঠিক কী দিন।", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun markGeminiStatusAsLive(pingMs: Long?) {
        binding.viewLiveDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
        binding.tvLiveStatus.text = "● LIVE (সক্রিয় ও সংযুক্ত)"
        binding.tvLiveStatus.setTextColor(Color.parseColor("#10B981"))
        binding.tvPingSummary.text = if (pingMs != null) {
            "পিংক: ${pingMs}ms • Gemini Live রেসপন্স সক্রিয় ✓"
        } else {
            "Gemini Live রেসপন্স সক্রিয় ও সংরক্ষিত ✓"
        }
    }

    private fun markGeminiStatusAsPending() {
        binding.viewLiveDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EAB308"))
        binding.tvLiveStatus.text = "অপেক্ষমান • Gemini Key দিন"
        binding.tvLiveStatus.setTextColor(Color.parseColor("#EAB308"))
        binding.tvPingSummary.text = "API Key যুক্ত করে টেস্ট করুন"
    }

    private fun markGeminiStatusAsFailed(code: Long) {
        binding.viewLiveDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        binding.tvLiveStatus.text = "ত্রুটি • অবৈধ Key বা নেটওয়ার্ক সমস্যা"
        binding.tvLiveStatus.setTextColor(Color.parseColor("#EF4444"))
        binding.tvPingSummary.text = if (code == -1L) "নেটওয়ার্ক টাইমআউট" else "HTTP $code Error"
    }

    // --- OpenAI Verification ---
    private fun testOpenAiKey(apiKey: String) {
        binding.btnTestOpenAiKey.isEnabled = false
        binding.tvOpenAiStatus.text = "OpenAI যাচাই চলছে..."
        binding.tvOpenAiStatus.setTextColor(Color.parseColor("#3B82F6"))
        binding.viewOpenAiDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))

        lifecycleScope.launch {
            val result = OpenAiClient.testApiKey(apiKey)
            binding.btnTestOpenAiKey.isEnabled = true
            if (result.first) {
                markOpenAiStatusAsConfigured(result.second)
                Snackbar.make(binding.root, "OpenAI Key সফলভাবে যাচাই হয়েছে! (Ping: ${result.second}ms)", Snackbar.LENGTH_LONG).show()
            } else {
                markOpenAiStatusAsFailed(result.second)
                Snackbar.make(binding.root, "OpenAI Key যাচাই ব্যর্থ হয়েছে। সঠিক কী নিশ্চিত করুন।", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun markOpenAiStatusAsConfigured(pingMs: Long?) {
        binding.viewOpenAiDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
        binding.tvOpenAiStatus.text = "● CONFIGURED (সংযুক্ত)"
        binding.tvOpenAiStatus.setTextColor(Color.parseColor("#10B981"))
        binding.tvOpenAiPingSummary.text = if (pingMs != null) {
            "পিংক: ${pingMs}ms • ChatGPT চ্যাট ও কোডিং সক্রিয় ✓"
        } else {
            "OpenAI ChatGPT সক্রিয় ও সংরক্ষিত ✓"
        }
    }

    private fun markOpenAiStatusAsOptional() {
        binding.viewOpenAiDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        binding.tvOpenAiStatus.text = "ঐচ্ছিক • OpenAI Key দিন (sk-...)"
        binding.tvOpenAiStatus.setTextColor(Color.parseColor("#94A3B8"))
        binding.tvOpenAiPingSummary.text = "চ্যাট ও কোডিং সহায়তার জন্য যুক্ত করতে পারেন"
    }

    private fun markOpenAiStatusAsFailed(code: Long) {
        binding.viewOpenAiDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        binding.tvOpenAiStatus.text = "ত্রুটি • অবৈধ OpenAI Key"
        binding.tvOpenAiStatus.setTextColor(Color.parseColor("#EF4444"))
        binding.tvOpenAiPingSummary.text = if (code == -1L) "নেটওয়ার্ক টাইমআউট" else "HTTP $code Error"
    }

    // --- ElevenLabs Verification ---
    private fun testElevenLabsKey(apiKey: String) {
        binding.btnTestElevenLabsKey.isEnabled = false
        binding.tvElevenLabsStatus.text = "ElevenLabs যাচাই চলছে..."
        binding.tvElevenLabsStatus.setTextColor(Color.parseColor("#3B82F6"))
        binding.viewElevenLabsDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))

        lifecycleScope.launch {
            val result = ElevenLabsClient.testApiKey(apiKey)
            binding.btnTestElevenLabsKey.isEnabled = true
            if (result.first) {
                markElevenLabsStatusAsConfigured(result.second)
                Snackbar.make(binding.root, "ElevenLabs Key সফলভাবে যাচাই হয়েছে! (Ping: ${result.second}ms)", Snackbar.LENGTH_LONG).show()
            } else {
                markElevenLabsStatusAsFailed(result.second)
                Snackbar.make(binding.root, "ElevenLabs Key যাচাই ব্যর্থ হয়েছে। সঠিক কী নিশ্চিত করুন।", Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun markElevenLabsStatusAsConfigured(pingMs: Long?) {
        binding.viewElevenLabsDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
        binding.tvElevenLabsStatus.text = "● CONFIGURED (সংযুক্ত)"
        binding.tvElevenLabsStatus.setTextColor(Color.parseColor("#10B981"))
        binding.tvElevenLabsPingSummary.text = if (pingMs != null) {
            "পিংক: ${pingMs}ms • মানুষের মতো প্রাকৃতিক ভয়েস সক্রিয় ✓"
        } else {
            "ElevenLabs আল্ট্রা-ন্যাচারাল ভয়েস সক্রিয় ✓"
        }
    }

    private fun markElevenLabsStatusAsOptional() {
        binding.viewElevenLabsDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
        binding.tvElevenLabsStatus.text = "ঐচ্ছিক • ElevenLabs API Key দিন"
        binding.tvElevenLabsStatus.setTextColor(Color.parseColor("#94A3B8"))
        binding.tvElevenLabsPingSummary.text = "মানুষের মতো প্রাকৃতিক কণ্ঠের জন্য যুক্ত করতে পারেন"
    }

    private fun markElevenLabsStatusAsFailed(code: Long) {
        binding.viewElevenLabsDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
        binding.tvElevenLabsStatus.text = "ত্রুটি • অবৈধ ElevenLabs Key"
        binding.tvElevenLabsStatus.setTextColor(Color.parseColor("#EF4444"))
        binding.tvElevenLabsPingSummary.text = if (code == -1L) "নেটওয়ার্ক টাইমআউট" else "HTTP $code Error"
    }

    private fun saveAllKeys() {
        val geminiKey = binding.etApiKey.text?.toString()?.trim() ?: ""
        if (geminiKey.isNotBlank()) {
            preferences.apiKey = geminiKey
        }
        val openAiKey = binding.etOpenAiKey.text?.toString()?.trim() ?: ""
        preferences.openAiApiKey = openAiKey

        val elevenLabsKey = binding.etElevenLabsKey.text?.toString()?.trim() ?: ""
        preferences.elevenLabsApiKey = elevenLabsKey
    }

    private fun proceedToPermissionPanel() {
        if (!binding.cbAgreement.isChecked) {
            Toast.makeText(this, "অনুগ্রহ করে প্রথমে শর্তাবলী ও নীতিমালায় টিক দিয়ে সম্মতি দিন।", Toast.LENGTH_LONG).show()
            binding.cbAgreement.requestFocus()
            return
        }

        val enteredKey = binding.etApiKey.text?.toString()?.trim() ?: ""
        if (enteredKey.isBlank() && preferences.apiKey.isBlank()) {
            Toast.makeText(this, "অনুগ্রহ করে আপনার Gemini API Key প্রদান করুন।", Toast.LENGTH_LONG).show()
            binding.etApiKey.requestFocus()
            return
        }

        saveAllKeys()
        preferences.isAgreementAccepted = true
        preferences.isSetupCompleted = true

        com.jarvis.assistant.background.JarvisConversationService.clearLiveError()
        com.jarvis.assistant.background.JarvisConversationService.restartSession()

        // Navigate to Permission Panel
        val intent = Intent(this, PermissionActivity::class.java).apply {
            putExtra("FROM_SETUP", true)
        }
        startActivity(intent)
        finish()
    }
}
