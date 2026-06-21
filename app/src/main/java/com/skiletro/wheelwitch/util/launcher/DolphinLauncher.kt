package com.skiletro.wheelwitch.util.launcher

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.documentfile.provider.DocumentFile
import com.skiletro.wheelwitch.data.DolphinPaths
import com.skiletro.wheelwitch.data.DolphinTree
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * Constants and helpers for the Dolphin Emulator package.
 *
 * [buildLaunchJson] is the pure JSON-shape helper that produces the
 * `dolphin-game-mod-descriptor` body Riivolution reads. [launch]
 * combines the descriptor with the physical path of the user-picked
 * ROM and fires an [Intent] at Dolphin's main activity with the
 * `AutoStartFile` extra pointing at the on-disk JSON.
 *
 * Path consistency invariant (PLAN §8): the descriptor's `base-file`,
 * `root`, and `xml` fields, plus the `AutoStartFile` extra, all derive
 * from the same [DolphinPaths.physicalRoot] call. This guarantees
 * Riivolution can `open(2)` every path the descriptor references.
 */
object DolphinLauncher {
  const val DOLPHIN_PACKAGE = "org.dolphinemu.dolphinemu"
  const val DOLPHIN_MAIN_ACTIVITY = "$DOLPHIN_PACKAGE.ui.main.MainActivity"

  /**
   * Filename of the launch descriptor at the WheelWitch root. Also
   * re-exported as [com.skiletro.wheelwitch.data.DolphinTree.LAUNCH_JSON_NAME]
   * so there is one source of truth.
   */
  const val RR_JSON_NAME = "rr_autostartfile.json"

  /** Intent extra key Dolphin's MainActivity reads for the auto-start file. */
  const val LAUNCH_EXTRA_NAME = "AutoStartFile"

  /** `display-name` value written into the launch descriptor. */
  const val DISPLAY_NAME = "Retro Rewind"

  /** `type` value for the `dolphin-game-mod-descriptor` schema. */
  const val DESCRIPTOR_TYPE = "dolphin-game-mod-descriptor"

  /** `version` value for the `dolphin-game-mod-descriptor` schema. */
  const val DESCRIPTOR_VERSION = 1

  /** Default Riivolution XML path under the pack root. */
  const val DEFAULT_XML_REL_PATH = "riivolution/RetroRewind6.xml"

  /** Returns true if the Dolphin package is installed on the device. */
  fun isDolphinInstalled(context: Context): Boolean =
    try {
      context.packageManager.getPackageInfo(DOLPHIN_PACKAGE, 0)
      true
    } catch (e: Exception) {
      Timber.tag("DolphinLauncher").d(e, "Dolphin package not installed")
      false
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

  /**
   * Writes the launch descriptor to the SAF tree and fires the
   * Dolphin intent with [LAUNCH_EXTRA_NAME] pointing at the
   * descriptor's physical path.
   *
   * Steps:
   * 1. Compute `base-file` (the ROM's physical path under
   *    [DolphinPaths.romDir]) and `root` (the pack dir).
   * 2. Build the descriptor with [buildLaunchJson] and write it via
   *    [DolphinTree.writeLaunchJson].
   * 3. Fire an intent at [DOLPHIN_MAIN_ACTIVITY] with
   *    [LAUNCH_EXTRA_NAME] = the JSON's physical path.
   *
   * Returns a failure (instead of throwing) if Dolphin isn't
   * installed, if the external files dir is unavailable, or if the
   * SAF write throws.
   */
  fun launch(
    context: Context,
    tree: DolphinTree,
    romFile: DocumentFile,
    xmlRelPath: String = DEFAULT_XML_REL_PATH,
  ): Result<Unit> =
    runCatching {
      if (!isDolphinInstalled(context)) {
        throw ActivityNotFoundException("Dolphin emulator is not installed")
      }
      val romName = romFile.name ?: error("ROM file has no name")
      val baseFilePath =
        File(DolphinPaths.romDir(context), romName).absolutePath
      val packRootPath = DolphinPaths.packDir(context).absolutePath
      val jsonPath = DolphinPaths.rrJsonFile(context).absolutePath

      val json = buildLaunchJson(baseFilePath, packRootPath, xmlRelPath)
      tree.writeLaunchJson(json)

      Timber.tag("DolphinLauncher")
        .i("Launching RR via AutoStartFile=%s base-file=%s", jsonPath, baseFilePath)

      context.startActivity(buildDolphinIntent(jsonPath))
    }

  /**
   * Builds the [Intent] that launches Dolphin's main activity with
   * the launch-descriptor's physical path. Extracted from [launch]
   * so the intent shape can be tested directly with a real [Intent]
   * instance (mocking `Context.startActivity` on Android's abstract
   * `Context.startActivity` drops the argument in MockK).
   */
  fun buildDolphinIntent(jsonPath: String): Intent =
    Intent().apply {
      component = ComponentName(DOLPHIN_PACKAGE, DOLPHIN_MAIN_ACTIVITY)
      putExtra(LAUNCH_EXTRA_NAME, jsonPath)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

  private fun optionObject(optionName: String, choice: Int): JSONObject =
    JSONObject()
      .put("choice", choice)
      .put("option-name", optionName)
      .put("section-name", DISPLAY_NAME)
}
