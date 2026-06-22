package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.data.DolphinTree
import com.skiletro.wheelwitch.data.RksysParser
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.data.SaveManager.Region
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.SaveFileInfo
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.util.prefs.Prefs
import com.skiletro.wheelwitch.util.prefs.PrefsKeys
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

/**
 * Owns the save file state: per-region parse, leaderboard merge,
 * backup/restore/delete, and slot selection.
 *
 * The pack install flow lives in [PackUpdateViewModel]. This VM
 * listens to its [UiState] via [packStatusFlow] and re-parses the
 * save whenever the pack state transitions to [UiState.Ready]. This
 * keeps the two VMs decoupled (no static companion pointers like
 * the deleted `SaveDataDelegate`) and survives process death
 * naturally; both VMs are reconstructed on the next composition
 * and re-collect the [packStatusFlow] from scratch.
 *
 * Multi-region: a user with multiple ROMs (one per region) has one
 * save file per region. [SaveManager.listRegions] walks the user's
 * [DolphinTree.romDir] and [refresh] reads + parses a save for each
 * present region in parallel. [selectedRegion] defaults to the first
 * region with a ROM.
 *
 * Tests can swap the [treeFactory], the [parser], and the
 * [leaderboardFetcher] lambdas to inject mocks without going through
 * SAF or the network.
 */
class SaveDataViewModel(
  application: Application,
  private val packStatusFlow: StateFlow<UiState>,
  private val treeFactory: (Context) -> DolphinTree? = ::defaultTreeFactory,
  private val parser: (ByteArray) -> SaveFileInfo = RksysParser::parse,
  private val leaderboardFetcher: suspend (String) -> Result<Pair<Int, String?>> = { code ->
    VersionFileParser.fetchPlayerLeaderboard(code)
  },
  private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : AndroidViewModel(application) {
  private val app = application
  private val prefs = Prefs.main(application)

  private val _saveInfos = MutableStateFlow<Map<Region, SaveFileInfo>>(emptyMap())
  val saveInfos: StateFlow<Map<Region, SaveFileInfo>> = _saveInfos.asStateFlow()

  private val _hasSave = MutableStateFlow<Map<Region, Boolean>>(emptyMap())
  val hasSave: StateFlow<Map<Region, Boolean>> = _hasSave.asStateFlow()

  private val _selectedRegion =
    MutableStateFlow(loadPersistedRegion(prefs.getString(PrefsKeys.SELECTED_REGION_KEY, null)))
  val selectedRegion: StateFlow<Region?> = _selectedRegion.asStateFlow()

  private val _selectedSlotIndex =
    MutableStateFlow(prefs.getInt(PrefsKeys.SELECTED_SLOT_KEY, 0))
  val selectedSlotIndex: StateFlow<Int> = _selectedSlotIndex.asStateFlow()

  private val _activeLicense = MutableStateFlow<LicenseInfo?>(null)
  val activeLicense: StateFlow<LicenseInfo?> = _activeLicense.asStateFlow()

  private val _cachedLeaderboardVrs = MutableStateFlow(readVrCache())
  val cachedLeaderboardVrs: StateFlow<Map<Int, Int>> = _cachedLeaderboardVrs.asStateFlow()

  private val _isLoading = MutableStateFlow(false)
  val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

  private val _error = MutableStateFlow<String?>(null)
  val error: StateFlow<String?> = _error.asStateFlow()

  init {
    viewModelScope.launch {
      packStatusFlow
        .map { (it as? UiState.Ready)?.status }
        .distinctUntilChanged()
        .collect { status ->
          if (status != null) refresh()
        }
    }
  }

  /**
   * Re-reads every region's save from the SAF tree, parses them in
   * parallel, and refreshes the leaderboard for the selected slot.
   * No-op if [treeFactory] returns null (no persisted SAF grant).
   */
  fun refresh() {
    viewModelScope.launch {
      _isLoading.value = true
      try {
        val tree =
          treeFactory(app)
            ?: run {
              _saveInfos.value = emptyMap()
              _hasSave.value = emptyMap()
              _activeLicense.value = null
              return@launch
            }
        val regions = SaveManager.listRegions(tree)
        if (regions.isEmpty()) {
          _saveInfos.value = emptyMap()
          _hasSave.value = emptyMap()
          _selectedRegion.value = null
          _activeLicense.value = null
          return@launch
        }
        val parsed =
          coroutineScope {
            regions
              .map { region ->
                async(ioDispatcher) {
                  val bytes = SaveManager.readSave(tree, region)
                  if (bytes != null) {
                    region to runCatching { parser(bytes) }.getOrNull()
                  } else null
                }
              }
              .awaitAll()
          }
        val infos =
          parsed.filterNotNull().filter { it.second != null }.associate { it.first to it.second!! }
        val hasSaves = regions.associateWith { SaveManager.hasSave(tree, it) }
        _saveInfos.value = infos
        _hasSave.value = hasSaves
        val target = pickSelectedRegion(regions)
        if (target != _selectedRegion.value) {
          _selectedRegion.value = target
        }
        refreshActiveLicense()
      } catch (e: Exception) {
        Timber.tag(TAG).e(e, "refresh failed")
        _error.value =
          e.message ?: app.getString(R.string.vm_failed_format, "read save data")
      } finally {
        _isLoading.value = false
      }
    }
  }

  /** Persists the selected region and refreshes the active license. */
  fun selectRegion(region: Region) {
    prefs.edit().putString(PrefsKeys.SELECTED_REGION_KEY, region.code).apply()
    _selectedRegion.value = region
    viewModelScope.launch { refreshActiveLicense() }
  }

  /**
   * Persists [index] as the selected slot and refreshes the active
   * license. The persisted value survives process death.
   */
  fun selectSlot(index: Int) {
    prefs.edit().putInt(PrefsKeys.SELECTED_SLOT_KEY, index).apply()
    _selectedSlotIndex.value = index
    viewModelScope.launch { refreshActiveLicense() }
  }

  /** Copies the [region] save bytes to the user-picked [dest] URI. */
  fun backupSave(region: Region, dest: Uri) {
    viewModelScope.launch {
      val tree = treeFactory(app)
      if (tree == null) {
        _error.value = app.getString(R.string.vm_save_not_configured)
        return@launch
      }
      SaveManager.backup(tree, region, dest).onFailure { e ->
        Timber.tag(TAG).e(e, "backup failed")
        _error.value = app.getString(R.string.vm_save_write_failed)
      }
      refresh()
    }
  }

  /** Overwrites the [region] save with the bytes from [source]. */
  fun restoreSave(region: Region, source: Uri) {
    viewModelScope.launch {
      val tree = treeFactory(app)
      if (tree == null) {
        _error.value = app.getString(R.string.vm_save_not_configured)
        return@launch
      }
      SaveManager.restore(tree, region, source).onFailure { e ->
        Timber.tag(TAG).e(e, "restore failed")
        _error.value = app.getString(R.string.vm_save_read_failed)
      }
      refresh()
    }
  }

  /** Deletes the [region] save file. Idempotent. */
  fun deleteSave(region: Region) {
    viewModelScope.launch {
      val tree = treeFactory(app)
      if (tree == null) {
        _error.value = app.getString(R.string.vm_save_not_configured)
        return@launch
      }
      SaveManager.delete(tree, region).onFailure { e ->
        Timber.tag(TAG).e(e, "delete failed")
        _error.value = e.message ?: app.getString(R.string.vm_failed_format, "delete save")
      }
      refresh()
    }
  }

  /** Clears [error]. */
  fun clearError() {
    _error.value = null
  }

  // --- internals --------------------------------------------------------

  private suspend fun refreshActiveLicense() {
    val region = _selectedRegion.value ?: return
    val saveInfo = _saveInfos.value[region] ?: run {
      _activeLicense.value = null
      return
    }
    val slotIndex = _selectedSlotIndex.value
    val base = saveInfo.licenses.getOrNull(slotIndex)?.takeIf { it.exists }
    if (base == null) {
      _activeLicense.value = null
      return
    }
    _activeLicense.value = base
    if (base.friendCode != null) {
      val result = withContext(ioDispatcher) { leaderboardFetcher(base.friendCode) }
      if (result.isSuccess) {
        val (vr, mii) = result.getOrThrow()
        _activeLicense.value =
          base.copy(leaderboardVr = vr, leaderboardMiiImageBase64 = mii)
        cacheAndPersistLeaderboardVr(slotIndex, vr)
      }
    }
  }

  private fun pickSelectedRegion(regions: List<Region>): Region? {
    if (regions.isEmpty()) return null
    val current = _selectedRegion.value
    return if (current != null && current in regions) current else regions.first()
  }

  /** Maps a persisted region code (e.g. `RMCP`) back to its [Region] enum, or null. */
  private fun loadPersistedRegion(code: String?): Region? =
    code?.let { c -> Region.entries.firstOrNull { it.code == c } }

  private fun cacheAndPersistLeaderboardVr(slotIndex: Int, vr: Int) {
    val current = _cachedLeaderboardVrs.value
    if (current[slotIndex] == vr) return
    val updated = current + (slotIndex to vr)
    _cachedLeaderboardVrs.value = updated
    val obj = JSONObject()
    updated.forEach { (slot, value) -> obj.put(slot.toString(), value) }
    prefs.edit().putString(PrefsKeys.LAST_LEADERBOARD_VR_KEY, obj.toString()).apply()
  }

  private fun readVrCache(): Map<Int, Int> {
    val raw = prefs.getString(PrefsKeys.LAST_LEADERBOARD_VR_KEY, null) ?: return emptyMap()
    return runCatching {
      val obj = JSONObject(raw)
      buildMap {
        obj.keys().forEach { key ->
          val slot = key.toIntOrNull() ?: return@forEach
          put(slot, obj.optInt(key, 0))
        }
      }
    }.getOrElse { emptyMap() }
  }

  /**
   * Internal tag + default [DolphinTree] factory plus the public
   * [factory] used by [androidx.lifecycle.viewmodel.compose.viewModel]
   * in the composition root. The default [ViewModelProvider] for
   * [AndroidViewModel] looks up a single-arg `(Application)`
   * constructor, which doesn't exist anymore. The second
   * `packStatusFlow` parameter requires a custom factory.
   *
   * The other constructor parameters (`treeFactory`, `parser`,
   * `leaderboardFetcher`, `ioDispatcher`) use their production
   * defaults; tests that need to swap them continue to construct
   * the VM directly with explicit arguments.
   */
  companion object {
    const val TAG = "SaveData"

    fun defaultTreeFactory(context: Context): DolphinTree? = DolphinTree.fromPersisted(context)

    /**
     * [ViewModelProvider.Factory] that wires the production
     * dependencies; the [Application] from [ViewModelProvider]'s
     * CreationExtras, and the [packStatusFlow] from the
     * already-constructed [PackUpdateViewModel]. The pack VM lives
     * in the parent scope (`MainScreen`) so it can be passed in
     * here without a circular construction.
     */
    fun factory(packUpdate: PackUpdateViewModel): ViewModelProvider.Factory =
      viewModelFactory {
        initializer {
          val app =
            this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
          SaveDataViewModel(application = app, packStatusFlow = packUpdate.state)
        }
      }
  }
}
