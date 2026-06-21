package com.skiletro.wheelwitch.viewmodel

import androidx.compose.runtime.Immutable
import com.skiletro.wheelwitch.model.PackStatus

/**
 * High-level state for the pack status checker.
 *
 * Lifecycle: `Idle -> Checking -> Ready | Error`. The previous
 * download / extract / apply-update states are gone — the install flow
 * has been ripped out for a planned rewrite.
 */
@Immutable
sealed class UiState {
    /** Initial state before the first check completes. */
    data object Idle : UiState()

    /** A status check is in progress. */
    data object Checking : UiState()

    /** A status check finished; [status] is the current pack state. */
    data class Ready(val status: PackStatus) : UiState()

    /** A status check failed; [message] is user-facing. */
    data class Error(val message: String) : UiState()
}
