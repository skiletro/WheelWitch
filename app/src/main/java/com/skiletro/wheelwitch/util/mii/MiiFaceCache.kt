package com.skiletro.wheelwitch.util.mii

import android.content.Context
import com.skiletro.wheelwitch.util.io.cacheSize
import java.io.File

/**
 * On-disk location shared between the Coil [coil3.ImageLoader] disk cache
 * and the Settings cache size/clear UI. The runtime Mii face cache
 * itself is owned by Coil (in-memory + on-disk, configured in
 * [com.skiletro.wheelwitch.WheelWitchApp]); this object only retains
 * the on-disk helpers so the Settings screen can report and clear the
 * cache without reaching into Coil's internals.
 *
 * The disk directory is `<cacheDir>/mii_faces/`. [init] / [initWith]
 * just record where the directory lives; they are idempotent and safe
 * to call multiple times.
 */
object MiiFaceCache {
  const val DIR_NAME = "mii_faces"

  private var cacheDir: File? = null

  /** The on-disk directory used by the Mii face cache. Null until [init] (or [initWith]) is called. */
  val directory: File?
    get() = cacheDir

  @Synchronized
  fun init(context: Context) {
    val dir = File(context.cacheDir, DIR_NAME).also { it.mkdirs() }
    cacheDir = dir
  }

  /** Test/init hook: point the cache at an arbitrary directory. */
  @Synchronized
  fun initWith(target: File) {
    target.mkdirs()
    cacheDir = target
  }

  @Synchronized
  fun clear() {
    val dir = cacheDir ?: return
    clear(dir)
  }

  /** Removes every file directly under [dir]. Safe to call before [init]. */
  @Synchronized
  fun clear(dir: File) {
    dir.listFiles()?.forEach { it.delete() }
  }

  @Synchronized
  fun cacheSize(): Long {
    val dir = cacheDir ?: return 0L
    return cacheSize(dir)
  }

  /** Returns the sum of file lengths under [dir], or 0 if it doesn't exist. */
  @Synchronized
  fun cacheSize(dir: File): Long = com.skiletro.wheelwitch.util.io.cacheSize(dir)
}
