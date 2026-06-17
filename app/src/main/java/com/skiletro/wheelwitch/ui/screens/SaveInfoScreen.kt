package com.skiletro.wheelwitch.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.model.LicenseInfo
import com.skiletro.wheelwitch.ui.theme.CtmkfFontFamily
import com.skiletro.wheelwitch.util.HttpClientProvider
import com.skiletro.wheelwitch.viewmodel.SaveInfoState
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request

private val httpClient get() = HttpClientProvider.client
private val cardShape = RoundedCornerShape(14.dp)
private val miiUrlBase = "https://mii-unsecure.ariankordi.net/miis/image.png"

@Composable
fun SaveInfoScreen(
    saveInfoState: SaveInfoState,
    onRefresh: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler(onBack = onClose)

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
                text = "Save Data",
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
                        TextButton(onClick = onRefresh) {
                            Text(text = "Try Again")
                        }
                    }
                }
                is SaveInfoState.Idle -> {}
                is SaveInfoState.Success -> {
                    val saveFileInfo = saveInfoState.info
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in 0..3 step 2) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val left = saveFileInfo.licenses.getOrNull(i)
                                val right = saveFileInfo.licenses.getOrNull(i + 1)
                                LicenseCard(
                                    license = left,
                                    modifier = Modifier.weight(1f)
                                )
                                LicenseCard(
                                    license = right,
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
    modifier: Modifier = Modifier
) {
    val info = license
    Surface(
        modifier = modifier.height(200.dp),
        shape = cardShape,
        color = if (info?.exists == true) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
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
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = info.miiName ?: "Player",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        fontFamily = CtmkfFontFamily
                    )
                    info.friendCode?.let { fc ->
                        Text(
                            text = fc,
                            style = MaterialTheme.typography.bodySmall,
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
                        StatLabel("BR", info.br)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    val rW = info.raceWins ?: 0
                    val rL = info.raceLosses ?: 0
                    Text(
                        text = "Race: W $rW / L $rL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val bW = info.battleWins ?: 0
                    val bL = info.battleLosses ?: 0
                    Text(
                        text = "Battle: W $bW / L $bL",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Empty",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun StatLabel(label: String, value: Int?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${value ?: 0}",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun MiiFace(
    imageBase64: String?,
    miiDataBase64: String?,
    modifier: Modifier = Modifier
) {
    val pngBitmap = remember(imageBase64) {
        imageBase64?.let { b64 ->
            try {
                val bytes = Base64.decode(b64, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) { null }
        }
    }

    if (pngBitmap != null) {
        Image(
            bitmap = pngBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .clip(RoundedCornerShape(10.dp))
        )
        return
    }

    var miiBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(miiDataBase64) {
        if (miiDataBase64 != null) {
            withContext(Dispatchers.IO) {
                val url = "$miiUrlBase?data=${URLEncoder.encode(miiDataBase64, "UTF-8")}&width=96&type=face"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                val bytes = response.body?.bytes()
                if (bytes != null) {
                    miiBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val bitmap = miiBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            )
        } else if (miiDataBase64 != null) {
            CircularProgressIndicator(
                strokeWidth = 3.dp
            )
        }
    }
}
