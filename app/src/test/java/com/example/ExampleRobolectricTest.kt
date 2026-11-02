package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("Runtime Explorer", appName)
  }

  @Test
  fun `verify uare library is accessible and initialized in app module`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    com.example.uare.core.RuntimeExplorer.initialize(context)
    val env = com.example.uare.core.RuntimeExplorer.getEnvironment()
    org.junit.Assert.assertNotNull(env)
    org.junit.Assert.assertTrue(env.pid >= 0)
    val handles = com.example.uare.core.RuntimeExplorer.objectRegistry.getAllHandles()
    org.junit.Assert.assertTrue(handles.isNotEmpty())
  }
}

