package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.domain.RewindPackManager
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.util.DolphinLauncher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class UiState {
    data object NoStorage : UiState()
    data class Ready(val status: PackStatus) : UiState()
    data object Checking : UiState()
    data class Downloading(val progress: Float, val message: String) : UiState()
    data class Extracting(val progress: Float) : UiState()
    data class ApplyingUpdate(
        val index: Int,
        val total: Int,
        val description: String,
        val progress: Float
    ) : UiState()

    data class ReadyToLaunch(val version: String) : UiState()
    data class Error(val message: String) : UiState()
}

data class SaveState(
    val hasSave: Boolean = false,
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("wheelwitch", Application.MODE_PRIVATE)
    private val _state = MutableStateFlow<UiState>(UiState.NoStorage)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState())
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private var storageUri: Uri? = null
    private var storage: PackStorage? = null

    init {
        RewindPackManager.initCacheDir(application.cacheDir)
        restoreStorageUri()
    }

    private fun restoreStorageUri() {
        val uriString = prefs.getString(STORAGE_URI_KEY, null)
        if (uriString != null) {
            storageUri = Uri.parse(uriString)
            storage = PackStorage(getApplication(), storageUri!!)
            checkStatus()
        } else {
            _state.value = UiState.NoStorage
        }
    }

    fun setStorageUri(uri: Uri) {
        prefs.edit().putString(STORAGE_URI_KEY, uri.toString()).apply()
        storageUri = uri
        storage = PackStorage(getApplication(), uri)
        checkStatus()
    }

    fun checkStatus() {
        viewModelScope.launch {
            _state.value = UiState.Checking
            val currentStorage = storage ?: run {
                _state.value = UiState.NoStorage
                return@launch
            }
            val status = withContext(Dispatchers.IO) {
                RewindPackManager.checkStatus(currentStorage)
            }
            _state.value = UiState.Ready(status)
            refreshSaveState()
        }
    }

    fun downloadOrUpdate(status: PackStatus) {
        viewModelScope.launch {
            val currentStorage = storage ?: run {
                _state.value = UiState.Error("Storage not configured")
                return@launch
            }
            try {
                when (status) {
                    is PackStatus.NotInstalled -> {
                        withContext(Dispatchers.IO) {
                            RewindPackManager.freshInstall(currentStorage) { progress ->
                                handleProgress(progress)
                            }
                        }
                    }
                    is PackStatus.UpdateAvailable -> {
                        withContext(Dispatchers.IO) {
                            RewindPackManager.incrementalUpdate(
                                currentStorage,
                                status.serverInfo,
                                status.currentVersion
                            ) { progress ->
                                handleProgress(progress)
                            }
                        }
                    }
                    is PackStatus.Installed -> {
                        _state.value = UiState.Error(
                            "Cannot reach the update server. " +
                                    "Please check your internet connection and try again."
                        )
                        return@launch
                    }
                    is PackStatus.UpToDate -> {
                        _state.value = UiState.ReadyToLaunch(getDisplayVersion())
                        refreshSaveState()
                        return@launch
                    }
                }
                _state.value = UiState.ReadyToLaunch(getDisplayVersion())
                refreshSaveState()
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun launchDolphin() {
        val app = getApplication<Application>()
        val currentStorage = storage ?: run {
            _state.value = UiState.Error("Storage not configured")
            return
        }
        val gameIsoPath = DolphinLauncher.getGameIsoPath(app)
        if (gameIsoPath.isNullOrBlank()) {
            _state.value = UiState.Error(
                "Please select your Mario Kart Wii ROM file first."
            )
            return
        }
        val rootPath = currentStorage.rootPath
        if (rootPath == null) {
            _state.value = UiState.Error("Cannot resolve storage path. Please pick a new storage folder.")
            return
        }

        viewModelScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    DolphinLauncher.generateLaunchJson(rootPath, gameIsoPath)
                }
                val rrJsonFile = File(rootPath, "RR.json")
                withContext(Dispatchers.IO) {
                    rrJsonFile.writeText(json)
                }
                DolphinLauncher.launchDolphin(app, rrJsonFile.absolutePath).getOrThrow()
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Failed to launch Dolphin")
            }
        }
    }

    fun setGameIsoPath(path: String) {
        DolphinLauncher.setGameIsoPath(getApplication(), path)
        val currentState = _state.value
        if (currentState is UiState.Error && currentState.message.contains("ROM file", ignoreCase = true)) {
            launchDolphin()
        }
    }

    fun clearError() {
        val currentState = _state.value
        if (currentState is UiState.Error) {
            checkStatus()
        }
    }

    fun backupSave(destUri: Uri) {
        viewModelScope.launch {
            val currentStorage = storage ?: return@launch
            val app = getApplication<Application>()
            withContext(Dispatchers.IO) {
                val data = currentStorage.readBytes(SaveManager.SAVE_RELATIVE)
                    ?: throw Exception("Save file not found")
                app.contentResolver.openOutputStream(destUri)?.use { it.write(data) }
                    ?: throw Exception("Cannot write to selected location")
            }
            _successMessage.value = "Backup successful"
            refreshSaveState()
        }
    }

    fun restoreSave(sourceUri: Uri) {
        viewModelScope.launch {
            val currentStorage = storage ?: return@launch
            val app = getApplication<Application>()
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val data = app.contentResolver.openInputStream(sourceUri)?.use { it.readBytes() }
                        ?: throw Exception("Cannot read from selected file")
                    currentStorage.writeBytes(SaveManager.SAVE_RELATIVE, data)
                }
            }
            result.onSuccess {
                _successMessage.value = "Restore successful"
            }.onFailure {
                _state.value = UiState.Error("Restore failed: ${it.message}")
            }
            refreshSaveState()
        }
    }

    fun deleteSave() {
        viewModelScope.launch {
            val currentStorage = storage ?: return@launch
            withContext(Dispatchers.IO) {
                SaveManager.deleteSave(currentStorage)
            }
            refreshSaveState()
        }
    }

    fun dismissSuccess() {
        _successMessage.value = null
    }

    private fun refreshSaveState() {
        viewModelScope.launch {
            val currentStorage = storage ?: return@launch
            val hasSave = withContext(Dispatchers.IO) {
                SaveManager.hasSaveFile(currentStorage)
            }
            _saveState.value = SaveState(hasSave)
        }
    }

    private fun handleProgress(progress: ProgressInfo) {
        _state.value = when (progress) {
            is ProgressInfo.Checking -> UiState.Checking
            is ProgressInfo.Downloading -> UiState.Downloading(progress.progress, progress.message)
            is ProgressInfo.Extracting -> UiState.Extracting(progress.progress)
            is ProgressInfo.ApplyingUpdate -> UiState.ApplyingUpdate(
                progress.index, progress.total, progress.description, progress.progress
            )
        }
    }

    private fun getDisplayVersion(): String {
        return try {
            val text = storage?.readFile("RetroRewind6/version.txt")?.trim() ?: "Unknown"
            text
        } catch (e: Exception) {
            "Unknown"
        }
    }

    companion object {
        private const val STORAGE_URI_KEY = "storage_tree_uri"
    }
}
