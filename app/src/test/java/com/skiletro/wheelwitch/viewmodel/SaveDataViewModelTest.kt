package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import com.google.common.truth.Truth.assertThat
import com.skiletro.wheelwitch.data.DolphinTree
import com.skiletro.wheelwitch.data.RksysParser
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.data.SaveManager.Region
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.SemVersion
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SaveDataViewModelTest {

  private val app: Application = mockk(relaxed = true)
  private lateinit var packStatus: MutableStateFlow<UiState>
  private lateinit var mockTree: DolphinTree
  private lateinit var vm: SaveDataViewModel

  private val leaderboardResult = mutableMapOf<String, Result<Pair<Int, String?>>>()

  @BeforeEach
  fun setUp() {
    // Replace both Main and IO with the test dispatcher so the
    // `async(Dispatchers.IO)` blocks inside refresh() synchronize
    // with the test scheduler. Without this, the IO work runs on
    // real IO threads that the test never waits for, and
    // `saveInfos` ends up empty when the test asserts.
    val testDispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(testDispatcher)
    ioDispatcher = testDispatcher
    packStatus = MutableStateFlow(UiState.Idle)
    mockTree = mockk(relaxed = true)
    mockkObject(SaveManager)
    leaderboardResult.clear()
    leaderboardResult["1234-5678-9012"] = Result.success(9999 to "mii-b64")
  }

  @AfterEach
  fun tearDown() {
    Dispatchers.resetMain()
    unmockkObject(SaveManager)
  }

  private lateinit var ioDispatcher: kotlinx.coroutines.CoroutineDispatcher

  private fun buildVm(
    tree: DolphinTree? = mockTree,
    leaderboard: suspend (String) -> Result<Pair<Int, String?>> = { code ->
      leaderboardResult[code] ?: Result.failure(RuntimeException("no stub for $code"))
    },
  ): SaveDataViewModel =
    SaveDataViewModel(
      application = app,
      packStatusFlow = packStatus as StateFlow<UiState>,
      treeFactory = { tree },
      leaderboardFetcher = leaderboard,
      ioDispatcher = ioDispatcher,
    )

  @Test
  fun `init with no tree keeps saveInfos empty and no error`() = runTest {
    vm = buildVm(tree = null)

    assertThat(vm.saveInfos.value).isEmpty()
    assertThat(vm.hasSave.value).isEmpty()
    assertThat(vm.activeLicense.value).isNull()
    assertThat(vm.error.value).isNull()
  }

  @Test
  fun `init with tree refreshes when packStatusFlow emits Ready`() = runTest {
    every { SaveManager.listRegions(mockTree) } returns emptyList()
    vm = buildVm()

    packStatus.value = UiState.Ready(PackStatus.UpToDate(SemVersion(3, 2, 6), SemVersion(3, 2, 6)))

    assertThat(vm.saveInfos.value).isEmpty()
    coVerify { SaveManager.listRegions(mockTree) }
  }

  @Test
  fun `refresh with no tree clears state without error`() = runTest {
    vm = buildVm(tree = null)
    vm.refresh()

    assertThat(vm.saveInfos.value).isEmpty()
    assertThat(vm.hasSave.value).isEmpty()
    assertThat(vm.error.value).isNull()
  }

  @Test
  fun `refresh with tree but no ROMs clears save state`() = runTest {
    every { SaveManager.listRegions(mockTree) } returns emptyList()
    vm = buildVm()

    vm.refresh()

    assertThat(vm.saveInfos.value).isEmpty()
    assertThat(vm.hasSave.value).isEmpty()
    assertThat(vm.selectedRegion.value).isNull()
    assertThat(vm.activeLicense.value).isNull()
  }

  @Test
  fun `refresh parses each region's save in parallel`() = runTest {
    val palBytes = rksysWithLicense(pid = 0x11111111L, name = "PAL", slot = 0)
    val usaBytes = rksysWithLicense(pid = 0x22222222L, name = "USA", slot = 0)
    val palInfo = RksysParser.parse(palBytes)
    val usaInfo = RksysParser.parse(usaBytes)

    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL, Region.USA)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns palBytes
    coEvery { SaveManager.readSave(mockTree, Region.USA) } returns usaBytes
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
    coEvery { SaveManager.hasSave(mockTree, Region.USA) } returns true
    vm = buildVm()

    vm.refresh()

    assertThat(vm.saveInfos.value).containsExactly(Region.PAL, palInfo, Region.USA, usaInfo)
    assertThat(vm.hasSave.value).containsExactly(Region.PAL, true, Region.USA, true)
    assertThat(vm.selectedRegion.value).isEqualTo(Region.PAL)
  }

  @Test
  fun `refreshActiveLicense merges leaderboard VR and Mii for selected slot`() = runTest {
    val bytes = rksysWithLicense(pid = 0x00000001L, name = "X", slot = 0)
    val info = RksysParser.parse(bytes)
    val friendCode = info.licenses[0].friendCode!!
    leaderboardResult[friendCode] = Result.success(9999 to "mii-b64")

    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns bytes
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
    vm = buildVm()

    vm.refresh()
    val active = vm.activeLicense.value
    assertThat(active).isNotNull()
    assertThat(active!!.friendCode).isEqualTo(friendCode)
    assertThat(active.leaderboardVr).isEqualTo(9999)
    assertThat(active.leaderboardMiiImageBase64).isEqualTo("mii-b64")
  }

  @Test
  fun `refreshActiveLicense returns null for a slot that does not exist`() = runTest {
    val bytes = rksysWithLicense(pid = 0x11111111L, name = "A", slot = 0)
    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns bytes
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
    vm = buildVm()

    vm.selectSlot(2) // slot 2 is `exists = false`
    vm.refresh()

    assertThat(vm.activeLicense.value).isNull()
  }

  @Test
  fun `selectRegion updates state and refreshes active license`() = runTest {
    val palBytes = rksysWithLicense(pid = 0x11111111L, name = "PAL", slot = 0)
    val usaBytes = rksysWithLicense(pid = 0x22222222L, name = "USA", slot = 0)
    val palInfo = RksysParser.parse(palBytes)
    val usaInfo = RksysParser.parse(usaBytes)

    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL, Region.USA)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns palBytes
    coEvery { SaveManager.readSave(mockTree, Region.USA) } returns usaBytes
    coEvery { SaveManager.hasSave(mockTree, any()) } returns true
    vm = buildVm()
    vm.refresh()

    vm.selectRegion(Region.USA)

    assertThat(vm.selectedRegion.value).isEqualTo(Region.USA)
    val active = vm.activeLicense.value
    assertThat(active).isNotNull()
    assertThat(active!!.friendCode).isEqualTo(usaInfo.licenses[0].friendCode)
  }

  @Test
  fun `selectSlot persists to prefs and refreshes active license`() = runTest {
    // Two valid slots: 0 and 1
    val bytes = ByteArray(0x20000)
    for ((slot, base) in RksysParser.LICENSE_BASES.withIndex()) {
      if (slot > 1) break
      writeAscii(bytes, base, "RKPD")
      writeUtf16Be(bytes, base + 0x14, if (slot == 0) "Zero" else "One")
      writeUInt32Be(bytes, base + 0x5C, if (slot == 0) 0x00000010L else 0x00000011L)
    }
    val info = RksysParser.parse(bytes)

    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns bytes
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
    vm = buildVm()
    vm.refresh()

    vm.selectSlot(1)

    assertThat(vm.selectedSlotIndex.value).isEqualTo(1)
    val active = vm.activeLicense.value
    assertThat(active).isNotNull()
    assertThat(active!!.slotIndex).isEqualTo(1)
    assertThat(active.miiName).isEqualTo("One")
    // sanity: the parsed info also has slot 0
    assertThat(info.licenses[0].exists).isTrue()
  }

  @Test
  fun `backupSave delegates to SaveManager backup and refreshes on success`() = runTest {
    coEvery { SaveManager.backup(mockTree, Region.PAL, any()) } returns Result.success(Unit)
    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns null
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns false
    vm = buildVm()

    vm.backupSave(Region.PAL, mockk(relaxed = true))

    coVerify { SaveManager.backup(mockTree, Region.PAL, any()) }
    coVerify { SaveManager.listRegions(mockTree) }
  }

  @Test
  fun `restoreSave delegates to SaveManager restore and refreshes on success`() = runTest {
    coEvery { SaveManager.restore(mockTree, Region.USA, any()) } returns Result.success(Unit)
    every { SaveManager.listRegions(mockTree) } returns listOf(Region.USA)
    coEvery { SaveManager.readSave(mockTree, Region.USA) } returns null
    coEvery { SaveManager.hasSave(mockTree, Region.USA) } returns false
    vm = buildVm()

    vm.restoreSave(Region.USA, mockk(relaxed = true))

    coVerify { SaveManager.restore(mockTree, Region.USA, any()) }
    coVerify { SaveManager.listRegions(mockTree) }
  }

  @Test
  fun `deleteSave delegates to SaveManager delete and refreshes on success`() = runTest {
    coEvery { SaveManager.delete(mockTree, Region.JPN) } returns Result.success(Unit)
    every { SaveManager.listRegions(mockTree) } returns listOf(Region.JPN)
    coEvery { SaveManager.readSave(mockTree, Region.JPN) } returns null
    coEvery { SaveManager.hasSave(mockTree, Region.JPN) } returns false
    vm = buildVm()

    vm.deleteSave(Region.JPN)

    coVerify { SaveManager.delete(mockTree, Region.JPN) }
    coVerify { SaveManager.listRegions(mockTree) }
  }

  // --- helpers ----------------------------------------------------------

  /**
   * Builds a minimal `rksys.dat` byte array with one valid license at
   * the given [slot] and a 20-byte UTF-16BE [name]. All other fields
   * (VR, race counts, Mii RFL) are zero. The array is large enough
   * for all 4 license slots.
   */
  private fun rksysWithLicense(pid: Long, name: String, slot: Int): ByteArray {
    val bytes = ByteArray(0x20000)
    val base = RksysParser.LICENSE_BASES[slot]
    writeAscii(bytes, base, "RKPD")
    writeUtf16Be(bytes, base + 0x14, name)
    writeUInt32Be(bytes, base + 0x5C, pid)
    return bytes
  }

  private fun writeAscii(bytes: ByteArray, offset: Int, text: String) {
    val data = text.encodeToByteArray()
    data.copyInto(bytes, offset)
  }

  private fun writeUtf16Be(bytes: ByteArray, offset: Int, text: String) {
    for ((i, c) in text.withIndex()) {
      val pos = offset + i * 2
      bytes[pos] = (c.code shr 8).toByte()
      bytes[pos + 1] = c.code.toByte()
    }
  }

  private fun writeUInt32Be(bytes: ByteArray, offset: Int, value: Long) {
    bytes[offset] = (value shr 24).toByte()
    bytes[offset + 1] = (value shr 16).toByte()
    bytes[offset + 2] = (value shr 8).toByte()
    bytes[offset + 3] = value.toByte()
  }
}
