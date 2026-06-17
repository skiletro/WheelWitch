package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.data.RksysParser
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.domain.RewindPackManager
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.Room
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.model.SaveFileInfo
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiWadInstaller
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
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

    data class Error(val message: String) : UiState()
}

@Immutable
data class SaveState(
    val hasSave: Boolean = false,
)

@Immutable
data class MiiMakerState(
    val hasWad: Boolean = false,
)

@Immutable
sealed class RoomsState {
    data object Idle : RoomsState()
    data object Loading : RoomsState()
    data class Success(
        val rooms: List<Room>,
        val playerCount: Int?,
        val serverConnectivity: ServerConnectivity
    ) : RoomsState()
    data class Error(val message: String) : RoomsState()
}

@Immutable
sealed class SaveInfoState {
    data object Idle : SaveInfoState()
    data object Loading : SaveInfoState()
    data class Success(val info: SaveFileInfo) : SaveInfoState()
    data class Error(val message: String) : SaveInfoState()
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val prefs = application.getSharedPreferences("wheelwitch", Application.MODE_PRIVATE)
    private val _state = MutableStateFlow<UiState>(UiState.NoStorage)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState())
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _miiMakerState = MutableStateFlow(MiiMakerState())
    val miiMakerState: StateFlow<MiiMakerState> = _miiMakerState.asStateFlow()

    private val _isInstallingWad = MutableStateFlow(false)
    val isInstallingWad: StateFlow<Boolean> = _isInstallingWad.asStateFlow()

    private var saveInfoJob: Job? = null
    private val _miiMakerError = MutableStateFlow<String?>(null)
    val miiMakerError: StateFlow<String?> = _miiMakerError.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentIsoPath = MutableStateFlow<String?>(null)
    val currentIsoPath: StateFlow<String?> = _currentIsoPath.asStateFlow()

    private val _saveInfoState = MutableStateFlow<SaveInfoState>(SaveInfoState.Idle)
    val saveInfoState: StateFlow<SaveInfoState> = _saveInfoState.asStateFlow()

    private var storageUri: Uri? = null
    private var storage: PackStorage? = null
    val storageRootPath: String? get() = storage?.rootPath

    init {
        RewindPackManager.initCacheDir(application.cacheDir)
        restoreStorageUri()
        refreshMiiMakerState()
    }

    private fun restoreStorageUri() {
        val uriString = prefs.getString(STORAGE_URI_KEY, null)
        if (uriString != null) {
            storageUri = Uri.parse(uriString)
            storage = PackStorage(getApplication(), storageUri!!)
            refreshIsoPath()
            checkStatus()
        } else {
            _state.value = UiState.NoStorage
        }
    }

    private fun refreshIsoPath() {
        val rootPath = storage?.rootPath ?: return
        _currentIsoPath.value = DolphinLauncher.readIsoPathFromLaunchJson(rootPath)
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
                withContext(Dispatchers.IO) {
                    SaveManager.backupSaveToCache(app, currentStorage)
                }

                val installedVersion = when (status) {
                    is PackStatus.NotInstalled -> {
                        withContext(Dispatchers.IO) {
                            RewindPackManager.freshInstall(currentStorage) { progress ->
                                handleProgress(progress)
                            }
                        }.getOrThrow()
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
                        }.getOrThrow()
                    }
                    is PackStatus.Installed -> {
                        _state.value = UiState.Error(
                            "Cannot reach the update server. " +
                                    "Please check your internet connection and try again."
                        )
                        return@launch
                    }
                    is PackStatus.UpToDate -> {
                        withContext(Dispatchers.IO) {
                            SaveManager.deleteSaveBackup(app)
                        }
                        _state.value = UiState.Ready(PackStatus.UpToDate(status.currentVersion, status.latestVersion))
                        refreshSaveState()
                        return@launch
                    }
                }
                withContext(Dispatchers.IO) {
                    SaveManager.deleteSaveBackup(app)
                }
                _state.value = UiState.Ready(PackStatus.UpToDate(installedVersion, installedVersion))
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
            return
        }
        if (!File(gameIsoPath).exists()) {
            clearIsoPath()
            _state.value = UiState.Error("Mario Kart Wii ROM not found. Please select it again.")
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
        val app = getApplication<Application>()
        DolphinLauncher.setGameIsoPath(app, path)
        val rootPath = storage?.rootPath
        if (rootPath != null) {
            viewModelScope.launch(Dispatchers.IO) {
                DolphinLauncher.writeLaunchJson(rootPath, path)
            }
        }
        _currentIsoPath.value = path
        val currentState = _state.value
        if (currentState is UiState.Error && currentState.message.contains("ROM file", ignoreCase = true)) {
            launchDolphin()
        }
    }

    fun clearIsoPath() {
        val app = getApplication<Application>()
        DolphinLauncher.setGameIsoPath(app, "")
        storage?.rootPath?.let { DolphinLauncher.deleteLaunchJson(it) }
        _currentIsoPath.value = null
    }

    fun launchMiiMaker() {
        val app = getApplication<Application>()

        viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) {
                    MiiWadInstaller.getCachedWadFile(app)
                }
                if (cached != null) {
                    MiiWadInstaller.launchWadFile(app, cached).getOrThrow()
                }
            } catch (e: Exception) {
                _miiMakerError.value = e.message ?: "Failed to launch Mii Maker"
            }
        }
    }

    fun installMiiMakerWad() {
        val app = getApplication<Application>()

        viewModelScope.launch {
            _isInstallingWad.value = true
            _miiMakerError.value = null
            try {
                if (!isNetworkAvailable(app)) {
                    _isInstallingWad.value = false
                    _miiMakerError.value = "No internet connection. Please connect to the internet and try again."
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    MiiWadInstaller.downloadAndExtractWad(app)
                }
                refreshMiiMakerState()
            } catch (e: Exception) {
                _miiMakerError.value = e.message ?: "Failed to install Mii Maker WAD"
            }
            _isInstallingWad.value = false
        }
    }

    private fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
            try {
                val currentStorage = storage ?: return@launch
                val hasSave = withContext(Dispatchers.IO) {
                    SaveManager.hasSaveFile(currentStorage)
                }
                _saveState.value = SaveState(hasSave)
            } catch (_: Exception) {
                _saveState.value = SaveState(false)
            }
        }
    }

    fun deleteWad() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val dir = File(getApplication<Application>().cacheDir, "mii_maker")
                dir.deleteRecursively()
            }
            refreshMiiMakerState()
        }
    }

    fun refreshMiiMakerState() {
        val hasWad = MiiWadInstaller.getCachedWadFile(getApplication()) != null
        _miiMakerState.value = MiiMakerState(hasWad)
    }

    fun refreshSaveFileInfo() {
        saveInfoJob?.cancel()
        saveInfoJob = viewModelScope.launch {
            _saveInfoState.value = SaveInfoState.Loading
            try {
                val currentStorage = storage ?: throw Exception("Storage not configured")
                val bytes = withContext(Dispatchers.IO) {
                    currentStorage.readBytes(SaveManager.SAVE_RELATIVE)
                }
                if (bytes == null) {
                    _saveInfoState.value = SaveInfoState.Error("No save file found")
                } else {
                    val saveInfo = RksysParser.parse(bytes)
                    _saveInfoState.value = SaveInfoState.Success(saveInfo)

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
                                lic.copy(leaderboard = result.getOrNull())
                            } else lic
                        }
                        _saveInfoState.value = SaveInfoState.Success(current.info.copy(licenses = updatedLicenses))
                    }
                }
            } catch (e: Exception) {
                _saveInfoState.value = SaveInfoState.Error(e.message ?: "Failed to read save data")
            }
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

    companion object {
        /** SharedPreferences key for the SAF storage tree URI. */
        private const val STORAGE_URI_KEY = "storage_tree_uri"
    }
}
