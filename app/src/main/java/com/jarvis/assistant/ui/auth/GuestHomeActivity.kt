package com.jarvis.assistant.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.databinding.ActivityGuestHomeBinding
import com.jarvis.assistant.ui.home.MainActivity

class GuestHomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnEnterGuestApp.setPreferenceAndStartMain()

        binding.btnUpgradeOrLogin.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun android.widget.Button.setPreferenceAndStartMain() {
        setOnClickListener {
            val intent = Intent(this@GuestHomeActivity, MainActivity::class.java).apply {
                putExtra("IS_GUEST", true)
            }
            startActivity(intent)
            finish()
        }
    }
}
