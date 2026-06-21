package com.skiletro.wheelwitch.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.skiletro.wheelwitch.model.SemVersion
import com.skiletro.wheelwitch.util.prefs.Prefs
import com.skiletro.wheelwitch.util.prefs.PrefsKeys
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * SAF-backed wrapper around the Dolphin user folder WheelWitch writes to.
 *
 * Holds a [ContentResolver] and a [DocumentFile] tree and exposes the
 * three subdirectories the rest of the app needs:
 *
 * ```
 * <tree root>/
 * └── User/Wii/WheelWitch/
 *     ├── pack/                       — extracted Retro Rewind contents
 *     ├── rom/                        — user-picked Mario Kart Wii ISO/RVZ/WBFS
 *     └── rr_autostartfile.json       — launch descriptor (dolphin-game-mod-descriptor)
 * ```
 *
 * Construction is cheap: lazy subdirectory properties defer their first
 * `findFile` / `createDirectory` until the UI actually asks for them.
 * This keeps the constructor side-effect free and lets tests stub
 * individual directories.
 *
 * The tree URI must be the user-picked SAF grant that matches
 * [DolphinPaths.expectedTreeId]. [Companion.validate] is the gate: the
 * stock SAF picker cannot be restricted to a single folder, so the
 * picker result is checked against the expected tree id before we
 * accept it.
 */
class DolphinTree(context: Context, val treeUri: Uri) {
  val resolver: ContentResolver = context.contentResolver

  val root: DocumentFile =
    DocumentFile.fromTreeUri(context, treeUri)
      ?: error("Invalid tree URI: $treeUri")

  val wheelWitchDir: DocumentFile by lazy {
    val userDir =
      findOrCreateDir(root, "User")
        ?: error("Cannot create or find User/ in Dolphin tree")
    val wiiDir =
      findOrCreateDir(userDir, "Wii")
        ?: error("Cannot create or find User/Wii/ in Dolphin tree")
    findOrCreateDir(wiiDir, "WheelWitch")
      ?: error("Cannot create or find User/Wii/WheelWitch/ in Dolphin tree")
  }

  val romDir: DocumentFile by lazy {
    findOrCreateDir(wheelWitchDir, "rom")
      ?: error("Cannot create or find rom/ in WheelWitch dir")
  }

  val packDir: DocumentFile by lazy {
    findOrCreateDir(wheelWitchDir, "pack")
      ?: error("Cannot create or find pack/ in WheelWitch dir")
  }

  /**
   * Copies the bytes at [source] into [romDir] as `<gameId>.<ext>`.
   * Replaces an existing file of the same name (idempotent re-pick).
   *
   * The caller is responsible for validating the source (e.g. via
   * [com.skiletro.wheelwitch.data.GameTypeParser]) before calling.
   */
  suspend fun copyRomFromSource(source: Uri, gameId: String, ext: String): DocumentFile =
    withContext(Dispatchers.IO) {
      val fileName = "$gameId.$ext"
      romDir.findFile(fileName)?.delete()
      val target =
        romDir.createFile("application/octet-stream", fileName)
          ?: error("Cannot create $fileName in rom/")
      val input = resolver.openInputStream(source) ?: error("Cannot open $source")
      input.use { i ->
        val output =
          resolver.openOutputStream(target.uri)
            ?: error("Cannot open output stream for ${target.uri}")
        output.use { o -> i.copyTo(o) }
      }
      target
    }

  /**
   * Extracts a Retro Rewind pack zip into [packDir]. Each non-directory
   * entry is written to the path implied by its name (nested
   * directories are created on demand). The [onProgress] callback
   * fires once per file entry, with the entry index in zip order.
   *
   * [zipFile] is a [File] (rather than a `ByteArray`) so the zip is
   * streamed from disk — the pack zip is multi-MB and the
   * [RewindPackManager][com.skiletro.wheelwitch.domain.RewindPackManager]
   * downloads it to `context.cacheDir` first.
   */
  suspend fun extractZipToPack(zipFile: File, onProgress: (Int) -> Unit) {
    withContext(Dispatchers.IO) {
      ZipInputStream(zipFile.inputStream()).use { zis ->
        var entry = zis.nextEntry
        var fileIndex = 0
        while (entry != null) {
          if (!entry.isDirectory) {
            writeZipEntry(entry, zis)
            onProgress(fileIndex)
            fileIndex++
          }
          zis.closeEntry()
          entry = zis.nextEntry
        }
      }
    }
  }

  /**
   * Writes [content] as [LAUNCH_JSON_NAME] at the WheelWitch root,
   * replacing any existing file. The returned [DocumentFile] is the
   * new launch descriptor.
   */
  fun writeLaunchJson(content: String): DocumentFile {
    val existing = wheelWitchDir.findFile(LAUNCH_JSON_NAME)
    if (existing != null) existing.delete()
    val file =
      wheelWitchDir.createFile("application/json", LAUNCH_JSON_NAME)
        ?: error("Cannot create $LAUNCH_JSON_NAME")
    val output = resolver.openOutputStream(file.uri)
      ?: error("Cannot open output stream for $LAUNCH_JSON_NAME")
    output.use { it.write(content.toByteArray(Charsets.UTF_8)) }
    return file
  }

  /**
   * Reads the [LAUNCH_JSON_NAME] contents if present, or null if the
   * launch descriptor has not been written yet.
   */
  fun readLaunchJson(): String? {
    val file = wheelWitchDir.findFile(LAUNCH_JSON_NAME) ?: return null
    val input = resolver.openInputStream(file.uri) ?: return null
    return input.use { it.readBytes().toString(Charsets.UTF_8) }
  }

  /**
   * Reads the `version.txt` file at the root of [packDir] and parses
   * it as a [SemVersion]. Returns null when the file is missing or
   * unparseable — both are treated as "no local version".
   */
  fun readVersion(): SemVersion? {
    val file = packDir.findFile(VERSION_FILE_NAME) ?: return null
    val text =
      resolver.openInputStream(file.uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        ?: return null
    val parsed = SemVersion.parse(text.trim())
    if (parsed == null) {
      Timber.tag("DolphinTree")
        .w("Could not parse version file contents: %s", text.trim())
    }
    return parsed
  }

  /**
   * Writes [version] to `version.txt` at the root of [packDir],
   * replacing any existing file. Called by
   * [com.skiletro.wheelwitch.domain.RewindPackManager] only after a
   * successful install/extract — see PLAN §"write `version.txt` after
   * a successful extract".
   */
  suspend fun writeVersion(version: SemVersion): Unit =
    withContext(Dispatchers.IO) {
      val existing = packDir.findFile(VERSION_FILE_NAME)
      if (existing != null) existing.delete()
      val file =
        packDir.createFile("text/plain", VERSION_FILE_NAME)
          ?: error("Cannot create $VERSION_FILE_NAME")
      val output =
        resolver.openOutputStream(file.uri)
          ?: error("Cannot open output stream for $VERSION_FILE_NAME")
      output.use { it.write(version.toString().encodeToByteArray()) }
    }

  /** Persists the SAF grant for [treeUri] across process death. */
  fun persistUriPermission() {
    resolver.takePersistableUriPermission(
      treeUri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
  }

  /** Releases the persisted SAF grant for [treeUri]. */
  fun releaseUriPermission() {
    resolver.releasePersistableUriPermission(
      treeUri,
      Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
    )
  }

  // --- internals --------------------------------------------------------

  private fun writeZipEntry(entry: ZipEntry, zis: ZipInputStream) {
    val parts = entry.name.split('/').filter { it.isNotEmpty() }
    if (parts.isEmpty()) return
    val fileName = parts.last()
    val parent =
      if (parts.size == 1) packDir
      else navigateToDir(packDir, parts.dropLast(1))
    val target =
      parent.createFile("application/octet-stream", fileName)
        ?: error("Cannot create ${entry.name} in pack/")
    val output = resolver.openOutputStream(target.uri)
      ?: error("Cannot open output stream for ${target.uri}")
    output.use { o -> zis.copyTo(o) }
  }

  private fun navigateToDir(root: DocumentFile, parts: List<String>): DocumentFile {
    var current = root
    for (part in parts) {
      val existing = current.findFile(part)
      current =
        if (existing != null && existing.isDirectory) {
          existing
        } else {
          current.createDirectory(part) ?: error("Cannot create $part in pack/")
        }
    }
    return current
  }

  private fun findOrCreateDir(parent: DocumentFile, name: String): DocumentFile? {
    val existing = parent.findFile(name)
    if (existing != null && existing.isDirectory) return existing
    return parent.createDirectory(name)
  }

  companion object {
    /**
     * Filename of the launch descriptor at the WheelWitch root.
     * Re-exported from [com.skiletro.wheelwitch.util.launcher.DolphinLauncher.RR_JSON_NAME]
     * — single source of truth lives in the launcher.
     */
    const val LAUNCH_JSON_NAME = com.skiletro.wheelwitch.util.launcher.DolphinLauncher.RR_JSON_NAME

    /** Filename of the pack version file at the root of the pack directory. */
    const val VERSION_FILE_NAME = "version.txt"

    /**
     * Returns success if [treeUri] points to the Dolphin user folder.
     * Two URI forms are accepted:
     *
     * 1. **Primary external storage** — the stock SAF picker's default:
     *    `content://com.android.externalstorage.documents/tree/primary%3AAndroid%2Fdata%2Forg.dolphinemu.dolphinemu%2Ffiles`.
     *    Document id is `primary:Android/data/org.dolphinemu.dolphinemu/files`.
     * 2. **Dolphin's own SAF provider** — surfaced by the picker on
     *    devices where Dolphin is installed:
     *    `content://org.dolphinemu.dolphinemu.user/tree/root%2F`.
     *    Document id is `root/`. This provider maps to the same
     *    physical folder and is the recommended path on modern
     *    Android — it does not require the legacy
     *    `MANAGE_EXTERNAL_STORAGE` permission.
     *
     * Subfolders of either root are rejected — the WheelWitch
     * subdirectory must be created at the top of Dolphin's user
     * folder, not inside some intermediate location.
     */
    fun validate(treeUri: Uri): Result<Unit> {
      val treeId =
        runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
          ?: return Result.failure(IllegalArgumentException("Not a tree URI: $treeUri"))

      // Primary external storage: primary:Android/data/<dolphin>/files
      if (
        treeUri.authority == "com.android.externalstorage.documents" &&
          treeId == DolphinPaths.expectedTreeId()
      ) {
        return Result.success(Unit)
      }

      // Dolphin's own SAF provider root, which maps to the same
      // physical folder. The picker surfaces this on devices where
      // Dolphin is installed; it lets apps write into Dolphin's
      // folder without the legacy MANAGE_EXTERNAL_STORAGE permission.
      if (treeUri.authority == "org.dolphinemu.dolphinemu.user" && treeId == "root/") {
        return Result.success(Unit)
      }

      return Result.failure(
        IllegalArgumentException(
          "Tree $treeUri (authority='${treeUri.authority}', docId='$treeId') " +
            "is not the Dolphin user folder. Expected either primary external storage " +
            "(primary:Android/data/org.dolphinemu.dolphinemu/files) or the Dolphin app's own storage root."
        )
      )
    }

    /**
     * Reconstructs the persisted [DolphinTree] from
     * [PrefsKeys.WHEELWITCH_TREE_URI_KEY], or returns null if the URI
     * is missing or no longer valid (e.g. the user revoked the SAF
     * grant). When the grant is lost, the persisted URI is cleared so
     * the UI can route to onboarding (see PLAN §5).
     */
    fun fromPersisted(context: Context): DolphinTree? {
      val prefs = Prefs.main(context)
      val uriString = prefs.getString(PrefsKeys.WHEELWITCH_TREE_URI_KEY, null) ?: return null
      val uri = Uri.parse(uriString)
      return try {
        DolphinTree(context, uri)
      } catch (e: Exception) {
        Timber.tag("DolphinTree")
          .w(e, "Persisted tree URI is no longer valid; clearing")
        prefs.edit().remove(PrefsKeys.WHEELWITCH_TREE_URI_KEY).apply()
        null
      }
    }

    /** Stores [tree]'s URI under [PrefsKeys.WHEELWITCH_TREE_URI_KEY] for [fromPersisted]. */
    fun persist(context: Context, tree: DolphinTree) {
      Prefs.main(context)
        .edit()
        .putString(PrefsKeys.WHEELWITCH_TREE_URI_KEY, tree.treeUri.toString())
        .apply()
    }
  }
}
