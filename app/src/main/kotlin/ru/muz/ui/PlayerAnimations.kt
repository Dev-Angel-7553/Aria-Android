package ru.muz.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.muz.player.PlayerState

@Composable
internal fun playerTransitionDuration(baseMs: Int): Int =
    if (LocalPlayerTransitions.current) animationDuration(baseMs) else 0

@Composable
internal fun <T> PlayerCrossfade(target: T, modifier: Modifier = Modifier, content: @Composable (T) -> Unit) {
    Crossfade(target, modifier, tween(playerTransitionDuration(240)), label = "player-content", content = content)
}

@Composable
internal fun loadingPhase(): Float {
    if (!LocalAnimateLoading.current) return 0.5f
    val transition = rememberInfiniteTransition(label = "loading")
    val phase by transition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(animationDuration(900)), RepeatMode.Reverse), label = "loading-phase")
    return phase
}

@Composable
internal fun PlaybackIcon(state: PlayerState, modifier: Modifier = Modifier, color: Color = PlayerAccent) {
    val mode = when {
        state.isBuffering -> 0
        state.playWhenReady || state.isPlaying -> 1
        else -> 2
    }
    PlayerCrossfade(mode, modifier) { value ->
        if (value == 0) {
            MuzLoadingIndicator(Modifier.fillMaxSize(), color)
        } else {
            Icon(if (value == 1) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                if (value == 1) "Пауза" else "Играть", tint = color, modifier = Modifier.fillMaxSize())
        }
    }
}
