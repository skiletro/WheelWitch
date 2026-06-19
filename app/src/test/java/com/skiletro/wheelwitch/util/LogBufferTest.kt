package com.skiletro.wheelwitch.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class LogBufferTest {

  @Test
  fun `starts empty`() {
    val buffer = LogBuffer(capacity = 5)
    assertThat(buffer.size()).isEqualTo(0)
    assertThat(buffer.snapshot()).isEmpty()
  }

  @Test
  fun `write appends in order`() {
    val buffer = LogBuffer(capacity = 5)
    val a = entry(1, "a")
    val b = entry(2, "b")
    val c = entry(3, "c")
    buffer.write(a)
    buffer.write(b)
    buffer.write(c)
    assertThat(buffer.snapshot()).containsExactly(a, b, c).inOrder()
    assertThat(buffer.size()).isEqualTo(3)
  }

  @Test
  fun `overflow drops oldest entries`() {
    val buffer = LogBuffer(capacity = 3)
    val a = entry(1, "a")
    val b = entry(2, "b")
    val c = entry(3, "c")
    val d = entry(4, "d")
    val e = entry(5, "e")
    buffer.write(a)
    buffer.write(b)
    buffer.write(c)
    buffer.write(d)
    buffer.write(e)
    assertThat(buffer.snapshot()).containsExactly(c, d, e).inOrder()
    assertThat(buffer.size()).isEqualTo(3)
  }

  @Test
  fun `clear empties the buffer`() {
    val buffer = LogBuffer(capacity = 5)
    buffer.write(entry(1, "a"))
    buffer.write(entry(2, "b"))
    buffer.clear()
    assertThat(buffer.size()).isEqualTo(0)
    assertThat(buffer.snapshot()).isEmpty()
  }

  @Test
  fun `snapshot is decoupled from the buffer`() {
    val buffer = LogBuffer(capacity = 5)
    buffer.write(entry(1, "a"))
    val snap = buffer.snapshot()
    buffer.write(entry(2, "b"))
    assertThat(snap).containsExactly(entry(1, "a"))
  }

  @Test
  fun `capacity must be positive`() {
    runCatching { LogBuffer(capacity = 0) }.also {
      assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
    }
  }

  private fun entry(timestamp: Long, msg: String) =
      LogEntry(timestampMillis = timestamp, level = 0, tag = "T", message = msg)
}
