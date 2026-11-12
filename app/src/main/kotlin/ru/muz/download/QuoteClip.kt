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

    fun segments(lineStarts: List<Long>, offsetMs: Int, range: Range, minMs: Long = 150): List<Pair<Int, Long>> {
        if (lineStarts.isEmpty() || range.lengthMs <= 0) return emptyList()
        val bounds = ArrayList<Long>(lineStarts.size + 1)
        bounds += range.startMs
        var floor = range.startMs
        lineStarts.drop(1).forEach { start ->
            floor = (start - offsetMs).coerceIn(floor, range.endMs)
            bounds += floor
        }
        bounds += range.endMs
        val out = mutableListOf<Pair<Int, Long>>()
        for (index in lineStarts.indices) {
            val length = bounds[index + 1] - bounds[index]
            if (length <= 0) continue
            if (length < minMs && out.isNotEmpty()) {
                val last = out.removeAt(out.lastIndex)
                out += last.first to last.second + length
            } else {
                out += index to length
            }
        }
        if (out.size > 1 && out.first().second < minMs) {
            val first = out.removeAt(0)
            val next = out.removeAt(0)
            out.add(0, next.first to first.second + next.second)
        }
        return out
    }

    fun <S> sample(clip: Range, stepMs: Long, state: (Long) -> S): List<Pair<S, Long>> {
        val out = mutableListOf<Pair<S, Long>>()
        if (clip.lengthMs <= 0 || stepMs <= 0) return out
        var at = 0L
        while (at < clip.lengthMs) {
            val length = minOf(stepMs, clip.lengthMs - at)
            val value = state(clip.startMs + at)
            if (out.isNotEmpty() && out.last().first == value) {
                out[out.lastIndex] = value to out.last().second + length
            } else {
                out += value to length
            }
            at += length
        }
        return out
    }

    fun label(ms: Long): String {
        val seconds = (ms / 1000).coerceAtLeast(0)
        return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    }
}
