package com.skiletro.wheelwitch.util.log

import android.util.Log
import timber.log.Timber

/** Release-build Timber tree: forwards WARN/ERROR to logcat, drops INFO/DEBUG. Open for test subclassing. */
open class AppReleaseLogTree : Timber.Tree() {
  override fun isLoggable(tag: String?, priority: Int): Boolean = priority >= Log.WARN

  override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
    if (priority == Log.ERROR) {
      Log.e(tag, message, t)
    } else {
      Log.w(tag, message, t)
    }
  }
}
