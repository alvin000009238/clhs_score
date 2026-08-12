package com.clhs.score.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.clhs.score.data.proto.EncryptedSessionPayload
import com.clhs.score.data.proto.SessionStorage
import com.google.protobuf.ByteString
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SessionStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val cipher = AesGcmSessionCipher(FixedKeyProvider())
    private val sessionA = AuthenticatedSession("A001", "token-a", mapOf("sid" to "cookie-a"))
    private val sessionB = AuthenticatedSession("B002", "token-b", mapOf("sid" to "cookie-b"))

    @Test
    fun generalAndReminderSessionsStaySeparated() = runTest {
        val store = store()

        store.saveSession(sessionA)
        store.saveReminderSession(sessionB, expiresAtMillis = 2_000L)

        assertEquals(sessionA, store.loadSession())
        assertEquals(sessionB, store.loadReminderSession(nowMillis = 1_000L))
    }

    @Test
    fun deleteAndLogoutClearEncryptedSessions() = runTest {
        val legacy = FakeLegacySource(general = sessionA, reminder = LegacyReminderSession(sessionB, 2_000L))
        val store = store(legacy = legacy)
        store.loadSession()

        store.clearReminderSession()
        assertNull(store.loadReminderSession(nowMillis = 1_000L))

        store.clear()
        assertNull(store.loadSession())
        assertTrue(legacy.clearAllCalled)
    }

    @Test
    fun expiredReminderIsDeleted() = runTest {
        val store = store()
        store.saveReminderSession(sessionA, expiresAtMillis = 1_000L)

        assertNull(store.loadReminderSession(nowMillis = 1_000L))
        assertNull(store.loadReminderSession(nowMillis = 999L))
    }

    @Test
    fun studentMismatchDeletesReminder() = runTest {
        val store = store()
        store.saveReminderSession(sessionA, expiresAtMillis = 2_000L)

        assertNull(
            store.loadReminderSession(
                nowMillis = 1_000L,
                expectedStudentNo = sessionB.studentNo,
            ),
        )
        assertNull(store.loadReminderSession(nowMillis = 1_000L))
    }

    @Test
    fun legacySessionsMigrateAndClearOnlyAfterVerification() = runTest {
        val legacy = FakeLegacySource(
            general = sessionA,
            reminder = LegacyReminderSession(sessionB, 2_000L),
        )
        val store = store(legacy = legacy)

        assertEquals(sessionA, store.loadSession())
        assertEquals(sessionB, store.loadReminderSession(nowMillis = 1_000L))
        assertTrue(legacy.generalCleared)
        assertTrue(legacy.reminderCleared)
    }

    @Test
    fun validNewStorageIsNotOverwrittenByLegacy() = runTest {
        val dataStore = dataStore()
        val existingPayload = cipher.encrypt(
            SessionSerializer.serialize(sessionA),
            GENERAL_AAD,
        )
        dataStore.updateData {
            it.toBuilder().setGeneralSession(existingPayload.toProto()).build()
        }
        val legacy = FakeLegacySource(general = sessionB)
        val store = store(dataStore = dataStore, legacy = legacy)

        assertEquals(sessionA, store.loadSession())
        assertTrue(legacy.generalCleared)
    }

    @Test
    fun validNewStorageRemainsUsableWhenLegacyCleanupMustRetry() = runTest {
        val dataStore = dataStore()
        val existingPayload = cipher.encrypt(
            SessionSerializer.serialize(sessionA),
            GENERAL_AAD,
        )
        dataStore.updateData {
            it.toBuilder().setGeneralSession(existingPayload.toProto()).build()
        }
        val legacy = FakeLegacySource(
            general = sessionB,
            generalClearFailure = SessionMigrationException(),
        )
        val store = store(dataStore = dataStore, legacy = legacy)

        assertEquals(sessionA, store.loadSession())
        assertFalse(legacy.generalCleared)
    }

    @Test
    fun corruptReminderStorageDoesNotBlockGeneralSession() = runTest {
        val dataStore = dataStore()
        val generalPayload = cipher.encrypt(
            SessionSerializer.serialize(sessionA),
            GENERAL_AAD,
        )
        dataStore.updateData {
            it.toBuilder()
                .setGeneralSession(generalPayload.toProto())
                .setReminderSession(corruptPayload())
                .build()
        }

        assertEquals(sessionA, store(dataStore = dataStore).loadSession())
    }

    @Test
    fun corruptGeneralStorageDoesNotBlockReminderSession() = runTest {
        val dataStore = dataStore()
        val reminderPayload = cipher.encrypt(
            SessionSerializer.serialize(sessionB, expiresAtMillis = 2_000L),
            REMINDER_AAD,
        )
        dataStore.updateData {
            it.toBuilder()
                .setGeneralSession(corruptPayload())
                .setReminderSession(reminderPayload.toProto())
                .build()
        }

        assertEquals(sessionB, store(dataStore = dataStore).loadReminderSession(nowMillis = 1_000L))
    }

    @Test
    fun normalSessionClearCannotReimportLeftoverLegacyData() = runTest {
        val legacy = FakeLegacySource(general = sessionA, retainAfterClear = true)
        val store = store(legacy = legacy)

        store.clearNormalSession()

        assertNull(store.loadSession())
    }

    @Test
    fun normalSessionClearDoesNotReadDataItIsDeleting() = runTest {
        val legacy = FakeLegacySource(
            general = sessionA,
            generalReadFailure = SessionMigrationException(),
        )
        val store = store(legacy = legacy)

        store.clearNormalSession()

        assertTrue(legacy.generalCleared)
        assertNull(store.loadSession())
    }

    @Test
    fun reminderSessionRemainsRecoverableWhenLegacyCleanupFails() = runTest {
        val dataStore = dataStore()
        val reminderPayload = cipher.encrypt(
            SessionSerializer.serialize(sessionB, expiresAtMillis = 2_000L),
            REMINDER_AAD,
        )
        dataStore.updateData {
            it.toBuilder()
                .setReminderSession(reminderPayload.toProto())
                .build()
        }
        val legacy = FakeLegacySource(
            reminder = LegacyReminderSession(sessionA, 2_000L),
            reminderClearFailure = SessionMigrationException(),
        )

        val error = runCatching {
            store(dataStore = dataStore, legacy = legacy).clearReminderSession()
        }.exceptionOrNull()

        assertTrue(error is SessionMigrationException)
        assertTrue(dataStore.data.first().hasReminderSession())
    }

    @Test
    fun corruptNewPayloadRecoversFromValidLegacy() = runTest {
        val dataStore = dataStore()
        dataStore.updateData {
            it.toBuilder().setGeneralSession(corruptPayload()).build()
        }
        val legacy = FakeLegacySource(general = sessionA)
        val store = store(dataStore = dataStore, legacy = legacy)

        assertEquals(sessionA, store.loadSession())
        assertTrue(legacy.generalCleared)
    }

    @Test
    fun corruptNewPayloadWithoutLegacyIsReported() = runTest {
        val dataStore = dataStore()
        dataStore.updateData {
            it.toBuilder().setGeneralSession(corruptPayload()).build()
        }

        val error = runCatching { store(dataStore = dataStore).loadSession() }.exceptionOrNull()

        assertTrue(error is SessionCorruptedException)
    }

    @Test
    fun failedMigrationWriteKeepsLegacyData() = runTest {
        val legacy = FakeLegacySource(general = sessionA)
        val store = store(
            legacy = legacy,
            cipher = object : SessionCipher {
                override suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): EncryptedPayload {
                    throw SessionKeyUnavailableException()
                }

                override suspend fun decrypt(payload: EncryptedPayload, associatedData: ByteArray): ByteArray =
                    error("not reached")
            },
        )

        val error = runCatching { store.loadSession() }.exceptionOrNull()

        assertTrue(error is SessionKeyUnavailableException)
        assertFalse(legacy.generalCleared)
    }

    @Test
    fun failedMigrationVerificationKeepsLegacyData() = runTest {
        val legacy = FakeLegacySource(general = sessionA)
        val mismatchingCipher = object : SessionCipher {
            override suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): EncryptedPayload =
                cipher.encrypt(plaintext, associatedData)

            override suspend fun decrypt(payload: EncryptedPayload, associatedData: ByteArray): ByteArray =
                SessionSerializer.serialize(sessionB)
        }

        val error = runCatching {
            store(legacy = legacy, cipher = mismatchingCipher).loadSession()
        }.exceptionOrNull()

        assertTrue(error is SessionMigrationException)
        assertFalse(legacy.generalCleared)
    }

    @Test
    fun corruptLegacyDataIsReportedWithoutDeletion() = runTest {
        val legacy = FakeLegacySource(generalReadFailure = SessionMigrationException())

        val error = runCatching { store(legacy = legacy).loadSession() }.exceptionOrNull()

        assertTrue(error is SessionMigrationException)
        assertFalse(legacy.generalCleared)
    }

    @Test
    fun logoutInvalidatesAnOlderPendingSave() = runTest {
        val encryptStarted = CompletableDeferred<Unit>()
        val releaseEncrypt = CompletableDeferred<Unit>()
        val blockingCipher = object : SessionCipher {
            override suspend fun encrypt(plaintext: ByteArray, associatedData: ByteArray): EncryptedPayload {
                encryptStarted.complete(Unit)
                releaseEncrypt.await()
                return cipher.encrypt(plaintext, associatedData)
            }

            override suspend fun decrypt(payload: EncryptedPayload, associatedData: ByteArray): ByteArray =
                cipher.decrypt(payload, associatedData)
        }
        val store = store(cipher = blockingCipher)

        val save = async { store.saveSession(sessionA) }
        encryptStarted.await()
        val logout = async { store.clear() }
        releaseEncrypt.complete(Unit)
        save.await()
        logout.await()

        assertNull(store.loadSession())
    }

    private fun store(
        dataStore: DataStore<SessionStorage> = dataStore(),
        cipher: SessionCipher = this.cipher,
        legacy: FakeLegacySource = FakeLegacySource(),
    ): SessionStore = SessionStore(
        dataStore = dataStore,
        cipher = cipher,
        legacySource = legacy,
        biometricStorage = NoOpBiometricStorage,
    )

    private fun dataStore(): DataStore<SessionStorage> {
        val file = File(temporaryFolder.root, "session-${fileCounter.incrementAndGet()}.pb")
        return DataStoreFactory.create(
            serializer = SessionStorageSerializer,
            produceFile = { file },
        )
    }

    private fun corruptPayload(): EncryptedSessionPayload = EncryptedSessionPayload.newBuilder()
        .setVersion(AesGcmSessionCipher.CURRENT_PAYLOAD_VERSION)
        .setKeyVersion(AesGcmSessionCipher.CURRENT_KEY_VERSION)
        .setIv(ByteString.copyFrom(ByteArray(AesGcmSessionCipher.IV_SIZE_BYTES)))
        .setCiphertext(ByteString.copyFrom(ByteArray(AesGcmSessionCipher.TAG_SIZE_BYTES)))
        .build()

    private class FixedKeyProvider : SessionKeyProvider {
        private val key = SecretKeySpec(ByteArray(32) { (it + 1).toByte() }, "AES")

        override fun getOrCreateEncryptionKey(version: Int): SecretKey = key

        override fun getDecryptionKey(version: Int): SecretKey = key
    }

    private class FakeLegacySource(
        var general: AuthenticatedSession? = null,
        var reminder: LegacyReminderSession? = null,
        private val biometric: BiometricSessionRecord? = null,
        private val generalReadFailure: SessionStorageException? = null,
        private val generalClearFailure: SessionStorageException? = null,
        private val reminderClearFailure: SessionStorageException? = null,
        private val retainAfterClear: Boolean = false,
    ) : LegacySessionSource {
        var generalCleared = false
        var reminderCleared = false
        var biometricCleared = false
        var clearAllCalled = false

        override fun readGeneral(): AuthenticatedSession? {
            generalReadFailure?.let { throw it }
            return general
        }

        override fun readReminder(): LegacyReminderSession? = reminder

        override fun readBiometric(): BiometricSessionRecord? = biometric

        override fun clearGeneral() {
            generalClearFailure?.let { throw it }
            generalCleared = true
            if (!retainAfterClear) general = null
        }

        override fun clearReminder() {
            reminderClearFailure?.let { throw it }
            reminderCleared = true
            if (!retainAfterClear) reminder = null
        }

        override fun clearBiometric() {
            biometricCleared = true
        }

        override fun clearAll() {
            clearAllCalled = true
            clearGeneral()
            clearReminder()
            clearBiometric()
        }
    }

    private object NoOpBiometricStorage : BiometricSessionStorage {
        override fun save(session: AuthenticatedSession, pin: String, cipher: Cipher) = Unit
        override fun load(cipher: Cipher): AuthenticatedSession? = null
        override fun loadWithPin(pin: String): AuthenticatedSession? = null
        override fun hasSession(): Boolean = false
        override fun pinIv(): ByteArray? = null
        override fun clear() = Unit
    }

    private companion object {
        val fileCounter = AtomicInteger()
        val GENERAL_AAD = "app/session/general/v1".encodeToByteArray()
        val REMINDER_AAD = "app/session/reminder/v1".encodeToByteArray()
    }
}
