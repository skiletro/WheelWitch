package com.skiletro.wheelwitch.util.log

/**
 * Thread-safe fixed-capacity ring buffer of [LogEntry]s.
 *
 * When the buffer is full, the oldest entry is dropped. Used by [MemoryBufferTree]
 * to back the in-memory "report bug" export and by tests to exercise overflow behaviour.
 */
class LogBuffer(private val capacity: Int = DEFAULT_CAPACITY) {
  init {
    require(capacity > 0) { "capacity must be > 0" }
  }

  private val entries = ArrayDeque<LogEntry>(capacity)
  private val lock = Any()

  /** Appends [entry], dropping the oldest entry if the buffer is at capacity. */
  fun write(entry: LogEntry) {
    synchronized(lock) {
      if (entries.size >= capacity) entries.removeFirst()
      entries.addLast(entry)
    }
  }

  /** Returns a stable snapshot of the buffer in insertion order (oldest first). */
  fun snapshot(): List<LogEntry> {
    synchronized(lock) { return entries.toList() }
  }

  /** Discards all entries. */
  fun clear() {
    synchronized(lock) { entries.clear() }
  }

  /** Current number of entries in the buffer. */
  fun size(): Int = synchronized(lock) { entries.size }

  companion object {
    const val DEFAULT_CAPACITY = 1000

    /** Process-wide buffer used by the app. Tests construct their own [LogBuffer] directly. */
    val default: LogBuffer = LogBuffer()
  }
}
