package com.skiletro.wheelwitch.viewmodel

import androidx.compose.runtime.Immutable
import com.skiletro.wheelwitch.model.PackStatus

/**
 * High-level state machine for the pack install / update flow.
 *
 * The lifecycle is:
 * `NoStorage -> Checking -> Downloading/Extracting/ApplyingUpdate -> Ready/Error`
 */
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
