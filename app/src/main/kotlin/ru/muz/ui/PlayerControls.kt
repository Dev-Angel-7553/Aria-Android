package ru.muz.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.muz.player.PlayerState
import ru.muz.player.RepeatMode

@Composable
internal fun PlayerTransport(
    state: PlayerState,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onShuffle: (() -> Unit)? = null,
    onRepeat: (() -> Unit)? = null,
    accented: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().height(92.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onShuffle != null) {
            TransportAction(Icons.Rounded.Shuffle, "Перемешать", state.shuffle, Modifier.width(48.dp), onShuffle)
        }
        TransportAction(Icons.Rounded.SkipPrevious, "Предыдущий", false, Modifier.weight(1f), onPrevious, prominent = true)
        PlayerPlayButton(state, onPlayPause, Modifier.weight(1.2f), accented = accented)
        TransportAction(Icons.Rounded.SkipNext, "Следующий", false, Modifier.weight(1f), onNext, prominent = true)
        if (onRepeat != null) {
            TransportAction(
                if (state.repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                when (state.repeat) { RepeatMode.ONE -> "Повтор одного трека"; RepeatMode.OFF -> "Повтор выключен"; else -> "Повтор очереди" },
                state.repeat != RepeatMode.OFF, Modifier.width(48.dp), onRepeat,
            )
        }
    }
}

@Composable
internal fun PlayerPlayButton(state: PlayerState, onClick: () -> Unit, modifier: Modifier = Modifier, compact: Boolean = false, accented: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val motion = LocalPlayerTransitions.current && !LocalReduceMotion.current
    val playing = state.isPlaying || state.playWhenReady
    val scale by animateFloatAsState(
        if (pressed && motion) 0.93f else 1f,
        if (motion) spring(dampingRatio = 0.7f, stiffness = 620f) else tween(0), label = "play-press",
    )
    val radius by animateDpAsState(
        if (playing) 20.dp else 32.dp,
        if (motion) spring(dampingRatio = 0.82f, stiffness = 400f) else tween(0), label = "play-radius",
    )
    val description = when { state.isBuffering -> "Загрузка"; playing -> "Пауза"; else -> "Играть" }
    Box(
        modifier.height(if (compact) 48.dp else 68.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(if (LocalPlayShape.current == ru.muz.settings.PlayShape.CIRCLE) RoundedCornerShape(radius) else playShape(LocalPlayShape.current))
            .background(if (accented) PlayerAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClickLabel = tr(description), onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        PlaybackIcon(state, Modifier.size(if (compact) 25.dp else 36.dp), if (accented) OnPlayerAccent else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TransportAction(
    icon: ImageVector, description: String, active: Boolean, modifier: Modifier,
    onClick: () -> Unit, prominent: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val duration = playerTransitionDuration(180)
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, tween(duration), label = "transport-press")
    val corner by animateDpAsState(if (pressed) 16.dp else 24.dp, tween(duration), label = "transport-corner")
    val background by animateColorAsState(
        when { active -> PlayerAccent.copy(alpha = 0.16f); prominent -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.055f); else -> Color.Transparent },
        tween(duration), label = "transport-background",
    )
    Box(
        modifier.height(if (prominent) 60.dp else 48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(corner)).background(background)
            .semantics { selected = active }
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, description, tint = if (active) PlayerAccent else MaterialTheme.colorScheme.onSurface.copy(alpha = if (prominent) 0.95f else 0.65f),
            modifier = Modifier.size(if (prominent) 30.dp else 22.dp))
    }
}
