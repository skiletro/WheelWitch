package com.skiletro.wheelwitch.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Standard screen header.
 *
 * Layout: [optional back] | title (centered, weighted) | [trailing slot] | [optional refresh].
 *
 * The [trailing] slot is rendered between the title and the refresh button and
 * is intended for small inline content such as a "5 minutes ago" label.
 *
 * Focus state for the back and refresh buttons is hoisted to the top of the
 * composable so the `var` declarations live in a stable, remembered scope.
 */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onRefresh: (() -> Unit)? = null,
    titleModifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    var backFocused by remember { mutableStateOf(false) }
    var refreshFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(PaddingValues(horizontal = 12.dp, vertical = 16.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .onFocusChanged { backFocused = it.isFocused }
                    .focusBorder(backFocused)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            androidx.compose.foundation.layout.Spacer(modifier = Modifier)
        }

        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .then(titleModifier)
        )

        trailing()

        if (onRefresh != null) {
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .onFocusChanged { refreshFocused = it.isFocused }
                    .focusBorder(refreshFocused)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = "Refresh",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
