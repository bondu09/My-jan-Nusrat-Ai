package com.jarvis.assistant.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.auth.AuthRepository
import com.jarvis.assistant.databinding.ActivityLoginBinding
import com.jarvis.assistant.entitlement.FeatureAccessManager
import com.jarvis.assistant.ui.home.MainActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val authRepository = com.jarvis.assistant.JarvisApp.instance.authRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""

            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (password.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            performLogin(email, password)
        }

        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvRegisterLink.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }
    }

    private fun performLogin(email: String, pass: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.login(email, pass)
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true

            if (result.isSuccess) {
                val user = result.getOrNull()
                if (user != null && !user.isEmailVerified) {
                    Snackbar.make(binding.root, "Please verify your email address before signing in.", Snackbar.LENGTH_LONG)
                        .setAction("Resend") {
                            user.sendEmailVerification()
                            Toast.makeText(this@LoginActivity, "Verification email sent.", Toast.LENGTH_SHORT).show()
                        }.show()
                    authRepository.logout()
                    return@launch
                }

                // Sync subscription entitlement from backend
                FeatureAccessManager.refreshEntitlement(user?.uid ?: "", user?.email ?: email)
                Toast.makeText(this@LoginActivity, "Login successful!", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Login failed"
                Snackbar.make(binding.root, "Authentication error: $err", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
