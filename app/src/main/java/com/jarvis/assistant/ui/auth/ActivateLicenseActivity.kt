package com.jarvis.assistant.ui.auth

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.auth.AuthRepository
import com.jarvis.assistant.databinding.ActivityActivateLicenseBinding
import com.jarvis.assistant.entitlement.FeatureAccessManager
import com.jarvis.assistant.ui.home.MainActivity
import kotlinx.coroutines.launch

class ActivateLicenseActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivateLicenseBinding
    private val authRepository = com.jarvis.assistant.JarvisApp.instance.authRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityActivateLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnPaste.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val pasteText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()?.trim() ?: ""
            if (pasteText.isNotBlank()) {
                binding.etLicenseKey.setText(pasteText)
                Toast.makeText(this, "License key pasted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Clipboard is empty", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnActivate.setOnClickListener {
            val key = binding.etLicenseKey.text?.toString()?.trim() ?: ""
            if (key.length < 6) {
                binding.tilLicense.error = "Please enter a valid license key"
                return@setOnClickListener
            }
            binding.tilLicense.error = null
            performActivation(key)
        }

        binding.btnContinueToApp.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun performActivation(key: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnActivate.isEnabled = false

        lifecycleScope.launch {
            val userId = authRepository.currentUser?.uid ?: ""
            val result = FeatureAccessManager.activateLicense(key, userId)
            binding.progressBar.visibility = View.GONE
            binding.btnActivate.isEnabled = true

            if (result.isSuccess) {
                val entitlement = result.getOrNull()
                binding.cardResult.visibility = View.VISIBLE
                binding.tvResultDetails.text = "Plan: ${entitlement?.planName}\nStatus: ACTIVE\nAll features successfully unlocked."
                Toast.makeText(this@ActivateLicenseActivity, "License activated successfully!", Toast.LENGTH_LONG).show()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Activation failed"
                Snackbar.make(binding.root, "License error: $err", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
