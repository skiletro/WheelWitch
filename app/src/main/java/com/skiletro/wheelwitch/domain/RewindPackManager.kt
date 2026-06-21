package com.skiletro.wheelwitch.domain

import android.content.Context
import com.skiletro.wheelwitch.data.DolphinTree
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.util.io.DownloadProgress
import com.skiletro.wheelwitch.util.io.FileDownloader
import com.skiletro.wheelwitch.util.net.HttpClientProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Owns the install/update flow for the Retro Rewind Pack.
 *
 * Reads the local pack version from [DolphinTree.readVersion] (the
 * `pack/version.txt` inside the SAF tree) and compares it against
 * the server manifest; performs full or incremental installs by
 * downloading the pack zip to [Context.getCacheDir] (where
 * `java.io.File` works) and then streaming it into the SAF tree via
 * [DolphinTree.extractZipToPack]. Writes the new version file *only
 * after* a successful extract — see PLAN §"write `version.txt` after a
 * successful extract".
 *
 * The downloader is [FileDownloader] (the hand-rolled OkHttp wrapper
 * that the project standardized on, per PLAN §"Downloader decision").
 * The pack zip is multi-MB so we use the
 * [HttpClientProvider.largeDownloadClient] (60s read timeout).
 */
class RewindPackManager(
  private val context: Context,
  private val tree: DolphinTree,
) {
  /**
   * Reads the local pack version and the server manifest and returns
   * the appropriate [PackStatus]. If the server is unreachable,
   * returns [PackStatus.Installed] when a local version exists or
   * [PackStatus.NotInstalled] otherwise.
   */
  suspend fun checkStatus(): PackStatus = withContext(Dispatchers.IO) {
    val local = tree.readVersion()
    val server = VersionFileParser.fetchServerInfo().getOrNull()
    if (server == null) {
      Timber.tag(TAG).w("Server info unavailable; localVersion=%s", local)
      return@withContext if (local != null) {
        PackStatus.Installed(local)
      } else {
        PackStatus.NotInstalled
      }
    }
    when {
      local == null -> {
        Timber.tag(TAG).d("Not installed; server latest=%s", server.latestVersion)
        PackStatus.NotInstalled
      }
      local >= server.latestVersion -> {
        Timber.tag(TAG)
          .d("Up to date: local=%s server=%s", local, server.latestVersion)
        PackStatus.UpToDate(local, server.latestVersion)
      }
      else -> {
        Timber.tag(TAG)
          .i("Update available: %s -> %s", local, server.latestVersion)
        PackStatus.UpdateAvailable(local, server.latestVersion, server)
      }
    }
  }

  /**
   * Downloads the full pack zip and extracts it into the SAF tree.
   * Always overwrites the local install — use this for a fresh install
   * or to recover from a corrupted state.
   */
  suspend fun installLatest(onProgress: (DownloadProgress) -> Unit): Result<Unit> = runCatching {
    val server = VersionFileParser.fetchServerInfo().getOrThrow()
    Timber.tag(TAG).i("Starting full install of %s", server.latestVersion)
    performInstall(VersionFileParser.getFullZipUrl(), onProgress)
    tree.writeVersion(server.latestVersion)
  }

  /**
   * Performs the smallest set of incremental updates that takes the
   * local pack from its current version to the server's latest
   * version. Falls back to a full reinstall if the local version is
   * missing or older than [MIN_INCREMENTAL_VERSION] — the pack format
   * changed incompatibly around that point, so partial updates are
   * not safe (see PLAN §"Min reinstall version").
   */
  suspend fun update(onProgress: (DownloadProgress) -> Unit): Result<Unit> = runCatching {
    val local = tree.readVersion()
    val server = VersionFileParser.fetchServerInfo().getOrThrow()
    val minVersion =
      SemVersion.parse(MIN_INCREMENTAL_VERSION)
        ?: error("Invalid MIN_INCREMENTAL_VERSION constant")
    if (local == null || local < minVersion) {
      Timber.tag(TAG)
        .i("Local version %s is below %s; doing full reinstall", local, minVersion)
      performInstall(VersionFileParser.getFullZipUrl(), onProgress)
    } else {
      val steps =
        server.allUpdates
          .filter { it.version > local && it.version <= server.latestVersion }
          .sortedBy { it.version }
      Timber.tag(TAG)
        .i("Applying %d incremental update steps from %s to %s", steps.size, local, server.latestVersion)
      for (step in steps) {
        performInstall(step.url, onProgress)
      }
    }
    tree.writeVersion(server.latestVersion)
  }

  /**
   * Downloads a pack zip to [Context.getCacheDir], extracts it into
   * the SAF tree's pack directory, and deletes the temp file. The
   * `onProgress` callback receives download progress only; extraction
   * progress is not surfaced (extraction is fast relative to download).
   */
  private suspend fun performInstall(
    url: String,
    onProgress: (DownloadProgress) -> Unit,
  ) {
    val zipFile = File(context.cacheDir, PACK_ZIP_NAME)
    FileDownloader.downloadToFile(
      url = url,
      targetFile = zipFile,
      onProgress = onProgress,
      client = HttpClientProvider.largeDownloadClient,
    )
    try {
      tree.extractZipToPack(zipFile) { /* extraction progress not surfaced */ }
    } finally {
      zipFile.delete()
    }
  }

  private companion object {
    /** Local versions older than this get a full reinstall, not an incremental update. */
    const val MIN_INCREMENTAL_VERSION = "3.2.6"

    /** Cached pack zip filename inside [Context.getCacheDir]. */
    const val PACK_ZIP_NAME = "RetroRewind.zip"

    const val TAG = "RewindPack"
  }
}
