package com.skiletro.wheelwitch.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class PackStorage(private val context: Context, private val rootUri: Uri) {
    private val resolver get() = context.contentResolver
    private val rootDoc: DocumentFile by lazy { DocumentFile.fromTreeUri(context, rootUri)!! }
    val rootPath: String? = resolveToRealPath(rootUri)

    companion object {
        const val COPY_BUFFER_SIZE = 262144

        private fun resolveToRealPath(treeUri: Uri): String? {
            val docId = try {
                DocumentsContract.getTreeDocumentId(treeUri)
            } catch (e: Exception) {
                return null
            }
            val parts = docId.split(":")
            if (parts.size < 2) return null
            val storageType = parts[0]
            val relativePath = parts[1]
            return when {
                storageType.equals("primary", ignoreCase = true) ->
                    "/storage/emulated/0/$relativePath"
                else ->
                    "/storage/$storageType/$relativePath"
            }
        }
    }

    fun readFile(childPath: String): String? {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return file.readText()
        val doc = resolveDoc(childPath) ?: return null
        return resolver.openInputStream(doc.uri)?.use { it.bufferedReader().readText() }
    }

    fun writeFile(childPath: String, content: String) {
        val file = resolveDirect(childPath)
        if (file != null) {
            file.parentFile?.mkdirs()
            file.writeText(content)
            return
        }
        ensureDocDirs(childPath)
        val doc = getOrCreateDoc(childPath)
        resolver.openOutputStream(doc.uri)?.use { it.write(content.toByteArray()) }
    }

    fun deleteFile(childPath: String): Boolean {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return file.delete()
        return resolveDoc(childPath)?.delete() ?: false
    }

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
                    if (!entry.isDirectory) {
                        ensureDocDirs(name)
                        val doc = getOrCreateDoc(name)
                        zf.getInputStream(entry).use { input ->
                            resolver.openOutputStream(doc.uri)?.use { output ->
                                input.copyTo(output, COPY_BUFFER_SIZE)
                            }
                        }
                    }
                }

                processed++
                if (processed % 50 == 0 || processed == total) {
                    onProgress(processed.toFloat() / total)
                }
            }
        }
    }

    fun fileExists(childPath: String): Boolean {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return true
        return resolveDoc(childPath) != null
    }

    fun readBytes(childPath: String): ByteArray? {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return file.readBytes()
        val doc = resolveDoc(childPath) ?: return null
        return resolver.openInputStream(doc.uri)?.use { it.readBytes() }
    }

    fun writeBytes(childPath: String, data: ByteArray) {
        val file = resolveDirect(childPath)
        if (file != null) {
            file.parentFile?.mkdirs()
            file.writeBytes(data)
            return
        }
        ensureDocDirs(childPath)
        val doc = getOrCreateDoc(childPath)
        resolver.openOutputStream(doc.uri)?.use { it.write(data) }
    }

    private fun resolveDirect(childPath: String): File? {
        return rootPath?.let { File(it, childPath) }
    }

    private fun resolveDoc(childPath: String): DocumentFile? {
        val parts = childPath.split("/")
        var current = rootDoc
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
        val parent = if (parentPath.isNotEmpty()) resolveDoc(parentPath)!! else rootDoc
        return parent.createFile("application/octet-stream", name) ?: error("Cannot create file: $path")
    }

    private fun ensureDocDirs(path: String) {
        val parts = path.split("/")
        if (parts.size <= 1) return
        var current = rootDoc
        for (i in 0 until parts.size - 1) {
            val existing = current.findFile(parts[i])
            current = existing ?: current.createDirectory(parts[i]) ?: error("Cannot create directory: ${parts[i]}")
        }
    }
}
