package com.jarvis.assistant.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.jarvis.assistant.databinding.ActivitySubscriptionStatusBinding
import com.jarvis.assistant.entitlement.FeatureAccessManager
import com.jarvis.assistant.entitlement.SubscriptionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionStatusActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySubscriptionStatusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySubscriptionStatusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        loadEntitlement()

        binding.btnRefresh.setOnClickListener {
            refreshEntitlement()
        }

        binding.btnManageWebsite.setOnClickListener {
            val url = "https://nusrat.ai/account/billing"
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            } catch (_: Exception) {
                Toast.makeText(this, "Unable to open link", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadEntitlement() {
        val entitlement = FeatureAccessManager.getCurrentEntitlement()
        binding.tvPlanName.text = entitlement.planName
        binding.tvStatusBadge.text = entitlement.subscriptionState.name
        
        when (entitlement.subscriptionState) {
            SubscriptionState.ACTIVE -> {
                binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#16834A"))
                binding.tvStatusBadge.setBackgroundResource(com.jarvis.assistant.R.drawable.bg_jarvis_status_badge_green)
            }
            SubscriptionState.EXPIRED, SubscriptionState.REVOKED -> {
                binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#C62828"))
                binding.tvStatusBadge.setBackgroundResource(com.jarvis.assistant.R.drawable.bg_jarvis_error_card)
            }
            else -> {
                binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#FF7A00"))
                binding.tvStatusBadge.setBackgroundResource(com.jarvis.assistant.R.drawable.bg_jarvis_status_badge)
            }
        }

        if (entitlement.expiryTimestamp > 0) {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvExpiry.text = "Expires on: ${sdf.format(Date(entitlement.expiryTimestamp))}"
        } else {
            binding.tvExpiry.text = "Expires on: N/A (Lifetime or Free)"
        }

        binding.tvSource.text = "Source: ${entitlement.activationSource.replace("_", " ").uppercase()}"
        
        binding.tvFeaturesList.text = entitlement.entitledFeatures.joinToString("\n") { "• ${it.replace("_", " ").capitalize()}" }
        
        if (entitlement.subscriptionState != SubscriptionState.ACTIVE) {
            loadPlans()
        }
    }

    private fun loadPlans() {
        lifecycleScope.launch {
            val plans = com.jarvis.assistant.entitlement.EntitlementRepository(this@SubscriptionStatusActivity).fetchPlans()
            if (plans.isNotEmpty()) {
                binding.tvPlansTitle.visibility = View.VISIBLE
                binding.layoutPlans.removeAllViews()
                plans.forEach { plan ->
                    val textView = android.widget.TextView(this@SubscriptionStatusActivity).apply {
                        text = "• ${plan["name"]} - ${plan["price"]}"
                        textSize = 14f
                        setPadding(0, 8, 0, 8)
                    }
                    binding.layoutPlans.addView(textView)
                }
            }
        }
    }

    private fun refreshEntitlement() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRefresh.isEnabled = false
        
        lifecycleScope.launch {
            FeatureAccessManager.refreshEntitlement()
            binding.progressBar.visibility = View.GONE
            binding.btnRefresh.isEnabled = true
            loadEntitlement()
            Toast.makeText(this@SubscriptionStatusActivity, "Subscription status updated", Toast.LENGTH_SHORT).show()
        }
    }
}
