package com.skiletro.wheelwitch.data

import android.content.ContentResolver
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.google.common.truth.Truth.assertThat
import com.skiletro.wheelwitch.data.SaveManager.Region
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SaveManagerTest {

  @Test
  fun `regionFromRomFileName returns correct region for all three codes`() {
    assertThat(SaveManager.regionFromRomFileName("RMCP01.iso")).isEqualTo(Region.PAL)
    assertThat(SaveManager.regionFromRomFileName("RMCE01.rvz")).isEqualTo(Region.USA)
    assertThat(SaveManager.regionFromRomFileName("RMCJ01.wbfs")).isEqualTo(Region.JPN)
  }

  @Test
  fun `regionFromRomFileName is case-insensitive`() {
    assertThat(SaveManager.regionFromRomFileName("rmcp01.iso")).isEqualTo(Region.PAL)
    assertThat(SaveManager.regionFromRomFileName("RmCe01.rvz")).isEqualTo(Region.USA)
  }

  @Test
  fun `regionFromRomFileName returns null for unknown prefix`() {
    assertThat(SaveManager.regionFromRomFileName("random.iso")).isNull()
    assertThat(SaveManager.regionFromRomFileName("")).isNull()
    assertThat(SaveManager.regionFromRomFileName("MKW.iso")).isNull()
  }

  @Test
  fun `listRegions returns distinct regions present in romDir`() {
    val tree = mockk<DolphinTree>(relaxed = true)
    val romDir = mockk<DocumentFile>(relaxed = true)
    every { tree.romDir } returns romDir
    every { romDir.listFiles() } returns
      arrayOf(
        mockRomFile("RMCP01.iso"),
        mockRomFile("RMCE01.rvz"),
        mockRomFile("RMCP01.iso"), // duplicate name — should dedupe
        mockRomFile("README.txt"), // unknown — should be filtered
      )

    val regions = SaveManager.listRegions(tree)

    assertThat(regions).containsExactly(Region.PAL, Region.USA).inOrder()
  }

  @Test
  fun `hasSave returns true when rksys dot dat exists for the region`() {
    val chain = setupSaveChain(region = Region.PAL, fileExists = true)
    assertThat(SaveManager.hasSave(chain.tree, Region.PAL)).isTrue()
  }

  @Test
  fun `hasSave returns false when rksys dot dat is missing for the region`() {
    val chain = setupSaveChain(region = Region.PAL, fileExists = false)
    assertThat(SaveManager.hasSave(chain.tree, Region.PAL)).isFalse()
  }

  @Test
  fun `backup copies save bytes to the user-picked destination`() = runTest {
    val chain = setupSaveChain(region = Region.PAL, fileExists = true, content = byteArrayOf(1, 2, 3))
    val dest = mockk<Uri>(relaxed = true)
    val destOutput = ByteArrayOutputStream()
    every { chain.resolver.openOutputStream(dest) } returns destOutput

    val result = SaveManager.backup(chain.tree, Region.PAL, dest)

    assertThat(result.isSuccess).isTrue()
    assertThat(destOutput.toByteArray()).isEqualTo(byteArrayOf(1, 2, 3))
  }

  @Test
  fun `restore reads source bytes and overwrites the region save`() = runTest {
    val chain = setupSaveChain(region = Region.USA, fileExists = true, content = byteArrayOf(0x55))
    val source = mockk<Uri>(relaxed = true)
    every { chain.resolver.openInputStream(source) } returns
      ByteArrayInputStream(byteArrayOf(7, 8, 9))
    val writeOutput = ByteArrayOutputStream()
    every { chain.resolver.openOutputStream(chain.saveFile.uri) } returns writeOutput

    val result = SaveManager.restore(chain.tree, Region.USA, source)

    assertThat(result.isSuccess).isTrue()
    assertThat(writeOutput.toByteArray()).isEqualTo(byteArrayOf(7, 8, 9))
  }

  @Test
  fun `delete invokes DocumentFile delete on the existing save file`() = runTest {
    val chain = setupSaveChain(region = Region.JPN, fileExists = true)

    val result = SaveManager.delete(chain.tree, Region.JPN)

    assertThat(result.isSuccess).isTrue()
    verify { chain.saveFile.delete() }
  }

  // --- helpers ----------------------------------------------------------

  /** Holds the mocks wired up by [setupSaveChain] so individual tests can inspect them. */
  private class MockSaveChain(
    val tree: DolphinTree,
    val resolver: ContentResolver,
    val packDir: DocumentFile,
    val regionDir: DocumentFile,
    val saveFile: DocumentFile,
  )

  /**
   * Wires up a save chain of the form
   * `packDir/riivolution/save/RetroWFC/<regionCode>/rksys.dat`. The
   * intermediate directory documents are explicitly marked as
   * directories (relaxed mocks default to `isDirectory = false`,
   * which would make the production `navigateOrCreate` try to create
   * a new directory and break the chain). `findFile` returns the
   * next link; `createDirectory` is also stubbed (to a relaxed
   * DocumentFile) so any un-stubbed branch still terminates cleanly.
   *
   * [content] is the bytes the resolver returns for the save file
   * input stream — used by the read paths.
   */
  private fun setupSaveChain(
    region: Region,
    fileExists: Boolean,
    content: ByteArray = ByteArray(0),
  ): MockSaveChain {
    val tree = mockk<DolphinTree>(relaxed = true)
    val resolver = mockk<ContentResolver>(relaxed = true)
    val packDir = mockk<DocumentFile>(relaxed = true)
    val dir1 = mockDir("riivolution")
    val dir2 = mockDir("save")
    val dir3 = mockDir("RetroWFC")
    val dir4 = mockDir(region.code)
    val saveFile = mockk<DocumentFile>(relaxed = true)
    val saveFileUri = mockk<Uri>(relaxed = true)

    every { tree.resolver } returns resolver
    every { tree.packDir } returns packDir

    every { packDir.findFile("riivolution") } returns dir1
    every { dir1.findFile("save") } returns dir2
    every { dir2.findFile("RetroWFC") } returns dir3
    every { dir3.findFile(region.code) } returns dir4
    every { dir4.findFile(SaveManager.SAVE_FILE_NAME) } returns saveFile
    every { dir4.createFile(any(), any()) } returns saveFile
    every { saveFile.exists() } returns fileExists
    every { saveFile.uri } returns saveFileUri
    every { resolver.openInputStream(saveFileUri) } returns ByteArrayInputStream(content)

    return MockSaveChain(tree, resolver, packDir, dir4, saveFile)
  }

  /** A DocumentFile mock with `isDirectory = true` so `navigateOrCreate` keeps the chain. */
  private fun mockDir(@Suppress("UNUSED_PARAMETER") name: String): DocumentFile {
    val dir = mockk<DocumentFile>(relaxed = true)
    every { dir.isDirectory } returns true
    return dir
  }

  private fun mockRomFile(name: String): DocumentFile {
    val file = mockk<DocumentFile>(relaxed = true)
    every { file.name } returns name
    return file
  }
}
