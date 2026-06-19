package com.skiletro.wheelwitch.util

import android.content.Context
import android.util.Log
import com.skiletro.wheelwitch.BuildConfig
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Flushes the in-memory [LogBuffer] to a timestamped text file in the app's
 * `cacheDir/logs/` directory, suitable for attaching to a bug-report email.
 *
 * The output is a plain-text report with a header (app version, git hash,
 * timestamp) followed by one line per captured [LogEntry]. Throwable stacks
 * are inlined into [LogEntry.message] by Timber, so they are rendered as part
 * of the message — the exporter does not re-render [LogEntry.throwable].
 */
object LogExporter {
  private const val DIR_NAME = "logs"
  private const val FILE_PREFIX = "wheelwitch-"
  private const val FILE_EXTENSION = ".log"
  private val TIMESTAMP_FORMAT = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)

  /**
   * Renders the current [LogBuffer] to a new file in `cacheDir/logs/`
   * and returns it. The caller is responsible for sharing or copying the file.
   */
  fun flushToCacheFile(context: Context): File {
    val dir = File(context.cacheDir, DIR_NAME)
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "$FILE_PREFIX${TIMESTAMP_FORMAT.format(Date())}$FILE_EXTENSION")
    file.writeText(buildReport(context))
    return file
  }

  /** Builds the report text. Exposed for tests so they can assert the format. */
  internal fun buildReport(context: Context): String {
    val sb = StringBuilder()
    sb.append("Wheel Witch v${BuildConfig.VERSION_NAME} ")
      .append("(build ${BuildConfig.VERSION_CODE}, git=${BuildConfig.GIT_HASH})\n")
      .append("Exported at ${Date()}\n")
      .append("Cache dir: ${context.cacheDir}\n")
      .append("=".repeat(60))
      .append('\n')
    val entries = LogBuffer.default.snapshot()
    if (entries.isEmpty()) {
      sb.append("(no log entries captured)\n")
    } else {
      for (entry in entries) {
        val tag = entry.tag ?: "?"
        sb.append(entry.timestampMillis)
          .append(' ')
          .append(entry.levelLabel())
          .append('/')
          .append(tag)
          .append(": ")
          .append(entry.message)
          .append('\n')
      }
    }
    return sb.toString()
  }

  /**
   * Returns a one-line human summary of the most recent error in the
   * buffer, for embedding in the bug-report email body.
   */
  fun lastErrorSummary(): String? {
    val lastError = LogBuffer.default.snapshot().lastOrNull { it.level >= Log.ERROR }
    return lastError?.let { entry ->
      val throwableInfo = entry.throwable?.let { t ->
        val msg = t.message
        if (msg.isNullOrBlank()) t.javaClass.simpleName else "${t.javaClass.simpleName}: $msg"
      }
      val base = entry.message.lineSequence().firstOrNull().orEmpty()
      if (throwableInfo != null) "$base | $throwableInfo" else base
    }
  }
}
