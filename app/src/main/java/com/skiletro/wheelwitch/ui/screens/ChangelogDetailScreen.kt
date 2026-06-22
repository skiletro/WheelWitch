package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.components.VersionHistoryContent
import com.skiletro.wheelwitch.ui.components.dpadScroll
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.theme.sectionShape
import com.skiletro.wheelwitch.viewmodel.VersionHistoryState
import com.skiletro.wheelwitch.viewmodel.VersionHistoryViewModel

@Composable
fun ChangelogDetailScreen(
    onClose: () -> Unit,
    viewModel: VersionHistoryViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state) {
        val current = state
        if (current is VersionHistoryState.Success
            && current.entries.isNotEmpty()
            && !hasRequestedFocus
        ) {
            focusRequester.requestFocus()
            hasRequestedFocus = true
        }
    }

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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .focusable()
                .onFocusChanged { isFocused = it.isFocused }
                .focusBorder(isFocused, sectionShape)
                .dpadScroll(listState, coroutineScope)
        ) {
            VersionHistoryContent(
                modifier = Modifier.fillMaxSize(),
                listState = listState,
                viewModel = viewModel
            )
        }
    }
}
