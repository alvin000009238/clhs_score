package com.clhs.score.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore

@RunWith(AndroidJUnit4::class)
class SessionKeystoreInstrumentedTest {
    @Test
    fun androidKeystoreKeyEncryptsAndDecryptsSessionPayload() = runBlocking {
        val aliasPrefix = "clhs_session_test_${System.nanoTime()}_v"
        try {
            val cipher = AesGcmSessionCipher(AndroidKeystoreSessionKeyProvider(aliasPrefix))
            val plaintext = "instrumented session".encodeToByteArray()
            val aad = "app/session/general/v1".encodeToByteArray()

            assertArrayEquals(plaintext, cipher.decrypt(cipher.encrypt(plaintext, aad), aad))
        } finally {
            KeyStore.getInstance("AndroidKeyStore").apply {
                load(null)
                deleteEntry("${aliasPrefix}1")
            }
        }
    }

    @Test
    fun biometricClearTombstonePreventsLegacyReimport() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("score_biometric_session", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        val legacy = StickyBiometricLegacySource()
        try {
            val storage = SharedPreferencesBiometricSessionStorage(context, legacy) {}

            assertTrue(storage.hasSession())
            storage.clear()

            assertFalse(SharedPreferencesBiometricSessionStorage(context, legacy) {}.hasSession())
        } finally {
            prefs.edit().clear().commit()
        }
    }

    private class StickyBiometricLegacySource : LegacySessionSource {
        private val record = BiometricSessionRecord("cipher", "iv", "salt", "pin", "pin-iv")

        override fun readGeneral(): AuthenticatedSession? = null
        override fun readReminder(): LegacyReminderSession? = null
        override fun readBiometric(): BiometricSessionRecord = record
        override fun clearGeneral() = Unit
        override fun clearReminder() = Unit
        override fun clearBiometric() = Unit
        override fun clearAll() = Unit
    }
}
