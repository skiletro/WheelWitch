package com.skiletro.wheelwitch.ui.screens

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.Player
import com.skiletro.wheelwitch.model.Room

private val samplePlayer = Player(name = "Jamie", friendCode = "1234-5678-9012", vr = 5000, br = 3000, isOpenHost = false, mii = null)
private val sampleHost = Player(name = "Hosty", friendCode = "9876-5432-1098", vr = 7200, br = 4000, isOpenHost = true, mii = null)
private val sampleRooms = listOf(
    Room(id = "1", type = "race", host = "Hosty", players = listOf(sampleHost, samplePlayer), averageVR = 6100, trackName = "GCN Peach Beach", roomType = "FFA", isPublic = true, isJoinable = true, isSuspended = false),
    Room(id = "2", type = "race", host = "Player2", players = listOf(Player(name = "Player2", friendCode = "1111-2222-3333", vr = 3000, br = 2500, isOpenHost = false, mii = null)), averageVR = 3000, trackName = null, roomType = "FFA", isPublic = true, isJoinable = false, isSuspended = false)
)

@Preview(showBackground = true, widthDp = 600, heightDp = 400)
@Composable
private fun RoomsScreenPreview() {
    RoomsScreen(rooms = sampleRooms, isLoading = false, errorMessage = null, onRefresh = {}, onClose = {})
}

@Preview(showBackground = true)
@Composable
private fun RoomsScreenLoadingPreview() {
    RoomsScreen(rooms = emptyList(), isLoading = true, errorMessage = null, onRefresh = {}, onClose = {})
}

@Preview(showBackground = true)
@Composable
private fun RoomsScreenErrorPreview() {
    RoomsScreen(rooms = emptyList(), isLoading = false, errorMessage = "Failed to connect", onRefresh = {}, onClose = {})
}

@Preview(showBackground = true)
@Composable
private fun RoomsScreenEmptyPreview() {
    RoomsScreen(rooms = emptyList(), isLoading = false, errorMessage = null, onRefresh = {}, onClose = {})
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
