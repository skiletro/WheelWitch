package com.skiletro.wheelwitch.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.zip.ZipFile

/**
 * Reads/writes pack files using direct `java.io.File` path resolution
 * when possible, falling back to SAF [DocumentFile] for metadata
 * operations.
 *
 * Each read/write method first tries [resolveDirect] (a plain `File` under
 * [rootPath]); if that returns null or the file is missing, it falls back
 * to [resolveDoc] (a SAF `DocumentFile`). The fallback exists for storage
 * roots whose content URI cannot be resolved to a real filesystem path
 * (e.g. some USB/OTG volumes).
 */
class PackStorage(private val context: Context, private val rootUri: Uri) {
    private val resolver get() = context.contentResolver
    private val rootDoc: DocumentFile? by lazy { DocumentFile.fromTreeUri(context, rootUri) }
    val rootPath: String? = resolveToRealPath(rootUri)

    companion object {
        const val COPY_BUFFER_SIZE = 256 * 1024

        /** Progress callback is invoked at most every N entries to avoid UI spam. */
        private const val PROGRESS_REPORT_EVERY_N_ENTRIES = 50

        private const val PRIMARY_STORAGE_PREFIX = "/storage/emulated/0/"
        private const val RAW_STORAGE_PREFIX = "/storage/raw/"
        private const val VOLUME_STORAGE_PREFIX = "/storage/"

        /**
         * Resolves a content URI to a real filesystem path
         * (e.g. "/storage/emulated/0/RetroRewind6").
         *
         * Note: for `raw:` URIs (e.g. USB/OTG volumes), the inferred path
         * is best-effort and may not actually exist on the device.
         */
        fun resolveContentUriToPath(uri: Uri): String? =
            buildPathFromDocId(
                docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull(),
                fallbackPath = uri.path,
            )

        private fun resolveToRealPath(treeUri: Uri): String? =
            buildPathFromDocId(
                docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull(),
                fallbackPath = null,
            )

        /**
         * Shared helper for both URI flavors. Splits [docId] on `":"` and
         * maps the storage-type prefix to a real path. Returns [fallbackPath]
         * when [docId] is null or has no `:` separator.
         */
        private fun buildPathFromDocId(docId: String?, fallbackPath: String?): String? {
            if (docId == null) return fallbackPath
            val parts = docId.split(":")
            if (parts.size < 2) return fallbackPath
            val storageType = parts[0]
            val relativePath = parts[1]
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

    /** Reads a text file relative to the storage root. Returns null if the file does not exist. */
    fun readFile(childPath: String): String? {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return file.readText()
        val doc = resolveDoc(childPath) ?: return null
        return resolver.openInputStream(doc.uri)?.use { it.bufferedReader().readText() }
    }

    /** Writes a text file relative to the storage root, creating parent directories as needed. */
    fun writeFile(childPath: String, content: String) {
        val file = resolveDirect(childPath)
        if (file != null) {
            file.parentFile?.mkdirs()
            file.writeText(content)
            return
        }
        ensureDocDirs(childPath)
        val doc = getOrCreateDoc(childPath)
        openOutput(childPath, doc).use { it.write(content.toByteArray()) }
    }

    /** Deletes a file relative to the storage root. Returns true if the file existed and was deleted. */
    fun deleteFile(childPath: String): Boolean {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return file.delete()
        return resolveDoc(childPath)?.delete() ?: false
    }

    /** Extracts a zip archive into the storage root, reporting progress as 0..1. */
    fun extractZip(zipFile: File, onProgress: (Float) -> Unit): Result<Unit> = runCatching {
        ZipFile(zipFile).use { zf ->
            val entries = zf.entries()
            val total = zf.size()
            if (total == 0) return@runCatching
            var processed = 0

            val useDirect = rootPath != null

            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                val name = entry.name.trimStart('/')
                if (name.isEmpty()) continue

                if (useDirect) {
                    val target = File(rootPath, name)
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
                } else {
                    // SAF: directories are created implicitly by ensureDocDirs
                    // below when the first file inside them is written.
                    if (!entry.isDirectory) {
                        ensureDocDirs(name)
                        val doc = getOrCreateDoc(name)
                        zf.getInputStream(entry).use { input ->
                            openOutput(name, doc).use { output ->
                                input.copyTo(output, COPY_BUFFER_SIZE)
                            }
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
    fun fileExists(childPath: String): Boolean {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return true
        return resolveDoc(childPath) != null
    }

    /** Reads raw bytes from a file relative to the storage root. Returns null if the file does not exist. */
    fun readBytes(childPath: String): ByteArray? {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return file.readBytes()
        val doc = resolveDoc(childPath) ?: return null
        return resolver.openInputStream(doc.uri)?.use { it.readBytes() }
    }

    /** Writes raw bytes to a file relative to the storage root, creating parent directories as needed. */
    fun writeBytes(childPath: String, data: ByteArray) {
        val file = resolveDirect(childPath)
        if (file != null) {
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            return
        }
        ensureDocDirs(childPath)
        val doc = getOrCreateDoc(childPath)
        openOutput(childPath, doc).use { it.write(data) }
    }

    /** Opens an output stream on [doc], failing with a descriptive message that includes [childPath]. */
    private fun openOutput(childPath: String, doc: DocumentFile) =
        requireNotNull(resolver.openOutputStream(doc.uri)) { "Failed to open output stream for $childPath" }

    private fun resolveDirect(childPath: String): File? {
        return rootPath?.let { File(it, childPath) }
    }

    private fun resolveDoc(childPath: String): DocumentFile? {
        val parts = childPath.split("/")
        var current = rootDoc ?: return null
        for (part in parts) {
            if (part.isEmpty()) continue
            current = current.findFile(part) ?: return null
        }
        return current
    }

    private fun getOrCreateDoc(path: String): DocumentFile {
        val existing = resolveDoc(path)
        if (existing != null) return existing
        val name = path.split("/").last()
        val parentPath = path.substringBeforeLast("/", "")
        if (parentPath.isNotEmpty()) ensureDocDirs(parentPath)
        val parent = parentDoc(parentPath)
        return parent.createFile("application/octet-stream", name) ?: error("Cannot create file: $path")
    }

    private fun parentDoc(parentPath: String): DocumentFile {
        if (parentPath.isEmpty()) {
            return rootDoc ?: error("Storage root not available")
        }
        return resolveDoc(parentPath) ?: error("Parent not found: $parentPath")
    }

    private fun ensureDocDirs(path: String) {
        val parts = path.split("/")
        if (parts.size <= 1) return
        var current = rootDoc ?: return
        for (i in 0 until parts.size - 1) {
            val existing = current.findFile(parts[i])
            current = existing ?: current.createDirectory(parts[i]) ?: error("Cannot create directory: ${parts[i]}")
        }
    }
}
