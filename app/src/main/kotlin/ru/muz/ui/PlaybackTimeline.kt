package ru.muz.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.isActive
import ru.muz.player.PlayerState
import ru.muz.player.formatDuration
import ru.muz.player.positionAt

@Composable
internal fun rememberPlaybackPosition(state: PlayerState): State<Long> {
    val latest by rememberUpdatedState(state)
    val now = remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(state.mediaId, state.isPlaying) {
        now.longValue = SystemClock.elapsedRealtime()
        if (state.isPlaying) {
            while (isActive) {
                withFrameNanos { now.longValue = SystemClock.elapsedRealtime() }
            }
        }
    }
    return remember { derivedStateOf { latest.positionAt(now.longValue) } }
}

@Composable
internal fun PlaybackTimeline(
    state: PlayerState,
    onSeek: (Long) -> Unit,
    accent: Color = PlayerAccent,
) {
    val position by rememberPlaybackPosition(state)
    var preview by remember(state.mediaId) { mutableStateOf<Float?>(null) }
    Column {
        key(state.mediaId) { ThinSeekBar(
            progress = if (state.durationMs > 0) position.toFloat() / state.durationMs else 0f,
            onProgress = { onSeek((state.durationMs * it).toLong()) },
            accent = accent,
            mediaId = state.mediaId,
            enabled = state.durationMs > 0,
            bufferedProgress = if (state.durationMs > 0) state.bufferedPositionMs.toFloat() / state.durationMs else 0f,
            onPreview = { preview = it },
        ) }
        Row(Modifier.fillMaxWidth()) {
            Text(formatDuration(preview?.let { (state.durationMs * it).toLong() } ?: position), color = SoftText, fontSize = 12.sp)
            Text(formatDuration(state.durationMs), color = SoftText, fontSize = 12.sp,
                modifier = Modifier.weight(1f), textAlign = TextAlign.End)
        }
    }
}
