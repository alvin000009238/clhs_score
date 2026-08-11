@file:Suppress("DEPRECATION")

package com.clhs.score.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.security.GeneralSecurityException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher

internal data class LegacyReminderSession(
    val session: AuthenticatedSession,
    val expiresAtMillis: Long,
)

internal data class BiometricSessionRecord(
    val sessionCiphertext: String,
    val sessionIv: String,
    val sessionSalt: String,
    val pinCiphertext: String,
    val pinIv: String,
)

internal interface LegacySessionSource {
    fun readGeneral(): AuthenticatedSession?
    fun readReminder(): LegacyReminderSession?
    fun readBiometric(): BiometricSessionRecord?
    fun clearGeneral()
    fun clearReminder()
    fun clearBiometric()
    fun clearAll()
}

@SuppressLint("UseKtx") // Migration requires the synchronous commit() success value before deleting data.
internal class EncryptedSharedPreferencesLegacySessionSource(context: Context) : LegacySessionSource {
    private val appContext = context.applicationContext
    private val legacyFile = File(appContext.applicationInfo.dataDir, "shared_prefs/$PREFS_NAME.xml")
    private val encryptedPreferences by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (error: Exception) {
            throw SessionMigrationException(error)
        }
    }

    override fun readGeneral(): AuthenticatedSession? = preferencesOrNull()?.readSession(
        studentNoKey = KEY_STUDENT_NO,
        tokenKey = KEY_API_TOKEN,
        cookiesKey = KEY_COOKIES,
    )

    override fun readReminder(): LegacyReminderSession? {
        val prefs = preferencesOrNull() ?: return null
        val hasAny = REMINDER_KEYS.any(prefs::contains)
        if (!hasAny) return null
        val session = prefs.readSession(
            studentNoKey = KEY_REMINDER_STUDENT_NO,
            tokenKey = KEY_REMINDER_API_TOKEN,
            cookiesKey = KEY_REMINDER_COOKIES,
        ) ?: throw SessionMigrationException()
        if (!prefs.contains(KEY_REMINDER_EXPIRES_AT)) throw SessionMigrationException()
        return LegacyReminderSession(session, prefs.getLong(KEY_REMINDER_EXPIRES_AT, 0L))
            .also { if (it.expiresAtMillis <= 0L) throw SessionMigrationException() }
    }

    override fun readBiometric(): BiometricSessionRecord? {
        val prefs = preferencesOrNull() ?: return null
        val hasAny = BIOMETRIC_KEYS.any(prefs::contains)
        if (!hasAny) return null
        return BiometricSessionRecord(
            sessionCiphertext = prefs.requiredString(KEY_BIOMETRIC_SESSION_CIPHER_TEXT),
            sessionIv = prefs.requiredString(KEY_BIOMETRIC_SESSION_IV),
            sessionSalt = prefs.requiredString(KEY_BIOMETRIC_SESSION_SALT),
            pinCiphertext = prefs.requiredString(KEY_BIOMETRIC_PIN_CIPHER_TEXT),
            pinIv = prefs.requiredString(KEY_BIOMETRIC_PIN_IV),
        )
    }

    override fun clearGeneral() = clearKeys(GENERAL_KEYS)

    override fun clearReminder() = clearKeys(REMINDER_KEYS)

    override fun clearBiometric() = clearKeys(BIOMETRIC_KEYS + LEGACY_BIOMETRIC_KEYS)

    override fun clearAll() {
        val prefs = preferencesOrNull() ?: return
        if (!prefs.edit().clear().commit()) throw SessionMigrationException()
    }

    private fun preferencesOrNull(): SharedPreferences? = if (legacyFile.exists()) encryptedPreferences else null

    private fun SharedPreferences.readSession(
        studentNoKey: String,
        tokenKey: String,
        cookiesKey: String,
    ): AuthenticatedSession? {
        val hasAny = listOf(studentNoKey, tokenKey, cookiesKey).any(::contains)
        if (!hasAny) return null
        val studentNo = requiredString(studentNoKey)
        val token = requiredString(tokenKey)
        val cookies = try {
            SchoolJson.parseToJsonElement(requiredString(cookiesKey)).jsonObject.entries
                .associate { (name, value) -> name to value.asPrimitiveOrNull()?.contentOrNull.orEmpty() }
                .filterValues(String::isNotBlank)
        } catch (error: SerializationException) {
            throw SessionMigrationException(error)
        } catch (error: IllegalArgumentException) {
            throw SessionMigrationException(error)
        }
        return AuthenticatedSession(studentNo, token, cookies).also {
            try {
                SessionSerializer.serialize(it)
            } catch (error: SessionStorageException) {
                throw SessionMigrationException(error)
            }
        }
    }

    private fun SharedPreferences.requiredString(key: String): String =
        getString(key, null)?.takeIf(String::isNotBlank) ?: throw SessionMigrationException()

    private fun clearKeys(keys: Collection<String>) {
        val prefs = preferencesOrNull() ?: return
        val editor = prefs.edit()
        keys.forEach(editor::remove)
        if (!editor.commit()) throw SessionMigrationException()
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
        const val KEY_BIOMETRIC_CIPHER_TEXT = "biometric_cipher_text"
        const val KEY_BIOMETRIC_IV = "biometric_iv"
        const val KEY_BIOMETRIC_SESSION_CIPHER_TEXT = "biometric_session_cipher_text"
        const val KEY_BIOMETRIC_SESSION_IV = "biometric_session_iv"
        const val KEY_BIOMETRIC_SESSION_SALT = "biometric_session_salt"
        const val KEY_BIOMETRIC_PIN_CIPHER_TEXT = "biometric_pin_cipher_text"
        const val KEY_BIOMETRIC_PIN_IV = "biometric_pin_iv"

        val GENERAL_KEYS = listOf(KEY_STUDENT_NO, KEY_API_TOKEN, KEY_COOKIES)
        val REMINDER_KEYS = listOf(
            KEY_REMINDER_STUDENT_NO,
            KEY_REMINDER_API_TOKEN,
            KEY_REMINDER_COOKIES,
            KEY_REMINDER_EXPIRES_AT,
        )
        val BIOMETRIC_KEYS = listOf(
            KEY_BIOMETRIC_SESSION_CIPHER_TEXT,
            KEY_BIOMETRIC_SESSION_IV,
            KEY_BIOMETRIC_SESSION_SALT,
            KEY_BIOMETRIC_PIN_CIPHER_TEXT,
            KEY_BIOMETRIC_PIN_IV,
        )
        val LEGACY_BIOMETRIC_KEYS = listOf(KEY_BIOMETRIC_CIPHER_TEXT, KEY_BIOMETRIC_IV)
    }
}

internal interface BiometricSessionStorage {
    fun save(session: AuthenticatedSession, pin: String, cipher: Cipher)
    fun load(cipher: Cipher): AuthenticatedSession?
    fun loadWithPin(pin: String): AuthenticatedSession?
    fun hasSession(): Boolean
    fun pinIv(): ByteArray?
    fun clear()
}

@SuppressLint("UseKtx") // The tombstone must be durably committed before legacy data can be ignored.
internal class SharedPreferencesBiometricSessionStorage(
    context: Context,
    private val legacySource: LegacySessionSource,
    private val deleteBiometricKey: () -> Unit = BiometricHelper::deleteSecretKey,
) : BiometricSessionStorage {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val lock = Any()
    private val secureRandom = java.security.SecureRandom()

    override fun save(session: AuthenticatedSession, pin: String, cipher: Cipher) = synchronized(lock) {
        val salt = ByteArray(16).also(secureRandom::nextBytes)
        val encryptedSession = BiometricHelper.encryptWithPin(session, pin, salt)
        val encryptedPin = BiometricHelper.encryptPin(pin, cipher)
        write(
            BiometricSessionRecord(
                sessionCiphertext = encryptedSession.cipherTextBase64,
                sessionIv = encryptedSession.ivBase64,
                sessionSalt = Base64.encodeToString(salt, Base64.NO_WRAP),
                pinCiphertext = encryptedPin.cipherTextBase64,
                pinIv = encryptedPin.ivBase64,
            ),
        )
    }

    override fun load(cipher: Cipher): AuthenticatedSession? = synchronized(lock) {
        val record = currentOrMigrated() ?: return@synchronized null
        try {
            val pin = BiometricHelper.decryptPin(record.pinCiphertext, cipher)
            decryptSession(record, pin)
        } catch (error: AEADBadTagException) {
            throw SessionCorruptedException("Biometric session authentication failed", error)
        } catch (error: GeneralSecurityException) {
            throw SessionCorruptedException("Biometric session cannot be decrypted", error)
        } catch (error: IllegalArgumentException) {
            throw SessionCorruptedException("Biometric session is malformed", error)
        }
    }

    override fun loadWithPin(pin: String): AuthenticatedSession? = synchronized(lock) {
        val record = currentOrMigrated() ?: return@synchronized null
        try {
            decryptSession(record, pin)
        } catch (_: AEADBadTagException) {
            null
        } catch (error: GeneralSecurityException) {
            throw SessionCorruptedException("Biometric session cannot be decrypted", error)
        } catch (error: IllegalArgumentException) {
            throw SessionCorruptedException("Biometric session is malformed", error)
        }
    }

    override fun hasSession(): Boolean = synchronized(lock) { currentOrMigrated() != null }

    override fun pinIv(): ByteArray? = synchronized(lock) {
        currentOrMigrated()?.let {
            try {
                Base64.decode(it.pinIv, Base64.NO_WRAP)
            } catch (error: IllegalArgumentException) {
                throw SessionCorruptedException("Biometric IV is malformed", error)
            }
        }
    }

    override fun clear() {
        synchronized(lock) {
            if (!prefs.edit().clear().putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true).commit()) {
                throw SessionStorageUnavailableException()
            }
            // The tombstone is authoritative; leftover legacy ciphertext/key cleanup can retry later.
            runCatching { legacySource.clearBiometric() }
            runCatching { deleteBiometricKey() }
        }
    }

    private fun decryptSession(record: BiometricSessionRecord, pin: String): AuthenticatedSession {
        val salt = Base64.decode(record.sessionSalt, Base64.NO_WRAP)
        return BiometricHelper.decryptWithPin(
            record.sessionCiphertext,
            record.sessionIv,
            pin,
            salt,
        )
    }

    private fun currentOrMigrated(): BiometricSessionRecord? {
        val current = readCurrent()
        if (current != null) {
            if (!prefs.getBoolean(KEY_LEGACY_MIGRATION_COMPLETE, false)) {
                markLegacyMigrationComplete()
            }
            runCatching { legacySource.clearBiometric() }
            return current
        }
        if (prefs.getBoolean(KEY_LEGACY_MIGRATION_COMPLETE, false)) {
            runCatching { legacySource.clearBiometric() }
            return null
        }
        val legacy = legacySource.readBiometric() ?: return null
        write(legacy)
        if (readCurrent() != legacy) throw SessionMigrationException()
        runCatching { legacySource.clearBiometric() }
        return legacy
    }

    private fun readCurrent(): BiometricSessionRecord? {
        val values = KEYS.map { prefs.getString(it, null) }
        if (values.all { it == null }) return null
        if (values.any { it.isNullOrBlank() }) throw SessionCorruptedException("Biometric session is incomplete")
        return BiometricSessionRecord(
            sessionCiphertext = values[0]!!,
            sessionIv = values[1]!!,
            sessionSalt = values[2]!!,
            pinCiphertext = values[3]!!,
            pinIv = values[4]!!,
        )
    }

    private fun write(record: BiometricSessionRecord) {
        val editor = prefs.edit()
            .putString(KEY_SESSION_CIPHER_TEXT, record.sessionCiphertext)
            .putString(KEY_SESSION_IV, record.sessionIv)
            .putString(KEY_SESSION_SALT, record.sessionSalt)
            .putString(KEY_PIN_CIPHER_TEXT, record.pinCiphertext)
            .putString(KEY_PIN_IV, record.pinIv)
            .putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true)
        if (!editor.commit()) throw SessionStorageUnavailableException()
    }

    private fun markLegacyMigrationComplete() {
        if (!prefs.edit().putBoolean(KEY_LEGACY_MIGRATION_COMPLETE, true).commit()) {
            throw SessionStorageUnavailableException()
        }
    }

    private companion object {
        const val PREFS_NAME = "score_biometric_session"
        const val KEY_SESSION_CIPHER_TEXT = "session_ciphertext"
        const val KEY_SESSION_IV = "session_iv"
        const val KEY_SESSION_SALT = "session_salt"
        const val KEY_PIN_CIPHER_TEXT = "pin_ciphertext"
        const val KEY_PIN_IV = "pin_iv"
        const val KEY_LEGACY_MIGRATION_COMPLETE = "legacy_migration_complete"
        val KEYS = listOf(
            KEY_SESSION_CIPHER_TEXT,
            KEY_SESSION_IV,
            KEY_SESSION_SALT,
            KEY_PIN_CIPHER_TEXT,
            KEY_PIN_IV,
        )
    }
}
