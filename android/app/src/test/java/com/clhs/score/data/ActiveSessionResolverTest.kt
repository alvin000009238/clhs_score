package com.clhs.score.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ActiveSessionResolverTest {
    @Test
    fun currentSessionPrefersUnlockedActiveSession() {
        val stored = AuthenticatedSession("stored", "stored-token", mapOf("a" to "1"))
        val active = AuthenticatedSession("active", "active-token", mapOf("b" to "2"))
        val resolver = ActiveSessionResolver(
            activeSessionProvider = { active },
            storedSessionProvider = { stored },
            biometricSessionPresentProvider = { true },
        )

        assertSame(active, resolver.currentSession())
    }

    @Test
    fun currentSessionFallsBackToStoredSessionWhenBiometricSessionIsAbsent() {
        val stored = AuthenticatedSession("stored", "stored-token", mapOf("a" to "1"))
        val resolver = ActiveSessionResolver(
            activeSessionProvider = { null },
            storedSessionProvider = { stored },
            biometricSessionPresentProvider = { false },
        )

        assertSame(stored, resolver.currentSession())
    }

    @Test
    fun currentSessionDoesNotUseStoredSessionWhenBiometricSessionIsPresent() {
        val stored = AuthenticatedSession("stored", "stored-token", mapOf("a" to "1"))
        val resolver = ActiveSessionResolver(
            activeSessionProvider = { null },
            storedSessionProvider = { stored },
            biometricSessionPresentProvider = { true },
        )

        assertNull(resolver.currentSession())
    }

    @Test
    fun requireSessionReportsNotLoggedInWhenNoAllowedSessionExists() {
        val resolver = ActiveSessionResolver(
            activeSessionProvider = { null },
            storedSessionProvider = { null },
            biometricSessionPresentProvider = { false },
        )

        val error = runCatching { resolver.requireSession() }.exceptionOrNull()

        assertEquals("未登入", error?.message)
    }
}
