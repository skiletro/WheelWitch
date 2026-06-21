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
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Coarse phase of the pack extraction. Emitted via [ExtractProgress] so
 * the UI can show a "Preparing folders…" line during the directory
 * pre-pass (when the typical RPC cost of `findFile`/`createDirectory`
 * is paid all at once) and a "Extracting files…" line with a live
 * current-file name during the per-file write pass.
 */
enum class ExtractingPhase {
  /** Directory pre-pass is running. */
  PreparingFolders,

  /** Files are being written. */
  WritingFiles,
}

/**
 * Per-file progress snapshot emitted by [DolphinTree.extractZipToPack].
 * Replaces the previous bare `Int` (file index) callback so the UI can
 * render a meaningful bar and a live file-name readout.
 */
data class ExtractProgress(
  val phase: ExtractingPhase,
  val filesDone: Int,
  val filesTotal: Int,
  val currentFile: String?,
  val bytesDone: Long,
  val bytesTotal: Long,
)

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
      ?: run {
        // Diagnostic: log the URI scheme/authority (no auth tokens) so
        // a bug report can show whether the picker returned an
        // unexpected form. fromTreeUri returns null when the URI is
        // malformed, the tree has been revoked, or the provider is
        // unreachable — all "re-onboard" cases.
        val docId =
          runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        Timber.tag(TAG)
          .e(
            "DocumentFile.fromTreeUri returned null for uri=%s authority=%s docId=%s",
            treeUri,
            treeUri.authority,
            docId,
          )
        error("Invalid tree URI: $treeUri")
      }

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
   * Extracts a Retro Rewind pack zip into [packDir]. Uses
   * [java.util.zip.ZipFile] (central-directory-based iteration, one
   * seek at the end of the file) instead of a streaming
   * `ZipInputStream` (which re-parses each Local File Header
   * sequentially).
   *
   * The flow is:
   * 1. Enumerate the central directory once to get [ZipEntry] objects
   *    with up-front size metadata.
   * 2. Pre-create every unique parent directory with a single
   *    `createDirectory` per dir, cache results in a
   *    `Map<List<String>, DocumentFile>` so the per-file write loop
   *    doesn't pay N `findFile` round-trips per path component.
   * 3. Write each file via [FileChannel.transferFrom] when the
   *    provider returns a [FileOutputStream] (most providers do),
   *    otherwise via a buffered copy with a 256 KB buffer.
   *
   * The [onProgress] callback fires with an [ExtractProgress] snapshot
   * at three points: once at the start of [ExtractingPhase.PreparingFolders],
   * once per created directory (with `filesDone` / `filesTotal` still
   * 0), and once per written file (with the new `currentFile` set).
   */
  suspend fun extractZipToPack(zipFile: File, onProgress: (ExtractProgress) -> Unit) {
    withContext(Dispatchers.IO) {
      ZipFile(zipFile).use { zip ->
        val entries = zip.entries().toList()
        val fileEntries = entries.filterNot { it.isDirectory }
        val filesTotal = fileEntries.size
        val bytesTotal = fileEntries.sumOf { it.size.coerceAtLeast(0L) }

        val byPath: Map<List<String>, DocumentFile> =
          precreateDirectories(fileEntries, onProgress, filesTotal, bytesTotal)

        var fileIndex = 0
        var bytesDone = 0L
        for (entry in fileEntries) {
          val currentFile = entry.name
          onProgress(
            ExtractProgress(
              phase = ExtractingPhase.WritingFiles,
              filesDone = fileIndex,
              filesTotal = filesTotal,
              currentFile = currentFile,
              bytesDone = bytesDone,
              bytesTotal = bytesTotal,
            )
          )
          val entrySize = entry.size.coerceAtLeast(0L)
          writeZipEntry(
            entry = entry,
            parent = byPath.getValue(parentParts(entry.name)),
            fileName = entry.name.substringAfterLast('/'),
            input = zip.getInputStream(entry),
          )
          bytesDone += entrySize
          fileIndex++
          onProgress(
            ExtractProgress(
              phase = ExtractingPhase.WritingFiles,
              filesDone = fileIndex,
              filesTotal = filesTotal,
              currentFile = currentFile,
              bytesDone = bytesDone,
              bytesTotal = bytesTotal,
            )
          )
        }
      }
    }
  }

  /**
   * Walks the file entries, collects the unique set of parent
   * directory paths, creates them in depth order (parents first so
   * each `createDirectory` only needs one IPC call), and returns a
   * map from path parts to the resulting [DocumentFile]. The empty
   * list is included as the key for `packDir` itself.
   */
  private fun precreateDirectories(
    fileEntries: List<ZipEntry>,
    onProgress: (ExtractProgress) -> Unit,
    filesTotal: Int,
    bytesTotal: Long,
  ): Map<List<String>, DocumentFile> {
    // For every file, add every ancestor directory to the set, not just
    // the direct parent. A file at a/b/c.bin contributes ["a"] and
    // ["a", "b"]; the file itself is never a directory. Without this,
    // a zip with files in sibling subtrees (e.g. apps/x/foo.bin and
    // apps/y/bar.bin, no file directly under apps/) would crash the
    // pre-pass with "Key [apps] is missing in the map".
    val uniqueParents = fileEntries
      .flatMap { file ->
        val parts = file.name.split('/').filter { it.isNotEmpty() }
        (1 until parts.size).map { parts.take(it) }
      }
      .toSet()
      .sortedBy { it.size }

    onProgress(
      ExtractProgress(
        phase = ExtractingPhase.PreparingFolders,
        filesDone = 0,
        filesTotal = filesTotal,
        currentFile = null,
        bytesDone = 0L,
        bytesTotal = bytesTotal,
      )
    )

    val byPath = mutableMapOf<List<String>, DocumentFile>(emptyList<String>() to packDir)
    for (parts in uniqueParents) {
      if (parts.isEmpty()) continue
      val parent = byPath.getValue(parts.dropLast(1))
      val name = parts.last()
      val existing = parent.findFile(name)
      val dir =
        if (existing != null && existing.isDirectory) {
          existing
        } else {
          parent.createDirectory(name) ?: error("Cannot create $name in pack/")
        }
      byPath[parts] = dir
    }
    return byPath
  }

  private fun parentParts(entryName: String): List<String> {
    val parts = entryName.split('/').filter { it.isNotEmpty() }
    return if (parts.size <= 1) emptyList() else parts.dropLast(1)
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
      Timber.tag(TAG)
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

  /**
   * Reads `Config/Dolphin.ini` (the INI file Dolphin uses for its
   * library paths) and returns its UTF-8 contents, or null if the
   * file does not exist yet. Used by [com.skiletro.wheelwitch.util.launcher.DolphinLauncher]
   * to upsert the WheelWitch `rom/` folder as an `ISOPathN` entry.
   */
  fun readConfigIni(): String? {
    val configDir =
      findOrCreateDir(root, "Config") ?: return null
    val file = configDir.findFile(CONFIG_INI_NAME) ?: return null
    return resolver.openInputStream(file.uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
  }

  /**
   * Writes [content] as `Config/Dolphin.ini`, creating the
   * `Config/` directory and replacing any existing file. Returns the
   * new INI [DocumentFile]. Used by the launch flow to register the
   * WheelWitch `rom/` folder with Dolphin's library.
   */
  fun writeConfigIni(content: String): DocumentFile {
    val configDir =
      findOrCreateDir(root, "Config")
        ?: error("Cannot create Config/ in Dolphin tree")
    val existing = configDir.findFile(CONFIG_INI_NAME)
    if (existing != null) existing.delete()
    val file =
      configDir.createFile("text/plain", CONFIG_INI_NAME)
        ?: error("Cannot create $CONFIG_INI_NAME")
    val output =
      resolver.openOutputStream(file.uri)
        ?: error("Cannot open output stream for $CONFIG_INI_NAME")
    output.use { it.write(content.toByteArray(Charsets.UTF_8)) }
    return file
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

  private fun writeZipEntry(
    entry: ZipEntry,
    parent: DocumentFile,
    fileName: String,
    input: InputStream,
  ) {
    val target =
      parent.createFile("application/octet-stream", fileName)
        ?: error("Cannot create ${entry.name} in pack/")
    val output =
      resolver.openOutputStream(target.uri)
        ?: error("Cannot open output stream for ${target.uri}")
    output.use { o ->
      if (o is FileOutputStream) {
        val size = entry.size.coerceAtLeast(0L)
        val channel = o.channel
        if (size > 0L) {
          channel.transferFrom(input.toReadableByteChannel(), 0L, size)
        } else {
          input.copyToWithBuffer(o, COPY_BUFFER_SIZE)
        }
      } else {
        input.copyToWithBuffer(o, COPY_BUFFER_SIZE)
      }
    }
  }

  private fun InputStream.toReadableByteChannel(): ReadableByteChannel = Channels.newChannel(this)

  private fun InputStream.copyToWithBuffer(out: OutputStream, bufferSize: Int) {
    val buffer = ByteArray(bufferSize)
    while (true) {
      val read = read(buffer)
      if (read == -1) break
      out.write(buffer, 0, read)
    }
  }

  private fun findOrCreateDir(parent: DocumentFile, name: String): DocumentFile? {
    val existing = parent.findFile(name)
    if (existing != null && existing.isDirectory) return existing
    return parent.createDirectory(name)
  }

  companion object {
    /** Tag used by Timber in this file's log lines. */
    const val TAG = "DolphinTree"

    /** Buffered-copy buffer used when [FileChannel.transferFrom] is not available. */
    const val COPY_BUFFER_SIZE: Int = 256 * 1024

    /**
     * Filename of the launch descriptor at the WheelWitch root.
     * Re-exported from [com.skiletro.wheelwitch.util.launcher.DolphinLauncher.RR_JSON_NAME]
     * — single source of truth lives in the launcher.
     */
    const val LAUNCH_JSON_NAME = com.skiletro.wheelwitch.util.launcher.DolphinLauncher.RR_JSON_NAME

    /** Filename of the pack version file at the root of the pack directory. */
    const val VERSION_FILE_NAME = "version.txt"

    /** Filename of Dolphin's `Config/Dolphin.ini` library-paths config. */
    const val CONFIG_INI_NAME = "Dolphin.ini"

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
      val uriString = prefs.getString(PrefsKeys.WHEELWITCH_TREE_URI_KEY, null)
      if (uriString == null) {
        Timber.tag(TAG)
          .i("fromPersisted: no tree URI under %s — user has not completed onboarding",
            PrefsKeys.WHEELWITCH_TREE_URI_KEY)
        return null
      }
      return try {
        val uri = Uri.parse(uriString)
        val docId = runCatching { DocumentsContract.getTreeDocumentId(uri) }.getOrNull()
        Timber.tag(TAG)
          .i("fromPersisted: rebuilding tree from authority=%s docId=%s",
            uri?.authority, docId)
        val tree = DolphinTree(context, uri)
        Timber.tag(TAG)
          .i("fromPersisted: tree built ok, wheelWitchDir=%s", tree.wheelWitchDir.uri)
        tree
      } catch (e: Exception) {
        Timber.tag(TAG)
          .e(e, "fromPersisted: rebuild failed for uriString=%s — clearing pref", uriString)
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
      Timber.tag(TAG)
        .i("persist: stored tree URI authority=%s", tree.treeUri.authority)
    }
  }
}
