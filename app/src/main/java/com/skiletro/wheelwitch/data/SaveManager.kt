package com.skiletro.wheelwitch.data

import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Multi-region I/O wrapper around `rksys.dat`.
 *
 * Path template inside the pack:
 * ```
 * <packRoot>/
 *   riivolution/
 *     save/
 *       RetroWFC/
 *         <regionCode>/      — e.g. RMCP, RMCE, RMCJ
 *           rksys.dat
 * ```
 *
 * The region is inferred from the user-picked ROM file name (the
 * `RMCP01.iso` / `RMCE01.rvz` / `RMCJ01.wbfs` uppercase convention
 * encodes it). [regionFromRomFileName] is the single source of truth
 * for that mapping; [listRegions] walks [DolphinTree.romDir] to find
 * which regions the user actually has ROMs for.
 *
 * All `backup` / `restore` / `delete` methods are suspend and return
 * [Result] so the view-model can surface errors without exceptions
 * crossing the I/O boundary.
 */
object SaveManager {
  /** Wii MKW region codes. The [code] is the 4-character ROM-file prefix. */
  enum class Region(val code: String) {
    PAL("RMCP"),
    USA("RMCE"),
    JPN("RMCJ"),
  }

  /** Filename of the save file inside the region directory. */
  const val SAVE_FILE_NAME = "rksys.dat"

  /**
   * Maps a ROM file name to its [Region]. Returns null if [name] does
   * not start with any known region code. Match is
   * case-insensitive so the helper tolerates the rare mixed-case pick
   * (the ROM is always uppercased on copy in
   * [DolphinTree.copyRomFromSource] but defensive parsing is cheap).
   */
  fun regionFromRomFileName(name: String): Region? =
    Region.entries.firstOrNull { name.startsWith(it.code, ignoreCase = true) }

  /**
   * Returns the regions for which the user has a ROM in
   * [DolphinTree.romDir]. Order matches the iteration order of
   * [DocumentFile.listFiles], which is platform-dependent but stable
   * for a given picker result.
   */
  fun listRegions(tree: DolphinTree): List<Region> {
    val files = tree.romDir.listFiles()
    return files.mapNotNull { regionFromRomFileName(it.name ?: "") }.distinct()
  }

  /**
   * True when a `rksys.dat` exists at
   * `<packRoot>/riivolution/save/RetroWFC/<region>/rksys.dat`.
   * Intermediate directories are created on demand if missing.
   */
  fun hasSave(tree: DolphinTree, region: Region): Boolean {
    val file = saveFile(tree, region) ?: return false
    return file.exists()
  }

  /**
   * Reads the raw `rksys.dat` bytes for [region] (null if absent).
   * Used by [com.skiletro.wheelwitch.viewmodel.SaveDataViewModel] to
   * feed [RksysParser]. Idempotent and side-effect free.
   */
  suspend fun readSave(tree: DolphinTree, region: Region): ByteArray? =
    withContext(Dispatchers.IO) { readSaveBytes(tree, region) }

  /**
   * Copies the user's [region] save bytes to a user-picked [dest]
   * URI (typically from `ACTION_CREATE_DOCUMENT`).
   */
  suspend fun backup(
    tree: DolphinTree,
    region: Region,
    dest: Uri,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val bytes =
          readSaveBytes(tree, region)
            ?: throw IOException("No save file for region $region")
        val output =
          tree.resolver.openOutputStream(dest)
            ?: throw IOException("Cannot open output stream for $dest")
        output.use { it.write(bytes) }
        Timber.tag(TAG).i("Backed up save for %s to %s", region, dest)
      }
    }

  /**
   * Reads the user-picked [source] URI (typically from
   * `ACTION_OPEN_DOCUMENT`) and overwrites the [region] save in
   * place. Intermediate directories are created on demand.
   */
  suspend fun restore(
    tree: DolphinTree,
    region: Region,
    source: Uri,
  ): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val input =
          tree.resolver.openInputStream(source)
            ?: throw IOException("Cannot open input stream for $source")
        val bytes = input.use { it.readBytes() }
        writeSaveBytes(tree, region, bytes)
        Timber.tag(TAG).i("Restored save for %s from %s", region, source)
      }
    }

  /**
   * Deletes the [region] save file. Returns success even if the file
   * was already absent — `delete` is idempotent by intent.
   */
  suspend fun delete(tree: DolphinTree, region: Region): Result<Unit> =
    withContext(Dispatchers.IO) {
      runCatching {
        val file = saveFile(tree, region)
        if (file != null && file.exists()) {
          file.delete()
        }
        Timber.tag(TAG).i("Deleted save for %s", region)
      }
    }

  // --- internals --------------------------------------------------------

  private fun readSaveBytes(tree: DolphinTree, region: Region): ByteArray? {
    val file = saveFile(tree, region)
    if (file == null || !file.exists()) return null
    return tree.resolver.openInputStream(file.uri)?.use { it.readBytes() }
  }

  private fun writeSaveBytes(tree: DolphinTree, region: Region, bytes: ByteArray) {
    val dir = saveDir(tree, region)
    val existing = dir.findFile(SAVE_FILE_NAME)
    if (existing != null) existing.delete()
    val file =
      dir.createFile("application/octet-stream", SAVE_FILE_NAME)
        ?: throw IOException("Cannot create $SAVE_FILE_NAME in ${region.code}/")
    val output =
      tree.resolver.openOutputStream(file.uri)
        ?: throw IOException("Cannot open output stream for ${file.uri}")
    output.use { it.write(bytes) }
  }

  private fun saveFile(tree: DolphinTree, region: Region): DocumentFile? {
    val dir = saveDir(tree, region)
    return dir.findFile(SAVE_FILE_NAME)
  }

  private fun saveDir(tree: DolphinTree, region: Region): DocumentFile =
    navigateOrCreate(tree.packDir, listOf("riivolution", "save", "RetroWFC", region.code))

  /** Walks [parts] under [root], creating intermediate directories as needed. */
  private fun navigateOrCreate(root: DocumentFile, parts: List<String>): DocumentFile {
    var current = root
    for (part in parts) {
      val existing = current.findFile(part)
      current =
        if (existing != null && existing.isDirectory) {
          existing
        } else {
          current.createDirectory(part)
            ?: throw IOException("Cannot create directory '$part' under ${current.uri}")
        }
    }
    return current
  }

  private const val TAG = "SaveManager"
}
