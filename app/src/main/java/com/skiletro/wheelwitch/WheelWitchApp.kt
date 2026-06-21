package com.skiletro.wheelwitch

import android.app.Application
import android.util.Log
import com.skiletro.wheelwitch.util.AppReleaseLogTree
import com.skiletro.wheelwitch.util.MemoryBufferTree
import com.skiletro.wheelwitch.util.MiiFaceCache
import com.skiletro.wheelwitch.util.OptionalFileTree
import com.skiletro.wheelwitch.util.Prefs
import com.skiletro.wheelwitch.util.PrefsKeys
import java.io.File
import timber.log.Timber

/** Application entry point. Plants Timber trees for logcat, the in-memory bug-report buffer, and the optional on-disk log file. */
class WheelWitchApp : Application() {
  override fun onCreate() {
    super.onCreate()
    MiiFaceCache.init(this)
    val prefs = Prefs.main(this)
    if (BuildConfig.DEBUG) {
      Timber.plant(Timber.DebugTree())
    } else {
      Timber.plant(AppReleaseLogTree())
    }
    Timber.plant(MemoryBufferTree(minPriority = if (BuildConfig.DEBUG) Log.VERBOSE else Log.WARN))
    Timber.plant(
      OptionalFileTree(
        isEnabled = { prefs.getBoolean(PrefsKeys.LOGGING_TO_FILE_KEY, false) },
        logFile = { logFilePath() },
        minPriority = if (BuildConfig.DEBUG) Log.VERBOSE else Log.WARN,
      )
    )
  }

  private fun logFilePath(): File? {
    val dir = File(cacheDir, "logs")
    if (!dir.exists()) dir.mkdirs()
    return File(dir, "wheelwitch.log")
  }
}
