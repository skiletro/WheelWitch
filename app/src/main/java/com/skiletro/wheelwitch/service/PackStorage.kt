package com.skiletro.wheelwitch.service

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.util.zip.ZipFile

class PackStorage(private val context: Context, private val rootUri: Uri) {
    private val resolver get() = context.contentResolver
    private val rootDoc: DocumentFile by lazy { DocumentFile.fromTreeUri(context, rootUri)!! }
    private val rootPath: String? = resolveToRealPath(rootUri)

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
                                input.copyTo(output, 8192)
                            }
                        }
                    }
                } else {
                    if (!entry.isDirectory) {
                        ensureDocDirs(name)
                        val doc = getOrCreateDoc(name)
                        zf.getInputStream(entry).use { input ->
                            resolver.openOutputStream(doc.uri)?.use { output ->
                                input.copyTo(output, 8192)
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

    fun getFileUri(childPath: String): Uri? {
        val file = resolveDirect(childPath)
        if (file?.exists() == true) return Uri.fromFile(file)
        return resolveDoc(childPath)?.uri
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

    companion object {
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
}
