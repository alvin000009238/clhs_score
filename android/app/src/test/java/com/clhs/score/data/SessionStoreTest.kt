package com.clhs.score.data

import android.content.Context
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Before
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import javax.crypto.Cipher
import javax.crypto.KeyGenerator

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], shadows = [ShadowMasterKey::class, ShadowEncryptedSharedPreferences::class, ShadowBiometricHelper::class])
class SessionStoreTest {

    @Before
    fun setup() {
        System.setProperty("javax.net.ssl.trustStoreType", "JKS")
    }

    @Test
    fun testSaveAndLoadSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)

        val session = AuthenticatedSession("123", "token", mapOf("cookie1" to "val1"))
        store.saveSession(session)

        val loaded = store.loadSession()
        assertEquals(session, loaded)
    }

    @Test
    fun testClearSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)

        val session = AuthenticatedSession("123", "token", mapOf("cookie1" to "val1"))
        store.saveSession(session)

        store.clearNormalSession()
        assertNull(store.loadSession())
    }

    @Test
    fun testReminderSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)

        val session = AuthenticatedSession("123", "token", mapOf("cookie1" to "val1"))
        store.saveReminderSession(session, 1000)

        val loaded = store.loadReminderSession(500)
        assertEquals(session, loaded)

        val expired = store.loadReminderSession(2000)
        assertNull(expired)
        assertNull(store.loadReminderSession(500))
    }

    @Test
    fun testClearAll() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)

        val session = AuthenticatedSession("123", "token", mapOf("cookie1" to "val1"))
        store.saveSession(session)
        store.saveReminderSession(session, 1000)

        store.clear()

        assertNull(store.loadSession())
        assertNull(store.loadReminderSession(500))
    }

    @Test
    fun testBiometricSession() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionStore(context)

        val session = AuthenticatedSession("123", "token", mapOf("cookie1" to "val1"))
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val key = keyGen.generateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)

        store.saveBiometricSession(session, "1234", cipher)

        assertTrue(store.hasBiometricSession())
        assertNotNull(store.getBiometricIv())

        val cipherDecrypt = Cipher.getInstance("AES/GCM/NoPadding")
        cipherDecrypt.init(Cipher.DECRYPT_MODE, key, cipher.parameters)

        val loaded = store.loadBiometricSession(cipherDecrypt)
        assertEquals(session, loaded)

        store.clearBiometricSession()
        assertFalse(store.hasBiometricSession())
    }
}

@Implements(androidx.security.crypto.MasterKey.Builder::class)
class ShadowMasterKey {
    @Implementation
    fun build(): androidx.security.crypto.MasterKey {
        return org.mockito.Mockito.mock(androidx.security.crypto.MasterKey::class.java)
    }
}

@Implements(androidx.security.crypto.EncryptedSharedPreferences::class)
class ShadowEncryptedSharedPreferences {
    companion object {
        @JvmStatic
        @Implementation
        fun create(
            context: Context,
            fileName: String,
            masterKey: androidx.security.crypto.MasterKey,
            prefKeyEncryptionScheme: androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme,
            prefValueEncryptionScheme: androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
        ): android.content.SharedPreferences {
            return context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        }
    }
}

@Implements(BiometricHelper::class)
class ShadowBiometricHelper {
    companion object {
        @JvmStatic
        @Implementation
        fun encryptWithPin(session: AuthenticatedSession, pin: String, salt: ByteArray): com.clhs.score.data.BiometricHelper.EncryptedData {
            return com.clhs.score.data.BiometricHelper.EncryptedData("cipher", Base64.encodeToString("iv".toByteArray(), Base64.NO_WRAP))
        }

        @JvmStatic
        @Implementation
        fun decryptWithPin(cipherText: String, ivBase64: String, pin: String, salt: ByteArray): AuthenticatedSession {
            return AuthenticatedSession("123", "token", mapOf("cookie1" to "val1"))
        }

        @JvmStatic
        @Implementation
        fun encryptPin(pin: String, cipher: Cipher): com.clhs.score.data.BiometricHelper.EncryptedData {
            return com.clhs.score.data.BiometricHelper.EncryptedData("pinCipher", Base64.encodeToString("pinIv".toByteArray(), Base64.NO_WRAP))
        }

        @JvmStatic
        @Implementation
        fun decryptPin(cipherText: String, cipher: Cipher): String {
            return "1234"
        }

        @JvmStatic
        @Implementation
        fun deleteSecretKey() {
            // Do nothing
        }
    }
}
