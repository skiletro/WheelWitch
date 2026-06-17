package com.skiletro.wheelwitch.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

object DolphinLauncher {
    private const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
    private const val DOLPHIN_MAIN_ACTIVITY = "$DOLPHIN_PACKAGE.ui.main.MainActivity"
    private const val EXTRA_AUTO_START_FILE = "AutoStartFile"
    private const val RR_JSON_NAME = "RR.json"
    private const val RIIVOLUTION_FOLDER = "riivolution"
    private const val XML_FILE_NAME = "RetroRewind6.xml"
    private const val GAME_ISO_PREFS_KEY = "game_iso_path"
    private const val MII_MAKER_ZIP_URL = "https://filecache45.gamebanana.com/mods/mii_channel_symbols_-_hacs.zip"
    private const val WAD_FILE_NAME = "Mii Channel Symbols - HACS.wad"
    private const val DOWNLOAD_BUFFER = 262144

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun setGameIsoPath(context: Context, path: String) {
        context.getSharedPreferences("wheelwitch", Context.MODE_PRIVATE)
            .edit()
            .putString(GAME_ISO_PREFS_KEY, path)
            .apply()
    }

    fun getGameIsoPath(context: Context): String? {
        return context.getSharedPreferences("wheelwitch", Context.MODE_PRIVATE)
            .getString(GAME_ISO_PREFS_KEY, null)
    }

    fun generateLaunchJson(
        storageRootPath: String,
        gameIsoPath: String,
        displayName: String = "RR"
    ): String {
        val xmlPath = "$storageRootPath/$RIIVOLUTION_FOLDER/$XML_FILE_NAME"

        return buildString {
            appendLine("{")
            appendLine("  \"base-file\": \"$gameIsoPath\",")
            appendLine("  \"display-name\": \"$displayName\",")
            appendLine("  \"riivolution\": {")
            appendLine("    \"patches\": [")
            appendLine("      {")
            appendLine("        \"options\": [")
            appendLine("          { \"choice\": 1, \"option-name\": \"Pack\", \"section-name\": \"Retro Rewind\" },")
            appendLine("          { \"choice\": 2, \"option-name\": \"My Stuff\", \"section-name\": \"Retro Rewind\" },")
            appendLine("          { \"choice\": 2, \"option-name\": \"Seperate Savegame\", \"section-name\": \"Retro Rewind\" }")
            appendLine("        ],")
            appendLine("        \"root\": \"$storageRootPath\",")
            appendLine("        \"xml\": \"$xmlPath\"")
            appendLine("      }")
            appendLine("    ]")
            appendLine("  },")
            appendLine("  \"type\": \"dolphin-game-mod-descriptor\",")
            appendLine("  \"version\": 1")
            append("}")
        }
    }

    fun launchDolphin(context: Context, jsonFilePath: String): Result<Unit> = runCatching {
        val intent = Intent().apply {
            setClassName(DOLPHIN_PACKAGE, DOLPHIN_MAIN_ACTIVITY)
            putExtra(EXTRA_AUTO_START_FILE, jsonFilePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun getCachedWadFile(context: Context): File? {
        val dir = File(context.cacheDir, "mii_maker")
        val wad = File(dir, WAD_FILE_NAME)
        return if (wad.exists()) wad else null
    }

    fun downloadAndExtractWad(context: Context): File {
        val cacheDir = File(context.cacheDir, "mii_maker")
        cacheDir.mkdirs()

        val zipFile = File(cacheDir, "mii_channel_symbols.zip")
        downloadToFile(MII_MAKER_ZIP_URL, zipFile)

        val wadFile = extractWad(zipFile, cacheDir)
        zipFile.delete()

        return wadFile
    }

    fun launchWadFile(context: Context, wadFile: File): Result<Unit> = runCatching {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            wadFile
        )

        val intent = Intent().apply {
            setClassName(DOLPHIN_PACKAGE, DOLPHIN_MAIN_ACTIVITY)
            data = contentUri
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun downloadToFile(urlString: String, targetFile: File) {
        val request = Request.Builder().url(urlString).build()
        val response = httpClient.newCall(request).execute()
        val body = response.body ?: error("No response body")

        body.byteStream().use { input ->
            targetFile.outputStream().use { output ->
                val buffer = ByteArray(DOWNLOAD_BUFFER)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    output.write(buffer, 0, bytesRead)
                }
            }
        }
    }

    private fun extractWad(zipFile: File, destDir: File): File {
        val wadFiles = mutableListOf<File>()
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".wad", ignoreCase = true)) {
                    val outFile = File(destDir, entry.name.substringAfterLast("/"))
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(DOWNLOAD_BUFFER)
                        var bytesRead: Int
                        while (zis.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
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
