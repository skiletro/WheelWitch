package com.skiletro.wheelwitch.ui.screens

import android.view.KeyEvent
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.ChangelogEntry
import com.skiletro.wheelwitch.ui.components.ChangelogCard
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.components.sectionShape
import com.skiletro.wheelwitch.viewmodel.VersionHistoryState
import com.skiletro.wheelwitch.viewmodel.VersionHistoryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun ChangelogDetailScreen(
    onClose: () -> Unit,
    viewModel: VersionHistoryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = stringResource(R.string.version_history_title),
            onBack = onClose,
            onRefresh = { viewModel.load() }
        )

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val s = state) {
                is VersionHistoryState.Idle, is VersionHistoryState.Loading -> {
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
                is VersionHistoryState.Error -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.version_history_failed),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = s.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
                is VersionHistoryState.Success -> ChangelogDetailList(s.entries)
            }
        }
    }
}

@Composable
private fun ChangelogDetailList(entries: List<ChangelogEntry>) {
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(entries) {
        if (entries.isNotEmpty() && !hasRequestedFocus) {
            focusRequester.requestFocus()
            hasRequestedFocus = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .focusBorder(isFocused, sectionShape)
            .dpadScroll(listState, coroutineScope)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.version }) { entry ->
                ChangelogCard(entry, modifier = Modifier.animateItem())
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}

private fun Modifier.dpadScroll(
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
