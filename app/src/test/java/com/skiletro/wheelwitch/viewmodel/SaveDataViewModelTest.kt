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
import kotlinx.coroutines.flow.first
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
  private var leaderboardCalls = 0

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
    leaderboardCalls = 0
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
      leaderboardCalls++
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
    assertThat(vm.mergedLicenses.value).isEmpty()
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
    assertThat(vm.mergedLicenses.value).isEmpty()
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
    assertThat(vm.mergedLicenses.value).isEmpty()
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
  fun `refresh populates mergedLicenses with leaderboard data for all 4 slots of selected region`() =
    runTest {
      // Slots 0 and 1 valid with leaderboard results; slots 2 and 3 empty.
      val bytes = ByteArray(0x20000)
      for ((slot, base) in RksysParser.LICENSE_BASES.withIndex()) {
        if (slot > 1) break
        writeAscii(bytes, base, "RKPD")
        writeUtf16Be(bytes, base + 0x14, if (slot == 0) "Zero" else "One")
        writeUInt32Be(bytes, base + 0x5C, if (slot == 0) 0x00000010L else 0x00000011L)
      }
      val info = RksysParser.parse(bytes)
      val fc0 = info.licenses[0].friendCode!!
      val fc1 = info.licenses[1].friendCode!!
      leaderboardResult[fc0] = Result.success(1000 to "mii-0")
      leaderboardResult[fc1] = Result.success(2000 to "mii-1")

      every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
      coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns bytes
      coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
      vm = buildVm()

      vm.refresh()

      val merged = vm.mergedLicenses.value[Region.PAL]
      assertThat(merged).hasSize(4)
      assertThat(merged!![0].leaderboardVr).isEqualTo(1000)
      assertThat(merged[0].leaderboardMiiImageBase64).isEqualTo("mii-0")
      assertThat(merged[1].leaderboardVr).isEqualTo(2000)
      assertThat(merged[1].leaderboardMiiImageBase64).isEqualTo("mii-1")
      assertThat(merged[2].exists).isFalse()
      assertThat(merged[3].exists).isFalse()
      assertThat(leaderboardCalls).isEqualTo(2)
    }

  @Test
  fun `activeLicense merges leaderboard VR and Mii for selected slot`() = runTest {
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
  fun `activeLicense is null for a slot that does not exist`() = runTest {
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
  fun `selectRegion triggers leaderboard fetch for the new region and updates activeLicense`() =
    runTest {
      val palBytes = rksysWithLicense(pid = 0x11111111L, name = "PAL", slot = 0)
      val usaBytes = rksysWithLicense(pid = 0x22222222L, name = "USA", slot = 0)
      val palInfo = RksysParser.parse(palBytes)
      val usaInfo = RksysParser.parse(usaBytes)
      val palFc = palInfo.licenses[0].friendCode!!
      val usaFc = usaInfo.licenses[0].friendCode!!
      leaderboardResult[palFc] = Result.success(1111 to "mii-pal")
      leaderboardResult[usaFc] = Result.success(2222 to "mii-usa")

      every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL, Region.USA)
      coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns palBytes
      coEvery { SaveManager.readSave(mockTree, Region.USA) } returns usaBytes
      coEvery { SaveManager.hasSave(mockTree, any()) } returns true
      vm = buildVm()
      vm.refresh()
      // After the initial refresh only the selected region (PAL) is
      // merged; USA is not.
      assertThat(vm.mergedLicenses.value).doesNotContainKey(Region.USA)
      val callsAfterRefresh = leaderboardCalls

      vm.selectRegion(Region.USA)

      assertThat(vm.selectedRegion.value).isEqualTo(Region.USA)
      val merged = vm.mergedLicenses.value[Region.USA]
      assertThat(merged).hasSize(4)
      assertThat(merged!![0].leaderboardVr).isEqualTo(2222)
      val active = vm.activeLicense.value
      assertThat(active).isNotNull()
      assertThat(active!!.friendCode).isEqualTo(usaFc)
      assertThat(active.leaderboardVr).isEqualTo(2222)
      // Exactly one new leaderboard call (USA slot 0).
      assertThat(leaderboardCalls - callsAfterRefresh).isEqualTo(1)
    }

  @Test
  fun `selectRegion with the same region is a no-op`() = runTest {
    val bytes = rksysWithLicense(pid = 0x00000001L, name = "X", slot = 0)
    val info = RksysParser.parse(bytes)
    leaderboardResult[info.licenses[0].friendCode!!] = Result.success(100 to "img")

    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns bytes
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
    vm = buildVm()
    vm.refresh()
    val callsAfterRefresh = leaderboardCalls

    vm.selectRegion(Region.PAL) // same region

    assertThat(leaderboardCalls).isEqualTo(callsAfterRefresh)
  }

  @Test
  fun `selectSlot persists to prefs and updates activeLicense from mergedLicenses without re-fetching`() =
    runTest {
      // Two valid slots: 0 and 1
      val bytes = ByteArray(0x20000)
      for ((slot, base) in RksysParser.LICENSE_BASES.withIndex()) {
        if (slot > 1) break
        writeAscii(bytes, base, "RKPD")
        writeUtf16Be(bytes, base + 0x14, if (slot == 0) "Zero" else "One")
        writeUInt32Be(bytes, base + 0x5C, if (slot == 0) 0x00000010L else 0x00000011L)
      }
      val info = RksysParser.parse(bytes)
      leaderboardResult[info.licenses[0].friendCode!!] = Result.success(1000 to "mii-0")
      leaderboardResult[info.licenses[1].friendCode!!] = Result.success(2000 to "mii-1")

      every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
      coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns bytes
      coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
      vm = buildVm()
      vm.refresh()
      val callsAfterRefresh = leaderboardCalls

      vm.selectSlot(1)

      assertThat(vm.selectedSlotIndex.value).isEqualTo(1)
      // The combined activeLicense re-projects from mergedLicenses
      // without an extra leaderboard round trip.
      val active = vm.activeLicense.first()
      assertThat(active).isNotNull()
      assertThat(active!!.slotIndex).isEqualTo(1)
      assertThat(active.miiName).isEqualTo("One")
      assertThat(active.leaderboardVr).isEqualTo(2000)
      assertThat(leaderboardCalls).isEqualTo(callsAfterRefresh)
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

  @Test
  fun `deleteSave updates mergedLicenses to four empty slots for the deleted region`() = runTest {
    val bytes = rksysWithLicense(pid = 0x00000010L, name = "Alice", slot = 0)
    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL)
    // First refresh: save exists and parses.
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns bytes
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
    vm = buildVm()
    vm.refresh()
    assertThat(vm.mergedLicenses.value[Region.PAL]?.get(0)?.exists).isTrue()

    // After delete: save is gone, refresh must publish 4 empty slots.
    coEvery { SaveManager.delete(mockTree, Region.PAL) } returns Result.success(Unit)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns null
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns false

    vm.deleteSave(Region.PAL)

    val merged = vm.mergedLicenses.value[Region.PAL]
    assertThat(merged).hasSize(4)
    merged!!.forEachIndexed { i, license ->
      assertThat(license.slotIndex).isEqualTo(i)
      assertThat(license.exists).isFalse()
    }
    assertThat(vm.activeLicense.value).isNull()
  }

  @Test
  fun `selectRegion after a delete publishes four empty slots for the new region`() = runTest {
    val palBytes = rksysWithLicense(pid = 0x00000010L, name = "Alice", slot = 0)
    val palInfo = RksysParser.parse(palBytes)
    every { SaveManager.listRegions(mockTree) } returns listOf(Region.PAL, Region.USA)
    coEvery { SaveManager.readSave(mockTree, Region.PAL) } returns palBytes
    // USA has no save from the start.
    coEvery { SaveManager.readSave(mockTree, Region.USA) } returns null
    coEvery { SaveManager.hasSave(mockTree, Region.PAL) } returns true
    coEvery { SaveManager.hasSave(mockTree, Region.USA) } returns false
    vm = buildVm()
    vm.refresh()

    // PAL is the initially selected region with real data.
    assertThat(vm.mergedLicenses.value[Region.PAL]).isEqualTo(palInfo.licenses)
    assertThat(vm.mergedLicenses.value).doesNotContainKey(Region.USA)

    vm.selectRegion(Region.USA)

    val usaMerged = vm.mergedLicenses.value[Region.USA]
    assertThat(usaMerged).hasSize(4)
    usaMerged!!.forEachIndexed { i, license ->
      assertThat(license.slotIndex).isEqualTo(i)
      assertThat(license.exists).isFalse()
    }
    assertThat(vm.activeLicense.value).isNull()
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
