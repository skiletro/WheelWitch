package com.skiletro.wheelwitch.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.model.PackStatus
import com.skiletro.wheelwitch.viewmodel.UiState

@Preview(showBackground = true)
@Composable
private fun ServerStatusOnlinePreview() {
    ServerStatusIndicator(playerCount = 128, connectivity = ServerConnectivity.Online)
}

@Preview(showBackground = true)
@Composable
private fun ServerStatusOfflinePreview() {
    ServerStatusIndicator(playerCount = null, connectivity = ServerConnectivity.Offline)
}

@Preview(showBackground = true)
@Composable
private fun ProgressButtonPreview() {
    ProgressButton(progress = 0.67f, label = "Downloading...")
}

@Preview(showBackground = true)
@Composable
private fun ProgressButtonIndeterminatePreview() {
    ProgressButton(progress = -1f, label = "Checking...")
}
