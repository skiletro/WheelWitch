package com.skiletro.wheelwitch.util.log

import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import timber.log.Timber

class MemoryBufferTreeTest {

  private lateinit var buffer: LogBuffer

  @BeforeEach
  fun setUp() {
    buffer = LogBuffer(capacity = 10)
    Timber.plant(MemoryBufferTree(buffer = buffer, minPriority = Log.VERBOSE))
  }

  @AfterEach
  fun tearDown() {
    Timber.uprootAll()
  }

  @Test
  fun `writes a LogEntry to the buffer`() {
    Timber.tag("MyTag").i("hello")
    val snap = buffer.snapshot()
    assertThat(snap).hasSize(1)
    assertThat(snap[0].level).isEqualTo(Log.INFO)
    assertThat(snap[0].tag).isEqualTo("MyTag")
    assertThat(snap[0].message).isEqualTo("hello")
    assertThat(snap[0].throwable).isNull()
  }

  @Test
  fun `preserves throwable on the LogEntry`() {
    val ex = RuntimeException("boom")
    Timber.tag("MyTag").e(ex, "failed")
    val snap = buffer.snapshot()
    assertThat(snap).hasSize(1)
    assertThat(snap[0].level).isEqualTo(Log.ERROR)
    assertThat(snap[0].message).contains("failed")
    assertThat(snap[0].throwable).isSameInstanceAs(ex)
  }

  @Test
  fun `drops entries below minPriority`() {
    Timber.uprootAll()
    Timber.plant(MemoryBufferTree(buffer = buffer, minPriority = Log.WARN))
    Timber.d("debug drop")
    Timber.i("info drop")
    Timber.w("warn keep")
    Timber.e("error keep")
    val snap = buffer.snapshot()
    assertThat(snap.map { it.message }).containsExactly("warn keep", "error keep")
  }

  @Test
  fun `default minPriority captures everything`() {
    Timber.uprootAll()
    Timber.plant(MemoryBufferTree(buffer = buffer))
    Timber.v("v")
    Timber.d("d")
    Timber.e("e")
    assertThat(buffer.snapshot()).hasSize(3)
  }
}
