package ru.muz.lyrics

sealed interface FlowItem {
    val startMs: Long
    val endMs: Long

    data class Line(val index: Int, val line: LyricLine) : FlowItem {
        override val startMs: Long get() = line.startMs
        override val endMs: Long get() = line.endMs
    }

    data class Pause(override val startMs: Long, override val endMs: Long) : FlowItem {
        fun progressAt(positionMs: Long): Float =
            ((positionMs - startMs).toFloat() / (endMs - startMs).coerceAtLeast(1)).coerceIn(0f, 1f)
    }
}

object LyricsFlow {
    const val MIN_PAUSE_MS = 6_000L

    fun build(lyrics: Lyrics, minPauseMs: Long = MIN_PAUSE_MS): List<FlowItem> {
        if (!lyrics.synced) return lyrics.lines.mapIndexed { i, line -> FlowItem.Line(i, line) }
        val result = mutableListOf<FlowItem>()
        val lines = lyrics.lines
        val firstSung = lines.firstOrNull { it.text.isNotBlank() }
        if (firstSung != null && firstSung.startMs >= minPauseMs) result += FlowItem.Pause(0, firstSung.startMs)
        lines.forEachIndexed { i, line ->
            val nextStart = lines.drop(i + 1).firstOrNull { it.text.isNotBlank() }?.startMs ?: line.endMs
            if (line.text.isBlank()) {
                if (result.lastOrNull() !is FlowItem.Pause && nextStart - line.startMs >= minPauseMs / 2) {
                    result += FlowItem.Pause(line.startMs, nextStart)
                }
                return@forEachIndexed
            }
            result += FlowItem.Line(i, line)
            val sungUntil = line.words.lastOrNull()?.endMs
            if (sungUntil != null && nextStart - sungUntil >= minPauseMs && i < lines.lastIndex) {
                result += FlowItem.Pause(sungUntil, nextStart)
            }
        }
        return result
    }

    fun activeAt(items: List<FlowItem>, positionMs: Long): Int {
        var found = -1
        for (i in items.indices) {
            if (items[i].startMs <= positionMs) found = i else break
        }
        return found
    }
}
