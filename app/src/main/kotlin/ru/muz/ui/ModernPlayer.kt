package ru.muz.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import ru.muz.player.PlayerState
import ru.muz.theme.ColorMath

@Composable
internal fun MovingCoverBackground(tint: Color, strength: Float, modifier: Modifier = Modifier, pulse: Float = 0f, playing: Boolean = true) {
    val base = AppBackground
    val dark = base.luminance() < 0.4f
    val argb = tint.toArgb()
    val hsl = ColorMath.hsl(argb)
    val second = Color(ColorMath.fromHsl(hsl[0] + 32f, (hsl[1] * 0.9f).coerceIn(0f, 1f), (hsl[2] * 0.72f).coerceIn(0.08f, 0.9f)))
    val third = Color(ColorMath.fromHsl(hsl[0] - 24f, (hsl[1] * 0.75f).coerceIn(0f, 1f), (hsl[2] * 1.05f).coerceIn(0.1f, 0.9f)))
    val ground = Color(ColorMath.blend(base.toArgb(), argb, if (dark) 0.34f * strength else 0.22f * strength))
    val reduce = LocalReduceMotion.current
    val phase = androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(reduce, playing) {
        if (!reduce && playing) {
            var previous = 0L
            while (true) {
                androidx.compose.runtime.withFrameNanos { now ->
                    if (previous != 0L) phase.floatValue = (phase.floatValue + (now - previous).coerceAtMost(50_000_000L) / 22_000_000_000f) % 1f
                    previous = now
                }
            }
        }
    }
    val energy = rememberAudioEnergy(pulse > 0f && !reduce)
    Canvas(modifier.fillMaxSize()) {
        val beat = energy.value * pulse
        drawRect(ground)
        val w = size.width
        val h = size.height
        val t = phase.floatValue * 2f * PI.toFloat()
        val blobs = listOf(
            Triple(tint, Offset(w * (0.25f + 0.18f * cos(t)), h * (0.18f + 0.1f * sin(t * 2f))), 0.95f),
            Triple(second, Offset(w * (0.8f + 0.15f * sin(t + 1.3f)), h * (0.45f + 0.14f * cos(t + 0.4f))), 0.85f),
            Triple(third, Offset(w * (0.35f + 0.2f * sin(t * 0.7f + 2f)), h * (0.85f + 0.08f * cos(t + 2.2f))), 0.75f),
        )
        blobs.forEach { (color, center, k) ->
            val radius = maxOf(w, h) * 0.62f * (1f + 0.14f * beat)
            drawCircle(
                Brush.radialGradient(listOf(color.copy(alpha = ((if (dark) 0.62f else 0.42f) * strength * k * (1f + 0.5f * beat)).coerceAtMost(1f)), Color.Transparent), center, radius),
                radius = radius,
                center = center,
            )
        }
        drawRect(if (dark) Color.Black.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.18f))
    }
}

private fun Color.luminance(): Float = (0.2126f * red + 0.7152f * green + 0.0722f * blue)

@Composable
internal fun UnifiedPlayerHeader(
    lyrics: Boolean,
    sleepLabel: String?,
    onBack: () -> Unit,
    onMore: () -> Unit,
    onCancelSleep: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(20.dp).clickable(onClickLabel = tr("Свернуть плеер"), onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.width(38.dp).height(5.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)))
        }
        Row(
            Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ExpandMore, "Свернуть") }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                PlayerCrossfade(lyrics) { showing ->
                    Text(if (showing) "Текст песни" else "Сейчас играет", fontWeight = FontWeight.Bold, maxLines = 1)
                }
                androidx.compose.animation.AnimatedVisibility(sleepLabel != null) {
                    Text(
                        "${sleepLabel.orEmpty()} · отменить",
                        color = PlayerAccent,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onCancelSleep).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            IconButton(onClick = onMore) { Icon(Icons.Rounded.MoreHoriz, "Ещё") }
        }
    }
}

@Composable
internal fun LyricsTrackStrip(
    state: PlayerState,
    liked: Boolean,
    onCover: () -> Unit,
    onArtist: () -> Unit,
    onLike: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayerCrossfade(
            state.coverUrl,
            Modifier.size(52.dp).playerArtworkTransition(state.mediaId, state.coverUrl).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = tr("Обложка"), onClick = onCover),
        ) { url -> Artwork(url, Modifier.fillMaxSize(), 10.dp) }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            androidx.compose.material3.Text(state.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            androidx.compose.material3.Text(
                state.artist,
                color = SoftText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable(onClick = onArtist),
            )
        }
        IconButton(onClick = onLike) {
            Icon(
                if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                if (liked) "Убрать из «Мне нравится»" else "Мне нравится",
                tint = if (liked) PlayerAccent else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
internal fun ModernLyricsDock(
    state: PlayerState,
    label: String,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onCover: () -> Unit,
    onQueue: () -> Unit,
    lyricsActive: Boolean = true,
    onLyrics: () -> Unit = {},
    onShuffle: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
    accentControls: Boolean = false,
    backdrop: GlassBackdrop? = null,
) {
    val shape = RoundedCornerShape(32.dp)
    val panel = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 10.dp)
        .then(
            if (backdrop != null) Modifier.glass(backdrop, shape, corner = 32.dp)
            else Modifier.shadow(10.dp, shape, ambientColor = Color.Black.copy(alpha = 0.18f), spotColor = Color.Black.copy(alpha = 0.22f))
                .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)),
        )
        .padding(horizontal = 14.dp, vertical = 8.dp)
    Column(panel) {
        PlaybackTimeline(state, onSeek, remaining = true, label = label)
        PlayerTransport(state, onPlayPause, onPrevious, onNext, onShuffle, onRepeat, accented = accentControls || !lyricsActive)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            DockIcon(Icons.Rounded.Lyrics, "Текст", active = lyricsActive, onClick = onLyrics)
            DockIcon(Icons.Rounded.Image, "Обложка", active = !lyricsActive, onClick = onCover)
            DockIcon(Icons.Rounded.QueueMusic, "Очередь", active = false, onClick = onQueue)
        }
    }
}

@Composable
private fun DockIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, active: Boolean, onClick: () -> Unit) {
    val background by androidx.compose.animation.animateColorAsState(
        if (active) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0f),
        tween(playerTransitionDuration(220)),
        label = "dock-icon",
    )
    val tint by androidx.compose.animation.animateColorAsState(
        if (active) MaterialTheme.colorScheme.onSurface else SoftText,
        tween(playerTransitionDuration(220)),
        label = "dock-tint",
    )
    Box(
        Modifier.size(44.dp).clip(CircleShape)
            .background(background)
            .semantics { selected = active }
            .clickable(role = androidx.compose.ui.semantics.Role.Tab, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, title, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
internal fun DockOverlay(visible: Boolean, modifier: Modifier, onShow: () -> Unit, scrim: Boolean = true, content: @Composable () -> Unit) {
    val duration = playerTransitionDuration(460)
    Box(modifier.fillMaxWidth()) {
        androidx.compose.animation.AnimatedVisibility(
            visible = visible,
            enter = androidx.compose.animation.slideInVertically(tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 2 } +
                androidx.compose.animation.fadeIn(tween(duration)),
            exit = androidx.compose.animation.slideOutVertically(tween(duration, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { it / 2 } +
                androidx.compose.animation.fadeOut(tween(duration)),
        ) {
            Box(
                Modifier.fillMaxWidth().then(
                    if (scrim) Modifier.background(
                        Brush.verticalGradient(listOf(Color.Transparent, AppBackground.copy(alpha = 0.55f), AppBackground.copy(alpha = 0.78f))),
                    ) else Modifier,
                ).padding(top = 28.dp),
            ) { content() }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = !visible,
            enter = androidx.compose.animation.fadeIn(tween(duration)),
            exit = androidx.compose.animation.fadeOut(tween(duration / 2)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Box(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClickLabel = tr("Открыть текст песни"), onClick = onShow), contentAlignment = Alignment.Center) {
                Box(Modifier.width(56.dp).height(4.dp).clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)))
            }
        }
    }
}
