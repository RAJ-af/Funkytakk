package com.example

import android.util.Base64
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.CleartextKeysetHandle
import com.google.crypto.tink.HybridDecrypt
import com.google.crypto.tink.HybridEncrypt
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.hybrid.HybridConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class E2EEAndVaultTest {

    @Before
    fun setup() {
        HybridConfig.register()
    }

    @Test
    fun testTinkHpkeEncryptionAndDecryption() {
        // 1. Generate HPKE Keyset Handle using RFC 9180 template
        val keyTemplate = KeyTemplates.get("DHKEM_X25519_HKDF_SHA256_HKDF_SHA256_AES_256_GCM")
        val privateKeysetHandle = KeysetHandle.generateNew(keyTemplate)

        // 2. Export public keyset to Base64
        val publicKeysetHandle = privateKeysetHandle.publicKeysetHandle
        val stream = ByteArrayOutputStream()
        CleartextKeysetHandle.write(publicKeysetHandle, BinaryKeysetWriter.withOutputStream(stream))
        val publicKeyBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

        assertNotNull(publicKeyBase64)
        assertTrue(publicKeyBase64.isNotEmpty())

        // 3. Encrypt message using public keyset
        val plaintext = "Hello FunkyTalk E2EE! This message is secret."
        val decodedPublicBytes = Base64.decode(publicKeyBase64, Base64.DEFAULT)
        val readPublicHandle = CleartextKeysetHandle.read(BinaryKeysetReader.withBytes(decodedPublicBytes))
        val hybridEncrypt = readPublicHandle.getPrimitive(HybridEncrypt::class.java)

        val ciphertext = hybridEncrypt.encrypt(plaintext.toByteArray(Charsets.UTF_8), ByteArray(0))
        val ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)

        // Ciphertext should not contain plaintext
        assertFalse(ciphertextBase64.contains(plaintext))

        // 4. Decrypt message using private keyset
        val hybridDecrypt = privateKeysetHandle.getPrimitive(HybridDecrypt::class.java)
        val decryptedBytes = hybridDecrypt.decrypt(Base64.decode(ciphertextBase64, Base64.DEFAULT), ByteArray(0))
        val decryptedText = String(decryptedBytes, Charsets.UTF_8)

        assertEquals(plaintext, decryptedText)
    }

    @Test
    fun testEnvelopeEncryptionWithPbkdf2Password() {
        // 1. Generate random 256-bit vault key
        val originalVaultKey = ByteArray(32).also { SecureRandom().nextBytes(it) }

        // 2. PBKDF2 Password key derivation (310,000 iterations, 256-bit AES)
        val password = "StrongMasterPassword123!"
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iterations = 310_000
        val keyLength = 256

        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLength)
        val kekBytes = factory.generateSecret(spec).encoded
        val kek = SecretKeySpec(kekBytes, "AES")

        // 3. Wrap vault key with AES-256-GCM
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, kek)
        val iv = cipher.iv
        val wrappedVaultKey = cipher.doFinal(originalVaultKey)

        assertNotEquals(originalVaultKey.toList(), wrappedVaultKey.toList())

        // 4. Unwrap with correct password
        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding")
        decryptCipher.init(Cipher.DECRYPT_MODE, kek, GCMParameterSpec(128, iv))
        val unwrappedKey = decryptCipher.doFinal(wrappedVaultKey)

        assertArrayEquals(originalVaultKey, unwrappedKey)

        // 5. Unwrap with wrong password should fail with AEADBadTagException
        val wrongSpec = PBEKeySpec("WrongPassword!".toCharArray(), salt, iterations, keyLength)
        val wrongKekBytes = factory.generateSecret(wrongSpec).encoded
        val wrongKek = SecretKeySpec(wrongKekBytes, "AES")

        val wrongCipher = Cipher.getInstance("AES/GCM/NoPadding")
        wrongCipher.init(Cipher.DECRYPT_MODE, wrongKek, GCMParameterSpec(128, iv))

        try {
            wrongCipher.doFinal(wrappedVaultKey)
            fail("Expected AEADBadTagException on incorrect password")
        } catch (e: Exception) {
            assertTrue(e is javax.crypto.AEADBadTagException)
        }
    }

    @Test
    fun testRecipientNotReadyEdgeCase() {
        val recipientName = "Rohan Sharma"
        val exception = com.example.repository.RecipientNotReadyException(recipientName)

        // Verify message is graceful and explains why (user hasn't opened app/generated keys)
        assertNotNull(exception.message)
        assertTrue(exception.message!!.contains(recipientName))
        assertTrue(exception.message!!.contains("isn't ready for secure chat yet"))
        assertEquals(recipientName, exception.recipientName)
    }
}
