package ru.muz.lyrics

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.gestures.scrollBy

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun LyricsScreen(
    lyrics: Lyrics,
    positionMs: Long,
    onSeek: (Long) -> Unit,
    settings: LyricsSettings = LyricsSettings.MEASURED,
    background: Color = Color(0xFF1A1A1A),
    modifier: Modifier = Modifier,
    discontinuity: Long = 0,
    onLongPressLine: ((Int) -> Unit)? = null,
) {
    if (lyrics.lines.isEmpty()) {
        Box(modifier.fillMaxSize().background(background)) {
            Text(
                text = "Синхронизированный текст для этого трека пока не найден",
                modifier = Modifier.fillMaxSize().wrapContentSize(),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                fontSize = settings.fontSize,
                textAlign = settings.textAlign,
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val activeIndex = if (lyrics.synced) lyrics.activeIndexAt(positionMs) else -1
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

    BoxWithConstraints(modifier.fillMaxSize().background(background)) {
        val anchorPx = with(LocalDensity.current) {
            (maxHeight * settings.anchorFraction).roundToPx()
        }

        val scrollDuration = ru.muz.ui.animationDuration(settings.scrollDurationMs)
        LaunchedEffect(lyrics, activeIndex, following, settings.autoScroll, anchorPx, scrollDuration, discontinuity) {
            if (lyrics.synced && following && settings.autoScroll) {
                androidx.compose.runtime.withFrameNanos { }
                listState.animateToLyric(activeIndex.coerceAtLeast(0), scrollDuration)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = settings.horizontalPaddingDp.dp,
                end = settings.horizontalPaddingDp.dp,
                top = if (lyrics.synced) maxHeight * settings.anchorFraction else 16.dp,
                bottom = if (lyrics.synced) maxHeight * (1f - settings.anchorFraction) else 16.dp,
            ),
        ) {
            itemsIndexed(lyrics.lines, key = { index, line -> "${line.startMs}-$index" }) { index, line ->
                val distance = if (activeIndex < 0) 1 else index - activeIndex
                LyricLineRow(
                    line = line,
                    isActive = index == activeIndex,
                    distance = distance,
                    progress = if (index == activeIndex && settings.karaokeFill) {
                        line.progressAt(positionMs)
                    } else if (index == activeIndex) {
                        1f
                    } else {
                        0f
                    },
                    settings = settings,
                    synced = lyrics.synced,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = (settings.lineSpacingDp / 2).dp)
                        .then(
                            when {
                                onLongPressLine != null -> Modifier.combinedClickable(
                                    onClick = { if (settings.tapToSeek && lyrics.synced) onSeek(line.startMs) },
                                    onLongClick = { onLongPressLine(index) },
                                )
                                settings.tapToSeek && lyrics.synced -> Modifier.clickable { onSeek(line.startMs) }
                                else -> Modifier
                            }
                        ),
                )
            }
        }
    }
}

private suspend fun androidx.compose.foundation.lazy.LazyListState.animateToLyric(
    index: Int,
    durationMs: Int,
) {
    val target = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
    if (target == null || durationMs <= 1) {
        scrollToItem(index, 0)
        return
    }
    val distance = target.offset.toFloat()
    if (kotlin.math.abs(distance) < 1f) return
    var previous = 0f
    scroll {
        animate(0f, distance, animationSpec = tween(durationMillis = durationMs)) { value, _ ->
            scrollBy(value - previous)
            previous = value
        }
    }
}

@Composable
private fun LyricLineRow(
    line: LyricLine,
    isActive: Boolean,
    distance: Int,
    progress: Float,
    settings: LyricsSettings,
    synced: Boolean,
    modifier: Modifier = Modifier,
) {
    val emphasisDuration = ru.muz.ui.animationDuration(220)
    val smoothProgress = progress.coerceIn(0f, 1f)
    val emphasis by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = tween(emphasisDuration),
        label = "lyric-emphasis",
    )
    val targetAlpha = when {
        !synced -> 0.9f
        isActive -> settings.activeUnsungAlpha
        !settings.fadeEdges -> settings.inactiveAlpha
        else -> fadedAlpha(distance, settings)
    }
    val baseAlpha by animateFloatAsState(targetAlpha, tween(emphasisDuration), label = "lyric-alpha")
    var layout by remember(line.text, settings.fontSize, settings.textAlign) {
        mutableStateOf<TextLayoutResult?>(null)
    }

    val activeScale = if (ru.muz.ui.LocalReduceMotion.current) 0f else settings.activeScale
    Box(modifier.graphicsLayer {
        scaleX = 1f + emphasis * activeScale
        scaleY = 1f + emphasis * activeScale
        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(
            if (settings.textAlign == androidx.compose.ui.text.style.TextAlign.Center) 0.5f else 0f, 0.5f,
        )
    }) {
        if (settings.boldActiveLine) {
            LyricText(line.text, 0f, true, settings, Modifier.clearAndSetSemantics { })
        }
        LyricText(
            text = line.text,
            alpha = baseAlpha,
            bold = isActive && settings.boldActiveLine,
            settings = settings,
            modifier = if (settings.boldActiveLine) Modifier.matchParentSize() else Modifier,
            onTextLayout = { layout = it },
        )
        if (isActive && smoothProgress > 0f && line.text.isNotBlank()) {
            LyricText(
                text = line.text,
                alpha = settings.sungAlpha,
                bold = settings.boldActiveLine,
                settings = settings,
                modifier = Modifier.clearAndSetSemantics { }.revealText(smoothProgress, line.text, layout, settings),
            )
        }
    }
}

@Composable
private fun LyricText(
    text: String,
    alpha: Float,
    bold: Boolean,
    settings: LyricsSettings,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {},
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha.coerceIn(0f, 1f)),
        fontSize = settings.fontSize,
        lineHeight = settings.lineHeight,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
        textAlign = settings.textAlign,
        style = androidx.compose.ui.text.TextStyle(
            shadow = if (settings.karaokeFillStyle != KaraokeFillStyle.SOLID && settings.glowBlurDp > 0f && alpha > 0.9f) {
                Shadow(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = settings.glowAlpha),
                    blurRadius = with(LocalDensity.current) { settings.glowBlurDp.dp.toPx() },
                )
            } else null,
        ),
        onTextLayout = onTextLayout,
    )
}

private fun fadedAlpha(distance: Int, settings: LyricsSettings): Float =
    when (kotlin.math.abs(distance)) {
        0, 1 -> settings.inactiveAlpha
        2 -> settings.inactiveAlpha * 0.82f
        3 -> settings.inactiveAlpha * 0.68f
        4 -> settings.inactiveAlpha * 0.48f
        else -> settings.inactiveAlpha * 0.24f
    }

private fun Modifier.revealText(
    fraction: Float,
    text: String,
    layout: TextLayoutResult?,
    settings: LyricsSettings,
): Modifier = drawWithContent {
    val result = layout ?: return@drawWithContent
    if (text.isEmpty()) return@drawWithContent
    val normalized = fraction.coerceIn(0f, 1f)
    val progressFraction = if (settings.karaokeFillStyle == KaraokeFillStyle.WORD_PULSE) {
        (normalized + 0.015f * kotlin.math.sin(normalized * kotlin.math.PI).toFloat()).coerceIn(0f, 1f)
    } else normalized
    val progress = progressFraction * text.length
    val path = Path()
    for (line in 0 until result.lineCount) {
        val lineStart = result.getLineStart(line)
        val lineEnd = result.getLineEnd(line, visibleEnd = true)
        if (lineEnd <= lineStart || progress <= lineStart) continue
        val startX = minOf(result.getLineLeft(line), result.getLineRight(line))
        val endX = maxOf(result.getLineLeft(line), result.getLineRight(line))
        val right = if (progress >= lineEnd) endX + 2f else {
            val index = progress.toInt().coerceIn(lineStart, lineEnd - 1)
            val glyph = result.getBoundingBox(index)
            glyph.left + glyph.width * (progress - index)
        }
        path.addRect(androidx.compose.ui.geometry.Rect(
            startX - 2f, result.getLineTop(line) - 2f,
            right.coerceAtLeast(startX), result.getLineBottom(line) + 2f,
        ))
    }
    clipPath(path) { this@drawWithContent.drawContent() }
}
