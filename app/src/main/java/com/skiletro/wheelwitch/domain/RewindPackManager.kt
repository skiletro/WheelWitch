package com.skiletro.wheelwitch.domain

import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.model.ServerInfo
import com.skiletro.wheelwitch.network.VersionFileParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

object RewindPackManager {
    private const val VERSION_FILE = "RetroRewind6/version.txt"
    private const val FOLDER_NAME = "RetroRewind6"
    private const val MIN_FULL_REINSTALL_VERSION = "3.2.6"
    private const val DOWNLOAD_BUFFER = 262144

    private var tempCacheDir: File? = null

    fun initCacheDir(cache: File) {
        tempCacheDir = File(cache, "rewind_pack_downloads")
        tempCacheDir?.mkdirs()
    }

    suspend fun checkStatus(
        storage: PackStorage
    ): PackStatus {
        val localVersion = readLocalVersion(storage)
        val serverInfo = VersionFileParser.fetchServerInfo().getOrNull()
            ?: return if (localVersion != null) PackStatus.Installed(localVersion) else PackStatus.NotInstalled

        return if (localVersion == null) {
            PackStatus.NotInstalled
        } else if (localVersion >= serverInfo.latestVersion) {
            PackStatus.UpToDate(localVersion, serverInfo.latestVersion)
        } else {
            PackStatus.UpdateAvailable(localVersion, serverInfo.latestVersion, serverInfo)
        }
    }

    suspend fun freshInstall(
        storage: PackStorage,
        onProgress: (ProgressInfo) -> Unit
    ): Result<SemVersion> = runCatching {
        val serverInfo = VersionFileParser.fetchServerInfo().getOrNull()
        val targetVersion = serverInfo?.latestVersion ?: error("Could not fetch version info")

        onProgress(ProgressInfo.Downloading(0f, "Downloading Retro Rewind Pack..."))
        val cacheBase = tempCacheDir ?: error("Cache not initialized")
        cacheBase.mkdirs()
        val zipFile = File.createTempFile("install_", ".zip", cacheBase)
        withContext(Dispatchers.IO) {
            downloadToFile(VersionFileParser.getFullZipUrl(), zipFile) { progress ->
                onProgress(ProgressInfo.Downloading(progress, "Downloading Retro Rewind Pack..."))
            }
        }

        onProgress(ProgressInfo.Extracting(0f))
        storage.extractZip(zipFile) { progress ->
            onProgress(ProgressInfo.Extracting(progress))
        }

        writeLocalVersion(storage, targetVersion)
        zipFile.delete()
        targetVersion
    }

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

        val deletionSteps = serverInfo.deletions
            .filter { it.version > currentVersion && it.version <= (steps.lastOrNull()?.version ?: currentVersion) }

        onProgress(ProgressInfo.Checking("Applying file deletions..."))
        for (deletion in deletionSteps) {
            storage.deleteFile(deletion.path)
        }

        onProgress(ProgressInfo.Checking("Downloading updates..."))
        val cacheBase = tempCacheDir ?: error("Cache not initialized")
        cacheBase.mkdirs()

        coroutineScope {
            val deferred = steps.map { step ->
                async(Dispatchers.IO) {
                    val file = File.createTempFile("update_", ".zip", cacheBase)
                    downloadToFile(step.url, file) { progress ->
                        onProgress(ProgressInfo.Downloading(progress,
                            "${step.description} - downloading"))
                    }
                    step to file
                }
            }

            val results = deferred.awaitAll()

            for ((i, pair) in results.withIndex()) {
                val (step, zipFile) = pair
                onProgress(ProgressInfo.ApplyingUpdate(i + 1, results.size, step.description, 0f))
                storage.extractZip(zipFile) { progress ->
                    onProgress(ProgressInfo.ApplyingUpdate(i + 1, results.size, step.description, progress))
                }
                writeLocalVersion(storage, step.version)
                zipFile.delete()
            }
        }

        serverInfo.latestVersion
    }

    private fun downloadToFile(urlString: String, targetFile: File, onProgress: (Float) -> Unit) {
        val request = Request.Builder().url(urlString).build()
        val response = VersionFileParser.okHttpClient().newCall(request).execute()
        val body = response.body ?: error("No response body")
        val totalBytes = body.contentLength()

        body.byteStream().use { input ->
            targetFile.outputStream().use { output ->
                val buffer = ByteArray(DOWNLOAD_BUFFER)
                var bytesRead: Int
                var totalRead = 0L
                var lastReported = -1f
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                    totalRead += bytesRead
                    if (totalBytes > 0) {
                        val p = totalRead.toFloat() / totalBytes
                        if (p - lastReported >= 0.01f) {
                            lastReported = p
                            onProgress(p)
                        }
                    }
                }
                onProgress(1f)
            }
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
