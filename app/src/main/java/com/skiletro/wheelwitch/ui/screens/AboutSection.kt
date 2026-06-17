package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.BuildConfig
import com.skiletro.wheelwitch.ui.components.focusBorder

@Composable
fun AboutSection() {
    var focused by remember { mutableStateOf(false) }
    val version = if (BuildConfig.DEBUG) "v${BuildConfig.VERSION_NAME}-debug-${BuildConfig.GIT_HASH}" else "v${BuildConfig.VERSION_NAME}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusable()
            .onFocusChanged { focused = it.isFocused }
            .focusBorder(focused),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Wheel Witch $version",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Manages Retro Rewind Pack for MKWii",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "https://update.rwfc.net",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "thanks to:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "ZPL",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Patchzy and the Wheel Wizard team",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Composables",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
