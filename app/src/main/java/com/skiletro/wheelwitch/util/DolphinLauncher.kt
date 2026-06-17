package com.skiletro.wheelwitch.util

import android.content.Context
import android.content.Intent

object DolphinLauncher {
    private const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
    private const val DOLPHIN_MAIN_ACTIVITY = "$DOLPHIN_PACKAGE.ui.main.MainActivity"
    private const val EXTRA_AUTO_START_FILE = "AutoStartFile"
    private const val RR_JSON_NAME = "RR.json"
    private const val RIIVOLUTION_FOLDER = "riivolution"
    private const val XML_FILE_NAME = "RetroRewind6.xml"
    private const val GAME_ISO_PREFS_KEY = "game_iso_path"

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
}
