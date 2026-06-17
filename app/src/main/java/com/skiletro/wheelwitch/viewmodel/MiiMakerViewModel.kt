package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.util.MiiWadInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

@Immutable
data class MiiMakerState(
    val hasWad: Boolean = false,
)

/**
 * Owns the Mii Channel WAD install / launch / delete state.
 *
 * [installMiiMakerWad] is guarded by a [Mutex] so concurrent user taps
 * cannot trigger parallel installs.
 */
class MiiMakerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val _miiMakerState = MutableStateFlow(MiiMakerState())
    val miiMakerState: StateFlow<MiiMakerState> = _miiMakerState.asStateFlow()

    private val _isInstallingWad = MutableStateFlow(false)
    val isInstallingWad: StateFlow<Boolean> = _isInstallingWad.asStateFlow()

    private val _miiMakerError = MutableStateFlow<String?>(null)
    val miiMakerError: StateFlow<String?> = _miiMakerError.asStateFlow()

    private val installMutex = Mutex()

    init {
        refreshMiiMakerState()
    }

    fun refreshMiiMakerState() {
        val hasWad = MiiWadInstaller.getCachedWadFile(getApplication()) != null
        _miiMakerState.value = MiiMakerState(hasWad)
    }

    fun clearError() {
        _miiMakerError.value = null
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
                _miiMakerError.value = e.message ?: app.getString(R.string.vm_mii_launch_failed)
            }
        }
    }

    fun installMiiMakerWad() {
        val app = getApplication<Application>()
        viewModelScope.launch {
            installMutex.withLock {
                _isInstallingWad.value = true
                _miiMakerError.value = null
                try {
                    if (!app.isNetworkAvailable()) {
                        _miiMakerError.value = app.getString(R.string.vm_no_internet)
                    } else {
                        withContext(Dispatchers.IO) {
                            MiiWadInstaller.downloadAndExtractWad(app)
                        }
                        refreshMiiMakerState()
                    }
                } catch (e: Exception) {
                    _miiMakerError.value = e.message ?: app.getString(R.string.vm_mii_install_failed)
                }
                _isInstallingWad.value = false
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
}
