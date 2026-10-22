package ru.muz.download

object QuoteClip {
    const val MIN_MS = 5_000L
    const val MAX_MS = 30_000L
    const val LEAD_IN_MS = 400L
    const val TAIL_MS = 900L

    data class Range(val startMs: Long, val endMs: Long) {
        val lengthMs: Long get() = endMs - startMs
    }

    fun fit(startMs: Long, endMs: Long, durationMs: Long): Range {
        val total = if (durationMs > 0) durationMs else Long.MAX_VALUE
        var start = startMs.coerceAtLeast(0)
        var length = (endMs - start).coerceIn(MIN_MS, MAX_MS)
        if (start + length > total) {
            start = (total - length).coerceAtLeast(0)
            length = minOf(length, total - start)
        }
        return Range(start, start + length)
    }

    fun forLines(firstStartMs: Long, lastEndMs: Long, offsetMs: Int, durationMs: Long, fixedLengthMs: Long? = null): Range {
        val start = firstStartMs - offsetMs - LEAD_IN_MS
        val end = if (fixedLengthMs != null) start + fixedLengthMs else lastEndMs - offsetMs + TAIL_MS
        return fit(start, end, durationMs)
    }

    fun label(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
