package com.skiletro.wheelwitch.service

import java.net.HttpURLConnection
import java.net.URL

data class UpdateEntry(
    val version: SemVersion,
    val url: String,
    val path: String,
    val description: String
)

data class DeletionEntry(
    val version: SemVersion,
    val path: String
)

object VersionFileParser {
    private const val RR_BASE = "https://update.rwfc.net/"
    private const val VERSION_URL = "${RR_BASE}RetroRewind/RetroRewindVersion.txt"
    private const val DELETE_URL = "${RR_BASE}RetroRewind/RetroRewindDelete.txt"
    private const val FULL_ZIP_URL = "${RR_BASE}RetroRewind/zip/RetroRewind.zip"

    data class ServerInfo(
        val latestVersion: SemVersion,
        val allUpdates: List<UpdateEntry>,
        val deletions: List<DeletionEntry>
    )

    fun fetchServerInfo(): Result<ServerInfo> = runCatching {
        val updates = fetchUpdates()
        if (updates.isEmpty()) error("No versions found on server")
        val deletions = fetchDeletions()
        val latestEntry = updates.maxBy { it.version }
        ServerInfo(latestVersion = latestEntry.version, allUpdates = updates, deletions = deletions)
    }

    private fun fetchUpdates(): List<UpdateEntry> {
        val text = fetchUrl(VERSION_URL)
        return text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(" ", limit = 4)
                if (parts.size < 4) return@mapNotNull null
                val version = parts[0].trim()
                val url = parts[1].trim()
                val path = parts[2].trim()
                val description = parts[3].trim()
                val parsed = SemVersion.parse(version) ?: return@mapNotNull null
                UpdateEntry(parsed, url, path, description)
            }
    }

    private fun fetchDeletions(): List<DeletionEntry> = runCatching {
        val text = fetchUrl(DELETE_URL)
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(" ", limit = 2)
                if (parts.size < 2) return@mapNotNull null
                val version = SemVersion.parse(parts[0].trim()) ?: return@mapNotNull null
                DeletionEntry(version, parts[1].trim())
            }
    }.getOrDefault(emptyList())

    fun getFullZipUrl(): String = FULL_ZIP_URL

    private fun fetchUrl(urlString: String): String {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { it.readText() }
    }
}

data class SemVersion(val major: Int, val minor: Int, val patch: Int, val preRelease: String? = null) :
    Comparable<SemVersion> {

    override fun compareTo(other: SemVersion): Int {
        major.compareTo(other.major).let { if (it != 0) return it }
        minor.compareTo(other.minor).let { if (it != 0) return it }
        patch.compareTo(other.patch).let { if (it != 0) return it }
        if (preRelease == null && other.preRelease != null) return 1
        if (preRelease != null && other.preRelease == null) return -1
        return 0
    }

    override fun toString(): String {
        return if (preRelease != null) "$major.$minor.$patch-$preRelease" else "$major.$minor.$patch"
    }

    companion object {
        fun parse(text: String): SemVersion? {
            val cleaned = text.trimStart('v', 'V')
            val dashIdx = cleaned.indexOf('-')
            val versionPart = if (dashIdx >= 0) cleaned.substring(0, dashIdx) else cleaned
            val preRelease = if (dashIdx >= 0) cleaned.substring(dashIdx + 1) else null
            val parts = versionPart.split(".")
            if (parts.size < 3) return null
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null
            return SemVersion(major, minor, patch, preRelease)
        }
    }
}
