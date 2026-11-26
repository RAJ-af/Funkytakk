package com.example

import com.example.data.SavedMomentsStore
import com.example.ui.chat.ChatAnimatedStickers
import com.example.util.ErrorMapper
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException
import javax.crypto.AEADBadTagException

class AuditFixesUnitTest {

    @Test
    fun `mapFirebaseAuthError returns Incorrect email or password for wrong and modern credential errors`() {
        val wrongPasswordEx = FirebaseAuthInvalidCredentialsException(
            "ERROR_WRONG_PASSWORD",
            "The password is invalid or the user does not have a password."
        )
        assertEquals("Incorrect email or password.", ErrorMapper.mapFirebaseAuthError(wrongPasswordEx))

        val invalidCredentialEx = FirebaseAuthInvalidCredentialsException(
            "ERROR_INVALID_CREDENTIAL",
            "The supplied auth credential is incorrect, malformed or has expired."
        )
        assertEquals("Incorrect email or password.", ErrorMapper.mapFirebaseAuthError(invalidCredentialEx))

        val invalidEmailEx = FirebaseAuthInvalidCredentialsException(
            "ERROR_INVALID_EMAIL",
            "The email address is badly formatted."
        )
        assertEquals("Please enter a valid email address.", ErrorMapper.mapFirebaseAuthError(invalidEmailEx))

        assertEquals(
            "Incorrect email or password.",
            ErrorMapper.mapFirebaseAuthError(RuntimeException("The supplied auth credential is incorrect, malformed or has expired."))
        )
    }

    @Test
    fun `ChatAnimatedStickers lottie payloads parse cleanly and round-trip tokens`() {
        assertEquals(12, ChatAnimatedStickers.allStickers.size)
        ChatAnimatedStickers.allStickers.forEach { sticker ->
            val jsonObj = JsonParser.parseString(sticker.lottieJson).asJsonObject
            assertTrue("Missing layers in ${sticker.id}", jsonObj.has("layers"))
            val layers = jsonObj.getAsJsonArray("layers")
            assertTrue("Expected multiple layers in ${sticker.id}", layers.size() >= 4)
            assertTrue("Missing easing handles in ${sticker.id}", sticker.lottieJson.contains(""""i":{"x":"""))
            assertTrue("Missing easing handles in ${sticker.id}", sticker.lottieJson.contains(""""o":{"x":"""))

            val token = ChatAnimatedStickers.encode(sticker)
            val parsed = ChatAnimatedStickers.parse(token)
            assertNotNull("Token should parse for ${sticker.id}", parsed)
            assertEquals(sticker.id, parsed!!.sticker.id)
            assertEquals("${sticker.emojiFallback} ${sticker.label} Sticker", ChatAnimatedStickers.readableLabel(token))
        }
    }

    @Test
    fun `SavedMomentsStore encrypts, round-trips, and rejects wrong Vault keys`() {
        val keyA = ByteArray(32) { idx -> (idx + 1).toByte() }
        val keyB = ByteArray(32) { idx -> (idx + 17).toByte() }

        val sample = listOf(
            MomentDto(
                id = "m_101",
                title = "Coffee walk in Seoul",
                content = "Practicing Korean ordering phrases today.",
                language = "Korean",
                likes_count = 12,
                comments_count = 3,
                author = MomentAuthorDto(
                    id = "user_42",
                    username = "mina_k",
                    name = "Mina",
                    country_code = "kr"
                )
            )
        )

        val plainBytes = SavedMomentsStore.serializeMoments(sample)
        val encrypted = SavedMomentsStore.encryptBytes(keyA, plainBytes)
        val decrypted = SavedMomentsStore.decryptBytes(keyA, encrypted)
        val restored = SavedMomentsStore.deserializeMoments(decrypted)

        assertEquals(1, restored.size)
        assertEquals("m_101", restored[0].id)
        assertEquals("Coffee walk in Seoul", restored[0].title)
        assertEquals("mina_k", restored[0].author.username)

        var rejectedWrongKey = false
        try {
            SavedMomentsStore.decryptBytes(keyB, encrypted)
        } catch (_: AEADBadTagException) {
            rejectedWrongKey = true
        }
        assertTrue("Decryption with wrong Vault key must fail authentication tag check", rejectedWrongKey)
    }

    @Test
    fun `UnknownHostException stackTraceToString produces non-blank trace`() {
        val trace = UnknownHostException("dulpqochkxtamqztauea.supabase.co").stackTraceToString()
        assertTrue(trace.contains("UnknownHostException"))
        assertTrue(trace.contains("dulpqochkxtamqztauea.supabase.co"))
    }
}
