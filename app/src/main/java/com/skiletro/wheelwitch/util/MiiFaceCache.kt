package com.skiletro.wheelwitch.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.atomic.AtomicInteger

/**
 * On-disk LRU cache of decoded Mii face bitmaps, keyed by SHA-256 of the
 * RFL (Mii data) payload.
 *
 * The cache is bounded to [MAX_CACHE_BYTES]; when a put pushes the total
 * over the limit, the least-recently-modified files are evicted.
 */
object MiiFaceCache {
    private const val DIR_NAME = "mii_faces"
    private const val MAX_CACHE_BYTES = 50L * 1024 * 1024
    private const val EVICT_EVERY_N_PUTS = 20

    private lateinit var cacheDir: File
    private val putCounter = AtomicInteger(0)

    @Synchronized
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, DIR_NAME).also { it.mkdirs() }
    }

    /** Test/init hook: point the cache at an arbitrary directory. */
    @Synchronized
    fun initWith(@Suppress("UNUSED_PARAMETER") context: Context, target: File) {
        target.mkdirs()
        cacheDir = target
    }

    @Synchronized
    fun get(rflDataBase64: String): Bitmap? {
        val file = fileFor(rflDataBase64)
        if (!file.exists()) return null
        file.setLastModified(System.currentTimeMillis())
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    @Synchronized
    fun put(rflDataBase64: String, bitmap: Bitmap) {
        val file = fileFor(rflDataBase64)
        if (file.exists()) return
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        if (putCounter.incrementAndGet() % EVICT_EVERY_N_PUTS == 0) {
            evictIfNeeded()
        }
    }

    @Synchronized
    fun clear() {
        clear(cacheDir)
    }

    /** Static helper: clear all files in [dir]. */
    @JvmStatic
    fun clear(dir: File) {
        dir.listFiles()?.forEach { it.delete() }
    }

    @Synchronized
    fun cacheSize(): Long {
        return com.skiletro.wheelwitch.ui.components.cacheSize(cacheDir)
    }

    /** Static helper: sum of file lengths in [dir], or 0 if it doesn't exist. */
    @JvmStatic
    fun cacheSize(dir: File): Long {
        return com.skiletro.wheelwitch.ui.components.cacheSize(dir)
    }

    private fun fileFor(rflDataBase64: String): File {
        val hash = sha256Hex(Base64.getDecoder().decode(rflDataBase64))
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
