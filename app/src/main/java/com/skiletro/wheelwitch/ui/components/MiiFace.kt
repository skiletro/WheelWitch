package com.skiletro.wheelwitch.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.util.HttpClientProvider
import com.skiletro.wheelwitch.util.MII_IMAGE_BASE_URL
import com.skiletro.wheelwitch.util.MiiFaceCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.net.URLEncoder
import java.util.Base64

/** Pixel width requested from the Mii image service; matches typical display size. */
private const val MII_FACE_FETCH_WIDTH = 96

/** Corner radius applied to rendered Mii face bitmaps. */
private val MiiFaceCorner = 10.dp

/**
 * Renders a Mii face from either an already-rendered PNG (base64) or raw
 * Mii data that needs to be fetched from the configured image service and
 * cached in [MiiFaceCache].
 *
 * - If [imageBase64] is non-null, it is decoded immediately and no network
 *   call is made.
 * - Otherwise, if [miiDataBase64] is non-null, the cached bitmap (if any) is
 *   shown, or a network fetch is performed and the result is shown.
 * - While a fetch is in flight, a `CircularProgressIndicator` is displayed.
 * - If neither input is non-null, nothing is shown (the modifier still takes
 *   the requested space).
 */
@Composable
fun MiiFace(
    imageBase64: String?,
    miiDataBase64: String?,
    modifier: Modifier = Modifier
) {
    val pngBitmap = remember(imageBase64) {
        imageBase64?.let { b64 ->
            try {
                val bytes = Base64.getDecoder().decode(b64)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            } catch (_: Exception) {
                null
            }
        }
    }

    if (pngBitmap != null) {
        Image(
            bitmap = pngBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(MiiFaceCorner))
        )
        return
    }

    var miiBitmap by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(miiDataBase64) {
        val data = miiDataBase64 ?: return@LaunchedEffect
        val cached = withContext(Dispatchers.IO) { MiiFaceCache.getAndTouch(data) }
        if (cached != null) {
            miiBitmap = cached
        } else {
            val fetched = withContext(Dispatchers.IO) {
                val url = "$MII_IMAGE_BASE_URL?data=${
                    URLEncoder.encode(data, "UTF-8")
                }&width=$MII_FACE_FETCH_WIDTH&type=face"
                val request = Request.Builder().url(url).build()
                HttpClientProvider.client.newCall(request).execute().use { response ->
                    val bytes = response.body?.bytes()
                    if (bytes != null) {
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            MiiFaceCache.put(data, bitmap)
                        }
                        bitmap
                    } else {
                        null
                    }
                }
            }
            miiBitmap = fetched
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = miiBitmap
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(MiiFaceCorner))
            )
        } else if (miiDataBase64 != null) {
            CircularProgressIndicator(strokeWidth = 3.dp)
        }
    }
}
