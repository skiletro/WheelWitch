package com.skiletro.wheelwitch.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.skiletro.wheelwitch.model.ServerConnectivity

@Preview(showBackground = true)
@Composable
private fun SuccessBannerPreview() {
    SuccessBanner("Backup successful")
}

@Preview(showBackground = true)
@Composable
private fun BottomLaunchBarPreview() {
    BottomLaunchBar(onLaunch = {}, onRefresh = {})
}

@Preview(showBackground = true)
@Composable
private fun BottomLaunchBarOnlinePreview() {
    BottomLaunchBar(onLaunch = {}, onRefresh = {}, playerCount = 42, serverConnectivity = ServerConnectivity.Online)
}

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
private fun NoStorageContent(onPickStorage: () -> Unit = {}) {
    HomeNoStorageContent(onPickStorage)
}

@Preview(showBackground = true)
@Composable
private fun ProgressContent(
    progress: Float = -1f,
    message: String = "Downloading..."
) {
    HomeProgressContent(progress, message)
}

@Preview(showBackground = true)
@Composable
private fun ReadyToLaunchContent(
    version: String = "3.2.6"
) {
    HomeReadyToLaunchContent(version)
}

@Preview(showBackground = true)
@Composable
private fun ErrorContentPreview() {
    HomeErrorContent("Something went wrong!", onRetry = {}, onPickIso = {})
}

@Preview(showBackground = true)
@Composable
private fun ErrorContentRomPreview() {
    HomeErrorContent("Please select your Mario Kart Wii ROM file first.", onRetry = {}, onPickIso = {})
}
