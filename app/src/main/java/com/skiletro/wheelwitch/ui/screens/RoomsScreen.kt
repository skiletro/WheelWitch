package com.skiletro.wheelwitch.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.Room
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

private val sidebarShape = RoundedCornerShape(12.dp)
private val httpClient = OkHttpClient()

@Composable
fun RoomsScreen(
    rooms: List<Room>,
    isLoading: Boolean,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    val selectedRoom = selectedRoomId?.let { id -> rooms.find { it.id == id } }
    val listFocusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Online Rooms",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) {
                Text(
                    text = "\u21BB",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onClose) {
                Text(
                    text = "\u2190 Back",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = onRefresh) {
                            Text(text = "Try Again")
                        }
                    }
                }
                rooms.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No rooms found.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            TextButton(onClick = onRefresh) {
                                Text(text = "Refresh")
                            }
                        }
                    }
                }
                else -> {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .weight(0.35f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(rooms, key = { it.id }) { room ->
                                RoomListItem(
                                    room = room,
                                    isSelected = room.id == selectedRoomId,
                                    onClick = {
                                        selectedRoomId = room.id
                                        listFocusRequester.requestFocus()
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        VerticalDivider()

                        Box(
                            modifier = Modifier
                                .weight(0.65f)
                                .fillMaxHeight()
                        ) {
                            if (selectedRoom != null) {
                                RoomDetail(selectedRoom)
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Select a room",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty()) {
            listFocusRequester.requestFocus()
        }
    }
}

@Composable
private fun RoomListItem(
    room: Room,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = Modifier
            .height(IntrinsicSize.Min)
            .then(modifier)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent,
                    shape = RoundedCornerShape(topEnd = 0.dp, bottomEnd = 0.dp, topStart = 12.dp, bottomStart = 12.dp)
                )
        )
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = sidebarShape,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
                .focusable()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.players.firstOrNull()?.name ?: "Empty",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${room.players.size} player${if (room.players.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (room.isJoinable) {
                    Surface(
                        color = Color(0xFF66BB6A),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "Open",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VerticalDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.outlineVariant)
    )
}

@Composable
private fun RoomDetail(room: Room) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${room.players.size} player${if (room.players.size != 1) "s" else ""}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (room.isJoinable) {
                        Surface(
                            color = Color(0xFF66BB6A),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Joinable",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                if (room.trackName != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = room.trackName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Room: ${room.id}  |  Type: ${room.roomType}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Average VR: ${room.averageVR}  |  Visibility: ${if (room.isPublic) "Public" else "Private"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Players",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        items(room.players, key = { "${room.id}_${it.friendCode}" }) { player ->
            MiiPlayerCard(player = player)
        }
    }
}

@Composable
private fun MiiPlayerCard(player: com.skiletro.wheelwitch.model.Player) {
    val miiBitmap by produceState<Bitmap?>(initialValue = null, key1 = player.mii?.data) {
        player.mii?.data?.let { base64 ->
            try {
                withContext(Dispatchers.IO) {
                    val url = "https://mii-unsecure.ariankordi.net/miis/image.png?data=${
                        URLEncoder.encode(base64, "UTF-8")
                    }&width=96&type=face"
                    val request = Request.Builder().url(url).build()
                    val response = httpClient.newCall(request).execute()
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    var miiTimedOut by remember { mutableStateOf(false) }
    LaunchedEffect(player.mii?.data) {
        if (player.mii?.data != null) {
            delay(5000)
            miiTimedOut = true
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().focusable()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (player.mii?.data != null) {
                val bitmap = miiBitmap
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                } else if (!miiTimedOut) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 3.dp
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player.name,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (player.mii?.name != null && player.mii.name != player.name) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "\u201C${player.mii.name}\u201D",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (player.isOpenHost) {
                        Text(
                            text = "Host",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = "FC: ${player.friendCode}  |  VR: ${player.vr}  |  BR: ${player.br}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
