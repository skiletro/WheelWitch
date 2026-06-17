package com.skiletro.wheelwitch.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.Room
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.components.statusColors
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import com.skiletro.wheelwitch.viewmodel.RoomsState

private val sidebarShape = RoundedCornerShape(12.dp)

@Composable
fun RoomsScreen(
    roomsState: RoomsState,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    val rooms = (roomsState as? RoomsState.Success)?.rooms ?: emptyList()
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    val selectedRoom = selectedRoomId?.let { id -> rooms.find { it.id == id } }
    val listFocusRequester = remember { FocusRequester() }
    var hasRequestedFocus by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = stringResource(R.string.rooms_title),
            onBack = onClose,
            onRefresh = onRefresh
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when (roomsState) {
                is RoomsState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is RoomsState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = roomsState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryActionButton(
                            text = stringResource(R.string.rooms_try_again),
                            onClick = onRefresh
                        )
                    }
                }
                is RoomsState.Success -> {
                    if (rooms.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = stringResource(R.string.rooms_no_rooms),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                PrimaryActionButton(
                                    text = stringResource(R.string.rooms_refresh),
                                    onClick = onRefresh
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(0.35f)
                                    .fillMaxHeight()
                                    .focusRequester(listFocusRequester),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(rooms, key = { it.id }) { room ->
                                    RoomListItem(
                                        room = room,
                                        isSelected = room.id == selectedRoomId,
                                        onClick = {
                                            selectedRoomId = room.id
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
                                            text = stringResource(R.string.rooms_select_a_room),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                is RoomsState.Idle -> {}
            }
        }
    }

    LaunchedEffect(rooms) {
        if (rooms.isNotEmpty() && !hasRequestedFocus) {
            listFocusRequester.requestFocus()
            hasRequestedFocus = true
        }
    }
}

@Composable
fun RoomListItem(
    room: Room,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
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
                .onFocusChanged { isFocused = it.isFocused }
                .focusBorder(isFocused, shape = sidebarShape)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = room.players.firstOrNull()?.name ?: stringResource(R.string.rooms_empty),
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontFamily = CtmkfFontFamily
                    )
                    Text(
                        text = stringResource(R.string.rooms_player_count_format, room.players.size, if (room.players.size == 1) "" else "s"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (room.isJoinable) {
                    Surface(
                        color = statusColors().ok,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.rooms_open),
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
