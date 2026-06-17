package com.skiletro.wheelwitch.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest

object MiiFaceCache {
    private const val DIR_NAME = "mii_faces"
    private const val MAX_CACHE_BYTES = 50L * 1024 * 1024

    private lateinit var cacheDir: File

    fun init(context: Context) {
        cacheDir = File(context.cacheDir, DIR_NAME).also { it.mkdirs() }
    }

    fun get(rflDataBase64: String): Bitmap? {
        val file = fileFor(rflDataBase64)
        if (!file.exists()) return null
        file.setLastModified(System.currentTimeMillis())
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun put(rflDataBase64: String, bitmap: Bitmap) {
        val file = fileFor(rflDataBase64)
        if (file.exists()) return
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        evictIfNeeded()
    }

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
    }

    fun cacheSize(): Long {
        if (!cacheDir.exists()) return 0
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0
    }

    private fun fileFor(rflDataBase64: String): File {
        val hash = sha256Hex(android.util.Base64.decode(rflDataBase64, android.util.Base64.DEFAULT))
        return File(cacheDir, "$hash.png")
    }

    private fun evictIfNeeded() {
        val files = cacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return
        var total = files.sumOf { it.length() }
        for (file in files) {
            if (total <= MAX_CACHE_BYTES) break
            total -= file.length()
            file.delete()
        }
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
