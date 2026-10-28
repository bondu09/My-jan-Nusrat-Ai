package com.jarvis.assistant.entitlement

import org.junit.Assert.*
import org.junit.Test

class EntitlementTest {

    @Test
    fun testDefaultFreeEntitlement() {
        val entitlement = UserEntitlement(
            userId = "test_user_1",
            email = "user@nusrat.ai",
            subscriptionState = SubscriptionState.FREE,
            planName = "Free Tier",
            expiryTimestamp = 0L,
            entitledFeatures = listOf("chat", "settings")
        )

        assertFalse(entitlement.isActiveSubscriber)
        assertEquals(AccessCategory.AUTHENTICATED_FREE, entitlement.accessCategory)
        assertTrue("chat" in entitlement.entitledFeatures)
        assertFalse("voice_neural" in entitlement.entitledFeatures)
    }

    @Test
    fun testActiveSubscriberEntitlement() {
        val activeExpiry = System.currentTimeMillis() + 86400000L // 1 day in future
        val entitlement = UserEntitlement(
            userId = "test_user_pro",
            email = "pro@nusrat.ai",
            subscriptionState = SubscriptionState.ACTIVE,
            planName = "Master Pro Plan",
            expiryTimestamp = activeExpiry,
            entitledFeatures = listOf("chat", "settings", "voice_neural", "unlimited_agents")
        )

        assertTrue(entitlement.isActiveSubscriber)
        assertEquals(AccessCategory.ACTIVE_SUBSCRIBER, entitlement.accessCategory)
        assertTrue("voice_neural" in entitlement.entitledFeatures)
    }

    @Test
    fun testGuestEntitlement() {
        val entitlement = UserEntitlement(
            userId = "",
            email = "",
            subscriptionState = SubscriptionState.FREE,
            planName = "Guest",
            expiryTimestamp = 0L,
            entitledFeatures = listOf("chat", "settings")
        )

        assertEquals(AccessCategory.GUEST, entitlement.accessCategory)
    }

    @Test
    fun testRevokedEntitlement() {
        val entitlement = UserEntitlement(
            userId = "test_user_revoked",
            email = "revoked@nusrat.ai",
            subscriptionState = SubscriptionState.REVOKED,
            planName = "Suspended",
            expiryTimestamp = 0L,
            entitledFeatures = listOf("chat")
        )

        assertFalse(entitlement.isActiveSubscriber)
        assertEquals(AccessCategory.RESTRICTED, entitlement.accessCategory)
    }
}
