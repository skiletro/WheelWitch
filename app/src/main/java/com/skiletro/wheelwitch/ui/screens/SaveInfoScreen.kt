package com.skiletro.wheelwitch.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.components.MiiFace
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.components.focusBorder
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import com.skiletro.wheelwitch.viewmodel.SaveInfoState

private val cardShape = RoundedCornerShape(14.dp)

@Composable
fun SaveInfoScreen(
    saveInfoState: SaveInfoState,
    selectedSlotIndex: Int,
    onSelectSlot: (Int) -> Unit,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ScreenHeader(
            title = stringResource(R.string.save_info_title),
            onBack = onClose,
            onRefresh = onRefresh
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            when (saveInfoState) {
                is SaveInfoState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is SaveInfoState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = saveInfoState.message,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        PrimaryActionButton(
                            text = stringResource(R.string.save_info_try_again),
                            onClick = onRefresh
                        )
                    }
                }
                is SaveInfoState.Idle -> {}
                is SaveInfoState.Success -> {
                    val saveFileInfo = saveInfoState.info
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in 0..3 step 2) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val left = saveFileInfo.licenses.getOrNull(i)
                                val right = saveFileInfo.licenses.getOrNull(i + 1)
                                LicenseCard(
                                    license = left,
                                    isSelected = left?.slotIndex == selectedSlotIndex,
                                    onSelect = { left?.let { onSelectSlot(it.slotIndex) } },
                                    modifier = Modifier.weight(1f)
                                )
                                LicenseCard(
                                    license = right,
                                    isSelected = right?.slotIndex == selectedSlotIndex,
                                    onSelect = { right?.let { onSelectSlot(it.slotIndex) } },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseCard(
    license: LicenseInfo?,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val info = license
    var isFocused by remember { mutableStateOf(false) }

    val backgroundColor = when {
        info?.exists != true -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val showBorder = isFocused || isSelected

    Surface(
        modifier = modifier
            .clickable(enabled = info?.exists == true) { onSelect() }
            .focusable()
            .onFocusChanged { isFocused = it.isFocused }
            .then(
                if (showBorder) Modifier.border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = cardShape
                ) else Modifier
            ),
        shape = cardShape,
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (info?.exists == true) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiiFace(
                        imageBase64 = info.leaderboard?.miiImageBase64,
                        miiDataBase64 = info.miiDataBase64,
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = info.miiName ?: stringResource(R.string.save_info_player_default),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            fontFamily = CtmkfFontFamily
                        )
                        info.friendCode?.let { fc ->
                            Text(
                                text = fc,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val displayVr = info.leaderboard?.vr ?: info.vr
                            StatLabel("VR", displayVr)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val rW = info.raceWins ?: 0
                        val rL = info.raceLosses ?: 0
                        Text(
                            text = stringResource(R.string.save_info_race_format, rW, rL),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(22.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = stringResource(R.string.home_active_license),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.save_info_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${value ?: 0}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
