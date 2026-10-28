package com.jarvis.assistant.entitlement

object FeatureAccessManager {
    private val repository: EntitlementRepository
        get() = com.jarvis.assistant.JarvisApp.instance.entitlementRepository

    private val authRepository: com.jarvis.assistant.auth.AuthRepository
        get() = com.jarvis.assistant.JarvisApp.instance.authRepository

    suspend fun refreshEntitlement(userId: String = "", email: String = ""): UserEntitlement {
        return repository.fetchEntitlementFromBackend(userId, email)
    }

    fun getCurrentEntitlement(): UserEntitlement {
        return repository.currentEntitlement.value
    }

    fun hasFeatureAccess(featureId: String): Boolean {
        val entitlement = getCurrentEntitlement()
        if (entitlement.subscriptionState == SubscriptionState.ACTIVE && entitlement.isActiveSubscriber) {
            return true
        }
        return featureId in entitlement.entitledFeatures
    }

    suspend fun activateLicense(key: String, userId: String): Result<UserEntitlement> {
        return repository.activateLicenseKey(key, userId)
    }

    fun clear() {
        repository.clearEntitlement()
    }
}
