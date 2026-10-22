package ru.muz.ui

import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import ru.muz.player.PlayerState
import ru.muz.player.formatDuration
import ru.muz.yandex.YandexClient

internal data class NowPlaying(val mediaId: String = "", val isPlaying: Boolean = false) {
    fun matches(trackId: String): Boolean =
        mediaId.isNotBlank() && trackId.isNotBlank() &&
            (mediaId == trackId || mediaId.substringBefore(':') == trackId.substringBefore(':'))
}

internal val LocalNowPlaying = compositionLocalOf { NowPlaying() }

@Composable
internal fun NowPlayingBars(playing: Boolean, modifier: Modifier = Modifier, color: Color = Accent) {
    val animated = playing && LocalAnimateLoading.current && !LocalReduceMotion.current
    val phase = if (animated) {
        val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "now-playing")
        val value by transition.animateFloat(
            0f, 1f,
            androidx.compose.animation.core.infiniteRepeatable(tween(animationDuration(1100)), androidx.compose.animation.core.RepeatMode.Restart),
            label = "now-playing-phase",
        )
        value
    } else 0f
    Canvas(modifier) {
        val barWidth = size.width / 5f
        for (index in 0..2) {
            val amount = if (!animated) listOf(0.45f, 0.75f, 0.55f)[index] else
                0.3f + 0.7f * kotlin.math.abs(kotlin.math.sin((phase * 2 * kotlin.math.PI + index * 1.3).toFloat()))
            val x = barWidth / 2 + index * barWidth * 2
            val top = size.height * (1f - amount)
            drawLine(color, androidx.compose.ui.geometry.Offset(x + barWidth / 2, size.height),
                androidx.compose.ui.geometry.Offset(x + barWidth / 2, top), barWidth, StrokeCap.Round)
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun ThinSeekBar(
    progress: Float,
    onProgress: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Accent,
    mediaId: String = "",
    enabled: Boolean = true,
    bufferedProgress: Float = 0f,
    onPreview: (Float?) -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    var draft by remember(mediaId) { mutableStateOf<Float?>(null) }
    var pending by remember(mediaId) { mutableStateOf<Float?>(null) }
    val previewCallback by rememberUpdatedState(onPreview)
    LaunchedEffect(draft, pending) { previewCallback(draft ?: pending) }
    LaunchedEffect(target, pending) {
        if (pending != null && kotlin.math.abs(target - pending!!) < 0.008f) pending = null
    }
    LaunchedEffect(pending) {
        if (pending != null) {
            kotlinx.coroutines.delay(1_500)
            pending = null
        }
    }
    val interactions = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val dragged by interactions.collectIsDraggedAsState()
    LaunchedEffect(interactions) {
        interactions.interactions.collect { interaction ->
            if (interaction is androidx.compose.foundation.interaction.DragInteraction.Cancel ||
                interaction is androidx.compose.foundation.interaction.PressInteraction.Cancel && !dragged
            ) draft = null
        }
    }
    val thumbHeight by animateDpAsState(
        if (draft != null) 28.dp else 20.dp, tween(animationDuration(120)), label = "seek-thumb",
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val shown = draft ?: pending ?: target
    androidx.compose.material3.Slider(
        value = shown,
        onValueChange = { draft = it },
        onValueChangeFinished = {
            draft?.let { value ->
                pending = value
                draft = null
                onProgress(value)
            }
        },
        enabled = enabled,
        interactionSource = interactions,
        modifier = modifier.fillMaxWidth().height(48.dp),
        thumb = {
            Box(Modifier.width(6.dp).height(thumbHeight).clip(RoundedCornerShape(3.dp))
                .background(if (enabled) accent else trackColor))
        },
        track = {
            Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                val y = size.height / 2
                fun segment(fraction: Float, color: Color) {
                    drawLine(color, androidx.compose.ui.geometry.Offset(0f, y),
                        androidx.compose.ui.geometry.Offset(size.width * fraction.coerceIn(0f, 1f), y),
                        size.height, StrokeCap.Round)
                }
                segment(1f, trackColor)
                segment(bufferedProgress, accent.copy(alpha = 0.25f))
                if (shown > 0f) segment(shown, if (enabled) accent else trackColor)
            }
        },
    )
}

@Composable
internal fun ScreenHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 29.sp, fontWeight = FontWeight.Black)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = SoftText, fontSize = 13.sp)
            }
        }
        action?.invoke()
    }
}

@Composable
internal fun Artwork(url: String?, modifier: Modifier, radius: Dp) {
    val scaledRadius = radius * LocalArtworkCornerScale.current * LocalCornerRadiusScale.current
    Box(
        modifier.clip(RoundedCornerShape(scaledRadius)).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                Icons.Filled.Headphones,
                contentDescription = null,
                tint = SoftText,
                modifier = Modifier.fillMaxSize(0.36f),
            )
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(if (LocalAnimateArtwork.current) animationDuration(240) else 0)
                    .memoryCacheKey(url)
                    .diskCacheKey(url)
                    .build(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    if (LocalAnimateArtwork.current) ArtworkPlaceholder()
                },
                error = { Icon(Icons.Filled.Headphones, null, tint = SoftText, modifier = Modifier.fillMaxSize(0.36f)) },
            )
        }
    }
}

@Composable
private fun ArtworkPlaceholder() {
    val alpha = 0.18f + loadingPhase() * 0.24f
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)))
}

@Composable
internal fun CompactPlayer(
    state: PlayerState,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit = {},
    swipeEnabled: Boolean = true,
    showProgress: Boolean = true,
) {
    val position by rememberPlaybackPosition(state)
    val progress = if (state.durationMs > 0) (position.toFloat() / state.durationMs).coerceIn(0f, 1f) else 0f
    val reduceMotion = LocalReduceMotion.current
    var drag by remember(state.mediaId) { mutableFloatStateOf(0f) }
    val animatedOffset by animateDpAsState(
        targetValue = (if (reduceMotion) 0f else drag / 4f).coerceIn(-28f, 28f).dp,
        animationSpec = tween(animationDuration(180)),
        label = "mini-player-swipe",
    )
    val swipeScale by animateFloatAsState(
        targetValue = if (drag == 0f || reduceMotion) 1f else 0.985f,
        animationSpec = tween(animationDuration(180)),
        label = "mini-player-scale",
    )
    val swipeModifier = if (swipeEnabled) {
        Modifier.pointerInput(state.mediaId) {
            detectHorizontalDragGestures(
                onDragStart = { drag = 0f },
                onHorizontalDrag = { change, amount ->
                    change.consume()
                    drag += amount
                },
                onDragEnd = {
                    when {
                        drag < -72f -> onNext()
                        drag > 72f -> onPrevious()
                    }
                    drag = 0f
                },
                onDragCancel = { drag = 0f },
            )
        }
    } else Modifier
    val shape = RoundedCornerShape(16.dp * LocalCornerRadiusScale.current)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp).offset(x = animatedOffset).graphicsLayer {
            scaleX = swipeScale
            scaleY = swipeScale
            alpha = if (reduceMotion) 1f else 1f - (kotlin.math.abs(drag) / 800f).coerceIn(0f, 0.16f)
        }.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .then(swipeModifier).clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerCrossfade(state.coverUrl, Modifier.size(48.dp)) { url -> Artwork(url, Modifier.fillMaxSize(), 10.dp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    state.title,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    state.artist,
                    color = SoftText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (LocalMiniPrevious.current) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.Filled.SkipPrevious, "Предыдущий")
                }
            }
            IconButton(onClick = onPlayPause) {
                PlaybackIcon(state, Modifier.size(26.dp), MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.Filled.SkipNext, "Следующий")
            }
        }
        if (showProgress) {
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp).height(2.dp).clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)),
            ) {
                Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(Accent))
            }
            Box(Modifier.height(4.dp))
        }
    }
}

@Composable
internal fun MyWaveCard(trackCount: Int = 0, onClick: () -> Unit) {
    val corners = LocalCornerRadiusScale.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp * corners))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(60.dp).clip(RoundedCornerShape(17.dp * corners))
                .background(Accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.GraphicEq, null, tint = Accent, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text(
                "Моя волна",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (trackCount > 0) "${ru.muz.text.tracksLabel(trackCount)} по вашему вкусу" else "Персональный поток по вашей истории",
                color = SoftText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Icon(Icons.Filled.PlayArrow, "Слушать", tint = Accent, modifier = Modifier.size(30.dp))
    }
}

@Composable
internal fun LoadingCards(modifier: Modifier = Modifier) {
    val corners = LocalCornerRadiusScale.current
    val phase = 0.25f + loadingPhase() * 0.3f
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MuzLoadingIndicator(Modifier.size(30.dp))
            Text("Собираем музыку", color = SoftText, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(3) { index ->
                Column(Modifier.width(154.dp)) {
                    Box(
                        Modifier.size(154.dp).clip(RoundedCornerShape(18.dp * corners))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = phase - index * 0.04f)),
                    )
                    Box(
                        Modifier.padding(top = 9.dp).width(120.dp).height(15.dp).clip(RoundedCornerShape(8.dp * corners))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = phase * 0.65f)),
                    )
                    Box(
                        Modifier.padding(top = 7.dp).width(76.dp).height(11.dp).clip(RoundedCornerShape(7.dp * corners))
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = phase * 0.45f)),
                    )
                }
            }
        }
    }
}

@Composable
internal fun PlaybackErrorBanner(message: String) {
    if (message.isBlank()) return
    Text(
        message,
        color = MaterialTheme.colorScheme.onErrorContainer,
        fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
internal fun LoadingRibbon(label: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MuzLoadingIndicator(Modifier.size(24.dp))
            Text(label, color = SoftText, fontSize = 13.sp, modifier = Modifier.padding(start = 9.dp))
        }
        MuzWavyProgress(Modifier.fillMaxWidth().padding(top = 7.dp))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MuzLoadingIndicator(
    modifier: Modifier = Modifier,
    color: Color = Accent,
) {
    val phase = loadingPhase()
    val animated = LocalAnimateLoading.current
    Canvas(modifier) {
        val width = size.width / 8f
        for (index in 0..2) {
            val amount = if (!animated) 0.55f else 0.25f + 0.65f *
                kotlin.math.abs(kotlin.math.sin((phase * kotlin.math.PI + index * 0.8))).toFloat()
            val x = size.width * (index + 1) / 4f
            val half = size.height * amount / 2f
            drawLine(color, androidx.compose.ui.geometry.Offset(x, center.y - half),
                androidx.compose.ui.geometry.Offset(x, center.y + half), width, StrokeCap.Round)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MuzWavyProgress(
    modifier: Modifier = Modifier,
    color: Color = Accent,
) {
    val phase = loadingPhase()
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    Canvas(modifier.height(4.dp)) {
        val y = size.height / 2f
        drawLine(trackColor, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Offset(size.width, y), size.height, StrokeCap.Round)
        val start = phase * size.width * 0.65f
        drawLine(color, androidx.compose.ui.geometry.Offset(start, y), androidx.compose.ui.geometry.Offset(start + size.width * 0.35f, y), size.height, StrokeCap.Round)
    }
}

@Composable
internal fun PlaylistCard(
    value: YandexClient.Playlist,
    onClick: (YandexClient.Playlist) -> Unit,
    onMore: (YandexClient.Playlist) -> Unit = {},
) {
    Column(Modifier.width(154.dp).clickable { onClick(value) }) {
        Artwork(value.coverUrl, Modifier.size(154.dp), 18.dp)
        Text(
            value.title,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 9.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(ru.muz.text.tracksLabel(value.trackCount), color = SoftText, fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { onMore(value) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.MoreVert, "Ещё", tint = SoftText, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
internal fun AlbumCard(value: YandexClient.Album, onClick: (YandexClient.Album) -> Unit) {
    Column(Modifier.width(154.dp).clickable { onClick(value) }) {
        Artwork(value.coverUrl, Modifier.size(154.dp), 18.dp)
        Text(value.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        Text(value.artist, color = SoftText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun ArtistCard(value: YandexClient.Artist, onClick: (YandexClient.Artist) -> Unit) {
    Column(Modifier.width(126.dp).clickable { onClick(value) }, horizontalAlignment = Alignment.CenterHorizontally) {
        Artwork(value.coverUrl, Modifier.size(126.dp), 63.dp)
        Text(value.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 9.dp))
        Text(if (value.trackCount > 0) ru.muz.text.tracksLabel(value.trackCount) else "Исполнитель", color = SoftText, fontSize = 12.sp)
    }
}

@Composable
internal fun PlaylistListRow(
    value: YandexClient.Playlist,
    onClick: (YandexClient.Playlist) -> Unit,
    onMore: (YandexClient.Playlist) -> Unit = {},
) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick(value) }
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(value.coverUrl, Modifier.size(58.dp), 12.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                value.title,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "Плейлист • ${ru.muz.text.tracksLabel(value.trackCount)}",
                color = SoftText,
                fontSize = 13.sp,
            )
        }
        IconButton(onClick = { onMore(value) }) { Icon(Icons.Filled.MoreVert, "Ещё", tint = SoftText) }
    }
}

@Composable
internal fun TrackRow(
    value: YandexClient.Track,
    compact: Boolean,
    scalePercent: Int = 100,
    onMore: (YandexClient.Track) -> Unit = {},
    onClick: () -> Unit,
) {
    val scale = scalePercent.coerceIn(80, 125) / 100f
    val artSize = (if (compact) 46.dp else 56.dp) * scale
    val nowPlaying = LocalNowPlaying.current
    val current = nowPlaying.matches(value.id)
    Row(
        Modifier.fillMaxWidth()
            .then(if (current) Modifier.background(Accent.copy(alpha = 0.08f)) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = (if (compact) 5.dp else 8.dp) * scale),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(value.coverUrl, Modifier.size(artSize), 11.dp)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                value.title,
                color = if (current) Accent else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value.explicit) ExplicitBadge(Modifier.padding(end = 6.dp))
                Text(
                    listOf(value.artist, value.album)
                        .filter { it.isNotBlank() }
                        .joinToString(" • "),
                    color = SoftText,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (current) {
            NowPlayingBars(nowPlaying.isPlaying, Modifier.size(width = 18.dp, height = 16.dp))
        } else {
            Text(formatDuration(value.durationMs), color = SoftText, fontSize = 12.sp)
        }
        IconButton(onClick = { onMore(value) }) { Icon(Icons.Filled.MoreVert, "Ещё", tint = SoftText) }
    }
}

@Composable
internal fun ExplicitBadge(modifier: Modifier = Modifier) {
    Box(
        modifier.size(15.dp).clip(RoundedCornerShape(3.dp)).background(SoftText.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center,
    ) {
        Text("E", color = MaterialTheme.colorScheme.surface, fontSize = 10.sp, fontWeight = FontWeight.Black, lineHeight = 10.sp)
    }
}

@Composable
internal fun CollectionHero(count: Int, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp * LocalCornerRadiusScale.current)).background(Elevated)
            .clickable(onClick = onClick).padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(18.dp * LocalCornerRadiusScale.current)).background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Favorite, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
        }
        Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
            Text("Мне нравится", fontSize = 19.sp, fontWeight = FontWeight.Black)
            Text(ru.muz.text.tracksLabel(count), color = SoftText)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Accent, modifier = Modifier.size(30.dp))
    }
}

@Composable
internal fun SectionTitle(value: String) {
    Text(
        value,
        fontSize = 20.sp,
        fontWeight = FontWeight.Black,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
    )
}

@Composable
internal fun EmptyHint(value: String) {
    Text(value, color = SoftText, modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp))
}
