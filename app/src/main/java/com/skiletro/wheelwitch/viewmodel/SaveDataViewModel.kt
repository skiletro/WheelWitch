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
import com.skiletro.wheelwitch.util.PrefsKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
 */
class SaveDataViewModel(application: Application) : AndroidViewModel(application), SaveDataDelegate {
    private val prefs = application.getSharedPreferences(PrefsKeys.PREFS_NAME, Application.MODE_PRIVATE)

    private val _hasSave = MutableStateFlow(false)
    val hasSave: StateFlow<Boolean> = _hasSave.asStateFlow()

    private val _saveInfoState = MutableStateFlow<SaveInfoState>(SaveInfoState.Idle)
    val saveInfoState: StateFlow<SaveInfoState> = _saveInfoState.asStateFlow()

    private val _selectedSlotIndex = MutableStateFlow(prefs.getInt(PrefsKeys.SELECTED_SLOT_KEY, 0))
    val selectedSlotIndex: StateFlow<Int> = _selectedSlotIndex.asStateFlow()

    private val _activeLicenseInfo = MutableStateFlow<LicenseInfo?>(null)
    val activeLicenseInfo: StateFlow<LicenseInfo?> = _activeLicenseInfo.asStateFlow()

    private var saveInfoJob: Job? = null

    init {
        PackUpdateViewModel.saveDataDelegate = this
        refreshActiveLicense()
    }

    override fun onPackStatusChanged() {
        refreshSaveState()
        refreshActiveLicense()
    }

    fun refreshSaveState() {
        viewModelScope.launch {
            try {
                val storage = PackUpdateViewModel.currentStorage ?: return@launch
                _hasSave.value = withContext(Dispatchers.IO) {
                    SaveManager.hasSaveFile(storage)
                }
            } catch (_: Exception) {
                _hasSave.value = false
            }
        }
    }

    fun backupSave(destUri: Uri) {
        viewModelScope.launch {
            val storage = PackUpdateViewModel.currentStorage ?: return@launch
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                val data = storage.readBytes(SaveManager.SAVE_RELATIVE)
                    ?: throw Exception("Save file not found")
                app.contentResolver.openOutputStream(destUri)?.use { it.write(data) }
                    ?: throw Exception("Cannot write to selected location")
            }
            refreshSaveState()
        }
    }

    fun restoreSave(sourceUri: Uri) {
        viewModelScope.launch {
            val storage = PackUpdateViewModel.currentStorage ?: return@launch
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                val data = app.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                    ?: throw Exception("Cannot read from selected file")
                storage.writeBytes(SaveManager.SAVE_RELATIVE, data)
            }
            refreshSaveState()
        }
    }

    fun deleteSave() {
        viewModelScope.launch {
            val storage = PackUpdateViewModel.currentStorage ?: return@launch
            withContext(Dispatchers.IO) {
                SaveManager.deleteSave(storage)
            }
            refreshSaveState()
        }
    }

    fun selectSlot(index: Int) {
        prefs.edit().putInt(PrefsKeys.SELECTED_SLOT_KEY, index).apply()
        _selectedSlotIndex.value = index
        refreshActiveLicense()
    }

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
                    }
                }
            } else {
                _activeLicenseInfo.value = null
            }
        }
    }

    fun refreshSaveFileInfo() {
        saveInfoJob?.cancel()
        saveInfoJob = viewModelScope.launch {
            _saveInfoState.value = SaveInfoState.Loading
            try {
                val storage = PackUpdateViewModel.currentStorage ?: throw Exception("Storage not configured")
                val bytes = withContext(Dispatchers.IO) {
                    storage.readBytes(SaveManager.SAVE_RELATIVE)
                }
                if (bytes == null) {
                    _saveInfoState.value = SaveInfoState.Error("No save file found")
                } else {
                    val saveInfo = RksysParser.parse(bytes)
                    _saveInfoState.value = SaveInfoState.Success(saveInfo)

                    val selectedIndex = _selectedSlotIndex.value
                    val isValid = saveInfo.licenses.getOrNull(selectedIndex)?.exists == true
                    if (!isValid) {
                        val fallback = saveInfo.licenses.indexOfFirst { it.exists }.let { if (it >= 0) it else 0 }
                        selectSlot(fallback)
                    }

                    val leaderboardDeferred = saveInfo.licenses.mapNotNull { license ->
                        if (license.exists && license.friendCode != null) {
                            async(Dispatchers.IO) {
                                license.slotIndex to VersionFileParser.fetchPlayerLeaderboard(license.friendCode)
                            }
                        } else null
                    }
                    val leaderboardResults = leaderboardDeferred.awaitAll()

                    val current = _saveInfoState.value
                    if (current is SaveInfoState.Success) {
                        val updatedLicenses = current.info.licenses.map { lic ->
                            val result = leaderboardResults.find { it.first == lic.slotIndex }?.second
                            if (result?.isSuccess == true) {
                                val (leaderboardVr, leaderboardMii) = result.getOrThrow()
                                lic.copy(
                                    leaderboardVr = leaderboardVr,
                                    leaderboardMiiImageBase64 = leaderboardMii
                                )
                            } else lic
                        }
                        _saveInfoState.value = SaveInfoState.Success(current.info.copy(licenses = updatedLicenses))
                    }
                }
            } catch (e: Exception) {
                _saveInfoState.value = SaveInfoState.Error(e.message ?: getApplication<Application>().getString(R.string.vm_save_info_failed))
            }
        }
    }
}
