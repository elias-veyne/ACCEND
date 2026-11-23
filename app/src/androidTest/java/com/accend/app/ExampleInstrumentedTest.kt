package com.accend.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
  @Test
  fun appHasExpectedIdentity() {
    val appContext = ApplicationProvider.getApplicationContext<android.content.Context>()
    assertEquals("com.accend.app", appContext.packageName)
    assertEquals("ACCEND", appContext.getString(com.accend.app.R.string.app_name))
  }
}
