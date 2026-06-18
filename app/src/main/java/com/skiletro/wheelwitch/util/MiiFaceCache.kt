package com.skiletro.wheelwitch.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.skiletro.wheelwitch.util.MiiFaceCache.MAX_CACHE_BYTES
import com.skiletro.wheelwitch.util.MiiFaceCache.evictIfNeeded
import com.skiletro.wheelwitch.util.MiiFaceCache.init
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
 *
 * All public functions are `@Synchronized` on the singleton instance so
 * concurrent puts/gets/evictions share a single monitor.
 */
object MiiFaceCache {
    private const val DIR_NAME = "mii_faces"
    private const val MAX_CACHE_BYTES = 50L * 1024 * 1024
    private const val EVICT_EVERY_N_PUTS = 20
    private const val PNG_COMPRESS_QUALITY = 100

    private lateinit var cacheDir: File
    private val putCounter = AtomicInteger(0)

    @Synchronized
    fun init(context: Context) {
        cacheDir = File(context.cacheDir, DIR_NAME).also { it.mkdirs() }
    }

    /**
     * Test/init hook: point the cache at an arbitrary directory.
     *
     * [context] is accepted to mirror [init]'s signature but is not used;
     * the caller-supplied [target] directory is the only thing needed.
     */
    @Synchronized
    fun initWith(@Suppress("UNUSED_PARAMETER") context: Context, target: File) {
        target.mkdirs()
        cacheDir = target
    }

    /**
     * Returns the cached bitmap for [rflDataBase64], or null if absent.
     *
     * Touches the file's last-modified time on hit so [evictIfNeeded]
     * evicts least-recently-used entries (LRU by access time, not insert
     * time). The side effect is why this is named `getAndTouch` rather
     * than `get`.
     */
    @Synchronized
    fun getAndTouch(rflDataBase64: String): Bitmap? {
        val file = fileFor(rflDataBase64)
        if (!file.exists()) return null
        file.setLastModified(System.currentTimeMillis())
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    @Synchronized
    fun put(rflDataBase64: String, bitmap: Bitmap) {
        val file = fileFor(rflDataBase64)
        if (file.exists()) return
        // PNG is lossless; the quality argument is ignored by the encoder.
        file.outputStream()
            .use { bitmap.compress(Bitmap.CompressFormat.PNG, PNG_COMPRESS_QUALITY, it) }
        if (putCounter.incrementAndGet() % EVICT_EVERY_N_PUTS == 0) {
            evictIfNeeded()
        }
    }

    @Synchronized
    fun clear() {
        clear(cacheDir)
    }

    /** Removes every file directly under [dir]. Safe to call before [init]. */
    @Synchronized
    fun clear(dir: File) {
        dir.listFiles()?.forEach { it.delete() }
    }

    @Synchronized
    fun cacheSize(): Long = cacheSize(cacheDir)

    /** Returns the sum of file lengths under [dir], or 0 if it doesn't exist. */
    @Synchronized
    fun cacheSize(dir: File): Long = com.skiletro.wheelwitch.util.cacheSize(dir)

    /** Returns `cacheDir/$sha256(rflData).png`. */
    private fun fileFor(rflDataBase64: String): File {
        val hash = sha256Hex(Base64.getDecoder().decode(rflDataBase64))
        return File(cacheDir, "$hash.png")
    }

    /**
     * Drops oldest files by last-modified time until the total size is at or
     * below [MAX_CACHE_BYTES]. No-op when the cache is already within budget.
     */
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
