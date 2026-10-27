package ru.muz.lyrics

import android.os.Build
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun FlowLyricsScreen(
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    settings: LyricsSettings,
    modifier: Modifier = Modifier,
    discontinuity: Long = 0,
    onLongPressLine: ((Int) -> Unit)? = null,
) {
    val items = remember(lyrics) { LyricsFlow.build(lyrics) }
    val listState = rememberLazyListState()
    val active = if (lyrics.synced) LyricsFlow.activeAt(items, positionMs) else -1
    val dragged by listState.interactionSource.collectIsDraggedAsState()
    var following by remember(lyrics) { mutableStateOf(true) }
    LaunchedEffect(dragged) {
        if (dragged) following = false
        else if (!following) {
            kotlinx.coroutines.delay(3_000)
            following = true
        }
    }
    LaunchedEffect(discontinuity) { following = true }
    val reduceMotion = ru.muz.ui.LocalReduceMotion.current
    val canBlur = settings.blurDistant && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && (following || !settings.sharpOnScroll)
    var shift by remember { mutableFloatStateOf(0f) }
    var elapsed by remember { mutableFloatStateOf(10_000f) }
    var waveFrom by remember { mutableIntStateOf(0) }
    val duration = ru.muz.ui.animationDuration(settings.scrollDurationMs.coerceAtLeast(380) + 360)

    BoxWithConstraints(modifier.fillMaxSize()) {
        val anchor = maxHeight * settings.anchorFraction
        LaunchedEffect(lyrics, active, following, settings.autoScroll, discontinuity) {
            if (!lyrics.synced || !following || !settings.autoScroll || active < 0) return@LaunchedEffect
            withFrameMillis { }
            val info = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == active }
            if (info == null || reduceMotion || duration <= 1) {
                listState.scrollToItem(active, 0)
                return@LaunchedEffect
            }
            val distance = info.offset.toFloat()
            if (abs(distance) < 1f) return@LaunchedEffect
            val residual = if (elapsed < 9_999f) shift * (1f - flowEase((elapsed / duration).coerceIn(0f, 1f))) else 0f
            shift = distance + residual
            waveFrom = active
            elapsed = 0f
            val consumed = listState.scrollBy(distance)
            if (abs(consumed - distance) > 0.5f) shift = consumed + residual
            val start = withFrameMillis { it }
            val total = duration + STAGGER_MS * 8f
            while (true) {
                val now = withFrameMillis { it }
                elapsed = (now - start).toFloat()
                if (elapsed >= total) break
            }
            elapsed = 10_000f
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    if (settings.fadeEdges) {
                        drawRect(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.07f to Color.Black,
                                0.86f to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                },
            contentPadding = PaddingValues(
                start = settings.horizontalPaddingDp.dp,
                end = settings.horizontalPaddingDp.dp,
                top = if (lyrics.synced) anchor else 24.dp,
                bottom = if (lyrics.synced) maxHeight - anchor else 32.dp,
            ),
        ) {
            itemsIndexed(items, key = { i, item -> "${item.startMs}-$i" }) { index, item ->
                val distance = if (active < 0) 1 else index - active
                val itemModifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        if (elapsed < 9_999f && shift != 0f) {
                            val delay = ((index - waveFrom).coerceAtLeast(0)) * STAGGER_MS
                            val local = ((elapsed - delay) / duration).coerceIn(0f, 1f)
                            translationY = shift * (1f - flowEase(local))
                        } else {
                            translationY = 0f
                        }
                    }
                when (item) {
                    is FlowItem.Pause -> PauseDots(
                        active = index == active,
                        progress = if (index == active) item.progressAt(positionMs) else 0f,
                        distance = distance,
                        canBlur = canBlur,
                        settings = settings,
                        modifier = itemModifier,
                    )
                    is FlowItem.Line -> FlowLine(
                        line = item.line,
                        active = index == active,
                        sung = active >= 0 && index < active,
                        distance = distance,
                        progress = if (index == active) {
                            if (settings.karaokeFill) item.line.progressAt(positionMs) else 1f
                        } else 0f,
                        synced = lyrics.synced,
                        canBlur = canBlur,
                        settings = settings,
                        modifier = itemModifier
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
                            .then(
                                if (lyrics.synced && (settings.tapToSeek || onLongPressLine != null)) {
                                    Modifier.combinedClickable(
                                        onClick = { if (settings.tapToSeek) onSeek(item.line.startMs) },
                                        onLongClick = onLongPressLine?.let { { it(item.index) } },
                                    )
                                } else Modifier,
                            )
                            .padding(horizontal = 6.dp, vertical = (settings.lineSpacingDp / 2).dp),
                    )
                }
            }
        }
    }
}

private const val STAGGER_MS = 55f

private val FLOW_EASING = androidx.compose.animation.core.CubicBezierEasing(0.3f, 0f, 0.12f, 1f)

private fun flowEase(t: Float): Float = FLOW_EASING.transform(t.coerceIn(0f, 1f))

@Composable
private fun FlowLine(
    line: LyricLine,
    active: Boolean,
    sung: Boolean,
    distance: Int,
    progress: Float,
    synced: Boolean,
    canBlur: Boolean,
    settings: LyricsSettings,
    modifier: Modifier,
) {
    val fade = ru.muz.ui.animationDuration(520)
    val alpha by animateFloatAsState(
        when {
            !synced -> 0.92f
            active && settings.karaokeFill -> settings.activeUnsungAlpha * 0.62f
            active -> 1f
            sung -> settings.inactiveAlpha * 0.8f
            else -> settings.inactiveAlpha
        },
        tween(fade, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "flow-alpha",
    )
    val overlay by animateFloatAsState(
        if (active && settings.karaokeFill) 1f else 0f,
        tween(fade, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "flow-overlay",
    )
    val shownProgress = if (active) progress else 1f
    val blurTarget = if (!canBlur || !synced || active) 0f else (kotlin.math.sqrt(abs(distance).coerceAtMost(6).toFloat()) * 1.85f).coerceAtMost(4.6f)
    val blur by animateFloatAsState(
        blurTarget,
        tween(ru.muz.ui.animationDuration(820), easing = androidx.compose.animation.core.CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)),
        label = "flow-blur",
    )
    val scale by animateFloatAsState(if (active || !synced) 1f else 0.985f, tween(fade, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "flow-scale")
    var layout by remember(line.text, settings.fontSize) { mutableStateOf<TextLayoutResult?>(null) }
    val base = MaterialTheme.colorScheme.onSurface
    val text = line.text
    Box(
        modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    if (settings.textAlign == TextAlign.Center) 0.5f else 0f,
                    0.5f,
                )
            }
            .then(if (blur > 0.05f) Modifier.blur(blur.dp) else Modifier),
    ) {
        Text(
            text,
            modifier = Modifier.fillMaxWidth(),
            color = base.copy(alpha = alpha),
            fontSize = settings.fontSize,
            lineHeight = settings.lineHeight,
            fontWeight = FontWeight.Bold,
            textAlign = settings.textAlign,
            onTextLayout = { layout = it },
        )
        if (overlay > 0.01f && shownProgress > 0f && text.isNotBlank()) {
            val soft = with(LocalDensity.current) { 18.dp.toPx() }
            Text(
                text,
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }.let {
                    if (settings.fillDirection == KaraokeDirection.VERTICAL) it.softRevealDown(shownProgress, soft) else it.softReveal(shownProgress, text, layout, soft)
                },
                color = base.copy(alpha = settings.sungAlpha.coerceAtMost(1f) * overlay),
                fontSize = settings.fontSize,
                lineHeight = settings.lineHeight,
                fontWeight = FontWeight.Bold,
                textAlign = settings.textAlign,
            )
        }
    }
}

private fun Modifier.softRevealDown(fraction: Float, soft: Float): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        if (fraction >= 0.999f) return@drawWithContent
        val far = size.height + size.width + 2_000f
        val edge = -soft + (size.height + soft) * fraction.coerceIn(0f, 1f)
        drawRect(
            Brush.verticalGradient(listOf(Color.Transparent, Color.Black), startY = edge, endY = edge + soft),
            Offset(-far, edge),
            Size(far * 2f, soft),
            blendMode = BlendMode.DstOut,
        )
        drawRect(Color.Black, Offset(-far, edge + soft), Size(far * 2f, far), blendMode = BlendMode.DstOut)
    }

private fun Modifier.softReveal(fraction: Float, text: String, layout: TextLayoutResult?, soft: Float): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithContent {
        drawContent()
        val result = layout ?: return@drawWithContent
        if (text.isEmpty() || fraction >= 0.999f) return@drawWithContent
        val progress = fraction.coerceIn(0f, 1f) * text.length
        val far = size.height + size.width + 2_000f
        val last = result.lineCount - 1
        for (line in 0..last) {
            val lineStart = result.getLineStart(line)
            val lineEnd = result.getLineEnd(line, visibleEnd = true)
            val top = if (line == 0) -far else result.getLineTop(line)
            val bottom = if (line == last) far else result.getLineTop(line + 1)
            if (lineEnd <= lineStart || progress >= lineEnd) continue
            if (progress <= lineStart) {
                drawRect(Color.Black, Offset(-far, top), Size(far * 2f, bottom - top), blendMode = BlendMode.DstOut)
                continue
            }
            val index = progress.toInt().coerceIn(lineStart, lineEnd - 1)
            val glyph = result.getBoundingBox(index)
            val x = glyph.left + glyph.width * (progress - index)
            drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = x - soft, endX = x),
                Offset(x - soft, top),
                Size(soft, bottom - top),
                blendMode = BlendMode.DstOut,
            )
            drawRect(Color.Black, Offset(x, top), Size(far, bottom - top), blendMode = BlendMode.DstOut)
        }
    }

@Composable
private fun PauseDots(
    active: Boolean,
    progress: Float,
    distance: Int,
    canBlur: Boolean,
    settings: LyricsSettings,
    modifier: Modifier,
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "pause")
    val breath by transition.animateFloat(
        0.92f,
        1.08f,
        androidx.compose.animation.core.infiniteRepeatable(tween(1100), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "pause-breath",
    )
    val shown by animateFloatAsState(if (active) 1f else 0.35f, tween(ru.muz.ui.animationDuration(520)), label = "pause-shown")
    val base = MaterialTheme.colorScheme.onSurface
    val blur = if (canBlur && !active) (abs(distance).coerceAtMost(4) * 1.1f) else 0f
    Row(
        modifier.height(40.dp).padding(horizontal = 10.dp)
            .then(if (blur > 0f) Modifier.blur(blur.dp) else Modifier)
            .graphicsLayer {
                val k = if (active) breath else 1f
                scaleX = k
                scaleY = k
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
                    if (settings.textAlign == TextAlign.Center) 0.5f else 0f,
                    0.5f,
                )
            },
        horizontalArrangement = if (settings.textAlign == TextAlign.Center) Arrangement.Center else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(3) { i ->
            val lit = (progress * 3f - i).coerceIn(0f, 1f)
            Box(
                Modifier.padding(end = 9.dp).size(11.dp).graphicsLayer {
                    val k = if (active) 1f else 0.75f
                    scaleX = k
                    scaleY = k
                }.clip(CircleShape)
                    .background(base.copy(alpha = shown * (0.35f + 0.65f * lit))),
            )
        }
    }
}
