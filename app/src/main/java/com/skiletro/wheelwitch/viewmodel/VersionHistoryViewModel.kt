package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.domain.ChangelogParser
import com.skiletro.wheelwitch.model.ChangelogEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
sealed class VersionHistoryState {
    data object Idle : VersionHistoryState()
    data object Loading : VersionHistoryState()
    data class Success(val entries: List<ChangelogEntry>) : VersionHistoryState()
    data class Error(val message: String) : VersionHistoryState()
}

/** Owns the changelog / version history state shown on the Version History screen. */
class VersionHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val _state = MutableStateFlow<VersionHistoryState>(VersionHistoryState.Idle)
    val state: StateFlow<VersionHistoryState> = _state.asStateFlow()

    init {
        load()
    }

    /**
     * Reloads the changelog from cache-or-network. Safe to call
     * repeatedly; resets to [VersionHistoryState.Loading] and refetches.
     * Construction calls this once, so simply instantiating the VM
     * triggers a network call (cached when possible).
     */
    fun load() {
        _state.value = VersionHistoryState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                ChangelogParser.fetchWithCache(app)
            }
            _state.value = result.fold(
                onSuccess = { VersionHistoryState.Success(it) },
                onFailure = { VersionHistoryState.Error(it.message ?: app.getString(R.string.version_history_failed)) }
            )
        }
    }
}
