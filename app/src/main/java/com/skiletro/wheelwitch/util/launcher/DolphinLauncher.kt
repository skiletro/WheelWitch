package com.skiletro.wheelwitch.util.launcher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Constants and helpers for the Dolphin Emulator package.
 *
 * The launch / RR.json / My Stuff logic has been ripped out for a
 * planned rewrite — only the package constants and
 * [isDolphinInstalled] remain. The launch helper will return a
 * `NotImplementedError` failure for now. [buildLaunchJson] is the
 * pure JSON-shape helper that the future `launch()` will feed the
 * SAF-write + intent path.
 */
object DolphinLauncher {
    const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
    const val DOLPHIN_MAIN_ACTIVITY = "$DOLPHIN_PACKAGE.ui.main.MainActivity"

    /** Name of the launch descriptor that used to be written to the storage root. */
    const val RR_JSON_NAME = "RR.json"

    /** `display-name` value written into the launch descriptor. */
    const val DISPLAY_NAME = "Retro Rewind"

    /** `type` value for the `dolphin-game-mod-descriptor` schema. */
    const val DESCRIPTOR_TYPE = "dolphin-game-mod-descriptor"

    /** `version` value for the `dolphin-game-mod-descriptor` schema. */
    const val DESCRIPTOR_VERSION = 1

    /** Default Riivolution XML path under the pack root. */
    const val DEFAULT_XML_REL_PATH = "riivolution/RetroRewind6.xml"

    /** Returns true if the Dolphin package is installed on the device. */
    fun isDolphinInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(DOLPHIN_PACKAGE, 0)
            true
        } catch (e: Exception) {
            Timber.tag("DolphinLauncher").d(e, "Dolphin package not installed")
            false
        }
    }

    /**
     * Build the `dolphin-game-mod-descriptor` JSON body that Riivolution
     * reads when launching Retro Rewind. Pure: no I/O, no Android state.
     *
     * The output matches the field names, ordering, and choice values
     * from `@matellush`'s working reference (see PLAN.md). The three
     * path fields (`base-file`, `root`, `xml`) are derived from the
     * caller's `baseFilePath` and `packRootPath` — for the launch to
     * succeed, all three must come from the same `physicalRoot` call
     * (the Path consistency invariant, PLAN §8). `xml` is the join of
     * `packRootPath` and [xmlRelPath], so passing the same pack root
     * for both `root` and the prefix of `xml` is automatic.
     */
    fun buildLaunchJson(
        baseFilePath: String,
        packRootPath: String,
        xmlRelPath: String = DEFAULT_XML_REL_PATH,
        packChoice: Int = 1,
        myStuffChoice: Int = 2,
        separateSavegameChoice: Int = 2,
    ): String {
      val xmlFullPath = "$packRootPath/$xmlRelPath"
      val options =
        JSONArray()
          .put(optionObject("Pack", packChoice))
          .put(optionObject("My Stuff", myStuffChoice))
          .put(optionObject("Separate Savegame", separateSavegameChoice))
      val patch =
        JSONObject()
          .put("options", options)
          .put("root", packRootPath)
          .put("xml", xmlFullPath)
      val patches = JSONArray().put(patch)
      val riivolution = JSONObject().put("patches", patches)
      return JSONObject()
        .put("base-file", baseFilePath)
        .put("display-name", DISPLAY_NAME)
        .put("riivolution", riivolution)
        .put("type", DESCRIPTOR_TYPE)
        .put("version", DESCRIPTOR_VERSION)
        .toString()
    }

    private fun optionObject(optionName: String, choice: Int): JSONObject =
      JSONObject()
        .put("choice", choice)
        .put("option-name", optionName)
        .put("section-name", DISPLAY_NAME)

    /**
     * Launch helper. The full launch flow is not implemented yet — see
     * TODO in the package-level kdoc. This returns a failure so callers
     * surface the message without crashing.
     */
    fun launchDolphin(context: Context, jsonFilePath: String): Result<Unit> =
        Result.failure(NotImplementedError("RR launch not implemented — see TODO"))
}
