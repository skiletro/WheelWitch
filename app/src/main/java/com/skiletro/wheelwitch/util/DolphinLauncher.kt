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
    const val RR_JSON_NAME = "RR.json"
    private const val RIIVOLUTION_FOLDER = "riivolution"
    private const val XML_FILE_NAME = "RetroRewind6.xml"
    private const val GAME_ISO_PREFS_KEY = "game_iso_path"

    /** Riivolution XML <choice id="..."> values used in the options array. */
    private const val PACK_CHOICE_ID = 1
    private const val SEPARATE_SAVE_CHOICE_ID = 2

    private const val SECTION_NAME = "Retro Rewind"
    private const val OPTION_NAME_PACK = "Pack"
    private const val OPTION_NAME_MY_STUFF = "My Stuff"
    // sic: matches the Riivolution XML option name verbatim (including the typo).
    private const val OPTION_NAME_SEPARATE_SAVEGAME = "Seperate Savegame"

    /**
     * Controls which custom content Riivolution loads on launch. The
     * [xmlChoice] of each variant is the `<choice id="...">` value in the
     * RetroRewind6.xml that selects that variant.
     *
     * - [Disabled]: omit the My Stuff option entirely.
     * - [MusicOnly]: choice 4 (music swaps only).
     * - [Everything]: choice 2 (music, tracks, characters, etc.).
     */
    enum class MyStuffMode(val xmlChoice: Int) {
        Disabled(0),
        MusicOnly(4),
        Everything(2)
    }

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
        context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(GAME_ISO_PREFS_KEY, path)
            .apply()
    }

    /** Returns the saved ISO path, or null if none was set. */
    fun getGameIsoPath(context: Context): String? {
        return context.getSharedPreferences(PrefsKeys.PREFS_NAME, Context.MODE_PRIVATE)
            .getString(GAME_ISO_PREFS_KEY, null)
    }

    /** Generates the Dolphin `RR.json` launch descriptor (kebab-case JSON fields: `base-file`, `display-name`, `riivolution`, etc.). */
    fun generateLaunchJson(
        storageRootPath: String,
        gameIsoPath: String,
        displayName: String = "RR",
        myStuffMode: MyStuffMode = MyStuffMode.Everything
    ): String {
        val xmlPath = "$storageRootPath/$RIIVOLUTION_FOLDER/$XML_FILE_NAME"

        return JSONObject().apply {
            put("base-file", gameIsoPath)
            put("display-name", displayName)
            put("riivolution", JSONObject().apply {
                put("patches", JSONArray().apply {
                    put(buildPatch(storageRootPath, xmlPath, myStuffMode))
                })
            })
            put("type", "dolphin-game-mod-descriptor")
            put("version", 1)
        }.toString(2)
    }

    /** Builds one patch entry: root, xml path, and the options array. */
    private fun buildPatch(storageRootPath: String, xmlPath: String, myStuffMode: MyStuffMode): JSONObject =
        JSONObject().apply {
            put("options", buildPatchOptions(myStuffMode))
            put("root", storageRootPath)
            put("xml", xmlPath)
        }

    /** Builds the ordered options array (Pack, optional My Stuff, Seperate Savegame). */
    private fun buildPatchOptions(myStuffMode: MyStuffMode): JSONArray = JSONArray().apply {
        put(option(PACK_CHOICE_ID, OPTION_NAME_PACK))
        if (myStuffMode != MyStuffMode.Disabled) {
            put(option(myStuffMode.xmlChoice, OPTION_NAME_MY_STUFF))
        }
        put(option(SEPARATE_SAVE_CHOICE_ID, OPTION_NAME_SEPARATE_SAVEGAME))
    }

    private fun option(choice: Int, name: String): JSONObject = JSONObject().apply {
        put("choice", choice)
        put("option-name", name)
        put("section-name", SECTION_NAME)
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
    fun writeLaunchJson(rootPath: String, gameIsoPath: String, myStuffMode: MyStuffMode = MyStuffMode.Everything) {
        File(rootPath, RR_JSON_NAME).writeText(generateLaunchJson(rootPath, gameIsoPath, myStuffMode = myStuffMode))
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
