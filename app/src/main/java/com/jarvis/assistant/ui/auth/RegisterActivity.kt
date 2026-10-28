package com.jarvis.assistant.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.jarvis.assistant.auth.AuthRepository
import com.jarvis.assistant.databinding.ActivityRegisterBinding
import com.jarvis.assistant.entitlement.FeatureAccessManager
import com.jarvis.assistant.ui.home.MainActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val authRepository = com.jarvis.assistant.JarvisApp.instance.authRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text?.toString()?.trim() ?: ""
            val email = binding.etEmail.text?.toString()?.trim() ?: ""
            val pass = binding.etPassword.text?.toString()?.trim() ?: ""
            val confirm = binding.etConfirmPassword.text?.toString()?.trim() ?: ""

            if (name.isBlank()) {
                binding.tilName.error = "Please enter your name"
                return@setOnClickListener
            } else {
                binding.tilName.error = null
            }

            if (email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.tilEmail.error = "Please enter a valid email"
                return@setOnClickListener
            } else {
                binding.tilEmail.error = null
            }

            if (pass.length < 6) {
                binding.tilPassword.error = "Password must be at least 6 characters"
                return@setOnClickListener
            } else {
                binding.tilPassword.error = null
            }

            if (pass != confirm) {
                binding.tilConfirmPassword.error = "Passwords do not match"
                return@setOnClickListener
            } else {
                binding.tilConfirmPassword.error = null
            }

            performRegistration(email, pass, name)
        }

        binding.tvLoginLink.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun performRegistration(email: String, pass: String, name: String) {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRegister.isEnabled = false

        lifecycleScope.launch {
            val result = authRepository.register(email, pass, name)
            binding.progressBar.visibility = View.GONE
            binding.btnRegister.isEnabled = true

            if (result.isSuccess) {
                Toast.makeText(this@RegisterActivity, "Account created successfully! Please verify your email.", Toast.LENGTH_LONG).show()
                val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                val err = result.exceptionOrNull()?.message ?: "Registration failed"
                Snackbar.make(binding.root, "Registration error: $err", Snackbar.LENGTH_LONG).show()
            }
        }
    }
}
