package com.clhs.score.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import com.clhs.score.data.proto.EncryptedSessionPayload
import com.clhs.score.data.proto.SessionStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher

private data class SessionStoreDependencies(
    val dataStore: DataStore<SessionStorage>,
    val cipher: SessionCipher,
    val legacySource: LegacySessionSource,
    val biometricStorage: BiometricSessionStorage,
)

private fun productionSessionStoreDependencies(context: Context): SessionStoreDependencies {
    val appContext = context.applicationContext
    val legacySource = EncryptedSharedPreferencesLegacySessionSource(appContext)
    return SessionStoreDependencies(
        dataStore = appContext.sessionDataStore,
        cipher = AesGcmSessionCipher(AndroidKeystoreSessionKeyProvider()),
        legacySource = legacySource,
        biometricStorage = SharedPreferencesBiometricSessionStorage(appContext, legacySource),
    )
}

class SessionStore private constructor(
    private val dependencies: SessionStoreDependencies,
) {
    constructor(context: Context) : this(productionSessionStoreDependencies(context))

    internal constructor(
        dataStore: DataStore<SessionStorage>,
        cipher: SessionCipher,
        legacySource: LegacySessionSource,
        biometricStorage: BiometricSessionStorage,
    ) : this(SessionStoreDependencies(dataStore, cipher, legacySource, biometricStorage))

    private val dataStore = dependencies.dataStore
    private val cipher = dependencies.cipher
    private val legacySource = dependencies.legacySource
    private val biometricStorage = dependencies.biometricStorage

    suspend fun saveSession(session: AuthenticatedSession) {
        val generation = generalWriteGeneration.get()
        storageMutex.withLock {
            if (generation != generalWriteGeneration.get()) return@withLock
            migrateGeneralLegacyIfNeeded()
            val payload = cipher.encrypt(SessionSerializer.serialize(session), GENERAL_AAD)
            if (generation != generalWriteGeneration.get()) return@withLock
            updateStorage { storage -> storage.toBuilder().setGeneralSession(payload.toProto()).build() }
        }
    }

    suspend fun loadSession(): AuthenticatedSession? = storageMutex.withLock {
        migrateGeneralLegacyIfNeeded()
        val storage = readStorage()
        if (!storage.hasGeneralSession()) return@withLock null
        decodeGeneral(storage.generalSession)
    }

    suspend fun saveReminderSession(session: AuthenticatedSession, expiresAtMillis: Long) {
        val generation = reminderWriteGeneration.get()
        storageMutex.withLock {
            if (generation != reminderWriteGeneration.get()) return@withLock
            migrateReminderLegacyIfNeeded()
            val payload = cipher.encrypt(
                SessionSerializer.serialize(session, expiresAtMillis),
                REMINDER_AAD,
            )
            if (generation != reminderWriteGeneration.get()) return@withLock
            updateStorage { storage -> storage.toBuilder().setReminderSession(payload.toProto()).build() }
        }
    }

    suspend fun loadReminderSession(
        nowMillis: Long = System.currentTimeMillis(),
        expectedStudentNo: String? = null,
    ): AuthenticatedSession? =
        storageMutex.withLock {
            migrateReminderLegacyIfNeeded()
            val storage = readStorage()
            if (!storage.hasReminderSession()) return@withLock null
            val reminder = decodeReminder(storage.reminderSession)
            if (reminder.expiresAtMillis!! <= nowMillis) {
                reminderWriteGeneration.incrementAndGet()
                updateStorage { current -> current.toBuilder().clearReminderSession().build() }
                return@withLock null
            }
            if (expectedStudentNo != null && reminder.session.studentNo != expectedStudentNo) {
                reminderWriteGeneration.incrementAndGet()
                updateStorage { current -> current.toBuilder().clearReminderSession().build() }
                return@withLock null
            }
            reminder.session
        }

    suspend fun clearReminderSession() {
        reminderWriteGeneration.incrementAndGet()
        storageMutex.withLock {
            legacySource.clearReminder()
            updateStorage { storage ->
                storage.toBuilder()
                    .clearReminderSession()
                    .setReminderLegacyMigrationComplete(true)
                    .build()
            }
        }
    }

    fun saveBiometricSession(session: AuthenticatedSession, pin: String, cipher: Cipher) {
        biometricStorage.save(session, pin, cipher)
    }

    fun loadBiometricSession(cipher: Cipher): AuthenticatedSession? = biometricStorage.load(cipher)

    fun loadSessionWithPin(pin: String): AuthenticatedSession? = biometricStorage.loadWithPin(pin)

    fun hasBiometricSession(): Boolean = biometricStorage.hasSession()

    fun getBiometricIv(): ByteArray? = biometricStorage.pinIv()

    fun clearBiometricSession() = biometricStorage.clear()

    suspend fun clearNormalSession() {
        generalWriteGeneration.incrementAndGet()
        storageMutex.withLock {
            legacySource.clearGeneral()
            updateStorage { storage ->
                storage.toBuilder()
                    .clearGeneralSession()
                    .setGeneralLegacyMigrationComplete(true)
                    .build()
            }
        }
    }

    suspend fun clear() {
        generalWriteGeneration.incrementAndGet()
        reminderWriteGeneration.incrementAndGet()
        storageMutex.withLock {
            updateStorage {
                SessionStorage.newBuilder()
                    .setLegacyMigrationComplete(true)
                    .setGeneralLegacyMigrationComplete(true)
                    .setReminderLegacyMigrationComplete(true)
                    .build()
            }
            var failure: SessionStorageException? = null
            try {
                legacySource.clearAll()
            } catch (error: SessionStorageException) {
                failure = error
            }
            try {
                biometricStorage.clear()
            } catch (error: SessionStorageException) {
                failure = failure?.also { it.addSuppressed(error) } ?: error
            }
            failure?.let { throw it }
        }
    }

    private suspend fun migrateGeneralLegacyIfNeeded() {
        var storage = readStorage()
        if (storage.legacyMigrationComplete || storage.generalLegacyMigrationComplete) return

        var generalToVerify: AuthenticatedSession? = null
        val builder = storage.toBuilder()

        if (storage.hasGeneralSession()) {
            try {
                decodeGeneral(storage.generalSession)
                try {
                    legacySource.clearGeneral()
                } catch (_: SessionStorageException) {
                    return
                }
            } catch (error: SessionStorageException) {
                val legacy = legacySource.readGeneral() ?: throw error
                builder.generalSession = cipher.encrypt(
                    SessionSerializer.serialize(legacy),
                    GENERAL_AAD,
                ).toProto()
                generalToVerify = legacy
            }
        } else {
            legacySource.readGeneral()?.let { legacy ->
                builder.generalSession = cipher.encrypt(
                    SessionSerializer.serialize(legacy),
                    GENERAL_AAD,
                ).toProto()
                generalToVerify = legacy
            }
        }

        if (generalToVerify != null) {
            updateStorage { builder.build() }
            storage = readStorage()
            if (!storage.hasGeneralSession() || decodeGeneral(storage.generalSession) != generalToVerify) {
                throw SessionMigrationException()
            }
            try {
                legacySource.clearGeneral()
            } catch (_: SessionStorageException) {
                return
            }
        }

        updateStorage { current ->
            current.toBuilder().setGeneralLegacyMigrationComplete(true).build()
        }
    }

    private suspend fun migrateReminderLegacyIfNeeded() {
        var storage = readStorage()
        if (storage.legacyMigrationComplete || storage.reminderLegacyMigrationComplete) return

        var reminderToVerify: LegacyReminderSession? = null
        val builder = storage.toBuilder()

        if (storage.hasReminderSession()) {
            try {
                decodeReminder(storage.reminderSession)
                try {
                    legacySource.clearReminder()
                } catch (_: SessionStorageException) {
                    return
                }
            } catch (error: SessionStorageException) {
                val legacy = legacySource.readReminder() ?: throw error
                builder.reminderSession = cipher.encrypt(
                    SessionSerializer.serialize(legacy.session, legacy.expiresAtMillis),
                    REMINDER_AAD,
                ).toProto()
                reminderToVerify = legacy
            }
        } else {
            legacySource.readReminder()?.let { legacy ->
                builder.reminderSession = cipher.encrypt(
                    SessionSerializer.serialize(legacy.session, legacy.expiresAtMillis),
                    REMINDER_AAD,
                ).toProto()
                reminderToVerify = legacy
            }
        }

        if (reminderToVerify != null) {
            updateStorage { builder.build() }
            storage = readStorage()
            if (!storage.hasReminderSession()) throw SessionMigrationException()
            val actual = decodeReminder(storage.reminderSession)
            if (actual.session != reminderToVerify.session ||
                actual.expiresAtMillis != reminderToVerify.expiresAtMillis
            ) {
                throw SessionMigrationException()
            }
            try {
                legacySource.clearReminder()
            } catch (_: SessionStorageException) {
                return
            }
        }

        updateStorage { current ->
            current.toBuilder().setReminderLegacyMigrationComplete(true).build()
        }
    }

    private suspend fun decodeGeneral(payload: EncryptedSessionPayload): AuthenticatedSession {
        val decoded = SessionSerializer.deserialize(cipher.decrypt(payload.toDomain(), GENERAL_AAD))
        if (decoded.expiresAtMillis != null) {
            throw SessionCorruptedException("General session contains reminder metadata")
        }
        return decoded.session
    }

    private suspend fun decodeReminder(payload: EncryptedSessionPayload): DecodedSession {
        val decoded = SessionSerializer.deserialize(cipher.decrypt(payload.toDomain(), REMINDER_AAD))
        if (decoded.expiresAtMillis == null || decoded.expiresAtMillis <= 0L) {
            throw SessionCorruptedException("Reminder session expiry is missing")
        }
        return decoded
    }

    private suspend fun readStorage(): SessionStorage = try {
        dataStore.data.first()
    } catch (error: CorruptionException) {
        throw SessionCorruptedException("Encrypted session DataStore is corrupted", error)
    } catch (error: IOException) {
        throw SessionStorageUnavailableException(error)
    }

    private suspend fun updateStorage(transform: (SessionStorage) -> SessionStorage): SessionStorage = try {
        dataStore.updateData(transform)
    } catch (error: CorruptionException) {
        throw SessionCorruptedException("Encrypted session DataStore is corrupted", error)
    } catch (error: IOException) {
        throw SessionStorageUnavailableException(error)
    }

    private companion object {
        val GENERAL_AAD = "app/session/general/v1".encodeToByteArray()
        val REMINDER_AAD = "app/session/reminder/v1".encodeToByteArray()

        // ponytail: one process-wide lock keeps migration/logout atomic; split only if measured contention appears.
        val storageMutex = Mutex()
        val generalWriteGeneration = AtomicLong()
        val reminderWriteGeneration = AtomicLong()
    }
}
