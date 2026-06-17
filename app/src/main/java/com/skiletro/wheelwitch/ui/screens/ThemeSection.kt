package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.ui.theme.ThemeMode

@Composable
fun ThemeSection(
    useDynamicColor: Boolean,
    onToggleDynamicColor: (Boolean) -> Unit,
    themeMode: ThemeMode,
    onChangeThemeMode: (ThemeMode) -> Unit
) {
    Text(
        text = "Theme",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleDynamicColor(!useDynamicColor) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Material You",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Use dynamic colors from your wallpaper",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = useDynamicColor,
            onCheckedChange = onToggleDynamicColor
        )
    }
    Spacer(modifier = Modifier.height(12.dp))
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Dark Mode",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = when (themeMode) {
                        ThemeMode.Light -> "Light"
                        ThemeMode.Dark -> "Dark"
                        ThemeMode.System -> "System"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(text = "\u25BC", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
            ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = when (mode) {
                                ThemeMode.Light -> "Light"
                                ThemeMode.Dark -> "Dark"
                                ThemeMode.System -> "System"
                            }
                        )
                    },
                    onClick = {
                        onChangeThemeMode(mode)
                        expanded = false
                    }
                )
            }
            }
        }
    }
}
