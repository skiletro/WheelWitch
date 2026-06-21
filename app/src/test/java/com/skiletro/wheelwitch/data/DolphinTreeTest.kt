package com.skiletro.wheelwitch.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import com.skiletro.wheelwitch.util.prefs.PrefsKeys
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class DolphinTreeTest {

  private lateinit var context: Context
  private lateinit var resolver: ContentResolver
  private lateinit var root: DocumentFile
  private lateinit var treeUri: Uri

  @BeforeEach
  fun setUp() {
    context = mockk(relaxed = true)
    resolver = mockk(relaxed = true)
    every { context.contentResolver } returns resolver
    root = mockk(relaxed = true)
    // Android's Uri stub returns null under unitTests.isReturnDefaultValues,
    // so we use a mock Uri and stub the static Uri.parse to return it.
    treeUri = mockk(relaxed = true)
    every { treeUri.toString() } returns "content://com.skiletro.wheelwitch.tree/tree/abc"
    mockkStatic(Uri::class)
    every { Uri.parse(any()) } returns treeUri

    mockkStatic(DocumentFile::class)
    every { DocumentFile.fromTreeUri(context, treeUri) } returns root
  }

  @AfterEach
  fun tearDown() {
    unmockkStatic(Uri::class)
    unmockkStatic(DocumentFile::class)
    unmockkStatic(DocumentsContract::class)
    unmockkStatic(com.skiletro.wheelwitch.util.prefs.Prefs::class)
  }

  // --- constructor & lazy subdirectories -------------------------------

  @Test
  fun `constructor error when DocumentFile fromTreeUri returns null`() {
    unmockkStatic(DocumentFile::class)
    mockkStatic(DocumentFile::class)
    every { DocumentFile.fromTreeUri(context, any()) } returns null
    val ex = assertThrows<IllegalStateException> { DolphinTree(context, treeUri) }
    assertThat(ex.message).contains("Invalid tree URI")
  }

  @Test
  fun `wheelWitchDir returns existing User Wii WheelWitch path without creating`() {
    val userDir = mockk<DocumentFile>(relaxed = true)
    val wiiDir = mockk<DocumentFile>(relaxed = true)
    val wheelWitchDir = mockk<DocumentFile>(relaxed = true)
    every { root.findFile("User") } returns userDir
    every { userDir.isDirectory } returns true
    every { userDir.findFile("Wii") } returns wiiDir
    every { wiiDir.isDirectory } returns true
    every { wiiDir.findFile("WheelWitch") } returns wheelWitchDir
    every { wheelWitchDir.isDirectory } returns true

    val tree = DolphinTree(context, treeUri)
    assertThat(tree.wheelWitchDir).isEqualTo(wheelWitchDir)
    verify(exactly = 0) { root.createDirectory(any<String>()) }
    verify(exactly = 0) { userDir.createDirectory(any<String>()) }
    verify(exactly = 0) { wiiDir.createDirectory(any<String>()) }
  }

  @Test
  fun `wheelWitchDir creates missing User Wii WheelWitch subdirectories`() {
    val userDir = mockk<DocumentFile>(relaxed = true)
    val wiiDir = mockk<DocumentFile>(relaxed = true)
    val wheelWitchDir = mockk<DocumentFile>(relaxed = true)
    every { root.findFile("User") } returns null
    every { root.createDirectory("User") } returns userDir
    every { userDir.findFile("Wii") } returns null
    every { userDir.createDirectory("Wii") } returns wiiDir
    every { wiiDir.findFile("WheelWitch") } returns null
    every { wiiDir.createDirectory("WheelWitch") } returns wheelWitchDir

    val tree = DolphinTree(context, treeUri)
    assertThat(tree.wheelWitchDir).isEqualTo(wheelWitchDir)
    verify(exactly = 1) { root.createDirectory("User") }
    verify(exactly = 1) { userDir.createDirectory("Wii") }
    verify(exactly = 1) { wiiDir.createDirectory("WheelWitch") }
  }

  @Test
  fun `romDir creates the rom subdirectory under wheelWitchDir`() {
    val (romDir, _, _) =
      setupDirChain().let { Triple(it.first, it.second, it.third) }
    val tree = DolphinTree(context, treeUri)
    assertThat(tree.romDir).isEqualTo(romDir)
  }

  @Test
  fun `packDir creates the pack subdirectory under wheelWitchDir`() {
    val (_, packDir, _) = setupDirChain()
    val tree = DolphinTree(context, treeUri)
    assertThat(tree.packDir).isEqualTo(packDir)
  }

  // --- validate --------------------------------------------------------

  @Test
  fun `validate succeeds for primary storage with the expected tree id and authority`() {
    // The @BeforeEach sets Uri.parse to return the shared `treeUri`
    // mock whose `authority` defaults to null. We stub it here to
    // simulate what a real primary-storage URI would carry.
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.getTreeDocumentId(any()) } returns DolphinPaths.expectedTreeId()
    every { treeUri.authority } returns "com.android.externalstorage.documents"
    val result = DolphinTree.validate(Uri.parse("content://x/tree/abc"))
    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun `validate succeeds for the Dolphin provider root URI`() {
    // The Dolphin app exposes its own SAF provider
    // (org.dolphinemu.dolphinemu.user). The picker surfaces it on
    // devices where Dolphin is installed; the document id is
    // "root/". This URI is a valid alternative to the primary-storage
    // form and does not require MANAGE_EXTERNAL_STORAGE.
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.getTreeDocumentId(any()) } returns "root/"
    every { treeUri.authority } returns "org.dolphinemu.dolphinemu.user"
    val result = DolphinTree.validate(Uri.parse("content://x/tree/abc"))
    assertThat(result.isSuccess).isTrue()
  }

  @Test
  fun `validate fails for a subfolder of the Dolphin provider`() {
    // A subfolder of the Dolphin provider (e.g. root/GameSettings)
    // is rejected — the WheelWitch subdirs must be created at the
    // top of Dolphin's user folder, not inside some intermediate
    // location.
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.getTreeDocumentId(any()) } returns "root/GameSettings"
    every { treeUri.authority } returns "org.dolphinemu.dolphinemu.user"
    val result = DolphinTree.validate(Uri.parse("content://x/tree/abc"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `validate fails for an unrelated tree id`() {
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.getTreeDocumentId(any()) } returns "primary:Documents"
    every { treeUri.authority } returns "com.android.externalstorage.documents"
    val result = DolphinTree.validate(Uri.parse("content://x/tree/abc"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
  }

  @Test
  fun `validate fails when the tree id matches but the authority is unknown`() {
    // Defense against authority confusion: a primary-storage-shaped
    // document id under a non-Dolphin authority (e.g. a different
    // app's SAF provider or a content:// tree) is rejected.
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.getTreeDocumentId(any()) } returns DolphinPaths.expectedTreeId()
    every { treeUri.authority } returns "com.example.otherapp"
    val result = DolphinTree.validate(Uri.parse("content://x/tree/abc"))
    assertThat(result.isFailure).isTrue()
  }

  @Test
  fun `validate wraps an exception from DocumentsContract as a failure`() {
    mockkStatic(DocumentsContract::class)
    every { DocumentsContract.getTreeDocumentId(any()) } throws
      IllegalArgumentException("not a tree uri")
    val result = DolphinTree.validate(Uri.parse("content://x/tree/abc"))
    assertThat(result.isFailure).isTrue()
    assertThat(result.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java)
  }

  // --- fromPersisted / persist ----------------------------------------

  @Test
  fun `fromPersisted returns null when no URI is persisted`() {
    val prefs = mockk<SharedPreferences>(relaxed = true)
    every { prefs.getString(PrefsKeys.WHEELWITCH_TREE_URI_KEY, null) } returns null
    mockPrefsMain(prefs)
    assertThat(DolphinTree.fromPersisted(context)).isNull()
  }

  @Test
  fun `fromPersisted returns the tree when the URI is valid`() {
    val prefs = mockk<SharedPreferences>(relaxed = true)
    every { prefs.getString(PrefsKeys.WHEELWITCH_TREE_URI_KEY, null) } returns
      treeUri.toString()
    mockPrefsMain(prefs)
    val tree = DolphinTree.fromPersisted(context)
    assertThat(tree).isNotNull()
    assertThat(tree!!.treeUri).isEqualTo(treeUri)
  }

  @Test
  fun `fromPersisted clears the pref and returns null when fromTreeUri returns null`() {
    // Override the setUp stub: this test wants fromTreeUri to fail.
    every { DocumentFile.fromTreeUri(context, any()) } returns null
    val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    val prefs = mockk<SharedPreferences>(relaxed = true)
    every { prefs.getString(PrefsKeys.WHEELWITCH_TREE_URI_KEY, null) } returns
      "content://stale/tree/uri"
    every { prefs.edit() } returns editor
    // Production code chains editor.remove(...).apply() — return the
    // editor from remove() so the .apply() call has a non-null target.
    every { editor.remove(any<String>()) } returns editor
    mockPrefsMain(prefs)

    assertThat(DolphinTree.fromPersisted(context)).isNull()
    verify { editor.remove(PrefsKeys.WHEELWITCH_TREE_URI_KEY) }
    verify { editor.apply() }
  }

  @Test
  fun `persist writes the tree URI to the shared pref`() {
    val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    val prefs = mockk<SharedPreferences>(relaxed = true)
    every { prefs.edit() } returns editor
    // editor.putString(...).apply() needs the putString result to be
    // non-null for the chained apply() to land.
    every { editor.putString(any<String>(), any<String>()) } returns editor
    mockPrefsMain(prefs)
    val tree = DolphinTree(context, treeUri)
    val expected = treeUri.toString()

    DolphinTree.persist(context, tree)

    verify { editor.putString(PrefsKeys.WHEELWITCH_TREE_URI_KEY, expected) }
    verify { editor.apply() }
  }

  // --- copyRomFromSource -----------------------------------------------

  @Test
  fun `copyRomFromSource copies bytes into romDir as gameId ext`() = runBlocking {
    val (romDir, _, _) = setupDirChain()
    val source = Uri.parse("content://picker/file/1")
    val sourceBytes = byteArrayOf(0x52, 0x4D, 0x43, 0x50, 0x30, 0x31) // "RMCP01"
    val output = ByteArrayOutputStream()
    val created = mockk<DocumentFile>(relaxed = true)
    every { created.uri } returns Uri.parse("content://tree/rom/RMCP01.iso")
    every { romDir.findFile("RMCP01.iso") } returns null
    every { romDir.createFile("application/octet-stream", "RMCP01.iso") } returns created
    every { resolver.openInputStream(source) } returns ByteArrayInputStream(sourceBytes)
    every { resolver.openOutputStream(created.uri) } returns output

    val tree = DolphinTree(context, treeUri)
    val target = tree.copyRomFromSource(source, "RMCP01", "iso")

    assertThat(target).isEqualTo(created)
    assertThat(output.toByteArray()).isEqualTo(sourceBytes)
  }

  @Test
  fun `copyRomFromSource deletes an existing file with the same name before creating`() = runBlocking {
    val (romDir, _, _) = setupDirChain()
    val source = Uri.parse("content://picker/file/1")
    val existing = mockk<DocumentFile>(relaxed = true)
    val created = mockk<DocumentFile>(relaxed = true)
    every { created.uri } returns Uri.parse("content://tree/rom/RMCP01.iso")
    every { romDir.findFile("RMCP01.iso") } returns existing
    every { romDir.createFile("application/octet-stream", "RMCP01.iso") } returns created
    every { resolver.openInputStream(source) } returns ByteArrayInputStream(byteArrayOf(1, 2, 3))
    every { resolver.openOutputStream(created.uri) } returns ByteArrayOutputStream()

    val tree = DolphinTree(context, treeUri)
    tree.copyRomFromSource(source, "RMCP01", "iso")
    verify { existing.delete() }
  }

  // --- extractZipToPack ------------------------------------------------

  @Test
  fun `extractZipToPack writes each file entry into packDir`(@TempDir tempDir: Path) = runBlocking {
    val (_, packDir, _) = setupDirChain()
    val zip = File(tempDir.toFile(), "pack.zip")
    ZipOutputStream(zip.outputStream()).use { zos ->
      zos.putNextEntry(ZipEntry("file1.txt"))
      zos.write("hello".encodeToByteArray())
      zos.closeEntry()
      zos.putNextEntry(ZipEntry("file2.bin"))
      zos.write(byteArrayOf(0x01, 0x02, 0x03))
      zos.closeEntry()
    }

    val outputs = mutableMapOf<String, ByteArrayOutputStream>()
    setupPackEntryWrite(packDir, "file1.txt", outputs)
    setupPackEntryWrite(packDir, "file2.bin", outputs)

    val tree = DolphinTree(context, treeUri)
    val progress = mutableListOf<ExtractProgress>()
    tree.extractZipToPack(zip) { progress.add(it) }

    // Sequence: 1 PreparingFolders + 2 files × 2 callbacks (before/after).
    // filesDone walks 0, 0, 1, 1, 2; phase walks PF, W, W, W, W.
    assertThat(progress.map { it.filesDone }).containsExactly(0, 0, 1, 1, 2).inOrder()
    assertThat(progress.map { it.filesTotal }).containsExactly(2, 2, 2, 2, 2).inOrder()
    assertThat(progress.map { it.phase })
      .containsExactly(
        ExtractingPhase.PreparingFolders,
        ExtractingPhase.WritingFiles,
        ExtractingPhase.WritingFiles,
        ExtractingPhase.WritingFiles,
        ExtractingPhase.WritingFiles,
      )
      .inOrder()
    assertThat(outputs["file1.txt"]?.toString(Charsets.UTF_8)).isEqualTo("hello")
    assertThat(outputs["file2.bin"]?.toByteArray()?.toList())
      .isEqualTo(byteArrayOf(0x01, 0x02, 0x03).toList())
  }

  @Test
  fun `extractZipToPack creates nested subdirectories for entries with a path`(@TempDir tempDir: Path) =
    runBlocking {
      val (_, packDir, _) = setupDirChain()
      val zip = File(tempDir.toFile(), "pack.zip")
      ZipOutputStream(zip.outputStream()).use { zos ->
        zos.putNextEntry(ZipEntry("riivolution/RetroRewind6.xml"))
        zos.write("<xml/>".encodeToByteArray())
        zos.closeEntry()
      }
      val riivolutionDir = mockk<DocumentFile>(relaxed = true)
      every { packDir.findFile("riivolution") } returns null
      every { packDir.createDirectory("riivolution") } returns riivolutionDir
      val created = mockk<DocumentFile>(relaxed = true)
      every { created.uri } returns Uri.parse("content://tree/pack/riivolution/RetroRewind6.xml")
      every { riivolutionDir.createFile("application/octet-stream", "RetroRewind6.xml") } returns
        created
      val output = ByteArrayOutputStream()
      every { resolver.openOutputStream(created.uri) } returns output

      val tree = DolphinTree(context, treeUri)
      val progress = mutableListOf<ExtractProgress>()
      tree.extractZipToPack(zip) { progress.add(it) }

      verify { packDir.createDirectory("riivolution") }
      assertThat(output.toString(Charsets.UTF_8)).isEqualTo("<xml/>")
      // The directory pre-pass fires first.
      assertThat(progress.first().phase).isEqualTo(ExtractingPhase.PreparingFolders)
      assertThat(progress.first().currentFile).isNull()
      // The file write phase follows, with the live entry name.
      val writingEntries = progress.filter { it.phase == ExtractingPhase.WritingFiles }
      assertThat(writingEntries.map { it.currentFile })
        .containsExactly("riivolution/RetroRewind6.xml", "riivolution/RetroRewind6.xml")
        .inOrder()
    }

  @Test
  fun `extractZipToPack skips directory entries`(@TempDir tempDir: Path) = runBlocking {
    val (_, packDir, _) = setupDirChain()
    val zip = File(tempDir.toFile(), "pack.zip")
    ZipOutputStream(zip.outputStream()).use { zos ->
      // Directory entry followed by a file entry — directory should not
      // produce a DocumentFile.createFile call.
      zos.putNextEntry(ZipEntry("subdir/"))
      zos.closeEntry()
      zos.putNextEntry(ZipEntry("subdir/inside.txt"))
      zos.write("data".encodeToByteArray())
      zos.closeEntry()
    }
    val subdirDir = mockk<DocumentFile>(relaxed = true)
    every { packDir.findFile("subdir") } returns null
    every { packDir.createDirectory("subdir") } returns subdirDir
    val created = mockk<DocumentFile>(relaxed = true)
    every { created.uri } returns Uri.parse("content://tree/pack/subdir/inside.txt")
    every { subdirDir.findFile("inside.txt") } returns null
    every { subdirDir.createFile("application/octet-stream", "inside.txt") } returns created
    val output = ByteArrayOutputStream()
    every { resolver.openOutputStream(created.uri) } returns output

    val tree = DolphinTree(context, treeUri)
    val progress = mutableListOf<ExtractProgress>()
    tree.extractZipToPack(zip) { progress.add(it) }

    // Only one file entry → one pair of WritingFiles callbacks.
    val writingEntries = progress.filter { it.phase == ExtractingPhase.WritingFiles }
    assertThat(writingEntries.map { it.filesDone }).containsExactly(0, 1).inOrder()
    assertThat(writingEntries.first().currentFile).isEqualTo("subdir/inside.txt")
    // Only the file entry produces a write.
    verify(exactly = 0) { packDir.createFile(any<String>(), any<String>()) }
    verify(exactly = 1) { subdirDir.createFile("application/octet-stream", "inside.txt") }
  }

  @Test
  fun `extractZipToPack reports PreparingFolders then WritingFiles phases`(@TempDir tempDir: Path) =
    runBlocking {
      val (_, packDir, _) = setupDirChain()
      val zip = File(tempDir.toFile(), "pack.zip")
      ZipOutputStream(zip.outputStream()).use { zos ->
        zos.putNextEntry(ZipEntry("a/"))
        zos.closeEntry()
        zos.putNextEntry(ZipEntry("a/b.txt"))
        zos.write("x".encodeToByteArray())
        zos.closeEntry()
      }
      val aDir = mockk<DocumentFile>(relaxed = true)
      every { packDir.findFile("a") } returns null
      every { packDir.createDirectory("a") } returns aDir
      val created = mockk<DocumentFile>(relaxed = true)
      every { created.uri } returns Uri.parse("content://tree/pack/a/b.txt")
      every { aDir.createFile("application/octet-stream", "b.txt") } returns created
      val output = ByteArrayOutputStream()
      every { resolver.openOutputStream(created.uri) } returns output

      val tree = DolphinTree(context, treeUri)
      val progress = mutableListOf<ExtractProgress>()
      tree.extractZipToPack(zip) { progress.add(it) }

      assertThat(progress.first().phase).isEqualTo(ExtractingPhase.PreparingFolders)
      assertThat(progress.first().filesTotal).isEqualTo(1)
      assertThat(progress.first().currentFile).isNull()
      // Every subsequent callback is in the writing phase.
      assertThat(progress.drop(1).map { it.phase })
        .containsExactly(ExtractingPhase.WritingFiles, ExtractingPhase.WritingFiles)
    }

  @Test
  fun `extractZipToPack creates every ancestor directory even when no file lives at intermediate levels`(
    @TempDir tempDir: Path
  ) = runBlocking {
    // File at a/b/c/leaf.txt — no files directly under a/ or a/b/ on
    // their own. The pre-pass must still create the full a -> a/b ->
    // a/b/c chain, which it didn't before the ancestor-set fix
    // (the direct-parent-only set missed "a" and "a/b" entirely).
    val (_, packDir, _) = setupDirChain()
    val zip = File(tempDir.toFile(), "pack.zip")
    ZipOutputStream(zip.outputStream()).use { zos ->
      zos.putNextEntry(ZipEntry("a/b/c/leaf.txt"))
      zos.write("deep".encodeToByteArray())
      zos.closeEntry()
    }
    val aDir = mockk<DocumentFile>(relaxed = true)
    val abDir = mockk<DocumentFile>(relaxed = true)
    val abcDir = mockk<DocumentFile>(relaxed = true)
    every { packDir.findFile("a") } returns null
    every { packDir.createDirectory("a") } returns aDir
    every { aDir.findFile("b") } returns null
    every { aDir.createDirectory("b") } returns abDir
    every { abDir.findFile("c") } returns null
    every { abDir.createDirectory("c") } returns abcDir
    val created = mockk<DocumentFile>(relaxed = true)
    every { created.uri } returns Uri.parse("content://tree/pack/a/b/c/leaf.txt")
    every { abcDir.createFile("application/octet-stream", "leaf.txt") } returns created
    val output = ByteArrayOutputStream()
    every { resolver.openOutputStream(created.uri) } returns output

    val tree = DolphinTree(context, treeUri)
    tree.extractZipToPack(zip) { /* no-op */ }

    verify { packDir.createDirectory("a") }
    verify { aDir.createDirectory("b") }
    verify { abDir.createDirectory("c") }
    assertThat(output.toString(Charsets.UTF_8)).isEqualTo("deep")
  }

  // --- writeLaunchJson / readLaunchJson --------------------------------

  @Test
  fun `writeLaunchJson writes content as rr_autostartfile json`() {
    val (_, _, wheelWitchDir) = setupDirChain()
    val file = mockk<DocumentFile>(relaxed = true)
    every { file.uri } returns Uri.parse("content://tree/rr_autostartfile.json")
    every { wheelWitchDir.findFile(DolphinTree.LAUNCH_JSON_NAME) } returns null
    every {
      wheelWitchDir.createFile("application/json", DolphinTree.LAUNCH_JSON_NAME)
    } returns file
    val output = ByteArrayOutputStream()
    every { resolver.openOutputStream(file.uri) } returns output

    val tree = DolphinTree(context, treeUri)
    val result = tree.writeLaunchJson("""{"base-file":"/x"}""")

    assertThat(result).isEqualTo(file)
    assertThat(output.toString(Charsets.UTF_8)).isEqualTo("""{"base-file":"/x"}""")
  }

  @Test
  fun `writeLaunchJson replaces an existing file with the same name`() {
    val (_, _, wheelWitchDir) = setupDirChain()
    val existing = mockk<DocumentFile>(relaxed = true)
    val file = mockk<DocumentFile>(relaxed = true)
    every { file.uri } returns Uri.parse("content://tree/rr_autostartfile.json")
    every { wheelWitchDir.findFile(DolphinTree.LAUNCH_JSON_NAME) } returns existing
    every {
      wheelWitchDir.createFile("application/json", DolphinTree.LAUNCH_JSON_NAME)
    } returns file
    val output = ByteArrayOutputStream()
    every { resolver.openOutputStream(file.uri) } returns output

    val tree = DolphinTree(context, treeUri)
    tree.writeLaunchJson("new")

    verify { existing.delete() }
  }

  @Test
  fun `readLaunchJson returns null when the file does not exist`() {
    val (_, _, wheelWitchDir) = setupDirChain()
    every { wheelWitchDir.findFile(DolphinTree.LAUNCH_JSON_NAME) } returns null

    val tree = DolphinTree(context, treeUri)
    assertThat(tree.readLaunchJson()).isNull()
  }

  @Test
  fun `readLaunchJson returns the file content as UTF-8`() {
    val (_, _, wheelWitchDir) = setupDirChain()
    val file = mockk<DocumentFile>(relaxed = true)
    every { file.uri } returns Uri.parse("content://tree/rr_autostartfile.json")
    every { wheelWitchDir.findFile(DolphinTree.LAUNCH_JSON_NAME) } returns file
    every { resolver.openInputStream(file.uri) } returns
      ByteArrayInputStream("payload".encodeToByteArray())

    val tree = DolphinTree(context, treeUri)
    assertThat(tree.readLaunchJson()).isEqualTo("payload")
  }

  // --- readVersion / writeVersion -------------------------------------

  @Test
  fun `readVersion returns null when the version file is missing`() {
    val (_, packDir, _) = setupDirChain()
    every { packDir.findFile(DolphinTree.VERSION_FILE_NAME) } returns null

    val tree = DolphinTree(context, treeUri)
    assertThat(tree.readVersion()).isNull()
  }

  @Test
  fun `readVersion returns null when the file contents are not a valid semver`() {
    val (_, packDir, _) = setupDirChain()
    val file = mockk<DocumentFile>(relaxed = true)
    every { file.uri } returns Uri.parse("content://tree/pack/version.txt")
    every { packDir.findFile(DolphinTree.VERSION_FILE_NAME) } returns file
    every { resolver.openInputStream(file.uri) } returns
      ByteArrayInputStream("not-a-version".encodeToByteArray())

    val tree = DolphinTree(context, treeUri)
    assertThat(tree.readVersion()).isNull()
  }

  @Test
  fun `readVersion parses a valid semver from the pack dir`() {
    val (_, packDir, _) = setupDirChain()
    val file = mockk<DocumentFile>(relaxed = true)
    every { file.uri } returns Uri.parse("content://tree/pack/version.txt")
    every { packDir.findFile(DolphinTree.VERSION_FILE_NAME) } returns file
    every { resolver.openInputStream(file.uri) } returns
      ByteArrayInputStream("3.2.6\n".encodeToByteArray())

    val tree = DolphinTree(context, treeUri)
    val version = tree.readVersion()
    assertThat(version).isNotNull()
    assertThat(version!!.major).isEqualTo(3)
    assertThat(version.minor).isEqualTo(2)
    assertThat(version.patch).isEqualTo(6)
  }

  @Test
  fun `writeVersion replaces an existing version file and writes the toString form`() = runBlocking {
    val (_, packDir, _) = setupDirChain()
    val existing = mockk<DocumentFile>(relaxed = true)
    val file = mockk<DocumentFile>(relaxed = true)
    val fileUri = mockk<Uri>(relaxed = true)
    every { file.uri } returns fileUri
    every { packDir.findFile(DolphinTree.VERSION_FILE_NAME) } returns existing
    every {
      packDir.createFile("text/plain", DolphinTree.VERSION_FILE_NAME)
    } returns file
    val output = ByteArrayOutputStream()
    every { resolver.openOutputStream(fileUri) } returns output

    val tree = DolphinTree(context, treeUri)
    tree.writeVersion(com.skiletro.wheelwitch.model.SemVersion(3, 2, 6))

    verify { existing.delete() }
    assertThat(output.toString(Charsets.UTF_8)).isEqualTo("3.2.6")
  }

  // --- persistUriPermission / releaseUriPermission --------------------

  @Test
  fun `persistUriPermission takes read and write flags`() {
    val tree = DolphinTree(context, treeUri)
    tree.persistUriPermission()
    verify {
      resolver.takePersistableUriPermission(
        treeUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    }
  }

  @Test
  fun `releaseUriPermission releases read and write flags`() {
    val tree = DolphinTree(context, treeUri)
    tree.releaseUriPermission()
    verify {
      resolver.releasePersistableUriPermission(
        treeUri,
        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
      )
    }
  }

  // --- readConfigIni / writeConfigIni ---------------------------------

  @Test
  fun `readConfigIni returns null when Config dir is missing`() {
    every { root.findFile("Config") } returns null
    every { root.createDirectory("Config") } returns null

    val tree = DolphinTree(context, treeUri)

    assertThat(tree.readConfigIni()).isNull()
  }

  @Test
  fun `readConfigIni returns null when Dolphin ini is missing under Config`() {
    val configDir = mockk<DocumentFile>(relaxed = true)
    every { root.findFile("Config") } returns configDir
    every { configDir.isDirectory } returns true
    every { configDir.findFile(DolphinTree.CONFIG_INI_NAME) } returns null

    val tree = DolphinTree(context, treeUri)

    assertThat(tree.readConfigIni()).isNull()
  }

  @Test
  fun `readConfigIni returns the file content as UTF-8`() {
    val configDir = mockk<DocumentFile>(relaxed = true)
    val file = mockk<DocumentFile>(relaxed = true)
    every { root.findFile("Config") } returns configDir
    every { configDir.isDirectory } returns true
    every { configDir.findFile(DolphinTree.CONFIG_INI_NAME) } returns file
    val fileUri = mockk<Uri>(relaxed = true)
    every { file.uri } returns fileUri
    every { resolver.openInputStream(fileUri) } returns
      ByteArrayInputStream("[General]\nISOPaths = 0\n".encodeToByteArray())

    val tree = DolphinTree(context, treeUri)

    assertThat(tree.readConfigIni()).isEqualTo("[General]\nISOPaths = 0\n")
  }

  @Test
  fun `writeConfigIni creates the Config dir and writes the file contents`() {
    val configDir = mockk<DocumentFile>(relaxed = true)
    val file = mockk<DocumentFile>(relaxed = true)
    val fileUri = mockk<Uri>(relaxed = true)
    every { file.uri } returns fileUri
    every { root.findFile("Config") } returns null
    every { root.createDirectory("Config") } returns configDir
    every { configDir.findFile(DolphinTree.CONFIG_INI_NAME) } returns null
    every {
      configDir.createFile("text/plain", DolphinTree.CONFIG_INI_NAME)
    } returns file
    val output = ByteArrayOutputStream()
    every { resolver.openOutputStream(fileUri) } returns output

    val tree = DolphinTree(context, treeUri)
    val result = tree.writeConfigIni("[General]\nISOPaths = 1\nISOPath0 = /x\n")

    assertThat(result).isEqualTo(file)
    assertThat(output.toString(Charsets.UTF_8))
      .isEqualTo("[General]\nISOPaths = 1\nISOPath0 = /x\n")
  }

  @Test
  fun `writeConfigIni deletes a pre-existing ini before creating`() {
    val configDir = mockk<DocumentFile>(relaxed = true)
    val existing = mockk<DocumentFile>(relaxed = true)
    val file = mockk<DocumentFile>(relaxed = true)
    val fileUri = mockk<Uri>(relaxed = true)
    every { file.uri } returns fileUri
    every { root.findFile("Config") } returns configDir
    every { configDir.isDirectory } returns true
    every { configDir.findFile(DolphinTree.CONFIG_INI_NAME) } returns existing
    every {
      configDir.createFile("text/plain", DolphinTree.CONFIG_INI_NAME)
    } returns file
    val output = ByteArrayOutputStream()
    every { resolver.openOutputStream(fileUri) } returns output

    val tree = DolphinTree(context, treeUri)
    tree.writeConfigIni("payload")

    verify { existing.delete() }
    assertThat(output.toString(Charsets.UTF_8)).isEqualTo("payload")
  }

  // --- helpers ---------------------------------------------------------

  /**
   * Stubs the lazy subdirectory chain so the full User/Wii/WheelWitch
   * path resolves to fresh [DocumentFile] mocks. Returns
   * `(romDir, packDir, wheelWitchDir)`.
   */
  private fun setupDirChain(): Triple<DocumentFile, DocumentFile, DocumentFile> {
    val userDir = mockk<DocumentFile>(relaxed = true)
    val wiiDir = mockk<DocumentFile>(relaxed = true)
    val wheelWitchDir = mockk<DocumentFile>(relaxed = true)
    val romDir = mockk<DocumentFile>(relaxed = true)
    val packDir = mockk<DocumentFile>(relaxed = true)
    every { root.findFile("User") } returns userDir
    every { userDir.isDirectory } returns true
    every { userDir.findFile("Wii") } returns wiiDir
    every { wiiDir.isDirectory } returns true
    every { wiiDir.findFile("WheelWitch") } returns wheelWitchDir
    every { wheelWitchDir.isDirectory } returns true
    every { wheelWitchDir.findFile("rom") } returns romDir
    every { wheelWitchDir.findFile("pack") } returns packDir
    every { romDir.isDirectory } returns true
    every { packDir.isDirectory } returns true
    return Triple(romDir, packDir, wheelWitchDir)
  }

  /** Stubs a top-level pack entry write: a file `<name>` is created and its bytes captured. */
  private fun setupPackEntryWrite(
    packDir: DocumentFile,
    name: String,
    outputs: MutableMap<String, ByteArrayOutputStream>,
  ) {
    val file = mockk<DocumentFile>(relaxed = true)
    // Use a unique mock Uri per file so the byte stream destinations
    // don't collapse — Uri.parse under unitTests.isReturnDefaultValues
    // returns null, so we can't rely on it.
    val uri = mockk<Uri>(relaxed = true)
    every { file.uri } returns uri
    every { packDir.createFile("application/octet-stream", name) } returns file
    val baos = ByteArrayOutputStream()
    outputs[name] = baos
    every { resolver.openOutputStream(uri) } returns baos
  }

  /** Stubs `Prefs.main(context)` to return [prefs] for tests that read or write prefs. */
  private fun mockPrefsMain(prefs: SharedPreferences) {
    mockkStatic("com.skiletro.wheelwitch.util.prefs.Prefs")
    io.mockk.every { com.skiletro.wheelwitch.util.prefs.Prefs.main(context) } returns prefs
  }
}
