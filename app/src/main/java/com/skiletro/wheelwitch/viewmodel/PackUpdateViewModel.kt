package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.domain.RewindPackManager
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiFaceCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Owns the pack install / update state machine and the storage URI.
 *
 * [currentStorage] is exposed via the companion object's [currentStorage]
 * getter so that [SaveDataViewModel] can read it without needing DI.
 */
class PackUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val prefs = application.getSharedPreferences("wheelwitch", Application.MODE_PRIVATE)

    private val _state = MutableStateFlow<UiState>(UiState.NoStorage)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentIsoPath = MutableStateFlow<String?>(null)
    val currentIsoPath: StateFlow<String?> = _currentIsoPath.asStateFlow()

    private var storageUri: Uri? = null
    private var storage: PackStorage? = null
    val storageRootPath: String? get() = storage?.rootPath

    init {
        RewindPackManager.initCacheDir(application.cacheDir)
        MiiFaceCache.init(application)
        restoreStorageUri()
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
        currentStorage = storage
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
            val serverVersion = when (status) {
                is PackStatus.UpdateAvailable -> status.latestVersion.toString()
                is PackStatus.UpToDate -> status.latestVersion.toString()
                else -> null
            }
            if (serverVersion != null) {
                prefs.edit().putString("last_server_version", serverVersion).apply()
            }
            _state.value = UiState.Ready(status)
            saveDataDelegate?.onPackStatusChanged()
        }
    }

    fun downloadOrUpdate(status: PackStatus) {
        viewModelScope.launch {
            val currentStorage = storage ?: run {
                _state.value = UiState.Error(app.getString(R.string.vm_storage_not_configured))
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
                            app.getString(R.string.home_cannot_reach_server)
                        )
                        return@launch
                    }
                    is PackStatus.UpToDate -> {
                        withContext(Dispatchers.IO) {
                            SaveManager.deleteSaveBackup(app)
                        }
                        _state.value = UiState.Ready(PackStatus.UpToDate(status.currentVersion, status.latestVersion))
                        saveDataDelegate?.onPackStatusChanged()
                        return@launch
                    }
                }
                withContext(Dispatchers.IO) {
                    SaveManager.deleteSaveBackup(app)
                }
                _state.value = UiState.Ready(PackStatus.UpToDate(installedVersion, installedVersion))
                saveDataDelegate?.onPackStatusChanged()
            } catch (e: Exception) {
                _state.value = UiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun launchDolphin() {
        val app = getApplication<Application>()
        val currentStorage = storage ?: run {
            _state.value = UiState.Error(app.getString(R.string.vm_storage_not_configured))
            return
        }
        val gameIsoPath = DolphinLauncher.getGameIsoPath(app)
        if (gameIsoPath.isNullOrBlank()) {
            return
        }
        if (!File(gameIsoPath).exists()) {
            clearIsoPath()
            _state.value = UiState.Error(app.getString(R.string.home_rom_not_found))
            return
        }
        val rootPath = currentStorage.rootPath
        if (rootPath == null) {
            _state.value = UiState.Error(app.getString(R.string.home_no_iso_cant_launch))
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
                _state.value = UiState.Error(e.message ?: app.getString(R.string.home_launch_failed))
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

    fun clearError() {
        val currentState = _state.value
        if (currentState is UiState.Error) {
            checkStatus()
        }
    }

    fun dismissSuccess() {
        _successMessage.value = null
    }

    fun setSuccessMessage(message: String) {
        _successMessage.value = message
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

        /**
         * The currently active [PackStorage] instance, shared with other
         * ViewModels (e.g. [SaveDataViewModel]) so they can read/write the
         * pack files without needing constructor injection.
         */
        @Volatile
        var currentStorage: PackStorage? = null
            private set

        /**
         * Callback invoked when the pack status changes, so dependent
         * ViewModels can refresh their state.
         */
        @Volatile
        var saveDataDelegate: SaveDataDelegate? = null
    }
}

/**
 * Interface implemented by [SaveDataViewModel] to receive notifications
 * about pack status changes without creating a hard coupling.
 */
interface SaveDataDelegate {
    fun onPackStatusChanged()
}

fun Context.isNetworkAvailable(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
