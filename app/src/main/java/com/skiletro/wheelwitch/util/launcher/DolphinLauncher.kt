package com.skiletro.wheelwitch.util.launcher

import android.content.Context
import timber.log.Timber

/**
 * Constants and helpers for the Dolphin Emulator package.
 *
 * The launch / RR.json / My Stuff logic has been ripped out for a
 * planned rewrite — only the package constants and
 * [isDolphinInstalled] remain. The launch helper will return a
 * `NotImplementedError` failure for now.
 */
object DolphinLauncher {
    const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
    const val DOLPHIN_MAIN_ACTIVITY = "$DOLPHIN_PACKAGE.ui.main.MainActivity"

    /** Name of the launch descriptor that used to be written to the storage root. */
    const val RR_JSON_NAME = "RR.json"

    /** Returns true if the Dolphin package is installed on the device. */
    fun isDolphinInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(DOLPHIN_PACKAGE, 0)
            true
        } catch (e: Exception) {
            Timber.tag("DolphinLauncher").d(e, "Dolphin package not installed")
            false
        }
    }

    /**
     * Launch helper. The full launch flow is not implemented yet — see
     * TODO in the package-level kdoc. This returns a failure so callers
     * surface the message without crashing.
     */
    fun launchDolphin(context: Context, jsonFilePath: String): Result<Unit> =
        Result.failure(NotImplementedError("RR launch not implemented — see TODO"))
}
