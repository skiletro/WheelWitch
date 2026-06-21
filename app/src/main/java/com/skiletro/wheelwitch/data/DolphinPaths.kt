package com.skiletro.wheelwitch.data

import android.content.Context
import com.skiletro.wheelwitch.util.launcher.DolphinLauncher
import java.io.File
import kotlin.text.Regex.Companion.escape

/**
 * Single source of truth for filesystem paths inside Dolphin's user folder.
 *
 * The physical root of Dolphin's per-user folder is computed from
 * WheelWitch's own external files dir via the package-swap trick: take
 * [Context.getExternalFilesDir], which returns
 * `/storage/emulated/<userId>/Android/data/<calling_pkg>/files`, and
 * replace `<calling_pkg>` with [DOLPHIN_PACKAGE]. The result is
 * `/storage/emulated/<userId>/Android/data/org.dolphinemu.dolphinemu/files`
 * — multi-user safe by construction for the common case where WheelWitch
 * and Dolphin live in the same user.
 *
 * All other helpers in this object compose on [physicalRoot], so every
 * downstream path stays consistent with the launch descriptor's
 * `base-file` / `root` / `xml` fields and the `AutoStartFile` extra.
 * That single source of truth is the path consistency invariant
 * required for Riivolution to resolve the launch descriptor.
 *
 * `DOLPHIN_PACKAGE` is re-exported from [DolphinLauncher] — do not
 * redefine it. The Mii WAD installer imports the same constant from
 * the same place, so a rename must happen in one spot.
 */
object DolphinPaths {
  /** Re-exported from [DolphinLauncher] — single source of truth. */
  const val DOLPHIN_PACKAGE: String = DolphinLauncher.DOLPHIN_PACKAGE

  /** Subpath under Dolphin's `files/` dir where WheelWitch keeps the pack, ROM, and launch descriptor. */
  const val WHEELWITCH_SUBPATH: String = "User/Wii/WheelWitch"

  /**
   * Absolute filesystem path of Dolphin's per-user folder.
   *
   * Computed via the package-swap trick. [Context.getExternalFilesDir]
   * is called with a `null` type, which returns
   * `/storage/emulated/<userId>/Android/data/<calling_pkg>/files`. The
   * calling package segment is then replaced with [DOLPHIN_PACKAGE].
   * This assumes WheelWitch and Dolphin are in the same user, which is
   * the common case; the work-profile mismatch is a known limitation
   * (see PLAN.md "Risks and unknowns").
   *
   * @throws IllegalStateException if [Context.getExternalFilesDir] returns null.
   */
  fun physicalRoot(context: Context): String {
    val ourPath =
      context.getExternalFilesDir(null)?.absolutePath
        ?: error("External files dir unavailable")
    return ourPath.replaceFirst(Regex(escape(context.packageName)), DOLPHIN_PACKAGE)
  }

  /** The directory where WheelWitch stores its pack, ROM, and launch descriptor. */
  fun wheelWitchDir(context: Context): File = File(physicalRoot(context), WHEELWITCH_SUBPATH)

  /** Directory containing the extracted pack contents (with `pack/version.txt` at its root). */
  fun packDir(context: Context): File = File(wheelWitchDir(context), "pack")

  /** Directory containing the user-picked Mario Kart Wii ROM, renamed to `<GAMEID>.<ext>`. */
  fun romDir(context: Context): File = File(wheelWitchDir(context), "rom")

  /** Path to the launch descriptor that Riivolution reads to launch Retro Rewind. */
  fun rrJsonFile(context: Context): File = File(wheelWitchDir(context), "rr_autostartfile.json")

  /** Path to the version file written by [com.skiletro.wheelwitch.domain.RewindPackManager] after a successful install. */
  fun versionFile(context: Context): File = File(packDir(context), "version.txt")

  /** Path to Dolphin's `Dolphin.ini` (where the `ISOPathN` block lives). */
  fun configIni(context: Context): File = File(physicalRoot(context), "Config/Dolphin.ini")

  /**
   * The expected SAF tree document id for the Dolphin user folder.
   *
   * The onboarding picker uses this with [DocumentsContract.getTreeDocumentId] to confirm the
   * user picked the right folder — anything else is rejected and re-prompted.
   */
  fun expectedTreeId(): String = "primary:Android/data/$DOLPHIN_PACKAGE/files"
}
