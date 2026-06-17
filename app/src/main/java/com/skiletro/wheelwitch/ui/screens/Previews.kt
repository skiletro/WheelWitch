package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.Player
import com.skiletro.wheelwitch.model.Room
import com.skiletro.wheelwitch.model.ServerConnectivity
import com.skiletro.wheelwitch.viewmodel.RoomsState
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.model.SaveFileInfo
import com.skiletro.wheelwitch.viewmodel.SaveInfoState
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton
import com.skiletro.wheelwitch.ui.components.TopBar

// ── Sample data ──

private val sampleLicenses = listOf(
    LicenseInfo(slotIndex = 0, exists = true, miiName = "Jamie", friendCode = "1234-5678-9012", vr = 5000, raceWins = 120, raceLosses = 45),
    LicenseInfo(slotIndex = 1, exists = true, miiName = "Guest", friendCode = "9876-5432-1098", vr = 7200, raceWins = 200, raceLosses = 60),
    LicenseInfo(slotIndex = 2, exists = false),
    LicenseInfo(slotIndex = 3, exists = false)
)

private val samplePlayer = Player(name = "Jamie", friendCode = "1234-5678-9012", vr = 5000, br = 3000, isOpenHost = false, mii = null)
private val sampleHost = Player(name = "Hosty", friendCode = "9876-5432-1098", vr = 7200, br = 4000, isOpenHost = true, mii = null)
private val sampleRooms = listOf(
    Room(id = "1", players = listOf(sampleHost, samplePlayer), averageVR = 6100, trackName = "GCN Peach Beach", roomType = "FFA", isPublic = true, isJoinable = true),
    Room(id = "2", players = listOf(Player(name = "Player2", friendCode = "1111-2222-3333", vr = 3000, br = 2500, isOpenHost = false, mii = null)), averageVR = 3000, trackName = null, roomType = "FFA", isPublic = true, isJoinable = false)
)

// ── Home screen previews ──

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

// ── Rooms previews ──

@Preview(showBackground = true, widthDp = 600, heightDp = 400)
@Composable
private fun RoomsScreenPreview() {
    RoomsScreen(
        roomsState = RoomsState.Success(sampleRooms, sampleRooms.sumOf { it.players.size }, ServerConnectivity.Online),
        onRefresh = {}, onClose = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun RoomsScreenLoadingPreview() {
    RoomsScreen(roomsState = RoomsState.Loading, onRefresh = {}, onClose = {})
}

@Preview(showBackground = true)
@Composable
private fun RoomsScreenErrorPreview() {
    RoomsScreen(roomsState = RoomsState.Error("Failed to connect"), onRefresh = {}, onClose = {})
}

@Preview(showBackground = true)
@Composable
private fun RoomsScreenEmptyPreview() {
    RoomsScreen(
        roomsState = RoomsState.Success(emptyList(), null, ServerConnectivity.Online),
        onRefresh = {}, onClose = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun RoomListItemSelectedPreview() {
    RoomListItem(room = sampleRooms[0], isSelected = true, onClick = {}, modifier = Modifier.width(200.dp))
}

@Preview(showBackground = true)
@Composable
private fun RoomListItemUnselectedPreview() {
    RoomListItem(room = sampleRooms[0], isSelected = false, onClick = {}, modifier = Modifier.width(200.dp))
}

@Preview(showBackground = true)
@Composable
private fun MiiPlayerCardHostPreview() {
    MiiPlayerCard(player = sampleHost)
}

@Preview(showBackground = true)
@Composable
private fun MiiPlayerCardPlayerPreview() {
    MiiPlayerCard(player = samplePlayer)
}

// ── Components previews ──

@Preview(showBackground = true)
@Composable
private fun PrimaryActionButtonDefaultPreview() {
    PrimaryActionButton(text = "Launch Retro Rewind", onClick = {})
}

@Preview(showBackground = true)
@Composable
private fun PrimaryActionButtonSubtextPreview() {
    PrimaryActionButton(text = "Check for updates", onClick = {}, subText = "Latest: v6.11.1")
}

@Preview(showBackground = true)
@Composable
private fun PrimaryActionButtonDisabledPreview() {
    PrimaryActionButton(text = "Launch Retro Rewind", onClick = {}, enabled = false)
}

// ── SaveInfo screen previews ──

@Preview(showBackground = true, widthDp = 600, heightDp = 400)
@Composable
private fun SaveInfoScreenLoadingPreview() {
    SaveInfoScreen(saveInfoState = SaveInfoState.Loading, selectedSlotIndex = 0, cachedLeaderboardVrs = emptyMap(), onSelectSlot = {}, onRefresh = {}, onClose = {})
}

@Preview(showBackground = true, widthDp = 600, heightDp = 400)
@Composable
private fun SaveInfoScreenErrorPreview() {
    SaveInfoScreen(saveInfoState = SaveInfoState.Error("Failed to load save data"), selectedSlotIndex = 0, cachedLeaderboardVrs = emptyMap(), onSelectSlot = {}, onRefresh = {}, onClose = {})
}

@Preview(showBackground = true, widthDp = 600, heightDp = 500)
@Composable
private fun SaveInfoScreenSuccessPreview() {
    SaveInfoScreen(saveInfoState = SaveInfoState.Success(SaveFileInfo(sampleLicenses)), selectedSlotIndex = 0, cachedLeaderboardVrs = emptyMap(), onSelectSlot = {}, onRefresh = {}, onClose = {})
}

// ── Room detail previews ──

@Preview(showBackground = true, widthDp = 400, heightDp = 500)
@Composable
private fun RoomDetailPreview() {
    RoomDetail(room = sampleRooms[0])
}

// ── TopBar previews ──

@Preview(showBackground = true, widthDp = 800)
@Composable
private fun TopBarPreview() {
    TopBar(
        onOpenSettings = {},
        onLaunchMiiMaker = {},
        miiMakerEnabled = true,
        onOpenOnlineMenu = {},
        onlineMenuEnabled = true,
        onOpenSaveInfo = {}
    )
}
