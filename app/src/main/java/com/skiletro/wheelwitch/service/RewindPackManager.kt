package com.skiletro.wheelwitch.service

import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class PackStatus {
    data object NotInstalled : PackStatus()
    data class Installed(val version: SemVersion) : PackStatus()
    data class UpdateAvailable(
        val currentVersion: SemVersion,
        val latestVersion: SemVersion,
        val serverInfo: VersionFileParser.ServerInfo
    ) : PackStatus()

    data object UpToDate : PackStatus()
}

sealed class ProgressInfo {
    data class Checking(val message: String) : ProgressInfo()
    data class Downloading(val progress: Float, val message: String) : ProgressInfo()
    data class Extracting(val progress: Float) : ProgressInfo()
    data class ApplyingUpdate(
        val index: Int, val total: Int,
        val description: String, val progress: Float
    ) : ProgressInfo()
}

object RewindPackManager {
    private const val VERSION_FILE = "RetroRewind6/version.txt"
    private const val FOLDER_NAME = "RetroRewind6"
    private const val MIN_FULL_REINSTALL_VERSION = "3.2.6"

    private val cacheDir: File? get() = null // set by initCacheDir

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
            PackStatus.UpToDate
        } else {
            PackStatus.UpdateAvailable(localVersion, serverInfo.latestVersion, serverInfo)
        }
    }

    suspend fun freshInstall(
        storage: PackStorage,
        onProgress: (ProgressInfo) -> Unit
    ): Result<SemVersion> = runCatching {
        val zipUrl = VersionFileParser.getFullZipUrl()
        val serverInfo = VersionFileParser.fetchServerInfo().getOrNull()
        val targetVersion = serverInfo?.latestVersion ?: error("Could not fetch version info")

        onProgress(ProgressInfo.Downloading(0f, "Downloading Retro Rewind Pack..."))
        val zipFile = downloadFile(zipUrl) { progress ->
            onProgress(ProgressInfo.Downloading(progress, "Downloading Retro Rewind Pack..."))
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
        serverInfo: VersionFileParser.ServerInfo,
        currentVersion: SemVersion,
        onProgress: (ProgressInfo) -> Unit
    ): Result<SemVersion> = runCatching {
        val minVersion = SemVersion.parse(MIN_FULL_REINSTALL_VERSION)
            ?: error("Invalid min version constant")
        if (currentVersion < minVersion) {
            onProgress(ProgressInfo.Checking("Version too old, performing full reinstall..."))
            return@runCatching freshInstall(storage, onProgress).getOrThrow()
        }

        val updatesToApply = serverInfo.allUpdates
            .filter { it.version > currentVersion }
            .sortedBy { it.version }

        val deletionEntries = serverInfo.deletions
            .filter { it.version > currentVersion && it.version <= (updatesToApply.lastOrNull()?.version ?: currentVersion) }

        onProgress(ProgressInfo.Checking("Applying file deletions..."))
        for (deletion in deletionEntries) {
            storage.deleteFile(deletion.path)
        }

        for ((i, update) in updatesToApply.withIndex()) {
            val progressMsg = "${update.description} (${i + 1}/${updatesToApply.size})"
            onProgress(ProgressInfo.Downloading(0f, "Downloading: $progressMsg"))

            val zipFile = downloadFile(update.url) { progress ->
                onProgress(ProgressInfo.Downloading(progress, progressMsg))
            }

            onProgress(ProgressInfo.ApplyingUpdate(i + 1, updatesToApply.size, update.description, 0f))
            storage.extractZip(zipFile) { progress ->
                onProgress(ProgressInfo.ApplyingUpdate(i + 1, updatesToApply.size, update.description, progress))
            }

            writeLocalVersion(storage, update.version)
            zipFile.delete()
        }

        serverInfo.latestVersion
    }

    private fun readLocalVersion(storage: PackStorage): SemVersion? {
        val text = storage.readFile(VERSION_FILE) ?: return null
        return SemVersion.parse(text.trim())
    }

    private fun writeLocalVersion(storage: PackStorage, version: SemVersion) {
        storage.writeFile(VERSION_FILE, version.toString())
    }

    private fun downloadFile(urlString: String, onProgress: (Float) -> Unit): File {
        val cacheBase = tempCacheDir ?: error("Cache not initialized. Call initCacheDir first.")
        cacheBase.mkdirs()
        val tempFile = File.createTempFile("download_", ".zip", cacheBase)

        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.connect()

        val totalBytes = connection.contentLengthLong
        val inputStream = connection.inputStream
        val outputStream = FileOutputStream(tempFile)

        val buffer = ByteArray(8192)
        var bytesRead: Int
        var totalRead = 0L
        var lastReportedProgress = -1f

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalRead += bytesRead
                val progress = if (totalBytes > 0) {
                    (totalRead.toFloat() / totalBytes)
                } else {
                    -1f
                }
                if (progress >= 0f && (progress - lastReportedProgress) >= 0.01f) {
                    lastReportedProgress = progress
                    onProgress(progress)
                }
            }
            onProgress(1f)
        } finally {
            outputStream.close()
            inputStream.close()
        }

        return tempFile
    }
}
