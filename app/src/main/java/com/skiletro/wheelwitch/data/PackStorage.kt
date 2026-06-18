package com.skiletro.wheelwitch.data

import android.net.Uri
import android.provider.DocumentsContract
import com.skiletro.wheelwitch.data.PackStorage.Companion.resolveContentUriToPath
import com.skiletro.wheelwitch.data.PackStorage.Companion.resolveTreeUriToPath
import java.io.File
import java.net.URLDecoder
import java.util.zip.ZipFile

/**
 * Reads/writes pack files using direct `java.io.File` I/O under
 * [rootPath]. The app must hold `MANAGE_EXTERNAL_STORAGE` permission
 * for direct file access to external storage on API 30+.
 *
 * URI→path resolution ([resolveContentUriToPath],
 * [resolveTreeUriToPath]) is retained for the SAF folder picker
 * flow: the user picks a content URI, we resolve it to a real path,
 * and all subsequent I/O operates on that path directly.
 */
class PackStorage(val rootPath: String) {

    companion object {
        const val COPY_BUFFER_SIZE = 256 * 1024

        /** Progress callback is invoked at most every N entries to avoid UI spam. */
        private const val PROGRESS_REPORT_EVERY_N_ENTRIES = 50

        private const val PRIMARY_STORAGE_PREFIX = "/storage/emulated/0/"
        private const val RAW_STORAGE_PREFIX = "/storage/raw/"
        private const val VOLUME_STORAGE_PREFIX = "/storage/"

        /**
         * Resolves a document content URI (from `ACTION_OPEN_DOCUMENT`)
         * to a real filesystem path (e.g. "/storage/emulated/0/file.iso").
         */
        fun resolveContentUriToPath(uri: Uri): String? =
            buildPathFromDocId(
                docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull(),
                fallbackPath = uri.path,
            )

        /**
         * Resolves a tree document URI (from `ACTION_OPEN_DOCUMENT_TREE`)
         * to a real filesystem path (e.g. "/storage/emulated/0/RetroRewind6").
         */
        fun resolveTreeUriToPath(treeUri: Uri): String? =
            buildPathFromDocId(
                docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull(),
                fallbackPath = null,
            )

        /**
         * Splits [docId] on `":"` and maps the storage-type prefix to a
         * real filesystem path. URL-decodes the relative portion so paths
         * with spaces (e.g. "primary:Retro%20Rewind") resolve correctly.
         * Returns [fallbackPath] when [docId] is null or has no `:`.
         */
        internal fun buildPathFromDocId(docId: String?, fallbackPath: String?): String? {
            if (docId == null) return fallbackPath
            val parts = docId.split(":")
            if (parts.size < 2) return fallbackPath
            val storageType = parts[0]
            val relativePath = URLDecoder.decode(parts[1], "UTF-8")
            return when {
                storageType.equals("primary", ignoreCase = true) ->
                    "$PRIMARY_STORAGE_PREFIX$relativePath"

                storageType.equals("raw", ignoreCase = true) ->
                    "$RAW_STORAGE_PREFIX$relativePath"

                else ->
                    "$VOLUME_STORAGE_PREFIX$storageType/$relativePath"
            }
        }
    }

    private val root = File(rootPath)

    /** Reads a text file relative to the storage root. Returns null if the file does not exist. */
    fun readFile(childPath: String): String? {
        val file = File(root, childPath)
        return if (file.exists()) file.readText() else null
    }

    /** Writes a text file relative to the storage root, creating parent directories as needed. */
    fun writeFile(childPath: String, content: String) {
        val file = File(root, childPath)
        file.parentFile?.mkdirs()
        file.writeText(content)
    }

    /** Deletes a file relative to the storage root. Returns true if the file existed and was deleted. */
    fun deleteFile(childPath: String): Boolean {
        val file = File(root, childPath)
        return file.exists() && file.delete()
    }

    /** Extracts a zip archive into the storage root, reporting progress as 0..1. */
    fun extractZip(zipFile: File, onProgress: (Float) -> Unit): Result<Unit> = runCatching {
        ZipFile(zipFile).use { zf ->
            val entries = zf.entries()
            val total = zf.size()
            if (total == 0) return@runCatching
            var processed = 0

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.trimStart('/')
                if (name.isEmpty()) continue

                val target = File(root, name)
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    zf.getInputStream(entry).use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output, COPY_BUFFER_SIZE)
                        }
                    }
                }

                processed++
                if (processed % PROGRESS_REPORT_EVERY_N_ENTRIES == 0 || processed == total) {
                    onProgress(processed.toFloat() / total)
                }
            }
        }
    }

    /** Returns true if a file exists at the given path relative to the storage root. */
    fun fileExists(childPath: String): Boolean = File(root, childPath).exists()

    /** Reads raw bytes from a file relative to the storage root. Returns null if the file does not exist. */
    fun readBytes(childPath: String): ByteArray? {
        val file = File(root, childPath)
        return if (file.exists()) file.readBytes() else null
    }

    /** Writes raw bytes to a file relative to the storage root, creating parent directories as needed. */
    fun writeBytes(childPath: String, data: ByteArray) {
        val file = File(root, childPath)
        file.parentFile?.mkdirs()
        file.writeBytes(data)
    }
}