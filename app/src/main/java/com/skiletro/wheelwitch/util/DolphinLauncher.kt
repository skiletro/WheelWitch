package com.skiletro.wheelwitch.util

import android.content.Context
import android.content.Intent
import org.json.JSONArray
import org.json.JSONObject

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

    fun launchDolphin(context: Context, jsonFilePath: String): Result<Unit> = runCatching {
        val intent = Intent().apply {
            setClassName(DOLPHIN_PACKAGE, DOLPHIN_MAIN_ACTIVITY)
            putExtra(EXTRA_AUTO_START_FILE, jsonFilePath)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
