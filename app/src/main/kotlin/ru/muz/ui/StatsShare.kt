package ru.muz.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.muz.download.TrackTransfer
import ru.muz.stats.PeriodStats
import ru.muz.theme.ColorMath

@Composable
internal fun StatsShareDialog(data: PeriodStats, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val layer = rememberGraphicsLayer()
    var busy by remember { mutableStateOf(false) }
    val accent = Accent

    fun export(share: Boolean) {
        if (busy) return
        busy = true
        scope.launch {
            runCatching {
                val bitmap = layer.toImageBitmap().asAndroidBitmap()
                val png = withContext(Dispatchers.Default) {
                    ByteArrayOutputStream().use { out ->
                        bitmap.copy(Bitmap.Config.ARGB_8888, false).compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.toByteArray()
                    }
                }
                val name = "Aria итоги ${data.period.title().lowercase()}.png"
                if (share) {
                    TrackTransfer.shareExport(context, name, "image/png", png, "Поделиться итогами").getOrThrow()
                } else {
                    val where = TrackTransfer.savePicture(context, name, png).getOrThrow()
                    Toast.makeText(context, "Сохранено: $where", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { Toast.makeText(context, "Не удалось сохранить картинку", Toast.LENGTH_SHORT).show() }
            busy = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Закрыть") }
                    Text("Итоги · ${data.period.title()}", fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                }
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    val density = LocalDensity.current
                    val fullW = with(density) { 1080.toDp() }
                    val fullH = with(density) { 1920.toDp() }
                    val scale = minOf(maxWidth / fullW, maxHeight / fullH)
                    val shape = RoundedCornerShape(18.dp)
                    Box(Modifier.size(fullW * scale, fullH * scale).shadow(12.dp, shape).clip(shape), contentAlignment = Alignment.Center) {
                        Box(
                            Modifier.requiredSize(fullW, fullH)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .drawWithContent {
                                    layer.record { this@drawWithContent.drawContent() }
                                    drawLayer(layer)
                                },
                        ) {
                            StatsCardContent(data, accent, fullW)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { export(false) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Сохранить")
                    }
                    Button(onClick = { export(true) }, enabled = !busy, modifier = Modifier.weight(1.3f)) {
                        Icon(Icons.Filled.Share, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Поделиться")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsCardContent(data: PeriodStats, accent: Color, w: Dp) {
    val density = LocalDensity.current
    fun Dp.sp(): TextUnit = with(density) { this@sp.toSp() }
    val deep = Color(ColorMath.blend(accent.toArgb(), 0xFF0B0B0D.toInt(), 0.78f))
    val ink = Color.White
    val soft = Color.White.copy(alpha = 0.7f)
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(accent.copy(alpha = 0.95f), deep, Color(0xFF09090B))))) {
        Column(Modifier.fillMaxSize().padding(horizontal = w * 0.08f, vertical = w * 0.1f)) {
            Text("МОЯ МУЗЫКА ${data.period.headline().uppercase()}", color = soft, fontSize = (w * 0.032f).sp(), fontWeight = FontWeight.Black, letterSpacing = (w * 0.004f).sp())
            Text(formatListening(data.listenedMs), color = ink, fontSize = (w * 0.13f).sp(), lineHeight = (w * 0.14f).sp(), fontWeight = FontWeight.Black, modifier = Modifier.padding(top = w * 0.02f))
            Text(
                "${data.uniqueTracks} треков · ${data.uniqueArtists} исполнителей · ${data.activeDays} дней с музыкой",
                color = soft,
                fontSize = (w * 0.034f).sp(),
            )
            Spacer(Modifier.height(w * 0.09f))
            if (data.topArtists.isNotEmpty()) {
                Text("Исполнители", color = ink, fontSize = (w * 0.05f).sp(), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(w * 0.03f))
                data.topArtists.take(3).forEachIndexed { i, artist ->
                    Row(Modifier.padding(vertical = w * 0.015f), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = accent, fontSize = (w * 0.06f).sp(), fontWeight = FontWeight.Black, modifier = Modifier.width(w * 0.08f))
                        Artwork(artist.coverUrl, Modifier.size(w * 0.13f).clip(CircleShape), w * 0.065f)
                        Column(Modifier.padding(start = w * 0.035f)) {
                            Text(artist.name, color = ink, fontSize = (w * 0.045f).sp(), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(formatListening(artist.listenedMs), color = soft, fontSize = (w * 0.032f).sp())
                        }
                    }
                }
                Spacer(Modifier.height(w * 0.07f))
            }
            if (data.topTracks.isNotEmpty()) {
                Text("Треки", color = ink, fontSize = (w * 0.05f).sp(), fontWeight = FontWeight.Black)
                Spacer(Modifier.height(w * 0.03f))
                data.topTracks.take(5).forEachIndexed { i, track ->
                    Row(Modifier.padding(vertical = w * 0.013f), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = accent, fontSize = (w * 0.05f).sp(), fontWeight = FontWeight.Black, modifier = Modifier.width(w * 0.08f))
                        Artwork(track.coverUrl, Modifier.size(w * 0.11f), w * 0.02f)
                        Column(Modifier.padding(start = w * 0.035f)) {
                            Text(track.title, color = ink, fontSize = (w * 0.042f).sp(), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(track.artist, color = soft, fontSize = (w * 0.032f).sp(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text("Aria", color = soft, fontSize = (w * 0.04f).sp(), fontWeight = FontWeight.Black)
        }
    }
}
