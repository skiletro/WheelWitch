package com.skiletro.wheelwitch.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.viewmodel.MiiMakerState

@Composable
fun MiiMakerSection(
    miiMakerState: MiiMakerState,
    isInstallingWad: Boolean,
    miiMakerError: String?,
    onInstallWad: () -> Unit,
    onDeleteWad: () -> Unit
) {
    Text(
        text = "Mii Channel WAD",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Status: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = if (miiMakerState.hasWad) "Installed" else "Not installed",
            style = MaterialTheme.typography.bodyMedium,
            color = if (miiMakerState.hasWad) Color(0xFF66BB6A) else MaterialTheme.colorScheme.error
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    if (miiMakerError != null) {
        Text(
            text = miiMakerError,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
    if (isInstallingWad) {
        val infiniteTransition = rememberInfiniteTransition(label = "wad")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1500, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "rotation"
        )
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(com.skiletro.wheelwitch.R.drawable.ic_hat_wizard),
                contentDescription = "Installing",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(40.dp)
                    .rotate(rotation)
            )
        }
    } else if (miiMakerState.hasWad) {
        var deleteFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onDeleteWad,
            shape = buttonShape,
            modifier = Modifier
                .onFocusChanged { deleteFocused = it.isFocused }
                .focusBorder(deleteFocused, color = MaterialTheme.colorScheme.onError),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        ) {
            Text(
                "Delete cached WAD",
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        var installFocused by remember { mutableStateOf(false) }
        Button(
            onClick = onInstallWad,
            shape = buttonShape,
            modifier = Modifier
                .height(56.dp)
                .fillMaxWidth()
                .onFocusChanged { installFocused = it.isFocused }
                .focusBorder(installFocused),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text(
                "Install Mii Channel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
