package ru.muz.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsFlowTest {
    private fun line(start: Long, end: Long, text: String, words: List<LyricWord> = emptyList()) = LyricLine(start, end, text, words)

    @Test
    fun longIntroBecomesPause() {
        val items = LyricsFlow.build(Lyrics(listOf(line(12_000, 15_000, "Первая"), line(15_000, 18_000, "Вторая")), true))
        assertTrue(items.first() is FlowItem.Pause)
        assertEquals(3, items.size)
        assertEquals(0, LyricsFlow.activeAt(items, 5_000))
        assertEquals(1, LyricsFlow.activeAt(items, 12_500))
    }

    @Test
    fun shortIntroHasNoPause() {
        val items = LyricsFlow.build(Lyrics(listOf(line(2_000, 5_000, "Первая")), true))
        assertTrue(items.single() is FlowItem.Line)
        assertEquals(-1, LyricsFlow.activeAt(items, 1_000))
    }

    @Test
    fun blankLineIsPause() {
        val items = LyricsFlow.build(
            Lyrics(listOf(line(1_000, 5_000, "Раз"), line(5_000, 20_000, ""), line(20_000, 24_000, "Два")), true),
        )
        assertEquals(listOf("line", "pause", "line"), items.map { if (it is FlowItem.Pause) "pause" else "line" })
        val pause = items[1] as FlowItem.Pause
        assertEquals(5_000L, pause.startMs)
        assertEquals(20_000L, pause.endMs)
        assertEquals(0.5f, pause.progressAt(12_500), 0.01f)
    }

    @Test
    fun wordTimingRevealsGap() {
        val words = listOf(LyricWord(1_000, 2_000, "Раз"))
        val items = LyricsFlow.build(Lyrics(listOf(line(1_000, 15_000, "Раз", words), line(15_000, 18_000, "Два")), true))
        assertEquals(3, items.size)
        assertEquals(2_000L, items[1].startMs)
        assertEquals(1, LyricsFlow.activeAt(items, 9_000))
        assertEquals(2, LyricsFlow.activeAt(items, 15_100))
    }

    @Test
    fun unsyncedKeepsAllLines() {
        val items = LyricsFlow.build(Lyrics(listOf(line(0, 0, "а"), line(0, 0, "б")), false))
        assertEquals(2, items.size)
    }
}
