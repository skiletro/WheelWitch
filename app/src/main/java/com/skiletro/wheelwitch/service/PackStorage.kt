package com.skiletro.wheelwitch.service

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

class PackStorage(private val context: Context, private val rootUri: Uri) {
    private val resolver get() = context.contentResolver
    val rootDocument: DocumentFile get() = DocumentFile.fromTreeUri(context, rootUri)!!

    fun readFile(childPath: String): String? {
        val file = resolveFile(rootDocument, childPath) ?: return null
        return resolver.openInputStream(file.uri)?.use { it.bufferedReader().readText() }
    }

    fun writeFile(childPath: String, content: String) {
        ensureDirectories(childPath)
        val file = getOrCreateFile(childPath)
        resolver.openOutputStream(file.uri)?.use { it.write(content.toByteArray()) }
    }

    fun deleteFile(childPath: String): Boolean {
        val file = resolveFile(rootDocument, childPath) ?: return false
        return file.delete()
    }

    fun extractZip(zipFile: File, onProgress: (Float) -> Unit): Result<Unit> = runCatching {
        var totalEntries = 0
        ZipInputStream(FileInputStream(zipFile)).use { countZip ->
            while (countZip.nextEntry != null) totalEntries++
        }
        if (totalEntries == 0) return@runCatching

        var processed = 0
        FileInputStream(zipFile).use { fis ->
            ZipInputStream(fis).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name.trimStart('/')
                    if (!entry.isDirectory && name.isNotEmpty()) {
                        ensureDirectories(name)
                        val target = getOrCreateFile(name)
                        resolver.openOutputStream(target.uri)?.use { output ->
                            zip.copyTo(output, 8192)
                        }
                    }
                    processed++
                    onProgress(processed.toFloat() / totalEntries)
                    entry = zip.nextEntry
                }
            }
        }
    }

    fun getFileUri(childPath: String): Uri? {
        return resolveFile(rootDocument, childPath)?.uri
    }

    private fun ensureDirectories(path: String) {
        val parts = path.split("/")
        if (parts.size <= 1) return
        var current = rootDocument
        for (i in 0 until parts.size - 1) {
            val existing = current.findFile(parts[i])
            current = existing ?: current.createDirectory(parts[i]) ?: error("Cannot create directory: ${parts[i]}")
        }
    }

    private fun getOrCreateFile(path: String): DocumentFile {
        val existing = resolveFile(rootDocument, path)
        if (existing != null) return existing
        val name = path.split("/").last()
        val parentPath = path.substringBeforeLast("/", "")
        if (parentPath.isNotEmpty()) ensureDirectories(parentPath)
        val parent = if (parentPath.isNotEmpty()) resolveFile(rootDocument, parentPath)!! else rootDocument
        return parent.createFile("application/octet-stream", name) ?: error("Cannot create file: $path")
    }

    private fun resolveFile(parent: DocumentFile, path: String): DocumentFile? {
        val parts = path.split("/")
        var current = parent
        for (part in parts) {
            if (part.isEmpty()) continue
            current = current.findFile(part) ?: return null
        }
        return current
    }
}
