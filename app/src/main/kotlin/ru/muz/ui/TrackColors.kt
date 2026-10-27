package ru.muz.ui

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.muz.theme.ColorMath

internal object TrackColors {
    var cover by mutableStateOf<Color?>(null)
}

internal fun fallbackCoverTint(url: String?): Color {
    val hash = url.orEmpty().hashCode()
    val red = 0.34f + ((hash ushr 16) and 0x7f) / 255f
    val green = 0.34f + ((hash ushr 8) and 0x7f) / 255f
    val blue = 0.34f + (hash and 0x7f) / 255f
    return Color(red.coerceAtMost(0.82f), green.coerceAtMost(0.82f), blue.coerceAtMost(0.82f))
}

internal suspend fun coverTintOf(context: Context, url: String): Color = withContext(Dispatchers.IO) {
    val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(96).build()
    val result = context.imageLoader.execute(request) as? SuccessResult
    val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap ?: return@withContext fallbackCoverTint(url)
    val small = android.graphics.Bitmap.createScaledBitmap(bitmap, 32, 32, true)
    val pixels = IntArray(32 * 32)
    small.getPixels(pixels, 0, 32, 0, 0, 32, 32)
    if (small !== bitmap) small.recycle()
    ColorMath.dominantVibrant(pixels)?.let { Color(it) } ?: fallbackCoverTint(url)
}
