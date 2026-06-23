package com.skiletro.wheelwitch.util.log

import android.content.Context
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import java.io.File
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class LogExporterTest {

  @TempDir lateinit var cacheDir: File

  private lateinit var context: Context
  private lateinit var logsDir: File

  @BeforeEach
  fun setUp() {
    LogBuffer.default.clear()
    context = mockk(relaxed = true)
    every { context.cacheDir } returns cacheDir
    logsDir = File(cacheDir, "logs").apply { mkdirs() }
  }

  @AfterEach
  fun tearDown() {
    LogBuffer.default.clear()
  }

  @Test
  fun `report does not contain empty-placeholder when buffer has entries`() {
    LogBuffer.default.write(LogEntry(0L, android.util.Log.INFO, "T", "hello"))
    val report = LogExporter.buildReport(context)
    assertThat(report).contains("hello")
    assertThat(report).doesNotContain("(no log entries captured)")
  }

  @Test
  fun `report shows empty placeholder when buffer is empty and no on-disk log`() {
    val report = LogExporter.buildReport(context)
    assertThat(report).contains("(no log entries captured)")
  }

  @Test
  fun `report includes active on-disk log under labelled section`() {
    File(logsDir, "wheelwitch.log").writeText("1700000000000 I/Old: from disk\n")
    val report = LogExporter.buildReport(context)
    assertThat(report).contains("--- on-disk log: wheelwitch.log ---")
    assertThat(report).contains("1700000000000 I/Old: from disk")
  }

  @Test
  fun `report includes rotated and active on-disk logs in chronological order`() {
    File(logsDir, "wheelwitch.log.1").writeText("older line\n")
    File(logsDir, "wheelwitch.log").writeText("newer line\n")
    val report = LogExporter.buildReport(context)
    val rotatedIdx = report.indexOf("--- on-disk log: wheelwitch.log.1 ---")
    val activeIdx = report.indexOf("--- on-disk log: wheelwitch.log ---")
    assertThat(rotatedIdx).isGreaterThan(-1)
    assertThat(activeIdx).isGreaterThan(rotatedIdx)
    assertThat(report).contains("older line")
    assertThat(report).contains("newer line")
  }

  @Test
  fun `report still includes on-disk log when in-memory buffer is empty`() {
    File(logsDir, "wheelwitch.log").writeText("persisted after process death\n")
    val report = LogExporter.buildReport(context)
    assertThat(report).contains("(no log entries captured)")
    assertThat(report).contains("--- on-disk log: wheelwitch.log ---")
    assertThat(report).contains("persisted after process death")
  }

  @Test
  fun `report omits on-disk section when neither file exists`() {
    val report = LogExporter.buildReport(context)
    assertThat(report).doesNotContain("on-disk log")
  }

  @Test
  fun `report appends trailing newline when on-disk file lacks one`() {
    File(logsDir, "wheelwitch.log").writeText("no trailing newline")
    val report = LogExporter.buildReport(context)
    assertThat(report).endsWith("\n")
  }
}
