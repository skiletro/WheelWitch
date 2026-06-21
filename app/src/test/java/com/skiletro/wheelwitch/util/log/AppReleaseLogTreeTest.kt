package com.skiletro.wheelwitch.util.log

import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import timber.log.Timber

class AppReleaseLogTreeTest {

  private val recorded = mutableListOf<Recorded>()
  private lateinit var tree: AppReleaseLogTree

  @BeforeEach
  fun setUp() {
    Timber.uprootAll()
    recorded.clear()
    tree =
      object : AppReleaseLogTree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
          recorded.add(Recorded(priority, tag, message, t))
          super.log(priority, tag, message, t)
        }
      }
    Timber.plant(tree)
  }

  @AfterEach
  fun tearDown() {
    Timber.uprootAll()
  }

  @Test
  fun `drops VERBOSE DEBUG and INFO`() {
    Timber.v("v drop")
    Timber.d("d drop")
    Timber.i("i drop")
    assertThat(recorded).isEmpty()
  }

  @Test
  fun `keeps WARN ERROR and ASSERT`() {
    Timber.w("w keep")
    Timber.e("e keep")
    Timber.wtf("a keep")
    assertThat(recorded.map { it.priority })
        .containsExactly(Log.WARN, Log.ERROR, Log.ASSERT)
        .inOrder()
  }

  private data class Recorded(
    val priority: Int,
    val tag: String?,
    val message: String,
    val throwable: Throwable?,
  )
}
