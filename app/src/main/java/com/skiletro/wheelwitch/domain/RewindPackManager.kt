package com.skiletro.wheelwitch.domain

import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.model.UpdateEntry
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.util.FileDownloader
import com.skiletro.wheelwitch.util.HttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/** Orchestrates Retro Rewind Pack installation, incremental updates, and status checks. */
object RewindPackManager {
    /** Path of the local version file relative to the storage root (NOT the server-side `RetroRewind/RetroRewindVersion.txt`). */
    internal const val VERSION_FILE = "RetroRewind6/version.txt"
    private const val MIN_FULL_REINSTALL_VERSION = "3.2.6"
    private const val DOWNLOAD_MESSAGE = "Downloading Retro Rewind Pack..."

    private var tempCacheDir: File? = null

    /** Initialises the temp download cache and cleans files older than 24 hours. */
    fun initCacheDir(cache: File) {
        val dir = File(cache, "rewind_pack_downloads")
        tempCacheDir = dir
        dir.mkdirs()
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        dir.listFiles()?.forEach { file ->
            if (file.lastModified() < cutoff) file.delete()
        }
    }

    /** Compares local version against the server to determine [PackStatus]. Returns [Installed] if the server is unreachable. */
    suspend fun checkStatus(
        storage: PackStorage
    ): PackStatus {
        val localVersion = readLocalVersion(storage)
        val serverInfo = VersionFileParser.fetchServerInfo().getOrNull()
        if (serverInfo == null) {
            return if (localVersion != null) PackStatus.Installed(localVersion) else PackStatus.NotInstalled
        }

        return if (localVersion == null) {
            PackStatus.NotInstalled
        } else if (localVersion >= serverInfo.latestVersion) {
            PackStatus.UpToDate(localVersion, serverInfo.latestVersion)
        } else {
            PackStatus.UpdateAvailable(localVersion, serverInfo.latestVersion, serverInfo)
        }
    }

    /**
     * Downloads the full Retro Rewind zip, extracts it, and writes the
     * local version file.
     *
     * Throws if [initCacheDir] has not been called first.
     */
    suspend fun freshInstall(
        storage: PackStorage,
        onProgress: (ProgressInfo) -> Unit
    ): Result<SemVersion> = runCatching {
        val serverInfo = VersionFileParser.fetchServerInfo().getOrNull()
        val targetVersion = serverInfo?.latestVersion ?: error("Could not fetch version info")

        onProgress(ProgressInfo.Downloading(0f, DOWNLOAD_MESSAGE))
        val cacheBase = tempCacheDir ?: error("Cache not initialized")
        cacheBase.mkdirs()
        val zipFile = File.createTempFile("install_", ".zip", cacheBase)
        withContext(Dispatchers.IO) {
            FileDownloader.downloadToFile(
                VersionFileParser.getFullZipUrl(),
                zipFile,
                onProgress = { progress ->
                    onProgress(ProgressInfo.Downloading(progress, DOWNLOAD_MESSAGE))
                },
                client = HttpClientProvider.largeDownloadClient
            )
        }

        onProgress(ProgressInfo.Extracting(0f))
        storage.extractZip(zipFile) { progress ->
            onProgress(ProgressInfo.Extracting(progress))
        }

        writeLocalVersion(storage, targetVersion)
        zipFile.delete()
        targetVersion
    }

    /**
     * Downloads incremental update zips, applies file deletions, then
     * extracts each zip in order, writing `version.txt` after every
     * successful step. Falls back to [freshInstall] for versions below
     * 3.2.6.
     *
     * Throws if [initCacheDir] has not been called first.
     */
    suspend fun incrementalUpdate(
        storage: PackStorage,
        serverInfo: ServerInfo,
        currentVersion: SemVersion,
        onProgress: (ProgressInfo) -> Unit
    ): Result<SemVersion> = runCatching {
        val minVersion = SemVersion.parse(MIN_FULL_REINSTALL_VERSION)
            ?: error("Invalid min version constant")
        if (currentVersion < minVersion) {
            onProgress(ProgressInfo.Checking("Version too old, performing full reinstall..."))
            return@runCatching freshInstall(storage, onProgress).getOrThrow()
        }

        val steps = serverInfo.allUpdates.filter { it.version > currentVersion }
        val deletionSteps = filterDeletions(serverInfo, currentVersion, steps)

        applyDeletions(storage, deletionSteps, onProgress)

        onProgress(ProgressInfo.Checking("Downloading updates..."))
        val cacheBase = tempCacheDir ?: error("Cache not initialized")
        cacheBase.mkdirs()

        // Downloads run in parallel; extraction is sequential and waits for all downloads.
        val results = coroutineScope {
            steps.map { step ->
                async(Dispatchers.IO) { downloadUpdateZip(step, cacheBase, onProgress) }
            }.awaitAll()
        }
        applyUpdateZips(storage, results, onProgress)

        serverInfo.latestVersion
    }

    /**
     * Filters deletions to only those whose version is greater than
     * [currentVersion] and at most the latest step we are updating to.
     * Deletions for versions beyond the latest step are skipped so we do
     * not delete files for updates we are not applying this run.
     */
    private fun filterDeletions(
        serverInfo: ServerInfo,
        currentVersion: SemVersion,
        steps: List<UpdateEntry>,
    ): List<com.skiletro.wheelwitch.model.DeletionEntry> {
        val maxStepVersion = steps.lastOrNull()?.version ?: currentVersion
        return serverInfo.deletions.filter { it.version > currentVersion && it.version <= maxStepVersion }
    }

    private fun applyDeletions(
        storage: PackStorage,
        deletions: List<com.skiletro.wheelwitch.model.DeletionEntry>,
        onProgress: (ProgressInfo) -> Unit,
    ) {
        onProgress(ProgressInfo.Checking("Applying file deletions..."))
        for (deletion in deletions) {
            storage.deleteFile(deletion.path)
        }
    }

    private suspend fun downloadUpdateZip(
        step: UpdateEntry,
        cacheBase: File,
        onProgress: (ProgressInfo) -> Unit,
    ): Pair<UpdateEntry, File> {
        val file = File.createTempFile("update_", ".zip", cacheBase)
        FileDownloader.downloadToFile(
            step.url,
            file,
            onProgress = { progress ->
                onProgress(ProgressInfo.Downloading(progress, "${step.description} - downloading"))
            },
            client = HttpClientProvider.largeDownloadClient
        )
        return step to file
    }

    private suspend fun applyUpdateZips(
        storage: PackStorage,
        results: List<Pair<UpdateEntry, File>>,
        onProgress: (ProgressInfo) -> Unit,
    ) {
        for ((i, pair) in results.withIndex()) {
            val (step, zipFile) = pair
            val displayIndex = i + 1
            onProgress(ProgressInfo.ApplyingUpdate(displayIndex, results.size, step.description, 0f))
            storage.extractZip(zipFile) { progress ->
                onProgress(ProgressInfo.ApplyingUpdate(displayIndex, results.size, step.description, progress))
            }
            writeLocalVersion(storage, step.version)
            zipFile.delete()
        }
    }

    private fun readLocalVersion(storage: PackStorage): SemVersion? {
        val text = storage.readFile(VERSION_FILE) ?: return null
        return SemVersion.parse(text.trim())
    }

    private fun writeLocalVersion(storage: PackStorage, version: SemVersion) {
        storage.writeFile(VERSION_FILE, version.toString())
    }
}
