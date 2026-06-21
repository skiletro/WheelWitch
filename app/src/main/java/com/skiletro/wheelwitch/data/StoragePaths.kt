package com.skiletro.wheelwitch.data

import java.io.File

/**
 * Hardcoded paths used by the update checker.
 *
 * The full install/launch flow has been ripped out and will be rewritten
 * later. For now, the checker only needs the path to the local version
 * file the pack writes on every successful install. The full
 * `MANAGE_EXTERNAL_STORAGE` flow, SAF picker, and `PackStorage` wrapper
 * are gone — `StoragePaths` is the entire storage layer.
 *
 * Path: `/storage/emulated/0/RetroRewind6/version.txt` (the same
 * location the install flow used to write, relative to the user's
 * chosen SAF tree).
 *
 * The [root] override exists for tests; production callers should
 * rely on the default.
 */
object StoragePaths {
    const val DEFAULT_ROOT = "/storage/emulated/0"

    fun versionFile(root: String = DEFAULT_ROOT): File =
        File(root, "RetroRewind6/version.txt")
}
