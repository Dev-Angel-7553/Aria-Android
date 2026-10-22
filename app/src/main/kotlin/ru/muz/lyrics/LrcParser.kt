package ru.muz.lyrics

object LrcParser {
    private val LINE_TIME = Regex("""\[(\d{1,3}):(\d{2})([.:]\d{1,3})?]""")
    private val WORD_TIME = Regex("""<(\d{1,3}):(\d{2})([.:]\d{1,3})?>""")
    private val OFFSET = Regex("""\[offset:([+-]?\d+)]""", RegexOption.IGNORE_CASE)
    private val METADATA = Regex("""\[[a-zA-Z]+:.*]""")

    fun parse(raw: String): Lyrics {
        val parsed = mutableListOf<LyricLine>()
        var sawTimestamp = false
        val offset = OFFSET.find(raw)?.groupValues?.get(1)?.toLongOrNull() ?: 0L

        for (rawLine in raw.lineSequence()) {
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isEmpty()) continue

            val stamps = LINE_TIME.findAll(line).toList()
            if (stamps.isEmpty()) continue
            if (stamps.first().range.first != 0) continue

            sawTimestamp = true
            val body = line.substring(stamps.last().range.last + 1)
            val words = parseWords(body, millisOf(stamps.first()))
            val text = if (words.isEmpty()) body.trim() else words.joinToString("") { it.text }

            for (stamp in stamps) {
                val start = (millisOf(stamp) + offset).coerceAtLeast(0)
                val shifted = if (words.isEmpty()) {
                    emptyList()
                } else {
                    val delta = millisOf(stamp) - millisOf(stamps.first()) + offset
                    words.map { it.copy(startMs = (it.startMs + delta).coerceAtLeast(0), endMs = (it.endMs + delta).coerceAtLeast(1)) }
                }
                parsed += LyricLine(startMs = start, endMs = start, text = text, words = shifted)
            }
        }

        if (!sawTimestamp) {
            val lines = raw.lineSequence()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !METADATA.matches(it) }
                .map { LyricLine(startMs = 0, endMs = 0, text = it) }
                .toList()
            return Lyrics(lines, synced = false)
        }

        parsed.sortBy { it.startMs }
        return Lyrics(closeEnds(parsed), synced = true)
    }

    private fun closeEnds(lines: List<LyricLine>): List<LyricLine> =
        lines.mapIndexed { i, line ->
            val next = lines.getOrNull(i + 1)?.startMs
            val fromWords = line.words.lastOrNull()?.endMs
            val end = next ?: fromWords ?: (line.startMs + 4_000)
            line.copy(endMs = maxOf(end, line.startMs + 1))
        }

    private fun parseWords(body: String, lineStartMs: Long): List<LyricWord> {
        val stamps = WORD_TIME.findAll(body).toList()
        if (stamps.isEmpty()) return emptyList()

        val words = mutableListOf<LyricWord>()
        val prefix = body.substring(0, stamps.first().range.first).trimStart()
        if (prefix.isNotEmpty()) {
            words += LyricWord(lineStartMs, maxOf(lineStartMs + 1, millisOf(stamps.first())), prefix)
        }
        for ((i, stamp) in stamps.withIndex()) {
            val from = stamp.range.last + 1
            val to = stamps.getOrNull(i + 1)?.range?.first ?: body.length
            if (from > to) continue
            val text = body.substring(from, to).let {
                if (i == stamps.lastIndex) it.trimEnd() else it
            }
            if (text.isEmpty()) continue
            val start = millisOf(stamp)
            val end = stamps.getOrNull(i + 1)?.let(::millisOf) ?: (start + 400)
            words += LyricWord(startMs = start, endMs = maxOf(start + 1, end), text = text)
        }

        return words
    }

    private fun millisOf(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val fraction = match.groupValues[3]
        val fractionMs = if (fraction.isEmpty()) {
            0L
        } else {
            fraction.drop(1).padEnd(3, '0').take(3).toLong()
        }
        return (minutes * 60 + seconds) * 1000 + fractionMs
    }
}
