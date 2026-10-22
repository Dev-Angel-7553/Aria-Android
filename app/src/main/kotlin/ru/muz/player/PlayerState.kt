package ru.muz.player

data class PlayerState(
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val coverUrl: String? = null,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val isPlaying: Boolean = false,
    val isLiked: Boolean = false,
    val repeat: RepeatMode = RepeatMode.OFF,
    val shuffle: Boolean = false,
    val error: String = "",
    val isBuffering: Boolean = false,
    val playWhenReady: Boolean = false,
    val bufferedPositionMs: Long = 0,
    val sampledAtMs: Long = 0,
    val playbackSpeed: Float = 1f,
    val discontinuity: Long = 0,
) {
    val progress: Float
        get() = if (durationMs <= 0) 0f else (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

internal fun PlayerState.positionAt(nowMs: Long): Long {
    val elapsed = if (isPlaying && !isBuffering && sampledAtMs > 0) {
        ((nowMs - sampledAtMs).coerceIn(0, 500) * playbackSpeed).toLong()
    } else 0L
    val position = (positionMs + elapsed).coerceAtLeast(0)
    return if (durationMs > 0) position.coerceAtMost(durationMs) else position
}

enum class RepeatMode { OFF, ALL, ONE }

fun formatDuration(ms: Long): String {
    if (ms < 0) return "0:00"
    val totalSeconds = ms / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
