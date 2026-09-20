package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FunkyTalk", appName)
  }

  @Test
  fun `parse funkytalk room deep link`() {
    val uri = Uri.parse("funkytalk://room/123456")
    assertEquals("funkytalk", uri.scheme)
    assertEquals("room", uri.host)
    assertEquals("123456", uri.pathSegments.firstOrNull())
  }
}
