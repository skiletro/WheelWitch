package com.skiletro.wheelwitch.domain

import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.network.VersionFileParser
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Determines the current pack status by comparing a hardcoded local
 * version file against the server's manifest.
 *
 * The install / update logic has been ripped out for a planned rewrite
 * — only [checkStatus] remains. The local version file path is fixed
 * at `<storageRoot>/RetroRewind6/version.txt`; the future rewrite
 * will reintroduce the storage picker and a real `PackStorage` layer.
 */
object RewindPackManager {
  /** Interim default storage root. Replaced in Layer 4 by [com.skiletro.wheelwitch.data.DolphinPaths.physicalRoot]. */
  private const val DEFAULT_ROOT = "/storage/emulated/0"

  /**
   * Reads the local version file and the server manifest, then returns
   * the appropriate [PackStatus]. If the server is unreachable, returns
   * [PackStatus.Installed] when a local version exists or
   * [PackStatus.NotInstalled] otherwise. A `SecurityException` from
   * reading the local file is treated as "no local version".
   *
   * [storageRoot] overrides the default hardcoded root, used by tests.
   */
  suspend fun checkStatus(storageRoot: String = DEFAULT_ROOT): PackStatus =
    withContext(Dispatchers.IO) {
      val localVersion = readLocalVersion(storageRoot)
      val serverInfo = VersionFileParser.fetchServerInfo().getOrNull()

      if (serverInfo == null) {
        Timber.tag("RewindPack").w("Server info unavailable; localVersion=%s", localVersion)
        return@withContext if (localVersion != null) {
          PackStatus.Installed(localVersion)
        } else {
          PackStatus.NotInstalled
        }
      }

      when {
        localVersion == null -> {
          Timber.tag("RewindPack").d("Not installed; server latest=%s", serverInfo.latestVersion)
          PackStatus.NotInstalled
        }

        localVersion >= serverInfo.latestVersion -> {
          Timber.tag("RewindPack")
            .d("Up to date: local=%s server=%s", localVersion, serverInfo.latestVersion)
          PackStatus.UpToDate(localVersion, serverInfo.latestVersion)
        }

        else -> {
          Timber.tag("RewindPack")
            .i("Update available: %s -> %s", localVersion, serverInfo.latestVersion)
          PackStatus.UpdateAvailable(localVersion, serverInfo.latestVersion, serverInfo)
        }
      }
    }

  private fun readLocalVersion(storageRoot: String): SemVersion? {
    val file = File(storageRoot, "RetroRewind6/version.txt")
    if (!file.exists()) return null
    val text =
      runCatching { file.readText().trim() }
        .onFailure {
          Timber.tag("RewindPack")
            .w(it, "Failed to read local version file at %s", file.absolutePath)
        }
        .getOrNull() ?: return null
    if (text.isEmpty()) return null
    return SemVersion.parse(text)
  }
}
