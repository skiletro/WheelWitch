package com.skiletro.wheelwitch.util

import android.util.Log

/**
 * A single captured log entry, held in [LogBuffer] and serialised by [LogExporter].
 *
 * Note: when [throwable] is non-null, [message] already includes the rendered stack trace
 * appended by Timber's [Timber.Tree] dispatch — consumers must not render [throwable]
 * separately, or the stack trace will be written twice.
 */
data class LogEntry(
  val timestampMillis: Long,
  val level: Int,
  val tag: String?,
  val message: String,
  val throwable: Throwable? = null,
) {
  /** Short uppercase label for [level] (e.g. "ERROR"). Returns "?" for unknown levels. */
  fun levelLabel(): String =
      when (level) {
        Log.VERBOSE -> "V"
        Log.DEBUG -> "D"
        Log.INFO -> "I"
        Log.WARN -> "W"
        Log.ERROR -> "E"
        Log.ASSERT -> "A"
        else -> "?"
      }
}
