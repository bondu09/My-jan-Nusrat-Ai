package com.jarvis.assistant.entitlement

import android.content.Context
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.jarvis.assistant.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class EntitlementRepository(private val context: Context) {
    private val db: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        val firestore = if (dbId.isEmpty()) Firebase.firestore else Firebase.firestore(dbId)
        try {
            val settings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
            firestore.firestoreSettings = settings
        } catch (_: Exception) {}
        firestore
    }
    private val _currentEntitlement = MutableStateFlow(UserEntitlement())
    val currentEntitlement: StateFlow<UserEntitlement> = _currentEntitlement.asStateFlow()

    suspend fun fetchEntitlementFromBackend(userId: String, email: String): UserEntitlement {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val activeUid = userId.ifBlank { user?.uid ?: "" }
        if (activeUid.isBlank()) return UserEntitlement()

        return try {
            val docRef = db.collection("users").document(activeUid)
            var snapshot = docRef.get().await()
            
            if (!snapshot.exists() && user != null) {
                // Auto-create basic profile if it doesn't exist
                val userData = mapOf(
                    "uid" to user.uid,
                    "email" to (user.email ?: email),
                    "displayName" to (user.displayName ?: "User"),
                    "subscriptionTier" to "basic",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                try {
                    docRef.set(userData).await()
                    snapshot = docRef.get().await()
                } catch (e: Exception) {
                    android.util.Log.e("EntitlementRepo", "Failed to create user profile", e)
                }
            }

            if (snapshot.exists()) {
                val tier = snapshot.getString("subscriptionTier") ?: "basic"
                val expiresAt = snapshot.getTimestamp("expiresAt")?.toDate()?.time ?: 0L
                
                val entitlement = UserEntitlement(
                    userId = activeUid,
                    email = snapshot.getString("email") ?: email,
                    subscriptionState = if (tier != "basic") SubscriptionState.ACTIVE else SubscriptionState.FREE,
                    planName = "NUSRAT AI ${tier.replaceFirstChar { it.uppercase() }} Plan",
                    expiryTimestamp = expiresAt,
                    entitledFeatures = getFeaturesForTier(tier),
                    activationSource = snapshot.getString("licenseKey")?.let { "license_key" } ?: "default",
                    isVerified = true
                )
                _currentEntitlement.value = entitlement
                entitlement
            } else {
                val fallback = UserEntitlement(userId = activeUid, email = email)
                _currentEntitlement.value = fallback
                fallback
            }
        } catch (e: Exception) {
            val error = UserEntitlement(errorMessage = e.message)
            _currentEntitlement.value = error
            error
        }
    }

    private fun getFeaturesForTier(tier: String): List<String> {
        return when (tier) {
            "master" -> listOf("chat", "settings", "offline_basic", "voice_neural", "unlimited_agents", "priority_cloud", "screen_automation", "premium_updates")
            "pro" -> listOf("chat", "settings", "offline_basic", "voice_neural", "priority_cloud")
            else -> listOf("chat", "settings", "offline_basic")
        }
    }

    suspend fun activateLicenseKey(licenseKey: String, userId: String): Result<UserEntitlement> {
        val trimmed = licenseKey.trim()
        val activeUid = userId.ifBlank { FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure(Exception("User not logged in")) }

        return try {
            val licenseDoc = db.collection("licenses").document(trimmed).get().await()
            if (!licenseDoc.exists()) {
                return Result.failure(Exception("License key not found."))
            }
            
            val isUsed = licenseDoc.getBoolean("isUsed") ?: false
            if (isUsed) {
                return Result.failure(Exception("License key already used."))
            }

            val tier = licenseDoc.getString("tier") ?: "pro"
            
            val batch = db.batch()
            batch.update(db.collection("licenses").document(trimmed), mapOf(
                "isUsed" to true,
                "usedBy" to activeUid,
                "usedAt" to FieldValue.serverTimestamp()
            ))
            
            val durationDays = if (tier == "master") 365L else 30L
            val expiresAt = Date(System.currentTimeMillis() + durationDays * 24 * 60 * 60 * 1000)
            
            batch.update(db.collection("users").document(activeUid), mapOf(
                "subscriptionTier" to tier,
                "licenseKey" to trimmed,
                "licenseActivatedAt" to FieldValue.serverTimestamp(),
                "expiresAt" to expiresAt,
                "updatedAt" to FieldValue.serverTimestamp()
            ))
            
            batch.commit().await()
            
            val updatedEntitlement = fetchEntitlementFromBackend(activeUid, "")
            Result.success(updatedEntitlement)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun clearEntitlement() {
        _currentEntitlement.value = UserEntitlement()
    }

    suspend fun fetchPlans(): List<Map<String, Any>> {
        return try {
            val snapshot = db.collection("plans").get().await()
            snapshot.documents.map { it.data ?: emptyMap() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

