package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testExtractBucketAndPath_simpleFilename() {
    val res = SupabaseMediaResolver.extractBucketAndPath("report_shot_12345.jpg")
    assertNotNull(res)
    assertEquals("reports", res?.first)
    assertEquals("report_shot_12345.jpg", res?.second)
  }

  @Test
  fun testExtractBucketAndPath_bucketPrefixed() {
    val res1 = SupabaseMediaResolver.extractBucketAndPath("reports/report_shot_12345.jpg")
    assertNotNull(res1)
    assertEquals("reports", res1?.first)
    assertEquals("report_shot_12345.jpg", res1?.second)

    val res2 = SupabaseMediaResolver.extractBucketAndPath("/reports/subfolder/clip.mp3")
    assertNotNull(res2)
    assertEquals("reports", res2?.first)
    assertEquals("subfolder/clip.mp3", res2?.second)
  }

  @Test
  fun testExtractBucketAndPath_fullStorageUrls() {
    val url1 = "https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/public/reports/evidence_shot.jpg"
    val res1 = SupabaseMediaResolver.extractBucketAndPath(url1)
    assertNotNull(res1)
    assertEquals("reports", res1?.first)
    assertEquals("evidence_shot.jpg", res1?.second)

    val url2 = "https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/authenticated/reports/evidence_audio.mp3"
    val res2 = SupabaseMediaResolver.extractBucketAndPath(url2)
    assertNotNull(res2)
    assertEquals("reports", res2?.first)
    assertEquals("evidence_audio.mp3", res2?.second)

    val url3 = "https://dulpqochkxtamqztauea.supabase.co/storage/v1/object/sign/reports/evidence.jpg?token=abc123xyz"
    val res3 = SupabaseMediaResolver.extractBucketAndPath(url3)
    assertNotNull(res3)
    assertEquals("reports", res3?.first)
    assertEquals("evidence.jpg", res3?.second)
  }

  @Test
  fun testExtractBucketAndPath_nullOrBlank() {
    assertNull(SupabaseMediaResolver.extractBucketAndPath(null))
    assertNull(SupabaseMediaResolver.extractBucketAndPath(""))
    assertNull(SupabaseMediaResolver.extractBucketAndPath("   "))
  }
}
