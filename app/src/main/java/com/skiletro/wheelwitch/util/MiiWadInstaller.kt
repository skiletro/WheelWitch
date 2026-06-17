package com.skiletro.wheelwitch.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.skiletro.wheelwitch.util.DolphinLauncher
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

/** Downloads and installs the Mii Channel WAD file for use as a Mii Maker in Dolphin. */
object MiiWadInstaller {
    private const val MII_MAKER_ZIP_URL = "https://filecache45.gamebanana.com/mods/mii_channel_symbols_-_hacs.zip"
    private const val WAD_FILE_NAME = "Mii Channel Symbols - HACS.wad"

    /** Returns the cached WAD file from `cache/mii_maker/`, or null if not yet downloaded. */
    fun getCachedWadFile(context: Context): File? {
        val dir = File(context.cacheDir, "mii_maker")
        val wad = File(dir, WAD_FILE_NAME)
        return if (wad.exists()) wad else null
    }

    /** Downloads the WAD zip from GameBanana, extracts the `.wad` file, and returns it. Validates WAD magic header before returning. */
    fun downloadAndExtractWad(context: Context): File {
        val cacheDir = File(context.cacheDir, "mii_maker")
        cacheDir.mkdirs()

        val zipFile = File(cacheDir, "mii_channel_symbols.zip")
        FileDownloader.downloadToFile(
            MII_MAKER_ZIP_URL,
            zipFile,
            client = HttpClientProvider.largeDownloadClient
        )

        val wadFile = extractWad(zipFile, cacheDir)
        zipFile.delete()

        return wadFile
    }

    fun isValidWad(file: File): Boolean {
        return try {
            val bytes = file.readBytes().take(4)
            bytes.size == 4 && bytes[0] == 0x00.toByte() && bytes[1] == 0x00.toByte() &&
                    bytes[2] == 0x00.toByte() && bytes[3] == 0x20.toByte()
        } catch (_: Exception) {
            false
        }
    }

    /** Launches Dolphin with the given WAD file via `ACTION_VIEW` + `FileProvider` content URI. Validates magic header first (0x00 0x00 0x00 0x20). */
    fun launchWadFile(context: Context, wadFile: File): Result<Unit> = runCatching {
        require(isValidWad(wadFile)) { "Cached WAD file is corrupted or incomplete" }
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            wadFile
        )

        val intent = Intent().apply {
            setClassName(DolphinLauncher.DOLPHIN_PACKAGE, DolphinLauncher.DOLPHIN_MAIN_ACTIVITY)
            data = contentUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun extractWadForTest(zipFile: File, destDir: File): File = extractWad(zipFile, destDir)

    private fun extractWad(zipFile: File, destDir: File): File {
        val wadFiles = mutableListOf<File>()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".wad", ignoreCase = true)) {
                    val outFile = File(destDir, entry.name.substringAfterLast("/"))
                    outFile.outputStream().use { output ->
                        zis.copyTo(output)
                    }
                    wadFiles.add(outFile)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return wadFiles.firstOrNull { it.name.contains("HACS", ignoreCase = true) }
            ?: wadFiles.firstOrNull()
            ?: error("No .wad file found in the archive")
    }
}
