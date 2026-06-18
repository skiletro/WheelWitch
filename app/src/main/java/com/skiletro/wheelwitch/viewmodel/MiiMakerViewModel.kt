package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.util.MiiWadInstaller
import com.skiletro.wheelwitch.util.isNetworkAvailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Owns the Mii Channel WAD install / launch / delete state.
 *
 * Exposes three pieces of state: [hasWad] (a cached WAD is present),
 * [isInstallingWad] (an install is in flight), and [miiMakerError]
 * (the last user-facing error message, if any).
 *
 * [installMiiMakerWad] is guarded by a [Mutex] so concurrent user taps
 * cannot trigger parallel installs.
 */
class MiiMakerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val _hasWad = MutableStateFlow(false)
    val hasWad: StateFlow<Boolean> = _hasWad.asStateFlow()

    private val _isInstallingWad = MutableStateFlow(false)
    val isInstallingWad: StateFlow<Boolean> = _isInstallingWad.asStateFlow()

    private val _miiMakerError = MutableStateFlow<String?>(null)
    val miiMakerError: StateFlow<String?> = _miiMakerError.asStateFlow()

    private val installMutex = Mutex()

    init {
        refreshHasWad()
    }

    /** Re-checks whether a cached WAD exists and updates [hasWad]. */
    fun refreshHasWad() {
        _hasWad.value = MiiWadInstaller.getCachedWadFile(app) != null
    }

    /** Clears the last error message shown to the user. */
    fun clearError() {
        _miiMakerError.value = null
    }

    /** Launches Dolphin with the cached Mii Channel WAD, if one is present. */
    fun launchMiiMaker() {
        viewModelScope.launch {
            try {
                val cached = withContext(Dispatchers.IO) {
                    MiiWadInstaller.getCachedWadFile(app)
                }
                if (cached != null) {
                    MiiWadInstaller.launchWadFile(app, cached).getOrThrow()
                }
            } catch (e: Exception) {
                _miiMakerError.value = e.message ?: app.getString(R.string.vm_mii_launch_failed)
            }
        }
    }

    /**
     * Downloads and extracts the Mii Channel WAD. No-op when there is no
     * network connection; in that case [miiMakerError] is populated.
     *
     * Guarded by [installMutex] so concurrent calls serialize.
     */
    fun installMiiMakerWad() {
        viewModelScope.launch {
            installMutex.withLock {
                _isInstallingWad.value = true
                _miiMakerError.value = null
                try {
                    if (!app.isNetworkAvailable()) {
                        _miiMakerError.value = app.getString(R.string.vm_no_internet)
                        return@withLock
                    }
                    withContext(Dispatchers.IO) {
                        MiiWadInstaller.downloadAndExtractWad(app)
                    }
                    refreshHasWad()
                } catch (e: Exception) {
                    _miiMakerError.value = e.message ?: app.getString(R.string.vm_mii_install_failed)
                } finally {
                    _isInstallingWad.value = false
                }
            }
        }
    }

    /** Deletes the cached WAD and refreshes [hasWad]. */
    fun deleteWad() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                MiiWadInstaller.clearCache(app)
            }
            refreshHasWad()
        }
    }
}
