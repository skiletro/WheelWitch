package com.skiletro.wheelwitch.ui.screens


import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.components.FocusableSurface
import com.skiletro.wheelwitch.ui.components.MiiFace
import com.skiletro.wheelwitch.ui.components.PrimaryActionButton
import com.skiletro.wheelwitch.ui.components.ScreenHeader
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import com.skiletro.wheelwitch.viewmodel.SaveInfoState

private val cardShape = RoundedCornerShape(14.dp)

@Composable
fun SaveInfoScreen(
    saveInfoState: SaveInfoState,
    selectedSlotIndex: Int,
    cachedLeaderboardVrs: Map<Int, Int>,
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
                            text = stringResource(R.string.action_retry),
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
                        // Render licenses in pairs (2x2 grid).
                        saveFileInfo.licenses.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val left = pair.getOrNull(0)
                                val right = pair.getOrNull(1)
                                LicenseCard(
                                    license = left,
                                    isSelected = left?.slotIndex == selectedSlotIndex,
                                    cachedLeaderboardVr = left?.slotIndex?.let { cachedLeaderboardVrs[it] },
                                    onSelect = { left?.let { onSelectSlot(it.slotIndex) } },
                                    modifier = Modifier.weight(1f)
                                )
                                LicenseCard(
                                    license = right,
                                    isSelected = right?.slotIndex == selectedSlotIndex,
                                    cachedLeaderboardVr = right?.slotIndex?.let { cachedLeaderboardVrs[it] },
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
    cachedLeaderboardVr: Int?,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        license?.exists != true -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    FocusableSurface(
        modifier = modifier,
        onClick = onSelect,
        enabled = license?.exists == true,
        selected = isSelected,
        shape = cardShape,
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (license?.exists == true) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiiFace(
                        imageBase64 = license.leaderboardMiiImageBase64,
                        miiDataBase64 = license.miiDataBase64,
                        modifier = Modifier.size(84.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = license.miiName ?: stringResource(R.string.player_default),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            fontFamily = CtmkfFontFamily
                        )
                        license.friendCode?.let { fc ->
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
                            val displayVr =
                                license.leaderboardVr ?: cachedLeaderboardVr ?: license.vr
                            StatLabel(stringResource(R.string.leaderboard_vr_label), displayVr)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        val raceWins = license.raceWins ?: 0
                        val raceLosses = license.raceLosses ?: 0
                        Text(
                            text = stringResource(
                                R.string.save_info_race_format,
                                raceWins,
                                raceLosses
                            ),
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
                            imageVector = ImageVector.vectorResource(R.drawable.ic_done),
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
                        text = stringResource(R.string.status_empty),
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
