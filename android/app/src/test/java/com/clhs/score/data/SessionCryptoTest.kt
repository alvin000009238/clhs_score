package com.clhs.score.data

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

class SessionCryptoTest {
    private val cipher = AesGcmSessionCipher(FixedKeyProvider())
    private val aad = "app/session/general/v1".encodeToByteArray()

    @Test
    fun encryptDecryptRoundTrip() = runTest {
        val plaintext = "private session".encodeToByteArray()

        val encrypted = cipher.encrypt(plaintext, aad)

        assertArrayEquals(plaintext, cipher.decrypt(encrypted, aad))
    }

    @Test
    fun repeatedEncryptionUsesDifferentIvAndCiphertext() = runTest {
        val plaintext = "same plaintext".encodeToByteArray()

        val first = cipher.encrypt(plaintext, aad)
        val second = cipher.encrypt(plaintext, aad)

        assertFalse(first.iv.contentEquals(second.iv))
        assertFalse(first.ciphertext.contentEquals(second.ciphertext))
    }

    @Test
    fun corruptedCiphertextIsRejected() = runTest {
        val encrypted = cipher.encrypt("session".encodeToByteArray(), aad)
        val corrupted = encrypted.copy(
            ciphertext = encrypted.ciphertext.copyOf().also { it[it.lastIndex] = (it.last() xor 1) },
        )

        assertCorrupted { cipher.decrypt(corrupted, aad) }
    }

    @Test
    fun modifiedIvIsRejected() = runTest {
        val encrypted = cipher.encrypt("session".encodeToByteArray(), aad)
        val corrupted = encrypted.copy(
            iv = encrypted.iv.copyOf().also { it[0] = (it[0] xor 1) },
        )

        assertCorrupted { cipher.decrypt(corrupted, aad) }
    }

    @Test
    fun wrongAssociatedDataIsRejected() = runTest {
        val encrypted = cipher.encrypt("session".encodeToByteArray(), aad)

        assertCorrupted {
            cipher.decrypt(encrypted, "app/session/reminder/v1".encodeToByteArray())
        }
    }

    @Test
    fun unsupportedPayloadVersionIsRejected() = runTest {
        val encrypted = cipher.encrypt("session".encodeToByteArray(), aad)

        val error = runCatching {
            cipher.decrypt(encrypted.copy(version = encrypted.version + 1), aad)
        }.exceptionOrNull()

        assertTrue(error is UnsupportedSessionPayloadException)
    }

    private suspend fun assertCorrupted(block: suspend () -> Unit) {
        val error = runCatching { block() }.exceptionOrNull()
        assertTrue(error is SessionCorruptedException)
    }

    private class FixedKeyProvider : SessionKeyProvider {
        private val key = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")

        override fun getOrCreateEncryptionKey(version: Int): SecretKey = key

        override fun getDecryptionKey(version: Int): SecretKey = key
    }
}

private infix fun Byte.xor(value: Int): Byte = (toInt() xor value).toByte()
