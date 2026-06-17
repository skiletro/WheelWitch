package com.skiletro.wheelwitch.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.skiletro.wheelwitch.ui.theme.ThemeMode
import com.skiletro.wheelwitch.viewmodel.MiiMakerState
import com.skiletro.wheelwitch.viewmodel.SaveState

@Preview(showBackground = true)
@Composable
private fun SaveDataSectionHasSavePreview() {
    SaveDataSection(saveState = SaveState(hasSave = true), onBackup = {}, onRestore = {}, onDelete = {})
}

@Preview(showBackground = true)
@Composable
private fun SaveDataSectionNoSavePreview() {
    SaveDataSection(saveState = SaveState(hasSave = false), onBackup = {}, onRestore = {}, onDelete = {})
}

@Preview(showBackground = true)
@Composable
private fun MiiMakerSectionInstalledPreview() {
    MiiMakerSection(miiMakerState = MiiMakerState(hasWad = true), isInstallingWad = false, onInstallWad = {}, onDeleteWad = {})
}

@Preview(showBackground = true)
@Composable
private fun MiiMakerSectionNotInstalledPreview() {
    MiiMakerSection(miiMakerState = MiiMakerState(hasWad = false), isInstallingWad = false, onInstallWad = {}, onDeleteWad = {})
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun ThemeSectionPreview() {
    ThemeSection(useDynamicColor = false, onToggleDynamicColor = {}, themeMode = ThemeMode.System, onChangeThemeMode = {})
}

@Preview(showBackground = true)
@Composable
private fun StorageSectionConfiguredPreview() {
    StorageSection("/storage/emulated/0/RetroRewind")
}

@Preview(showBackground = true)
@Composable
private fun StorageSectionNotConfiguredPreview() {
    StorageSection(null)
}
@Preview(showBackground = true)
@Composable
private fun CacheSectionPreview() {
    CacheSection()
}

@Preview(showBackground = true)
@Composable
private fun AboutSectionPreview() {
    AboutSection()
}
