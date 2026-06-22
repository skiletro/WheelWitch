package com.skiletro.wheelwitch.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.skiletro.wheelwitch.R
import com.skiletro.wheelwitch.util.mii.MII_IMAGE_BASE_URL
import com.skiletro.wheelwitch.util.mii.MiiFaceCache
import com.skiletro.wheelwitch.util.net.HttpClientProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import timber.log.Timber
import java.net.URLEncoder
import java.util.Base64

/** Corner radius applied to rendered Mii face bitmaps. */
private val MiiFaceCorner = 10.dp

/** Default Mii RFL data shown when the real Mii fetch fails. */
private const val DEFAULT_MII_DATA_BASE64 =
  "AwAIMK89McaWg1Qak3k3LDSvLMdoMwAAIgBNAGkAaQAAAAAAAAAAAAAAAAAAAEBAAAAhAQJoRBgmNEYUgRIXaA0AACkAUkhQVAAgAEIAbwBtAGIAZQByAAAAAAAAACHC"

/**
 * Renders a Mii face from either an already-rendered PNG (base64) or raw
 * Mii data that needs to be fetched from the configured image service and
 * cached in [MiiFaceCache].
 *
 * Resolution order:
 * - If [imageBase64] is non-null, it is decoded immediately and no network
 *   call is made.
 * - Otherwise, if [miiDataBase64] is non-null, the cached bitmap (if any) is
 *   shown, or a network fetch is performed and the result is shown.
 * - While a fetch is in flight, a `CircularProgressIndicator` is displayed.
 * - If the real Mii fetch fails (network error, decode error, etc.), a
 *   fallback is attempted by fetching the default Mii identified by
 *   [DEFAULT_MII_DATA_BASE64] through the same endpoint. The default Mii's
 *   bitmap is cached so subsequent calls hit the cache.
 * - If even the default Mii fetch fails, a face icon (`ic_face_up`) is
 *   shown as a static placeholder instead of an infinite spinner.
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
            } catch (e: Exception) {
                Timber.tag("MiiFace").w(e, "Failed to decode inline Mii PNG")
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
    var fetchFailed by remember { mutableStateOf(false) }

    LaunchedEffect(miiDataBase64) {
        val data = miiDataBase64 ?: return@LaunchedEffect
        miiBitmap = null
        fetchFailed = false

        val cached = withContext(Dispatchers.IO) { MiiFaceCache.getAndTouch(data) }
        if (cached != null) {
            miiBitmap = cached
            return@LaunchedEffect
        }

        val fetched = fetchMiiBitmap(data)
        if (fetched != null) {
            miiBitmap = fetched
            MiiFaceCache.put(data, fetched)
            return@LaunchedEffect
        }

        val defaultCached =
            withContext(Dispatchers.IO) { MiiFaceCache.getAndTouch(DEFAULT_MII_DATA_BASE64) }
        val default =
            defaultCached
                ?: fetchMiiBitmap(DEFAULT_MII_DATA_BASE64)
                    ?.also { MiiFaceCache.put(DEFAULT_MII_DATA_BASE64, it) }
        if (default != null) {
            miiBitmap = default
            return@LaunchedEffect
        }

        fetchFailed = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val bitmap = miiBitmap
        when {
            bitmap != null -> {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(MiiFaceCorner))
                )
            }
            fetchFailed && miiDataBase64 != null -> {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_face_up),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
            miiDataBase64 != null -> {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        }
    }
}

/**
 * Fetches a Mii face bitmap from [MII_IMAGE_BASE_URL] for the given RFL
 * [data]. Returns null on any failure (network, decode, non-2xx response
 * with non-image body). Safe to call from any dispatcher.
 */
private suspend fun fetchMiiBitmap(data: String): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            val url =
                "$MII_IMAGE_BASE_URL?data=${URLEncoder.encode(data, "UTF-8")}" +
                    "&width=300&type=face&shaderType=switch"
            val request = Request.Builder().url(url).build()
            HttpClientProvider.client.newCall(request).execute().use { response ->
                val bytes = response.body?.bytes()
                if (bytes != null) BitmapFactory.decodeByteArray(bytes, 0, bytes.size) else null
            }
        } catch (e: Exception) {
            Timber.tag("MiiFace").w(e, "Mii fetch failed")
            null
        }
    }
