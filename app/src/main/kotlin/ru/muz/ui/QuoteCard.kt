package ru.muz.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FormatColorFill
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import kotlin.math.roundToInt
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FormatAlignCenter
import androidx.compose.material.icons.rounded.FormatAlignLeft
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.FormatQuote
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertLink
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.muz.download.QuoteClip
import ru.muz.download.QuoteVideo
import ru.muz.download.TrackTransfer
import ru.muz.lyrics.LyricLine
import ru.muz.theme.ColorMath

internal enum class QuoteStyle(val title: String) {
    COVER("Размытие"),
    ALBUM("Обложка"),
    SPLIT("Половина"),
    POLAROID("Полароид"),
    NEON("Неон"),
    GRADIENT("Градиент"),
    ACCENT("Акцент"),
    DARK("Тёмный"),
    LIGHT("Светлый"),
}

internal enum class QuoteMotion(val title: String, val icon: ImageVector) {
    FILL("Заливка по строкам", Icons.Rounded.FormatColorFill),
    HIGHLIGHT("Подсветка строк", Icons.Rounded.Lyrics),
    REVEAL("Появление строк", Icons.Rounded.AutoAwesome),
    STATIC("Без анимации", Icons.Rounded.Image),
}

internal data class LineFrame(val alpha: List<Float>, val fill: List<Float>?)

private fun quantize(value: Float, steps: Int = 32): Float = (value.coerceIn(0f, 1f) * steps).roundToInt() / steps.toFloat()

internal fun lineFrame(motion: QuoteMotion, lines: List<LyricLine>, lyricMs: Long): LineFrame? = when (motion) {
    QuoteMotion.STATIC -> null
    QuoteMotion.FILL -> LineFrame(List(lines.size) { 1f }, lines.map { quantize(it.progressAt(lyricMs)) })
    QuoteMotion.HIGHLIGHT -> {
        val active = lines.indexOfLast { it.startMs <= lyricMs }.coerceAtLeast(0)
        LineFrame(lines.indices.map { if (it == active) 1f else if (it < active) 0.55f else 0.32f }, null)
    }
    QuoteMotion.REVEAL -> LineFrame(lines.map { quantize((lyricMs - it.startMs + 120) / 420f, 12) }, null)
}

internal enum class QuoteFormat(val title: String, val heightPx: Int) {
    STORY("Сторис", 1920),
    POST("Пост", 1350),
    SQUARE("Квадрат", 1080),
    ;

    val ratio: Float get() = CARD_WIDTH_PX.toFloat() / heightPx
}

internal data class QuoteMusic(
    val link: String? = null,
    val resolveAudio: (suspend () -> Result<String>)? = null,
    val durationMs: Long = 0,
    val positionMs: Long = 0,
    val offsetMs: Int = 0,
    val synced: Boolean = false,
)

private const val CARD_WIDTH_PX = 1080
private const val FRAME_STEP_MS = 100L
private const val RENDER_SHARE = 45
private const val MAX_LINES = 8

private enum class QuoteTab(val title: String, val icon: ImageVector) {
    STYLE("Фон", Icons.Rounded.Palette),
    FORMAT("Формат", Icons.Rounded.AspectRatio),
    TEXT("Текст", Icons.Rounded.TextFields),
    LINES("Строки", Icons.Rounded.FormatListBulleted),
    MUSIC("Музыка", Icons.Rounded.MusicNote),
}

private enum class QuoteOutput { IMAGE, VIDEO }

private enum class ClipMode(val title: String, val lengthMs: Long?) {
    LINES("Под строки", null),
    SHORT("15 с", 15_000),
    LONG("30 с", 30_000),
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

private data class CardLook(
    val style: QuoteStyle,
    val textScale: Float,
    val centered: Boolean,
    val bold: Boolean,
    val quoteMark: Boolean,
    val showTrack: Boolean,
    val showBrand: Boolean,
)

@Composable
internal fun QuoteEditor(
    lines: List<LyricLine>,
    initial: Int,
    title: String,
    artist: String,
    coverUrl: String?,
    music: QuoteMusic = QuoteMusic(),
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val usable = remember(lines) { lines.mapIndexedNotNull { index, line -> if (line.text.isBlank()) null else index } }
    var selected by remember(lines, initial) {
        val start = usable.firstOrNull { it >= initial } ?: usable.firstOrNull()
        val next = usable.firstOrNull { start != null && it > start }
        mutableStateOf(listOfNotNull(start, next).toSet())
    }
    var tab by remember { mutableStateOf(QuoteTab.STYLE) }
    var format by remember { mutableStateOf(QuoteFormat.STORY) }
    var look by remember {
        mutableStateOf(CardLook(QuoteStyle.COVER, 1f, centered = false, bold = true, quoteMark = true, showTrack = true, showBrand = true))
    }
    val canVideo = music.resolveAudio != null
    var output by remember { mutableStateOf(QuoteOutput.IMAGE) }
    var clipMode by remember { mutableStateOf(if (music.synced) ClipMode.LINES else ClipMode.SHORT) }
    var attachLink by remember { mutableStateOf(!music.link.isNullOrBlank()) }
    var motion by remember { mutableStateOf(if (music.synced) QuoteMotion.FILL else QuoteMotion.STATIC) }
    var renderFrame by remember { mutableStateOf<LineFrame?>(null) }
    var previewFrame by remember { mutableStateOf<LineFrame?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(-1) }
    var job by remember { mutableStateOf<Job?>(null) }
    val coverColor = rememberCoverColor(coverUrl)
    val layer = rememberGraphicsLayer()
    val ordered = selected.sorted()
    val quote = ordered.map { lines[it].text }
    val clip = remember(ordered, clipMode, music) {
        val first = ordered.firstOrNull()?.let(lines::get)
        val last = ordered.lastOrNull()?.let(lines::get)
        if (music.synced && first != null && last != null) {
            QuoteClip.forLines(first.startMs, last.endMs, music.offsetMs, music.durationMs, clipMode.lengthMs)
        } else {
            QuoteClip.fit(music.positionMs, music.positionMs + (clipMode.lengthMs ?: 15_000), music.durationMs)
        }
    }
    val shareText = music.link?.takeIf { attachLink && it.isNotBlank() }?.let { "$artist — $title\n$it" }
    val selectedLines = ordered.map { lines[it] }
    val animatedVideo = output == QuoteOutput.VIDEO && music.synced && motion != QuoteMotion.STATIC
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(animatedVideo, motion, ordered, clip, busy, reduceMotion) {
        previewFrame = null
        if (!animatedVideo || busy) return@LaunchedEffect
        if (reduceMotion) {
            previewFrame = lineFrame(motion, selectedLines, clip.startMs + clip.lengthMs / 2 + music.offsetMs)
            return@LaunchedEffect
        }
        val start = androidx.compose.runtime.withFrameMillis { it }
        while (true) {
            val now = androidx.compose.runtime.withFrameMillis { it }
            val at = clip.startMs + (now - start) % clip.lengthMs.coerceAtLeast(1)
            previewFrame = lineFrame(motion, selectedLines, at + music.offsetMs)
        }
    }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_SHORT).show()

    suspend fun renderImage(format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG): ByteArray {
        val bitmap = layer.toImageBitmap().asAndroidBitmap()
        return withContext(Dispatchers.Default) {
            ByteArrayOutputStream().use { out ->
                bitmap.copy(Bitmap.Config.ARGB_8888, false).compress(format, if (format == Bitmap.CompressFormat.PNG) 100 else 92, out)
                out.toByteArray()
            }
        }
    }

    fun export(share: Boolean) {
        if (busy || quote.isEmpty()) return
        busy = true
        progress = -1
        job = scope.launch {
            val name = "Aria ${artist.take(40)} — ${title.take(40)}"
            try {
                val png = renderImage()
                if (output == QuoteOutput.IMAGE) {
                    if (share) {
                        TrackTransfer.shareExport(context, "$name.png", "image/png", png, "Поделиться карточкой", shareText).getOrThrow()
                    } else {
                        toast("Сохранено: ${TrackTransfer.savePicture(context, "$name.png", png).getOrThrow()}")
                    }
                } else {
                    progress = 0
                    val url = music.resolveAudio?.invoke()?.getOrNull() ?: error("Не удалось получить трек. Нужен интернет")
                    val plan = if (animatedVideo) {
                        QuoteClip.sample(clip, FRAME_STEP_MS) { song -> lineFrame(motion, selectedLines, song + music.offsetMs) }
                    } else emptyList()
                    val frames = if (plan.size > 1) {
                        try {
                            plan.mapIndexed { number, (frame, length) ->
                                renderFrame = frame
                                androidx.compose.runtime.withFrameNanos { }
                                androidx.compose.runtime.withFrameNanos { }
                                val bytes = renderImage(Bitmap.CompressFormat.JPEG)
                                progress = (number + 1) * RENDER_SHARE / plan.size
                                withContext(Dispatchers.IO) {
                                    TrackTransfer.sharedFile(context, "aria_quote_frame_$number.jpg").apply { writeBytes(bytes) }
                                } to length
                            }
                        } finally {
                            renderFrame = null
                        }
                    } else {
                        listOf(
                            withContext(Dispatchers.IO) {
                                TrackTransfer.sharedFile(context, "aria_quote_frame.png").apply { writeBytes(png) }
                            } to clip.lengthMs,
                        )
                    }
                    val base = if (plan.size > 1) RENDER_SHARE else 0
                    val video = try {
                        QuoteVideo.render(context, frames, url, clip, TrackTransfer.sharedFile(context, "$name.mp4")) { progress = base + it * (100 - base) / 100 }
                    } finally {
                        if (frames.size > 1) withContext(Dispatchers.IO) { frames.forEach { it.first.delete() } }
                    }
                    if (share) {
                        TrackTransfer.shareFile(context, video, "video/mp4", "Поделиться видео", shareText)
                    } else {
                        toast("Сохранено: ${TrackTransfer.saveVideo(context, video, "$name.mp4").getOrThrow()}")
                    }
                }
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Throwable) {
                toast(error.message?.takeIf { output == QuoteOutput.VIDEO } ?: "Не удалось сохранить карточку")
            } finally {
                busy = false
                progress = -1
                job = null
            }
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        DialogSystemBars()
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Закрыть") }
                    Column(Modifier.weight(1f)) {
                        Text("Карточка", fontSize = 18.sp, fontWeight = FontWeight.Black)
                        Text(
                            buildString {
                                append(format.title)
                                append(" · ")
                                append(if (output == QuoteOutput.VIDEO) "видео ${QuoteClip.label(clip.lengthMs)}" else "картинка")
                            },
                            color = SoftText,
                            fontSize = 12.sp,
                        )
                    }
                }
                BoxWithConstraints(
                    Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val density = LocalDensity.current
                    val fullW = with(density) { CARD_WIDTH_PX.toDp() }
                    val fullH = with(density) { format.heightPx.toDp() }
                    val scale = minOf(maxWidth / fullW, maxHeight / fullH)
                    val shape = RoundedCornerShape(18.dp)
                    Box(
                        Modifier.size(fullW * scale, fullH * scale).shadow(16.dp, shape).clip(shape),
                        contentAlignment = Alignment.Center,
                    ) {
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
                            QuoteCardContent(quote, title, artist, coverUrl, coverColor, look, renderFrame ?: previewFrame)
                        }
                        if (busy && progress >= 0) {
                            Column(
                                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).padding(24.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("Собираем видео", color = Color.White, fontWeight = FontWeight.Bold)
                                LinearProgressIndicator(
                                    progress = { progress.coerceIn(0, 100) / 100f },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                )
                                Text("${progress.coerceIn(0, 100)}%", color = Color.White.copy(alpha = 0.8f), fontSize = 13.sp)
                                TextButton(onClick = { job?.cancel() }) { Text("Отменить", color = Color.White) }
                            }
                        }
                    }
                }
                Column(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainer),
                ) {
                    QuoteTabs(tab) { tab = it }
                    Box(Modifier.fillMaxWidth().height(168.dp)) {
                        when (tab) {
                            QuoteTab.STYLE -> StylePicker(look.style, coverUrl, coverColor) { look = look.copy(style = it) }
                            QuoteTab.FORMAT -> FormatPicker(format) { format = it }
                            QuoteTab.TEXT -> TextOptions(look) { look = it }
                            QuoteTab.LINES -> LinePicker(lines, usable, selected, initial, music.synced) { selected = it }
                            QuoteTab.MUSIC -> MusicOptions(
                                output = output,
                                canVideo = canVideo,
                                clipMode = clipMode,
                                clip = clip,
                                synced = music.synced,
                                hasLink = !music.link.isNullOrBlank(),
                                attachLink = attachLink,
                                motion = motion,
                                onOutput = { output = it },
                                onClipMode = { clipMode = it },
                                onAttachLink = { attachLink = it },
                                onMotion = { motion = it },
                            )
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedButton(onClick = { export(false) }, enabled = !busy, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Сохранить", maxLines = 1)
                        }
                        Button(onClick = { export(true) }, enabled = !busy, modifier = Modifier.weight(1.3f)) {
                            Icon(if (output == QuoteOutput.VIDEO) Icons.Rounded.Movie else Icons.Rounded.Share, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (output == QuoteOutput.VIDEO) "Видео" else "Поделиться", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuoteTabs(selected: QuoteTab, onSelect: (QuoteTab) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
        QuoteTab.entries.forEach { item ->
            val active = item == selected
            Column(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (active) Accent.copy(alpha = 0.16f) else Color.Transparent)
                    .clickable { onSelect(item) }
                    .padding(vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(item.icon, null, tint = if (active) Accent else SoftText, modifier = Modifier.size(20.dp))
                Text(
                    item.title,
                    fontSize = 11.sp,
                    color = if (active) Accent else SoftText,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StylePicker(selected: QuoteStyle, coverUrl: String?, coverColor: Color, onSelect: (QuoteStyle) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(QuoteStyle.entries) { style ->
            val active = style == selected
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onSelect(style) }) {
                Box(
                    Modifier.width(64.dp).height(96.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(
                            BorderStroke(if (active) 2.5.dp else 1.dp, if (active) Accent else SoftText.copy(alpha = 0.25f)),
                            RoundedCornerShape(14.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    QuoteBackground(style, coverUrl, coverColor, 64.dp)
                    val withArt = !coverUrl.isNullOrBlank()
                    if (style == QuoteStyle.ALBUM && withArt) {
                        AsyncImage(
                            model = coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.align(Alignment.TopCenter).padding(top = 10.dp).size(30.dp).clip(RoundedCornerShape(5.dp)),
                        )
                    }
                    if (style == QuoteStyle.POLAROID && withArt) {
                        Box(
                            Modifier.align(Alignment.TopCenter).padding(top = 9.dp).graphicsLayer { rotationZ = -4f }
                                .background(Color.White).padding(start = 3.dp, end = 3.dp, top = 3.dp, bottom = 8.dp),
                        ) {
                            AsyncImage(model = coverUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(28.dp))
                        }
                    }
                    val lower = style == QuoteStyle.ALBUM || style == QuoteStyle.SPLIT || style == QuoteStyle.POLAROID
                    androidx.compose.material3.Text(
                        "Aa",
                        color = inkFor(style),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        style = if (style == QuoteStyle.NEON) LocalTextStyle.current.copy(
                            shadow = androidx.compose.ui.graphics.Shadow(neonOf(coverColor), Offset.Zero, 14f),
                        ) else LocalTextStyle.current,
                        modifier = if (lower) Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp) else Modifier,
                    )
                    if (active) {
                        Box(
                            Modifier.align(Alignment.TopEnd).padding(5.dp).size(18.dp).clip(CircleShape).background(Accent),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(13.dp))
                        }
                    }
                }
                Text(
                    style.title,
                    fontSize = 12.sp,
                    color = if (active) Accent else SoftText,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun FormatPicker(selected: QuoteFormat, onSelect: (QuoteFormat) -> Unit) {
    Row(
        Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuoteFormat.entries.forEach { format ->
            val active = format == selected
            Column(
                Modifier.weight(1f).fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) Accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable { onSelect(format) }
                    .padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier.fillMaxHeight(0.86f)
                            .aspectRatio(format.ratio, matchHeightConstraintsFirst = true)
                            .border(2.dp, if (active) Accent else SoftText.copy(alpha = 0.6f), RoundedCornerShape(6.dp)),
                    )
                }
                Text(format.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = if (active) Accent else MaterialTheme.colorScheme.onSurface)
                Text("1080×${format.heightPx}", fontSize = 11.sp, color = SoftText)
            }
        }
    }
}

@Composable
private fun TextOptions(look: CardLook, onChange: (CardLook) -> Unit) {
    Column(Modifier.fillMaxSize().padding(top = 6.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Размер", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            StyledSlider(
                value = look.textScale,
                onValueChange = { onChange(look.copy(textScale = it)) },
                valueRange = 0.6f..1.5f,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Text("${(look.textScale * 100).toInt()}%", color = SoftText, fontSize = 13.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
        }
        Text(
            "Текст сам уменьшится, если не влезает",
            color = SoftText,
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                OptionChip("Слева", Icons.Rounded.FormatAlignLeft, !look.centered) { onChange(look.copy(centered = false)) }
            }
            item {
                OptionChip("По центру", Icons.Rounded.FormatAlignCenter, look.centered) { onChange(look.copy(centered = true)) }
            }
            item { OptionChip("Жирный", Icons.Rounded.FormatBold, look.bold) { onChange(look.copy(bold = !look.bold)) } }
            item { OptionChip("Кавычки", Icons.Rounded.FormatQuote, look.quoteMark) { onChange(look.copy(quoteMark = !look.quoteMark)) } }
            item { OptionChip("Трек", Icons.Rounded.MusicNote, look.showTrack) { onChange(look.copy(showTrack = !look.showTrack)) } }
            item { OptionChip("Подпись Aria", null, look.showBrand) { onChange(look.copy(showBrand = !look.showBrand)) } }
        }
    }
}

@Composable
private fun OptionChip(label: String, icon: ImageVector?, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            when {
                selected -> Icon(Icons.Rounded.Check, null, modifier = Modifier.size(FilterChipDefaults.IconSize))
                icon != null -> Icon(icon, null, modifier = Modifier.size(FilterChipDefaults.IconSize))
            }
        },
    )
}

@Composable
private fun LinePicker(
    lines: List<LyricLine>,
    usable: List<Int>,
    selected: Set<Int>,
    initial: Int,
    synced: Boolean,
    onChange: (Set<Int>) -> Unit,
) {
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (usable.indexOfFirst { it >= initial } - 1).coerceAtLeast(0))
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Выбрано ${selected.size} из $MAX_LINES",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = Accent,
                modifier = Modifier.weight(1f),
            )
            if (selected.size > 1) {
                TextButton(onClick = { onChange(setOf(selected.min())) }) { Text("Оставить одну", fontSize = 13.sp) }
            }
        }
        LazyColumn(state = state, contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp)) {
            items(usable) { index ->
                val checked = index in selected
                Row(
                    Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (checked) Accent.copy(alpha = 0.14f) else Color.Transparent)
                        .clickable {
                            onChange(
                                when {
                                    checked && selected.size > 1 -> selected - index
                                    !checked && selected.size < MAX_LINES -> selected + index
                                    else -> selected
                                },
                            )
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (synced) {
                        Text(
                            QuoteClip.label(lines[index].startMs),
                            color = if (checked) Accent else SoftText,
                            fontSize = 12.sp,
                            modifier = Modifier.width(40.dp),
                        )
                    }
                    Text(
                        lines[index].text,
                        modifier = Modifier.weight(1f),
                        fontSize = 14.sp,
                        fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (checked) Icon(Icons.Rounded.Check, null, tint = Accent, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun MusicOptions(
    output: QuoteOutput,
    canVideo: Boolean,
    clipMode: ClipMode,
    clip: QuoteClip.Range,
    synced: Boolean,
    hasLink: Boolean,
    attachLink: Boolean,
    motion: QuoteMotion,
    onOutput: (QuoteOutput) -> Unit,
    onClipMode: (ClipMode) -> Unit,
    onAttachLink: (Boolean) -> Unit,
    onMotion: (QuoteMotion) -> Unit,
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutputCard("Картинка", "PNG, без звука", Icons.Rounded.Image, output == QuoteOutput.IMAGE, true, Modifier.weight(1f)) {
                onOutput(QuoteOutput.IMAGE)
            }
            OutputCard(
                "Видео с музыкой",
                if (canVideo) "MP4 · ${QuoteClip.label(clip.lengthMs)}" else "недоступно",
                Icons.Rounded.Movie,
                output == QuoteOutput.VIDEO,
                canVideo,
                Modifier.weight(1f),
            ) { onOutput(QuoteOutput.VIDEO) }
        }
        androidx.compose.animation.AnimatedVisibility(output == QuoteOutput.VIDEO && synced, enter = revealIn(), exit = revealOut()) {
            Column {
                Text("Текст в видео", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(QuoteMotion.entries) { item ->
                        OptionChip(item.title, item.icon, item == motion) { onMotion(item) }
                    }
                }
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (output == QuoteOutput.VIDEO) {
                items(ClipMode.entries.filter { synced || it.lengthMs != null }) { mode ->
                    FilterChip(selected = mode == clipMode, onClick = { onClipMode(mode) }, label = { Text(mode.title) })
                }
            }
            if (hasLink) {
                item { OptionChip("Ссылка на трек", Icons.Rounded.InsertLink, attachLink) { onAttachLink(!attachLink) } }
            }
        }
        Text(
            when {
                output == QuoteOutput.IMAGE && hasLink -> "Ссылка на трек уйдёт вместе с картинкой, если приложение её принимает"
                output == QuoteOutput.IMAGE -> "Выберите видео, чтобы к карточке приложился кусок песни"
                synced && motion == QuoteMotion.FILL -> "Звучит ${QuoteClip.label(clip.startMs)}–${QuoteClip.label(clip.endMs)}, строки заливаются по мере пения"
                synced && motion == QuoteMotion.HIGHLIGHT -> "Звучит ${QuoteClip.label(clip.startMs)}–${QuoteClip.label(clip.endMs)}, строки подсвечиваются в такт"
                synced && motion == QuoteMotion.REVEAL -> "Звучит ${QuoteClip.label(clip.startMs)}–${QuoteClip.label(clip.endMs)}, строки появляются, когда их поют"
                synced -> "Звучит ${QuoteClip.label(clip.startMs)}–${QuoteClip.label(clip.endMs)}, ровно на выбранных строках"
                else -> "У текста нет таймингов, фрагмент берётся с текущего места трека: ${QuoteClip.label(clip.startMs)}"
            },
            color = SoftText,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
private fun OutputCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) Accent.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(if (selected) 2.dp else 0.dp, if (selected) Accent else Color.Transparent, RoundedCornerShape(16.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (selected) Accent else SoftText.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 10.dp)) {
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
            )
            Text(subtitle, fontSize = 11.sp, color = SoftText, maxLines = 1)
        }
    }
}

private fun inkFor(style: QuoteStyle): Color =
    if (style == QuoteStyle.LIGHT || style == QuoteStyle.POLAROID) Color(0xFF16130A) else Color.White

private fun neonOf(coverColor: Color): Color {
    val hsl = ColorMath.hsl(coverColor.toArgb())
    return Color(ColorMath.fromHsl(hsl[0], 0.92f, 0.64f))
}

private const val SPLIT_FRACTION = 0.46f

@Composable
private fun QuoteBackground(style: QuoteStyle, coverUrl: String?, coverColor: Color, width: Dp) {
    val accent = Accent
    val deep = Color(ColorMath.blend(coverColor.toArgb(), 0xFF0B0B0D.toInt(), 0.72f))
    val toned = Color(ColorMath.blend(coverColor.toArgb(), 0xFF000000.toInt(), 0.18f))
    when (style) {
        QuoteStyle.COVER -> {
            Box(Modifier.fillMaxSize().background(deep))
            if (!coverUrl.isNullOrBlank()) {
                val blurNative = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .allowHardware(false)
                        .apply { if (!blurNative) size(24) }
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.15f; scaleY = 1.15f }
                        .then(if (blurNative) Modifier.blur(width * 0.07f) else Modifier),
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.28f), Color.Black.copy(alpha = 0.42f), Color.Black.copy(alpha = 0.72f))),
                ),
            )
        }
        QuoteStyle.ALBUM -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(toned, deep, Color(0xFF09090B)))))
        QuoteStyle.GRADIENT -> Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(toned, deep))))
        QuoteStyle.ACCENT -> Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(listOf(accent, Color(ColorMath.blend(accent.toArgb(), 0xFF000000.toInt(), 0.55f)))),
            ),
        )
        QuoteStyle.DARK -> Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF1C1C20), Color(0xFF0E0E10)))))
        QuoteStyle.LIGHT -> Box(Modifier.fillMaxSize().background(Color(0xFFF6F3EC)))
        QuoteStyle.POLAROID -> Box(
            Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFFF3EEE4), Color(0xFFE6DFD2)))),
        )
        QuoteStyle.NEON -> {
            val neon = neonOf(coverColor)
            val second = Color(ColorMath.fromHsl(ColorMath.hsl(neon.toArgb())[0] + 150f, 0.85f, 0.58f))
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                drawRect(Color(0xFF06060B))
                drawCircle(
                    Brush.radialGradient(listOf(neon.copy(alpha = 0.42f), Color.Transparent), Offset(size.width * 0.12f, size.height * 0.16f), size.maxDimension * 0.55f),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.12f, size.height * 0.16f),
                )
                drawCircle(
                    Brush.radialGradient(listOf(second.copy(alpha = 0.28f), Color.Transparent), Offset(size.width * 0.95f, size.height * 0.9f), size.maxDimension * 0.5f),
                    radius = size.maxDimension * 0.5f,
                    center = Offset(size.width * 0.95f, size.height * 0.9f),
                )
            }
        }
        QuoteStyle.SPLIT -> {
            Box(Modifier.fillMaxSize().background(deep))
            if (!coverUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(coverUrl).allowHardware(false).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().fillMaxHeight(SPLIT_FRACTION),
                )
            }
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        SPLIT_FRACTION * 0.62f to Color.Transparent,
                        SPLIT_FRACTION to deep,
                        1f to deep,
                    ),
                ),
            )
        }
    }
}

@Composable
private fun QuoteCardContent(
    quote: List<String>,
    title: String,
    artist: String,
    coverUrl: String?,
    coverColor: Color,
    look: CardLook,
    frame: LineFrame? = null,
) {
    val accent = Accent
    val style = look.style
    val ink = inkFor(style)
    val inkSoft = ink.copy(alpha = 0.7f)
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth
        val h = maxHeight
        fun Dp.sp(): TextUnit = with(density) { this@sp.toSp() }
        QuoteBackground(style, coverUrl, coverColor, w)
        val base = when {
            quote.size <= 1 -> 0.105f
            quote.size == 2 -> 0.095f
            quote.size <= 4 -> 0.08f
            else -> 0.066f
        }
        val small = (w * 0.036f).sp()
        val pad = w * 0.08f
        Column(Modifier.fillMaxSize().padding(horizontal = pad, vertical = pad * if (h > w * 1.5f) 1.4f else 1f)) {
            if (style == QuoteStyle.SPLIT) Spacer(Modifier.height(h * SPLIT_FRACTION - pad * 0.6f))
            if (style == QuoteStyle.POLAROID && !coverUrl.isNullOrBlank()) {
                val art = minOf(w * 0.5f, h * 0.3f)
                Column(
                    Modifier
                        .align(if (look.centered) Alignment.CenterHorizontally else Alignment.Start)
                        .graphicsLayer { rotationZ = -3.5f }
                        .shadow(w * 0.025f, RoundedCornerShape(w * 0.006f))
                        .background(Color(0xFFFFFEFB))
                        .padding(start = w * 0.03f, end = w * 0.03f, top = w * 0.03f, bottom = w * 0.1f),
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(coverUrl).allowHardware(false).build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(art),
                    )
                }
                Spacer(Modifier.height(w * 0.07f))
            }
            if (style == QuoteStyle.ALBUM && !coverUrl.isNullOrBlank()) {
                val art = minOf(w * 0.46f, h * 0.3f)
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(coverUrl).allowHardware(false).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(if (look.centered) Alignment.CenterHorizontally else Alignment.Start)
                        .size(art)
                        .shadow(w * 0.03f, RoundedCornerShape(w * 0.03f))
                        .clip(RoundedCornerShape(w * 0.03f)),
                )
                Spacer(Modifier.height(w * 0.06f))
            }
            if (look.quoteMark && style != QuoteStyle.SPLIT) {
                Text(
                    "“",
                    color = when (style) {
                        QuoteStyle.LIGHT, QuoteStyle.POLAROID -> accent
                        QuoteStyle.NEON -> neonOf(coverColor)
                        else -> ink.copy(alpha = 0.55f)
                    },
                    fontSize = (w * 0.2f).sp(),
                    lineHeight = (w * 0.2f).sp(),
                    fontWeight = FontWeight.Black,
                    modifier = Modifier
                        .align(if (look.centered) Alignment.CenterHorizontally else Alignment.Start)
                        .height(w * 0.11f),
                )
            }
            FittedQuote(
                lines = quote,
                frame = frame,
                glow = if (style == QuoteStyle.NEON) neonOf(coverColor) else null,
                dimInk = if (style == QuoteStyle.LIGHT || style == QuoteStyle.POLAROID) ink.copy(alpha = 0.22f) else ink.copy(alpha = 0.3f),
                maxSize = (w * base * look.textScale).sp(),
                color = ink,
                centered = look.centered,
                bold = look.bold,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = w * 0.03f),
            )
            if (look.showTrack || look.showBrand) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (look.showTrack) {
                        if (style != QuoteStyle.ALBUM && style != QuoteStyle.SPLIT && style != QuoteStyle.POLAROID && !coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current).data(coverUrl).allowHardware(false).build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(w * 0.12f).clip(RoundedCornerShape(w * 0.022f)),
                            )
                            Spacer(Modifier.width(w * 0.032f))
                        }
                        Column(Modifier.weight(1f)) {
                            androidx.compose.material3.Text(title, color = ink, fontSize = small * 1.2f, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            androidx.compose.material3.Text(artist, color = inkSoft, fontSize = small, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    if (look.showBrand) {
                        Spacer(Modifier.width(w * 0.03f))
                        BrandMark(ink, inkSoft, w)
                    }
                }
            }
        }
    }
}

@Composable
private fun BrandMark(ink: Color, inkSoft: Color, w: Dp) {
    val density = LocalDensity.current
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(ink.copy(alpha = 0.12f)).padding(horizontal = w * 0.025f, vertical = w * 0.012f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(w * 0.03f)) {
            val bar = size.width / 5f
            listOf(0.55f, 1f, 0.7f).forEachIndexed { i, k ->
                val barH = size.height * k
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(i * bar * 2f, size.height - barH),
                    size = Size(bar, barH),
                    cornerRadius = CornerRadius(bar / 2f),
                )
            }
        }
        Spacer(Modifier.width(w * 0.012f))
        Text("Aria", color = inkSoft, fontSize = with(density) { (w * 0.032f).toSp() }, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FittedQuote(
    lines: List<String>,
    frame: LineFrame?,
    glow: Color?,
    dimInk: Color,
    maxSize: TextUnit,
    color: Color,
    centered: Boolean,
    bold: Boolean,
    modifier: Modifier,
) {
    val measurer = rememberTextMeasurer()
    val base = LocalTextStyle.current
    val align = if (centered) TextAlign.Center else TextAlign.Start
    val weight = if (bold) FontWeight.Bold else FontWeight.Medium
    val text = lines.joinToString("\n")
    val ranges = remember(lines) {
        var at = 0
        lines.map { line -> (at until at + line.length).also { at += line.length + 1 } }
    }
    val fill = frame?.fill
    val styled = androidx.compose.ui.text.buildAnnotatedString {
        lines.forEachIndexed { index, line ->
            val alpha = frame?.alpha?.getOrNull(index) ?: 1f
            val tone = if (fill != null) dimInk else color
            pushStyle(androidx.compose.ui.text.SpanStyle(color = tone.copy(alpha = tone.alpha * alpha)))
            append(line)
            pop()
            if (index < lines.lastIndex) append("\n")
        }
    }
    BoxWithConstraints(modifier, contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart) {
        val maxW = constraints.maxWidth
        val maxH = constraints.maxHeight
        val fitted = remember(text, maxSize, maxW, maxH, weight, align, base) {
            if (maxW <= 0 || maxH <= 0 || maxH == Constraints.Infinity) return@remember maxSize.value
            var size = maxSize.value
            val floor = maxSize.value * 0.25f
            while (size > floor) {
                val result = measurer.measure(
                    text = text,
                    style = base.merge(TextStyle(fontSize = size.sp, lineHeight = (size * 1.2f).sp, fontWeight = weight, textAlign = align)),
                    constraints = Constraints(maxWidth = maxW),
                )
                if (result.size.height <= maxH) break
                size *= 0.94f
            }
            size
        }
        val style = base.merge(
            TextStyle(
                fontSize = fitted.sp,
                lineHeight = (fitted * 1.2f).sp,
                fontWeight = weight,
                textAlign = align,
                shadow = glow?.let { androidx.compose.ui.graphics.Shadow(it.copy(alpha = 0.85f), Offset.Zero, fitted * 0.9f) },
            ),
        )
        var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
        Box(Modifier.fillMaxWidth()) {
            androidx.compose.material3.Text(styled, style = style, onTextLayout = { layout = it }, modifier = Modifier.fillMaxWidth())
            if (fill != null) {
                androidx.compose.material3.Text(
                    text,
                    color = color,
                    style = style,
                    modifier = Modifier.fillMaxWidth().drawWithContent {
                        val result = layout ?: return@drawWithContent
                        val path = androidx.compose.ui.graphics.Path()
                        ranges.forEachIndexed { index, range ->
                            val amount = fill.getOrNull(index) ?: 0f
                            if (amount <= 0f || range.isEmpty()) return@forEachIndexed
                            val first = result.getLineForOffset(range.first)
                            val last = result.getLineForOffset(range.last)
                            val widths = (first..last).map { (result.getLineRight(it) - result.getLineLeft(it)).coerceAtLeast(0f) }
                            var remaining = widths.sum() * amount
                            (first..last).forEachIndexed { visual, line ->
                                if (remaining <= 0f) return@forEachIndexed
                                val left = result.getLineLeft(line)
                                val take = minOf(widths[visual], remaining)
                                path.addRect(androidx.compose.ui.geometry.Rect(left - 2f, result.getLineTop(line), left + take, result.getLineBottom(line)))
                                remaining -= take
                            }
                        }
                        clipPath(path) { this@drawWithContent.drawContent() }
                    },
                )
            }
        }
    }
}
