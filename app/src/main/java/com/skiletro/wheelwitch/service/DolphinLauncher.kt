package com.skiletro.wheelwitch.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import java.io.File

object DolphinLauncher {
    private const val RR_JSON_NAME = "RR.json"
    private const val RIIVOLUTION_FOLDER = "riivolution"
    private const val XML_FILE_NAME = "RetroRewind6.xml"
    private const val GAME_ISO_PREFS_KEY = "game_iso_uri"

    private val prefs by lazy { mutableMapOf<String, String?>() }

    fun setGameIsoUri(context: Context, uri: String) {
        context.getSharedPreferences("wheelwitch", Context.MODE_PRIVATE)
            .edit()
            .putString(GAME_ISO_PREFS_KEY, uri)
            .apply()
    }

    fun getGameIsoUri(context: Context): String? {
        return context.getSharedPreferences("wheelwitch", Context.MODE_PRIVATE)
            .getString(GAME_ISO_PREFS_KEY, null)
    }

    fun generateLaunchJson(
        context: Context,
        storageRootUri: Uri,
        gameIsoUri: String
    ): Result<File> = runCatching {
        val rootPath = resolveContentUriToPath(context, storageRootUri)
            ?: error("Cannot resolve storage root path")

        val xmlPath = "$rootPath/$RIIVOLUTION_FOLDER/$XML_FILE_NAME"
        val rootRiivolutionPath = rootPath

        val launchConfig = buildString {
            appendLine("{")
            appendLine("  \"baseFile\": \"$gameIsoUri\",")
            appendLine("  \"displayName\": \"Retro Rewind\",")
            appendLine("  \"riivolution\": {")
            appendLine("    \"patches\": [")
            appendLine("      {")
            appendLine("        \"options\": [")
            appendLine("          { \"choice\": 1, \"optionName\": \"Pack\", \"sectionName\": \"Retro Rewind\" },")
            appendLine("          { \"choice\": 0, \"optionName\": \"My Stuff\", \"sectionName\": \"Retro Rewind\" }")
            appendLine("        ],")
            appendLine("        \"root\": \"$rootRiivolutionPath\",")
            appendLine("        \"xml\": \"$xmlPath\"")
            appendLine("      }")
            appendLine("    ]")
            appendLine("  },")
            appendLine("  \"type\": \"dolphin-game-mod-descriptor\",")
            appendLine("  \"version\": 1")
            append("}")
        }

        val rrJsonFile = File(context.cacheDir, RR_JSON_NAME)
        rrJsonFile.writeText(launchConfig)
        rrJsonFile
    }

    fun launchDolphin(context: Context, rrJsonFile: File): Result<Unit> = runCatching {
        val packageManager = context.packageManager
        val dolphinPackage = "org.dolphinemu.dolphinemu"

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    rrJsonFile
                ),
                "application/json"
            )
            setPackage(dolphinPackage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (intent.resolveActivity(packageManager) == null) {
            error("Dolphin Emulator is not installed. Please install it from the Play Store.")
        }

        context.startActivity(intent)
    }

    private fun resolveContentUriToPath(context: Context, treeUri: Uri): String? {
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
            storageType.equals("primary", ignoreCase = true) -> {
                "/storage/emulated/0/$relativePath"
            }
            else -> {
                "/storage/$storageType/$relativePath"
            }
        }
    }
}
