package com.jarvis.assistant.entitlement

enum class SubscriptionState {
    UNKNOWN,
    FREE,
    ACTIVE,
    EXPIRED,
    REVOKED,
    PENDING,
    ERROR
}

enum class AccessCategory {
    GUEST,
    AUTHENTICATED_FREE,
    ACTIVE_SUBSCRIBER,
    RESTRICTED
}

data class UserEntitlement(
    val userId: String = "",
    val email: String = "",
    val subscriptionState: SubscriptionState = SubscriptionState.FREE,
    val planName: String = "Free Tier",
    val expiryTimestamp: Long = 0L,
    val entitledFeatures: List<String> = listOf("chat", "settings", "offline_basic"),
    val activationSource: String = "default",
    val isVerified: Boolean = false,
    val errorMessage: String? = null
) {
    val isActiveSubscriber: Boolean
        get() = subscriptionState == SubscriptionState.ACTIVE && (expiryTimestamp == 0L || expiryTimestamp > System.currentTimeMillis())

    val accessCategory: AccessCategory
        get() = when {
            userId.isBlank() -> AccessCategory.GUEST
            isActiveSubscriber -> AccessCategory.ACTIVE_SUBSCRIBER
            subscriptionState == SubscriptionState.REVOKED || subscriptionState == SubscriptionState.ERROR -> AccessCategory.RESTRICTED
            else -> AccessCategory.AUTHENTICATED_FREE
        }
}
