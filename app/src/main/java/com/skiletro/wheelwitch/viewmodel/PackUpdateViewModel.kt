package com.skiletro.wheelwitch.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.skiletro.wheelwitch.domain.RewindPackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Owns the pack status check state. The install / update / launch logic
 * has been ripped out for a planned rewrite — only [checkStatus] and
 * [clearError] remain.
 */
class PackUpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        checkStatus()
    }

    /**
     * Re-checks the local version against the server manifest. Emits
     * [UiState.Checking] then [UiState.Ready] (with the resolved
     * [com.skiletro.wheelwitch.model.PackStatus]) or [UiState.Error].
     */
    fun checkStatus() {
        viewModelScope.launch {
            _state.value = UiState.Checking
            _state.value = runCatching { RewindPackManager.checkStatus() }
                .fold(
                    onSuccess = {
                        Timber.tag("PackUpdate").d("checkStatus -> %s", it::class.simpleName)
                        UiState.Ready(it)
                    },
                    onFailure = { e ->
                        Timber.tag("PackUpdate").e(e, "checkStatus failed")
                        UiState.Error(e.message ?: e::class.simpleName.orEmpty())
                    },
                )
        }
    }

    /** Clears an error state and re-checks status. No-op for non-error states. */
    fun clearError() {
        if (_state.value is UiState.Error) checkStatus()
    }
}
