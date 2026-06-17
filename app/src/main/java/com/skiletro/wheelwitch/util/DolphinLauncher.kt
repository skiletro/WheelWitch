package com.skiletro.wheelwitch.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Launches Dolphin Emulator with Retro Rewind configuration and manages the RR.json launch descriptor. */
object DolphinLauncher {
    const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
    const val DOLPHIN_MAIN_ACTIVITY = "$DOLPHIN_PACKAGE.ui.main.MainActivity"
    private const val EXTRA_AUTO_START_FILE = "AutoStartFile"
    private const val RR_JSON_NAME = "RR.json"
    private const val RIIVOLUTION_FOLDER = "riivolution"
    private const val XML_FILE_NAME = "RetroRewind6.xml"
    private const val GAME_ISO_PREFS_KEY = "game_iso_path"

    /** Returns true if the Dolphin package is installed on the device. */
    fun isDolphinInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(DOLPHIN_PACKAGE, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Opens the Dolphin download page in the browser. */
    fun openDolphinDownload(context: Context) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dolphin-emu.org/download/"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Persists the selected Mario Kart Wii ISO path to SharedPreferences. */
    fun setGameIsoPath(context: Context, path: String) {
        context.getSharedPreferences("wheelwitch", Context.MODE_PRIVATE)
            .edit()
            .putString(GAME_ISO_PREFS_KEY, path)
            .apply()
    }

    /** Returns the saved ISO path, or null if none was set. */
    fun getGameIsoPath(context: Context): String? {
        return context.getSharedPreferences("wheelwitch", Context.MODE_PRIVATE)
            .getString(GAME_ISO_PREFS_KEY, null)
    }

    /** Generates the Dolphin `RR.json` launch descriptor (kebab-case JSON fields: `base-file`, `display-name`, `riivolution`, etc.). */
    fun generateLaunchJson(
        storageRootPath: String,
        gameIsoPath: String,
        displayName: String = "RR"
    ): String {
        val xmlPath = "$storageRootPath/$RIIVOLUTION_FOLDER/$XML_FILE_NAME"

        return JSONObject().apply {
            put("base-file", gameIsoPath)
            put("display-name", displayName)
            put("riivolution", JSONObject().apply {
                put("patches", JSONArray().apply {
                    put(JSONObject().apply {
                        put("options", JSONArray().apply {
                            put(JSONObject().apply {
                                put("choice", 1)
                                put("option-name", "Pack")
                                put("section-name", "Retro Rewind")
                            })
                            put(JSONObject().apply {
                                put("choice", 2)
                                put("option-name", "My Stuff")
                                put("section-name", "Retro Rewind")
                            })
                            put(JSONObject().apply {
                                put("choice", 2)
                                put("option-name", "Seperate Savegame")
                                put("section-name", "Retro Rewind")
                            })
                        })
                        put("root", storageRootPath)
                        put("xml", xmlPath)
                    })
                })
            })
            put("type", "dolphin-game-mod-descriptor")
            put("version", 1)
        }.toString(2)
    }

    /** Reads the `base-file` field from an existing RR.json, or null if missing/parse fails. */
    fun readIsoPathFromLaunchJson(rootPath: String): String? {
        val file = File(rootPath, RR_JSON_NAME)
        return if (file.exists()) {
            try {
                JSONObject(file.readText()).optString("base-file", "").takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        } else null
    }

    /** Writes a complete RR.json to the storage root. */
    fun writeLaunchJson(rootPath: String, gameIsoPath: String) {
        File(rootPath, RR_JSON_NAME).writeText(generateLaunchJson(rootPath, gameIsoPath))
    }

    /** Deletes the RR.json from the storage root. */
    fun deleteLaunchJson(rootPath: String) {
        File(rootPath, RR_JSON_NAME).delete()
    }

    /** Launches Dolphin with the given RR.json path via the `AutoStartFile` intent extra. */
    fun launchDolphin(context: Context, jsonFilePath: String): Result<Unit> = runCatching {
        val intent = Intent().apply {
            setClassName(DOLPHIN_PACKAGE, DOLPHIN_MAIN_ACTIVITY)
            putExtra(EXTRA_AUTO_START_FILE, jsonFilePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
