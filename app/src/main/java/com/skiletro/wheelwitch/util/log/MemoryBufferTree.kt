package com.skiletro.wheelwitch.util.log

import timber.log.Timber

/**
 * Captures every log call into a [LogBuffer] for later export.
 *
 * [minPriority] follows [android.util.Log] priority constants (e.g.
 * [android.util.Log.VERBOSE], [android.util.Log.WARN]). Calls below this priority
 * are silently dropped.
 */
class MemoryBufferTree(
  private val buffer: LogBuffer = LogBuffer.default,
  private val minPriority: Int = android.util.Log.VERBOSE,
) : Timber.Tree() {
  override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= minPriority

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    buffer.write(LogEntry(System.currentTimeMillis(), priority, tag, message, t))
  }
}
