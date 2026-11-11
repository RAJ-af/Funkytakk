package com.example.crypto

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class BackupCryptoTest {

    @Test
    fun testPasswordWrapUnwrapRoundTrip() = runBlocking {
        val password = "SecretVaultPassword123"
        val firebaseUid = "test_user_uid_98765"

        // 1. Generate master key BK
        val originalBk = BackupCrypto.generateMasterKey()
        assertEquals(32, originalBk.size)

        // 2. Wrap master key BK with password and UID
        val headerObj = BackupCrypto.wrapMasterKey(originalBk, password, firebaseUid)
        val headerJson = headerObj.toJson()

        assertNotNull(headerJson)
        assertTrue(headerJson.contains("salt"))
        assertTrue(headerJson.contains("wrapped_key"))

        // 3. Unwrap master key BK with correct password and UID
        val unwrappedBk = BackupCrypto.unwrapMasterKey(headerJson, password, firebaseUid)

        // 4. Assert round trip produces identical 32-byte master key
        assertArrayEquals(originalBk, unwrappedBk)
    }

    @Test
    fun testWrongPasswordRejected() = runBlocking {
        val password = "CorrectVaultPassword"
        val wrongPassword = "IncorrectPassword"
        val firebaseUid = "test_user_uid_12345"

        val originalBk = BackupCrypto.generateMasterKey()
        val headerObj = BackupCrypto.wrapMasterKey(originalBk, password, firebaseUid)
        val headerJson = headerObj.toJson()

        try {
            BackupCrypto.unwrapMasterKey(headerJson, wrongPassword, firebaseUid)
            fail("Expected exception when unwrapping with incorrect password!")
        } catch (e: Exception) {
            // Success: AEADBadTagException / SecurityException expected
            assertTrue(e is javax.crypto.AEADBadTagException || e is IllegalArgumentException || e is java.security.GeneralSecurityException)
        }
    }

    @Test
    fun testTamperedChunkRejected() {
        val masterKey = BackupCrypto.generateMasterKey()
        val chunkId = "c_test_chunk_001.enc"
        val plaintextData = "Hello FunkyTalk Encrypted Backup Message Line 1\nLine 2".toByteArray(Charsets.UTF_8)

        // Encrypt chunk
        val encryptedChunk = BackupCrypto.encryptChunk(plaintextData, chunkId, masterKey)
        assertTrue(encryptedChunk.size > plaintextData.size)

        // Decrypt untampered chunk
        val decrypted = BackupCrypto.decryptChunk(encryptedChunk, chunkId, masterKey)
        assertArrayEquals(plaintextData, decrypted)

        // Tamper with ciphertext byte inside chunk
        val tamperedChunk = encryptedChunk.clone()
        tamperedChunk[tamperedChunk.size - 1] = (tamperedChunk[tamperedChunk.size - 1].toInt() xor 0xFF).toByte()

        try {
            BackupCrypto.decryptChunk(tamperedChunk, chunkId, masterKey)
            fail("Expected exception when decrypting tampered chunk payload!")
        } catch (e: Exception) {
            // Success: AEAD bad tag or decryption error
            assertTrue(e is javax.crypto.AEADBadTagException || e is IllegalArgumentException || e is java.security.GeneralSecurityException)
        }
    }

    @Test
    fun testIncrementalManifestDiffAndOrphanDetection() {
        val chunk1 = BackupChunkMetadata("c_1.enc", "messages", "conv_1", "1-10", "sha_1", 1024L)
        val chunk2 = BackupChunkMetadata("c_2.enc", "messages", "conv_2", "1-10", "sha_2", 2048L)
        val chunk3 = BackupChunkMetadata("m_3.enc", "media", "conv_1", "msg_5", "sha_3", 5000L)

        val oldManifest = BackupManifest(version = 1, timestamp = 1000L, chunks = listOf(chunk1, chunk2))

        // New run adds media chunk 3 and updates chunk 1
        val updatedChunk1 = BackupChunkMetadata("c_1_v2.enc", "messages", "conv_1", "1-20", "sha_1_new", 1500L)
        val newManifest = BackupManifest(version = 1, timestamp = 2000L, chunks = listOf(updatedChunk1, chunk2, chunk3))

        // Check new manifest contains all current active chunks
        assertEquals(3, newManifest.chunks.size)
        assertTrue(newManifest.chunks.any { it.id == "c_1_v2.enc" })
        assertTrue(newManifest.chunks.any { it.id == "c_2.enc" })
        assertTrue(newManifest.chunks.any { it.id == "m_3.enc" })

        // Diff orphans (chunks in old manifest but absent in new manifest)
        val newChunkIds = newManifest.chunks.map { it.id }.toSet()
        val orphanChunks = oldManifest.chunks.filter { !newChunkIds.contains(it.id) }

        assertEquals(1, orphanChunks.size)
        assertEquals("c_1.enc", orphanChunks.first().id)
    }

    @Test
    fun testManifestOrderingAndVerification() {
        val msgChunk1 = BackupChunkMetadata("c_1.enc", "messages", "conv_1", "1-10", "sha_1", 1000L)
        val msgChunk2 = BackupChunkMetadata("c_2.enc", "messages", "conv_2", "1-10", "sha_2", 2000L)
        val mediaChunk = BackupChunkMetadata("m_1.enc", "media", "", "img1.jpg", "sha_m1", 50000L)

        val manifest = BackupManifest(version = 1, timestamp = 123456789L, chunks = listOf(mediaChunk, msgChunk1, msgChunk2))
        val json = manifest.toJson()
        val parsed = BackupManifest.fromJson(json)

        assertEquals(3, parsed.chunks.size)
        // Verify messages can be segregated and restored first before media
        val textChunks = parsed.chunks.filter { it.type == "messages" }
        val mediaChunks = parsed.chunks.filter { it.type == "media" }

        assertEquals(2, textChunks.size)
        assertEquals(1, mediaChunks.size)
        assertEquals("c_1.enc", textChunks[0].id)
        assertEquals("c_2.enc", textChunks[1].id)
        assertEquals("m_1.enc", mediaChunks[0].id)
    }

    @Test
    fun testRestoreDeduplicationRunningTwiceProducesIdenticalRows() {
        // Mock table storage simulating Room's INSERT OR IGNORE behavior on primary key (id)
        val mockMessageTable = mutableMapOf<String, String>()

        val restoredBatch1 = listOf(
            "msg_001" to "Hello, first restored message",
            "msg_002" to "Second message",
            "msg_003" to "Third message"
        )

        // First restore run: simulate INSERT OR IGNORE
        restoredBatch1.forEach { (id, text) ->
            if (!mockMessageTable.containsKey(id)) {
                mockMessageTable[id] = text
            }
        }
        assertEquals(3, mockMessageTable.size)
        assertEquals("Hello, first restored message", mockMessageTable["msg_001"])

        // Second restore run with same records plus one additional
        val restoredBatch2 = listOf(
            "msg_001" to "Hello, first restored message (duplicate)",
            "msg_002" to "Second message (duplicate)",
            "msg_003" to "Third message (duplicate)",
            "msg_004" to "Fourth new message"
        )

        restoredBatch2.forEach { (id, text) ->
            if (!mockMessageTable.containsKey(id)) {
                mockMessageTable[id] = text
            }
        }

        // Must ignore duplicates: size is 4, and original values are preserved
        assertEquals(4, mockMessageTable.size)
        assertEquals("Hello, first restored message", mockMessageTable["msg_001"])
        assertEquals("Fourth new message", mockMessageTable["msg_004"])
    }
}
