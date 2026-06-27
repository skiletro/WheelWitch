package com.skiletro.wheelwitch.ui.components

import android.view.KeyEvent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.ChangelogEntry
import com.skiletro.wheelwitch.viewmodel.VersionHistoryState
import com.skiletro.wheelwitch.viewmodel.VersionHistoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Shared body for the changelog / version-history list. Used both as
 * the inline home-screen background and inside the overlay
 * [com.skiletro.wheelwitch.ui.screens.ChangelogDetailScreen] (which
 * adds a [ScreenHeader] and gamepad focus/scroll wiring on top).
 *
 * The optional [listState] lets callers hoist the scroll position so
 * they can attach their own focus/scroll modifiers to a parent. The
 * optional [highlightVersion] marks a single entry with a "NEW" pill;
 * pass `null` (the default) for the ChangelogDetailScreen overlay.
 */
@Composable
fun VersionHistoryContent(
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    highlightVersion: String? = null,
    viewModel: VersionHistoryViewModel = viewModel()
) {
  val state by viewModel.state.collectAsState()

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    when (val s = state) {
      is VersionHistoryState.Idle, is VersionHistoryState.Loading -> LoadingContent()
      is VersionHistoryState.Error -> ErrorContent(s.message)
      is VersionHistoryState.Success -> ChangelogList(s.entries, listState, highlightVersion)
    }
  }
}

@Composable
private fun LoadingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.version_history_loading),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.version_history_failed),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun ChangelogList(
    entries: List<ChangelogEntry>,
    listState: LazyListState,
    highlightVersion: String?
) {
  LazyColumn(
    state = listState,
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(entries, key = { it.version }) { entry ->
      ChangelogCard(
        entry = entry,
        isNew = highlightVersion != null && entry.version == highlightVersion,
        modifier = Modifier.animateItem()
      )
    }
    item { Spacer(modifier = Modifier.height(8.dp)) }
  }
}

/**
 * Gamepad dpad handler that animates a 300dp scroll per press and a
 * raw dispatch on key repeat. Intended to be attached to a focusable
 * wrapper around a [LazyColumn] backed by [listState].
 */
fun Modifier.dpadScroll(
    listState: LazyListState,
    coroutineScope: CoroutineScope
): Modifier = this.onPreviewKeyEvent { composeEvent ->
    val native = composeEvent.nativeKeyEvent ?: return@onPreviewKeyEvent false
    if (native.action != KeyEvent.ACTION_DOWN) return@onPreviewKeyEvent false

    val delta = when (native.keyCode) {
        KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT -> 300f
        KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_LEFT -> -300f
        else -> return@onPreviewKeyEvent false
    }

    if (delta < 0
        && listState.firstVisibleItemIndex == 0
        && listState.firstVisibleItemScrollOffset == 0
    ) return@onPreviewKeyEvent false

    if (native.repeatCount > 0) {
        listState.dispatchRawDelta(delta)
    } else {
        coroutineScope.launch {
            listState.animateScrollBy(delta, tween(durationMillis = 200))
        }
    }
    true
}
