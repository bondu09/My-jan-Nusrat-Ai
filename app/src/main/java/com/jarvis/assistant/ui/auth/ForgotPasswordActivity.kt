package com.jarvis.assistant.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.auth.AuthRepository
import com.jarvis.assistant.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val authRepository = com.jarvis.assistant.JarvisApp.instance.authRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnReset.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }
            binding.tilEmail.error = null
            sendReset(email)
        }
    }

    private fun sendReset(email: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnReset.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.sendPasswordReset(email)
            binding.progressBar.visibility = View.GONE
            binding.btnReset.isEnabled = true

            if (result.isSuccess) {
                Toast.makeText(this@ForgotPasswordActivity, "Password reset email sent!", Toast.LENGTH_LONG).show()
                finish()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Failed to send reset email"
                Snackbar.make(binding.root, "Error: $err", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
