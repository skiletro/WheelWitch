package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.data.RksysParser
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.SaveFileInfo
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.util.Prefs
import com.skiletro.wheelwitch.util.PrefsKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber

@Immutable
sealed class SaveInfoState {
    data object Idle : SaveInfoState()
    data object Loading : SaveInfoState()
    data class Success(val info: SaveFileInfo) : SaveInfoState()
    data class Error(val message: String) : SaveInfoState()
}

/**
 * Owns the save file state: presence, backup/restore, per-slot info,
 * and the license currently active in the selected slot.
 *
 * Registers itself as the [SaveDataDelegate] in [PackUpdateViewModel]
 * during [init] so it receives pack status change notifications. The
 * registration is overwritten if a new [SaveDataViewModel] is created
 * (e.g. on process death + restoration), which is fine because only the
 * latest instance is relevant.
 */
class SaveDataViewModel(application: Application) : AndroidViewModel(application),
    SaveDataDelegate {
    private val prefs = Prefs.main(application)
    private val app = application

    private val _hasSave = MutableStateFlow(false)
    val hasSave: StateFlow<Boolean> = _hasSave.asStateFlow()

    private val _saveInfoState = MutableStateFlow<SaveInfoState>(SaveInfoState.Idle)
    val saveInfoState: StateFlow<SaveInfoState> = _saveInfoState.asStateFlow()

    private val _selectedSlotIndex = MutableStateFlow(prefs.getInt(PrefsKeys.SELECTED_SLOT_KEY, 0))
    val selectedSlotIndex: StateFlow<Int> = _selectedSlotIndex.asStateFlow()

    private val _activeLicenseInfo = MutableStateFlow<LicenseInfo?>(null)
    val activeLicenseInfo: StateFlow<LicenseInfo?> = _activeLicenseInfo.asStateFlow()

    private val _cachedLeaderboardVrs = MutableStateFlow(readVrCache())
    val cachedLeaderboardVrs: StateFlow<Map<Int, Int>> = _cachedLeaderboardVrs.asStateFlow()

    private var saveInfoJob: Job? = null

    init {
        PackUpdateViewModel.saveDataDelegate = this
        refreshActiveLicense()
    }

    override fun onPackStatusChanged() {
        refreshSaveState()
        refreshActiveLicense()
    }

    /** Re-checks whether a save file exists under the current storage root. */
    fun refreshSaveState() {
        viewModelScope.launch {
            try {
                val storage = PackUpdateViewModel.currentStorage ?: return@launch
                _hasSave.value = withContext(Dispatchers.IO) {
                    SaveManager.hasSaveFile(storage)
                }
            } catch (e: Exception) {
                Timber.tag("SaveData").w(e, "Failed to check save state")
                _hasSave.value = false
            }
        }
    }

    /** Backs up the save file to the user-picked [destUri]. */
    fun backupSave(destUri: Uri) {
        viewModelScope.launch {
            val storage = PackUpdateViewModel.currentStorage ?: return@launch
            try {
                withContext(Dispatchers.IO) {
                    val data = storage.readBytes(SaveManager.SAVE_RELATIVE)
                        ?: throw Exception(app.getString(R.string.status_save_not_found))
                    app.contentResolver.openOutputStream(destUri)?.use { it.write(data) }
                        ?: throw Exception(app.getString(R.string.vm_save_write_failed))
                }
                Timber.tag("SaveData").i("Save backed up to %s", destUri)
                refreshSaveState()
            } catch (e: Exception) {
                Timber.tag("SaveData").e(e, "Save backup failed")
            }
        }
    }

    /** Restores the save file from the user-picked [sourceUri]. */
    fun restoreSave(sourceUri: Uri) {
        viewModelScope.launch {
            val storage = PackUpdateViewModel.currentStorage ?: return@launch
            try {
                withContext(Dispatchers.IO) {
                    val data = app.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                        ?: throw Exception(app.getString(R.string.vm_save_read_failed))
                    storage.writeBytes(SaveManager.SAVE_RELATIVE, data)
                }
                Timber.tag("SaveData").i("Save restored from %s", sourceUri)
                refreshSaveState()
            } catch (e: Exception) {
                Timber.tag("SaveData").e(e, "Save restore failed")
            }
        }
    }

    /** Deletes the save file under the current storage root. */
    fun deleteSave() {
        viewModelScope.launch {
            val storage = PackUpdateViewModel.currentStorage ?: return@launch
            withContext(Dispatchers.IO) {
                SaveManager.deleteSave(storage)
            }
            Timber.tag("SaveData").i("Save deleted")
            refreshSaveState()
        }
    }

    /**
     * Persists [index] as the selected slot and refreshes [activeLicenseInfo]
     * for the new slot.
     */
    fun selectSlot(index: Int) {
        prefs.edit().putInt(PrefsKeys.SELECTED_SLOT_KEY, index).apply()
        _selectedSlotIndex.value = index
        refreshActiveLicense()
    }

    /**
     * Loads the active license for the selected slot. Emits the base
     * license first, then re-emits with leaderboard data once the
     * network fetch resolves (so the UI shows something immediately
     * and enriches it when the leaderboard arrives).
     */
    fun refreshActiveLicense() {
        viewModelScope.launch {
            val storage = PackUpdateViewModel.currentStorage ?: return@launch
            val bytes = withContext(Dispatchers.IO) {
                storage.readBytes(SaveManager.SAVE_RELATIVE)
            }
            if (bytes == null) {
                _activeLicenseInfo.value = null
                return@launch
            }
            val saveInfo = RksysParser.parse(bytes)
            val selectedIndex = _selectedSlotIndex.value
            val license = saveInfo.licenses.getOrNull(selectedIndex)?.takeIf { it.exists }

            if (license != null) {
                _activeLicenseInfo.value = license
                if (license.friendCode != null) {
                    val result = withContext(Dispatchers.IO) {
                        VersionFileParser.fetchPlayerLeaderboard(license.friendCode)
                    }
                    if (result.isSuccess) {
                        val (leaderboardVr, leaderboardMii) = result.getOrThrow()
                        _activeLicenseInfo.value = license.copy(
                            leaderboardVr = leaderboardVr,
                            leaderboardMiiImageBase64 = leaderboardMii
                        )
                        cacheAndPersistLeaderboardVr(license.slotIndex, leaderboardVr)
                    }
                }
            } else {
                _activeLicenseInfo.value = null
            }
        }
    }

    /**
     * Loads the full save file info for all 4 license slots, fetching
     * leaderboard data for each existing license in parallel. Falls
     * back to the first existing slot if the persisted selection is no
     * longer valid.
     */
    fun refreshSaveFileInfo() {
        saveInfoJob?.cancel()
        saveInfoJob = viewModelScope.launch {
            _saveInfoState.value = SaveInfoState.Loading
            try {
                val storage = PackUpdateViewModel.currentStorage
                    ?: throw Exception(app.getString(R.string.error_storage_not_configured))
                val bytes = withContext(Dispatchers.IO) {
                    storage.readBytes(SaveManager.SAVE_RELATIVE)
                }
                if (bytes == null) {
                    _saveInfoState.value =
                        SaveInfoState.Error(app.getString(R.string.status_save_not_found))
                    return@launch
                }

                val saveInfo = RksysParser.parse(bytes)
                _saveInfoState.value = SaveInfoState.Success(saveInfo)

                resolveFallbackSlot(saveInfo)
                mergeLeaderboards(saveInfo)
            } catch (e: Exception) {
                Timber.tag("SaveData").e(e, "refreshSaveFileInfo failed")
                _saveInfoState.value = SaveInfoState.Error(
                    e.message ?: app.getString(
                        R.string.vm_failed_format,
                        "read save data"
                    )
                )
            }
        }
    }

    /**
     * If the persisted slot selection is no longer valid (no license or
     * missing), switch to the first existing slot (or slot 0 as a last
     * resort).
     */
    private suspend fun resolveFallbackSlot(saveInfo: SaveFileInfo) {
        val selectedIndex = _selectedSlotIndex.value
        val isValid = saveInfo.licenses.getOrNull(selectedIndex)?.exists == true
        if (!isValid) {
            val idx = saveInfo.licenses.indexOfFirst { it.exists }
            val fallback = if (idx >= 0) idx else 0
            selectSlot(fallback)
        }
    }

    /**
     * Fetches leaderboard data for every existing license in parallel
     * and re-emits [SaveInfoState.Success] with the merged VR/Mii data.
     */
    private suspend fun mergeLeaderboards(saveInfo: SaveFileInfo) {
        val leaderboardResults = coroutineScope {
            val leaderboardDeferred = saveInfo.licenses.mapNotNull { license ->
                if (license.exists && license.friendCode != null) {
                    async(Dispatchers.IO) {
                        license.slotIndex to VersionFileParser.fetchPlayerLeaderboard(license.friendCode)
                    }
                } else null
            }
            leaderboardDeferred.awaitAll()
        }

        val current = _saveInfoState.value as? SaveInfoState.Success ?: return
        val updatedLicenses = current.info.licenses.map { lic ->
            val result = leaderboardResults.find { it.first == lic.slotIndex }?.second
            if (result?.isSuccess == true) {
                val (leaderboardVr, leaderboardMii) = result.getOrThrow()
                cacheAndPersistLeaderboardVr(lic.slotIndex, leaderboardVr)
                lic.copy(
                    leaderboardVr = leaderboardVr,
                    leaderboardMiiImageBase64 = leaderboardMii
                )
            } else lic
        }
        _saveInfoState.value = SaveInfoState.Success(current.info.copy(licenses = updatedLicenses))
    }

    /** Updates the in-memory VR cache and persists it to SharedPreferences. */
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
        }.getOrElse {
            Timber.tag("SaveData").w(it, "Failed to read VR cache")
            emptyMap()
        }
    }
}
