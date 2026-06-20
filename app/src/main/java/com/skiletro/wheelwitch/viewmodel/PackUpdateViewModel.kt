package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.data.GameTypeParser
import com.skiletro.wheelwitch.data.PackStorage
import com.skiletro.wheelwitch.data.SaveManager
import com.skiletro.wheelwitch.domain.RewindPackManager
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.model.ProgressInfo
import com.skiletro.wheelwitch.util.DolphinLauncher
import com.skiletro.wheelwitch.util.MiiFaceCache
import com.skiletro.wheelwitch.util.Prefs
import com.skiletro.wheelwitch.util.PrefsKeys
import com.skiletro.wheelwitch.viewmodel.PackUpdateViewModel.Companion.currentStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

/**
 * Owns the pack install / update state machine and the storage URI.
 *
 * [currentStorage] is exposed via the companion object's [currentStorage]
 * getter so that [SaveDataViewModel] can read it without needing DI.
 * Companion-object state is used because AndroidViewModel instantiation
 * is framework-controlled, so constructor injection is not available.
 */
class PackUpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val prefs = Prefs.main(application)

    private val _state = MutableStateFlow<UiState>(UiState.NoStorage)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _currentIsoPath = MutableStateFlow<String?>(null)
    val currentIsoPath: StateFlow<String?> = _currentIsoPath.asStateFlow()

    private val _gameInfo = MutableStateFlow<GameTypeParser.GameInfo?>(null)
    val gameInfo: StateFlow<GameTypeParser.GameInfo?> = _gameInfo.asStateFlow()

    private val _myStuffMode = MutableStateFlow(DolphinLauncher.MyStuffMode.Everything)
    val myStuffMode: StateFlow<DolphinLauncher.MyStuffMode> = _myStuffMode.asStateFlow()

    private var storage: PackStorage? = null
    val storageRootPath: String? get() = storage?.rootPath

    init {
        RewindPackManager.initCacheDir(application.cacheDir)
        MiiFaceCache.init(application)
        restoreStorageUri()
        _myStuffMode.value = readMyStuffMode()
    }

    private fun restoreStorageUri() {
        val uriString = prefs.getString(PrefsKeys.STORAGE_URI_KEY, null)
        if (uriString == null) {
            _state.value = UiState.NoStorage
            return
        }
        val path = PackStorage.resolveTreeUriToPath(Uri.parse(uriString))
        if (path == null) {
            _state.value = UiState.NoStorage
            return
        }
        storage = PackStorage(path)
        currentStorage = storage
        refreshIsoPath()
        refreshGameInfo()
        checkStatus()
    }

    private fun refreshIsoPath() {
        val rootPath = storage?.rootPath ?: return
        _currentIsoPath.value = DolphinLauncher.readIsoPathFromLaunchJson(rootPath)
    }

    /** Persists [uri] as the pack storage root and re-checks the pack status. */
    fun setStorageUri(uri: Uri) {
        prefs.edit().putString(PrefsKeys.STORAGE_URI_KEY, uri.toString()).apply()
        val path = PackStorage.resolveTreeUriToPath(uri)
        if (path == null) {
            _state.value = UiState.Error(app.getString(R.string.home_no_iso_cant_launch))
            return
        }
        storage = PackStorage(path)
        currentStorage = storage
        checkStatus()
    }

    /** Re-checks the local version against the server and updates [state]. */
    fun checkStatus() {
        viewModelScope.launch {
            _state.value = UiState.Checking
            val activeStorage = storage ?: run {
                _state.value = UiState.NoStorage
                return@launch
            }
            val status = withContext(Dispatchers.IO) {
                RewindPackManager.checkStatus(activeStorage)
            }
            Timber.tag("PackUpdate").d("checkStatus -> %s", status::class.simpleName)
            // Both UpdateAvailable and UpToDate carry the server's latest version;
            // Kotlin cannot smart-cast across combined `is` branches, so they stay separate.
            val serverVersion = when (status) {
                is PackStatus.UpdateAvailable -> status.latestVersion.toString()
                is PackStatus.UpToDate -> status.latestVersion.toString()
                else -> null
            }
            if (serverVersion != null) {
                prefs.edit().putString(PrefsKeys.LAST_SERVER_VERSION_KEY, serverVersion).apply()
            }
            _state.value = UiState.Ready(status)
            saveDataDelegate?.onPackStatusChanged()
        }
    }

    /**
     * Installs or updates the pack according to [status]. Handles four
     * cases: fresh install, incremental update, "server unreachable"
     * fallback, and "already up to date" no-op. Emits progress via
     * [state] and clears the save backup on success.
     */
    fun downloadOrUpdate(status: PackStatus) {
        viewModelScope.launch {
            val activeStorage = storage ?: run {
                _state.value = UiState.Error(app.getString(R.string.error_storage_not_configured))
                return@launch
            }
            try {
                withContext(Dispatchers.IO) {
                    SaveManager.backupSaveToCache(app, activeStorage)
                }
                Timber.tag("PackUpdate").d("Save backed up to cache before install/update")

                val installedVersion = when (status) {
                    is PackStatus.NotInstalled -> {
                        Timber.tag("PackUpdate").i("Starting fresh install")
                        performFreshInstall(activeStorage)
                    }
                    is PackStatus.UpdateAvailable -> {
                        Timber.tag("PackUpdate")
                            .i("Starting incremental update from %s to %s", status.currentVersion, status.latestVersion)
                        performIncrementalUpdate(activeStorage, status)
                    }
                    is PackStatus.Installed -> {
                        Timber.tag("PackUpdate")
                            .w("Server unreachable but local install present")
                        _state.value =
                            UiState.Error(app.getString(R.string.error_cannot_reach_server))
                        return@launch
                    }

                    is PackStatus.UpToDate -> {
                        handleAlreadyUpToDate(status)
                        return@launch
                    }
                }
                withContext(Dispatchers.IO) {
                    SaveManager.deleteSaveBackup(app)
                }
                _state.value =
                    UiState.Ready(PackStatus.UpToDate(installedVersion, installedVersion))
                saveDataDelegate?.onPackStatusChanged()
                Timber.tag("PackUpdate").i("Install/update complete: v%s", installedVersion)
            } catch (e: Exception) {
                Timber.tag("PackUpdate").e(e, "Install/update failed")
                _state.value = UiState.Error(e.message ?: app.getString(R.string.vm_unknown_error))
            }
        }
    }

    private suspend fun performFreshInstall(storage: PackStorage): com.skiletro.wheelwitch.model.SemVersion {
        return withContext(Dispatchers.IO) {
            RewindPackManager.freshInstall(storage) { progress ->
                handleProgress(progress)
            }
        }.getOrThrow()
    }

    private suspend fun performIncrementalUpdate(
        storage: PackStorage,
        status: PackStatus.UpdateAvailable,
    ): com.skiletro.wheelwitch.model.SemVersion {
        return withContext(Dispatchers.IO) {
            RewindPackManager.incrementalUpdate(
                storage,
                status.serverInfo,
                status.currentVersion,
            ) { progress ->
                handleProgress(progress)
            }
        }.getOrThrow()
    }

    private suspend fun handleAlreadyUpToDate(status: PackStatus.UpToDate) {
        withContext(Dispatchers.IO) {
            SaveManager.deleteSaveBackup(app)
        }
        _state.value =
            UiState.Ready(PackStatus.UpToDate(status.currentVersion, status.latestVersion))
        saveDataDelegate?.onPackStatusChanged()
    }

    /** Launches Dolphin with the configured ISO and RR.json; reports errors via [state]. */
    fun launchDolphin() {
        val activeStorage = storage ?: run {
            _state.value = UiState.Error(app.getString(R.string.error_storage_not_configured))
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
        val rootPath = activeStorage.rootPath

        viewModelScope.launch {
            try {
                val mode = readMyStuffMode()
                val json = withContext(Dispatchers.IO) {
                    DolphinLauncher.generateLaunchJson(rootPath, gameIsoPath, myStuffMode = mode)
                }
                val rrJsonFile = File(rootPath, DolphinLauncher.RR_JSON_NAME)
                withContext(Dispatchers.IO) {
                    rrJsonFile.writeText(json)
                }
                DolphinLauncher.launchDolphin(app, rrJsonFile.absolutePath).getOrThrow()
                Timber.tag("PackUpdate").i("Dolphin launched with %s", rrJsonFile.absolutePath)
            } catch (e: Exception) {
                Timber.tag("PackUpdate").e(e, "Dolphin launch failed")
                _state.value =
                    UiState.Error(e.message ?: app.getString(R.string.home_launch_failed))
            }
        }
    }

    /** Persists [path] as the ISO path and regenerates RR.json if storage is configured. */
    fun setGameIsoPath(path: String) {
        DolphinLauncher.setGameIsoPath(app, path)
        val rootPath = storage?.rootPath
        if (rootPath != null) {
            val mode = readMyStuffMode()
            viewModelScope.launch(Dispatchers.IO) {
                DolphinLauncher.writeLaunchJson(rootPath, path, mode)
            }
        }
        _currentIsoPath.value = path
        refreshGameInfo()
    }

    /** Clears the persisted ISO path and deletes the existing RR.json. */
    fun clearIsoPath() {
        DolphinLauncher.setGameIsoPath(app, "")
        storage?.rootPath?.let { DolphinLauncher.deleteLaunchJson(it) }
        _currentIsoPath.value = null
        _gameInfo.value = null
    }

    private fun refreshGameInfo() {
        val path = _currentIsoPath.value ?: run {
            _gameInfo.value = null
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    _gameInfo.value = null
                    return@launch
                }
                val header = file.inputStream().use { input ->
                    val buf = ByteArray(4096)
                    val bytesRead = input.read(buf)
                    if (bytesRead <= 0) ByteArray(0) else buf.copyOf(bytesRead)
                }
                val info = GameTypeParser.parseGameInfo(file.name, header)
                _gameInfo.value = info.takeIf { it.format != GameTypeParser.GameFormat.Invalid }
            } catch (e: Exception) {
                Timber.tag("PackUpdate").w(e, "Failed to parse game info for %s", path)
                _gameInfo.value = null
            }
        }
    }

    /** Persists [mode] and regenerates RR.json with the new My Stuff choice. */
    fun setMyStuffMode(mode: DolphinLauncher.MyStuffMode) {
        _myStuffMode.value = mode
        prefs.edit().putString(PrefsKeys.RIIVOLUTION_MY_STUFF_MODE_KEY, mode.name).apply()
        regenerateLaunchJson()
    }

    private fun readMyStuffMode(): DolphinLauncher.MyStuffMode {
        val name = prefs.getString(PrefsKeys.RIIVOLUTION_MY_STUFF_MODE_KEY, null)
            ?: return DolphinLauncher.MyStuffMode.Everything
        return try {
            DolphinLauncher.MyStuffMode.valueOf(name)
        } catch (_: IllegalArgumentException) {
            DolphinLauncher.MyStuffMode.Everything
        }
    }

    private fun regenerateLaunchJson() {
        val rootPath = storage?.rootPath ?: return
        val gameIsoPath = DolphinLauncher.getGameIsoPath(app) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            DolphinLauncher.writeLaunchJson(rootPath, gameIsoPath, readMyStuffMode())
        }
    }

    /** Clears an error state and re-checks status. No-op for non-error states. */
    fun clearError() {
        val currentState = _state.value
        if (currentState is UiState.Error) {
            checkStatus()
        }
    }

    /** Clears the one-shot success message. */
    fun dismissSuccess() {
        _successMessage.value = null
    }

    /** Sets a one-shot success message to be shown and then dismissed. */
    fun setSuccessMessage(message: String) {
        _successMessage.value = message
    }

    private fun handleProgress(progress: ProgressInfo) {
        _state.value = when (progress) {
            is ProgressInfo.Checking -> UiState.Checking
            is ProgressInfo.Downloading ->
                UiState.Downloading(
                    progress.progress,
                    progress.bytesPerSecond,
                    progress.bytesDownloaded,
                    progress.totalBytes,
                    progress.message,
                )
            is ProgressInfo.Extracting -> UiState.Extracting(progress.progress)
            is ProgressInfo.ApplyingUpdate -> UiState.ApplyingUpdate(
                progress.index, progress.total, progress.description, progress.progress
            )
        }
    }

    companion object {
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
