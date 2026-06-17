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
import com.skiletro.wheelwitch.domain.RewindPackManager.VERSION_FILE
import com.skiletro.wheelwitch.network.VersionFileParser
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.model.Room
import com.skiletro.wheelwitch.model.SaveFileInfo
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiWadInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
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

data class MiiMakerState(
    val hasWad: Boolean = false,
)

class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("wheelwitch", Application.MODE_PRIVATE)
    private val _state = MutableStateFlow<UiState>(UiState.NoStorage)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _saveState = MutableStateFlow(SaveState())
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _miiMakerState = MutableStateFlow(MiiMakerState())
    val miiMakerState: StateFlow<MiiMakerState> = _miiMakerState.asStateFlow()

    private val _playerCount = MutableStateFlow<Int?>(null)
    val playerCount: StateFlow<Int?> = _playerCount.asStateFlow()

    private val _serverConnectivity = MutableStateFlow<ServerConnectivity>(ServerConnectivity.Unknown)
    val serverConnectivity: StateFlow<ServerConnectivity> = _serverConnectivity.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()

    private val _isLoadingRooms = MutableStateFlow(false)
    val isLoadingRooms: StateFlow<Boolean> = _isLoadingRooms.asStateFlow()

    private val _roomsError = MutableStateFlow<String?>(null)
    val roomsError: StateFlow<String?> = _roomsError.asStateFlow()

    private val _isInstallingWad = MutableStateFlow(false)
    val isInstallingWad: StateFlow<Boolean> = _isInstallingWad.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentIsoPath = MutableStateFlow<String?>(null)
    val currentIsoPath: StateFlow<String?> = _currentIsoPath.asStateFlow()

    private val _saveFileInfo = MutableStateFlow<SaveFileInfo?>(null)
    val saveFileInfo: StateFlow<SaveFileInfo?> = _saveFileInfo.asStateFlow()

    private val _isLoadingSaveInfo = MutableStateFlow(false)
    val isLoadingSaveInfo: StateFlow<Boolean> = _isLoadingSaveInfo.asStateFlow()

    private val _saveInfoError = MutableStateFlow<String?>(null)
    val saveInfoError: StateFlow<String?> = _saveInfoError.asStateFlow()

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
        val file = File(rootPath, "RR.json")
        _currentIsoPath.value = if (file.exists()) {
            try {
                JSONObject(file.readText()).optString("base-file", "").takeIf { it.isNotEmpty() }
            } catch (_: Exception) {
                null
            }
        } else null
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
            val app = getApplication<Application>()
            val (count, connectivity) = withContext(Dispatchers.IO) {
                if (!isNetworkAvailable(app)) {
                    null to ServerConnectivity.NoInternet
                } else {
                    val result = VersionFileParser.fetchPlayerCount()
                    if (result.isSuccess) {
                        result.getOrNull() to ServerConnectivity.Online
                    } else {
                        null to ServerConnectivity.Offline
                    }
                }
            }
            _playerCount.value = count
            _serverConnectivity.value = connectivity
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
        val app = getApplication<Application>()
        DolphinLauncher.setGameIsoPath(app, path)
        val currentStorage = storage
        val rootPath = currentStorage?.rootPath
        if (rootPath != null) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    val json = DolphinLauncher.generateLaunchJson(rootPath, path)
                    File(rootPath, "RR.json").writeText(json)
                }
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
        val rootPath = storage?.rootPath
        if (rootPath != null) {
            File(rootPath, "RR.json").delete()
        }
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
                _state.value = UiState.Error(e.message ?: "Failed to launch Mii Maker")
            }
        }
    }

    fun installMiiMakerWad() {
        val app = getApplication<Application>()

        viewModelScope.launch {
            _isInstallingWad.value = true
            try {
                if (!isNetworkAvailable(app)) {
                    _isInstallingWad.value = false
                    _state.value = UiState.Error("No internet connection. Please connect to the internet and try again.")
                    return@launch
                }
                withContext(Dispatchers.IO) {
                    MiiWadInstaller.downloadAndExtractWad(app)
                }
                refreshMiiMakerState()
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Failed to install Mii Maker WAD")
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
            val currentStorage = storage ?: return@launch
            val hasSave = withContext(Dispatchers.IO) {
                SaveManager.hasSaveFile(currentStorage)
            }
            _saveState.value = SaveState(hasSave)
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

    fun fetchRooms() {
        viewModelScope.launch {
            _isLoadingRooms.value = true
            _roomsError.value = null
            val result = withContext(Dispatchers.IO) {
                VersionFileParser.fetchRooms()
            }
            result.onSuccess { rooms ->
                _rooms.value = rooms
            }.onFailure { e ->
                _roomsError.value = e.message ?: "Failed to load rooms"
                _rooms.value = emptyList()
            }
            _isLoadingRooms.value = false
        }
    }

    fun refreshSaveFileInfo() {
        viewModelScope.launch {
            _isLoadingSaveInfo.value = true
            _saveInfoError.value = null
            try {
                val currentStorage = storage ?: throw Exception("Storage not configured")
                val bytes = withContext(Dispatchers.IO) {
                    currentStorage.readBytes(SaveManager.SAVE_RELATIVE)
                }
                if (bytes == null) {
                    _saveInfoError.value = "No save file found"
                    _saveFileInfo.value = null
                } else {
                    _saveFileInfo.value = RksysParser.parse(bytes)
                }
            } catch (e: Exception) {
                _saveInfoError.value = e.message ?: "Failed to read save data"
                _saveFileInfo.value = null
            }
            _isLoadingSaveInfo.value = false
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
            val text = storage?.readFile(VERSION_FILE)?.trim() ?: "Unknown"
            text
        } catch (e: Exception) {
            "Unknown"
        }
    }

    companion object {
        private const val STORAGE_URI_KEY = "storage_tree_uri"
    }
}
