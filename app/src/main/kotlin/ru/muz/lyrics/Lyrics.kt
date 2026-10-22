package ru.muz.lyrics

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList(),
) {
    val hasWordTiming: Boolean get() = words.isNotEmpty()

    fun progressAt(positionMs: Long): Float {
        if (positionMs <= startMs) return 0f
        if (positionMs >= endMs) return 1f

        if (!hasWordTiming) {
            val span = (endMs - startMs).coerceAtLeast(1)
            return ((positionMs - startMs).toFloat() / span).coerceIn(0f, 1f)
        }

        val total = words.sumOf { it.text.length }.coerceAtLeast(1)
        var consumed = 0f
        for (word in words) {
            if (positionMs >= word.endMs) {
                consumed += word.text.length.toFloat()
                continue
            }
            if (positionMs <= word.startMs) break
            val span = (word.endMs - word.startMs).coerceAtLeast(1)
            val within = (positionMs - word.startMs).toFloat() / span
            consumed += word.text.length * within.coerceIn(0f, 1f)
            break
        }
        return (consumed / total).coerceIn(0f, 1f)
    }
}

data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
) {
    fun activeIndexAt(positionMs: Long): Int {
        if (!synced || lines.isEmpty() || positionMs < lines.first().startMs) return -1
        var low = 0
        var high = lines.lastIndex
        var found = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].startMs <= positionMs) {
                found = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return found
    }

    companion object {
        val EMPTY = Lyrics(emptyList(), synced = false)
    }
}

internal fun stablePlaybackPosition(
    incomingMs: Long,
    lastMs: Long,
    seekThresholdMs: Long = 1_200L,
): Long = if (incomingMs + seekThresholdMs < lastMs) incomingMs else maxOf(incomingMs, lastMs)
