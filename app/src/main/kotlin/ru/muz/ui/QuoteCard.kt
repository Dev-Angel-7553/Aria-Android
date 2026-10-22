package ru.muz.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.muz.download.TrackTransfer
import ru.muz.theme.ColorMath

internal enum class QuoteStyle(val title: String) {
    COVER("Обложка"),
    GRADIENT("Градиент"),
    ACCENT("Акцент"),
    DARK("Тёмный"),
    LIGHT("Светлый"),
}

internal enum class QuoteFormat(val title: String, val ratio: Float) {
    STORY("Сторис", 9f / 16f),
    POST("Пост", 4f / 5f),
    SQUARE("Квадрат", 1f),
}

@Composable
internal fun rememberCoverColor(url: String?): Color {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(Color(0xFF3A3A40)) }
    LaunchedEffect(url) {
        if (url.isNullOrBlank()) return@LaunchedEffect
        color = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(96).build()
            val bitmap = ((context.imageLoader.execute(request) as? SuccessResult)?.drawable as? BitmapDrawable)?.bitmap
                ?: return@withContext Color(0xFF3A3A40)
            val small = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
            val pixels = IntArray(32 * 32)
            small.getPixels(pixels, 0, 32, 0, 0, 32, 32)
            if (small !== bitmap) small.recycle()
            ColorMath.dominantVibrant(pixels)?.let { Color(it) } ?: Color(0xFF3A3A40)
        }
    }
    return color
}

@Composable
internal fun QuoteEditor(
    lines: List<String>,
    initial: Int,
    title: String,
    artist: String,
    coverUrl: String?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val usable = remember(lines) { lines.mapIndexedNotNull { index, text -> if (text.isBlank()) null else index } }
    var selected by remember(lines, initial) {
        val start = usable.firstOrNull { it >= initial } ?: usable.firstOrNull()
        val next = usable.firstOrNull { start != null && it > start }
        mutableStateOf(listOfNotNull(start, next).toSet())
    }
    var style by remember { mutableStateOf(QuoteStyle.COVER) }
    var format by remember { mutableStateOf(QuoteFormat.STORY) }
    var textScale by remember { mutableStateOf(1f) }
    var centered by remember { mutableStateOf(false) }
    var showTrack by remember { mutableStateOf(true) }
    var showBrand by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    val coverColor = rememberCoverColor(coverUrl)
    val layer = rememberGraphicsLayer()
    val quote = selected.sorted().map { lines[it] }

    suspend fun renderPng(): ByteArray {
        val bitmap = layer.toImageBitmap().asAndroidBitmap()
        return withContext(Dispatchers.Default) {
            ByteArrayOutputStream().use { out ->
                bitmap.copy(Bitmap.Config.ARGB_8888, false).compress(Bitmap.CompressFormat.PNG, 100, out)
                out.toByteArray()
            }
        }
    }

    fun export(share: Boolean) {
        if (busy || quote.isEmpty()) return
        busy = true
        scope.launch {
            runCatching {
                val png = renderPng()
                val name = "Aria ${artist.take(40)} — ${title.take(40)}.png"
                if (share) {
                    TrackTransfer.shareExport(context, name, "image/png", png, "Поделиться карточкой").getOrThrow()
                } else {
                    val where = TrackTransfer.savePicture(context, name, png).getOrThrow()
                    android.widget.Toast.makeText(context, "Сохранено: $where", android.widget.Toast.LENGTH_SHORT).show()
                }
            }.onFailure {
                android.widget.Toast.makeText(context, "Не удалось сохранить карточку", android.widget.Toast.LENGTH_SHORT).show()
            }
            busy = false
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Закрыть") }
                    Text("Карточка с цитатой", fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                }
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.fillMaxHeight().aspectRatio(format.ratio, matchHeightConstraintsFirst = true)
                            .clip(RoundedCornerShape(18.dp))
                            .drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                    ) {
                        QuoteCardContent(quote, title, artist, coverUrl, coverColor, style, textScale, centered, showTrack, showBrand)
                    }
                }
                Column(
                    Modifier.fillMaxWidth().height(300.dp).verticalScroll(rememberScrollState()).padding(bottom = 8.dp),
                ) {
                    ChipRow(QuoteStyle.entries, style, { it.title }) { style = it }
                    ChipRow(QuoteFormat.entries, format, { it.title }) { format = it }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Размер текста", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        IconButton(onClick = { centered = !centered }) {
                            Icon(if (centered) Icons.Filled.FormatAlignCenter else Icons.Filled.FormatAlignLeft, "Выравнивание")
                        }
                    }
                    Slider(
                        value = textScale,
                        onValueChange = { textScale = it },
                        valueRange = 0.6f..1.5f,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    QuoteSwitch("Название и обложка трека", showTrack) { showTrack = it }
                    QuoteSwitch("Подпись Aria", showBrand) { showBrand = it }
                    Text(
                        "Строки · выбрано ${selected.size} из 8",
                        fontWeight = FontWeight.Bold,
                        color = Accent,
                        modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                    )
                    usable.forEach { index ->
                        val checked = index in selected
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                selected = when {
                                    checked && selected.size > 1 -> selected - index
                                    !checked && selected.size < 8 -> selected + index
                                    else -> selected
                                }
                            }.padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (checked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                null,
                                tint = if (checked) Accent else SoftText,
                            )
                            Text(lines[index], modifier = Modifier.padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(onClick = { export(false) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Save, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сохранить")
                    }
                    Button(onClick = { export(true) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.Share, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Поделиться")
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> ChipRow(values: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(values) { value ->
            FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label(value)) })
        }
    }
}

@Composable
private fun QuoteSwitch(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun QuoteCardContent(
    quote: List<String>,
    title: String,
    artist: String,
    coverUrl: String?,
    coverColor: Color,
    style: QuoteStyle,
    textScale: Float,
    centered: Boolean,
    showTrack: Boolean,
    showBrand: Boolean,
) {
    val accent = Accent
    val deep = Color(ColorMath.blend(coverColor.toArgb(), 0xFF0B0B0D.toInt(), 0.7f))
    val ink = if (style == QuoteStyle.LIGHT) Color(0xFF16130A) else Color.White
    val inkSoft = ink.copy(alpha = 0.72f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = maxWidth
        when (style) {
            QuoteStyle.COVER -> {
                Box(Modifier.fillMaxSize().background(deep))
                if (!coverUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(coverUrl).allowHardware(false).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().then(
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(width * 0.09f) else Modifier,
                        ),
                    )
                }
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.6f)))))
            }
            QuoteStyle.GRADIENT -> Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(coverColor, deep))),
            )
            QuoteStyle.ACCENT -> Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(accent, Color(ColorMath.blend(accent.toArgb(), 0xFF000000.toInt(), 0.55f)))),
                ),
            )
            QuoteStyle.DARK -> Box(Modifier.fillMaxSize().background(Color(0xFF141416)))
            QuoteStyle.LIGHT -> Box(Modifier.fillMaxSize().background(Color(0xFFF6F3EC)))
        }
        val baseSize = when {
            quote.size <= 1 -> 0.105f
            quote.size == 2 -> 0.095f
            quote.size <= 4 -> 0.08f
            else -> 0.066f
        }
        val density = LocalDensity.current
        val fontSize = with(density) { (width * baseSize * textScale).toSp() }
        val small = with(density) { (width * 0.038f).toSp() }
        val padding = width * 0.08f
        Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                "“",
                color = if (style == QuoteStyle.LIGHT) accent else ink.copy(alpha = 0.5f),
                fontSize = with(density) { (width * 0.2f).toSp() },
                fontWeight = FontWeight.Black,
                modifier = Modifier.height(width * 0.14f),
            )
            Column(Modifier.fillMaxWidth()) {
                quote.forEach { line ->
                    Text(
                        line,
                        color = ink,
                        fontSize = fontSize,
                        lineHeight = fontSize * 1.18f,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (centered) TextAlign.Center else TextAlign.Start,
                        modifier = Modifier.fillMaxWidth().padding(vertical = width * 0.008f),
                    )
                }
            }
            Column(Modifier.fillMaxWidth()) {
                if (showTrack) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(coverUrl).allowHardware(false).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(width * 0.13f).clip(RoundedCornerShape(width * 0.025f)),
                            )
                            Spacer(Modifier.width(width * 0.035f))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(title, color = ink, fontSize = small * 1.2f, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(artist, color = inkSoft, fontSize = small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (showBrand) {
                    Text(
                        "Aria",
                        color = inkSoft,
                        fontSize = small,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().padding(top = width * 0.03f),
                    )
                }
            }
        }
    }
}
