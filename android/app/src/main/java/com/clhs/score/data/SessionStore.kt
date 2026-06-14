@file:Suppress("DEPRECATION")

package com.clhs.score.data

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom
import javax.crypto.Cipher
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put

class SessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val secureRandom = SecureRandom()
    private val masterKey = MasterKey.Builder(appContext)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    private val prefs = EncryptedSharedPreferences.create(
        appContext,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun saveSession(session: AuthenticatedSession) {
        prefs.edit {
            putString(KEY_STUDENT_NO, session.studentNo)
            putString(KEY_API_TOKEN, session.apiToken)
            putString(KEY_COOKIES, session.cookiesJson())
        }
    }

    fun loadSession(): AuthenticatedSession? {
        return loadSessionFromKeys(KEY_STUDENT_NO, KEY_API_TOKEN, KEY_COOKIES)
    }

    fun saveReminderSession(session: AuthenticatedSession, expiresAtMillis: Long) {
        prefs.edit {
            putString(KEY_REMINDER_STUDENT_NO, session.studentNo)
            putString(KEY_REMINDER_API_TOKEN, session.apiToken)
            putString(KEY_REMINDER_COOKIES, session.cookiesJson())
            putLong(KEY_REMINDER_EXPIRES_AT, expiresAtMillis)
        }
    }

    fun loadReminderSession(nowMillis: Long = System.currentTimeMillis()): AuthenticatedSession? {
        val expiresAt = prefs.getLong(KEY_REMINDER_EXPIRES_AT, 0L)
        if (expiresAt <= nowMillis) {
            clearReminderSession()
            return null
        }
        return loadSessionFromKeys(
            studentNoKey = KEY_REMINDER_STUDENT_NO,
            tokenKey = KEY_REMINDER_API_TOKEN,
            cookiesKey = KEY_REMINDER_COOKIES,
        )
    }

    fun clearReminderSession() {
        prefs.edit {
            remove(KEY_REMINDER_STUDENT_NO)
            remove(KEY_REMINDER_API_TOKEN)
            remove(KEY_REMINDER_COOKIES)
            remove(KEY_REMINDER_EXPIRES_AT)
        }
    }

    fun saveBiometricSession(session: AuthenticatedSession, pin: String, cipher: Cipher) {
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val encryptedSession = BiometricHelper.encryptWithPin(session, pin, salt)
        val encryptedPin = BiometricHelper.encryptPin(pin, cipher)

        prefs.edit {
            putString(KEY_BIOMETRIC_SESSION_CIPHER_TEXT, encryptedSession.cipherTextBase64)
            putString(KEY_BIOMETRIC_SESSION_IV, encryptedSession.ivBase64)
            putString(KEY_BIOMETRIC_SESSION_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            putString(KEY_BIOMETRIC_PIN_CIPHER_TEXT, encryptedPin.cipherTextBase64)
            putString(KEY_BIOMETRIC_PIN_IV, encryptedPin.ivBase64)
        }
    }

    fun loadBiometricSession(cipher: Cipher): AuthenticatedSession? {
        val pinCipherText = prefs.getString(KEY_BIOMETRIC_PIN_CIPHER_TEXT, null) ?: return null
        val pin = runCatching {
            BiometricHelper.decryptPin(pinCipherText, cipher)
        }.getOrNull() ?: return null

        return loadSessionWithPin(pin)
    }

    fun loadSessionWithPin(pin: String): AuthenticatedSession? {
        return runCatching {
            val sessionCipherText = prefs.getString(KEY_BIOMETRIC_SESSION_CIPHER_TEXT, null)
                ?: return@runCatching null
            val sessionIv = prefs.getString(KEY_BIOMETRIC_SESSION_IV, null)
                ?: return@runCatching null
            val saltStr = prefs.getString(KEY_BIOMETRIC_SESSION_SALT, null)
                ?: return@runCatching null
            val salt = Base64.decode(saltStr, Base64.NO_WRAP)
            BiometricHelper.decryptWithPin(sessionCipherText, sessionIv, pin, salt)
        }.getOrNull()
    }

    fun hasBiometricSession(): Boolean {
        return !prefs.getString(KEY_BIOMETRIC_PIN_CIPHER_TEXT, null).isNullOrBlank() &&
               !prefs.getString(KEY_BIOMETRIC_SESSION_CIPHER_TEXT, null).isNullOrBlank()
    }

    fun getBiometricIv(): ByteArray? {
        val ivStr = prefs.getString(KEY_BIOMETRIC_PIN_IV, null) ?: return null
        return runCatching { Base64.decode(ivStr, Base64.NO_WRAP) }.getOrNull()
    }

    fun clearBiometricSession() {
        prefs.edit {
            remove(KEY_BIOMETRIC_CIPHER_TEXT) // old
            remove(KEY_BIOMETRIC_IV) // old
            remove(KEY_BIOMETRIC_SESSION_CIPHER_TEXT)
            remove(KEY_BIOMETRIC_SESSION_IV)
            remove(KEY_BIOMETRIC_SESSION_SALT)
            remove(KEY_BIOMETRIC_PIN_CIPHER_TEXT)
            remove(KEY_BIOMETRIC_PIN_IV)
        }
        runCatching { BiometricHelper.deleteSecretKey() }
    }

    fun clearNormalSession() {
        prefs.edit {
            remove(KEY_STUDENT_NO)
            remove(KEY_API_TOKEN)
            remove(KEY_COOKIES)
        }
    }

    fun clear() {
        prefs.edit { clear() }
        runCatching { BiometricHelper.deleteSecretKey() }
    }

    private fun JsonObject.toStringMap(): Map<String, String> = entries.associate { (key, value) ->
        key to value.asPrimitiveOrNull()?.contentOrNull.orEmpty()
    }.filterValues { it.isNotBlank() }

    private fun AuthenticatedSession.cookiesJson(): String = buildJsonObject {
        cookies.forEach { (name, value) -> put(name, value) }
    }.toString()

    private fun loadSessionFromKeys(
        studentNoKey: String,
        tokenKey: String,
        cookiesKey: String,
    ): AuthenticatedSession? {
        val studentNo = prefs.getString(studentNoKey, null)?.takeIf { it.isNotBlank() } ?: return null
        val token = prefs.getString(tokenKey, null)?.takeIf { it.isNotBlank() } ?: return null
        val rawCookies = prefs.getString(cookiesKey, "{}").orEmpty()
        val cookies = runCatching {
            SchoolJson.parseToJsonElement(rawCookies).jsonObject.toStringMap()
        }.getOrElse { emptyMap() }
        if (cookies.isEmpty()) return null
        return AuthenticatedSession(studentNo = studentNo, apiToken = token, cookies = cookies)
    }

    private companion object {
        const val PREFS_NAME = "score_session"
        const val KEY_STUDENT_NO = "student_no"
        const val KEY_API_TOKEN = "api_token"
        const val KEY_COOKIES = "cookies"
        const val KEY_REMINDER_STUDENT_NO = "reminder_student_no"
        const val KEY_REMINDER_API_TOKEN = "reminder_api_token"
        const val KEY_REMINDER_COOKIES = "reminder_cookies"
        const val KEY_REMINDER_EXPIRES_AT = "reminder_expires_at"
        const val KEY_BIOMETRIC_CIPHER_TEXT = "biometric_cipher_text" // old
        const val KEY_BIOMETRIC_IV = "biometric_iv" // old
        const val KEY_BIOMETRIC_SESSION_CIPHER_TEXT = "biometric_session_cipher_text"
        const val KEY_BIOMETRIC_SESSION_IV = "biometric_session_iv"
        const val KEY_BIOMETRIC_SESSION_SALT = "biometric_session_salt"
        const val KEY_BIOMETRIC_PIN_CIPHER_TEXT = "biometric_pin_cipher_text"
        const val KEY_BIOMETRIC_PIN_IV = "biometric_pin_iv"
    }
}
