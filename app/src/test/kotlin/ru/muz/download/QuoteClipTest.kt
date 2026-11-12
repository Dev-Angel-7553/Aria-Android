package ru.muz.download

import org.junit.Assert.assertEquals
import org.junit.Test

class QuoteClipTest {
    @Test
    fun linesGetLeadInAndTail() {
        val range = QuoteClip.forLines(60_000, 68_000, 0, 200_000)
        assertEquals(59_600L, range.startMs)
        assertEquals(68_900L, range.endMs)
    }

    @Test
    fun shortSelectionStretchesToMinimum() {
        val range = QuoteClip.forLines(10_000, 11_000, 0, 200_000)
        assertEquals(QuoteClip.MIN_MS, range.lengthMs)
    }

    @Test
    fun longSelectionIsCapped() {
        val range = QuoteClip.forLines(10_000, 90_000, 0, 200_000)
        assertEquals(QuoteClip.MAX_MS, range.lengthMs)
    }

    @Test
    fun offsetShiftsAudio() {
        val range = QuoteClip.forLines(60_000, 68_000, 1_000, 200_000)
        assertEquals(58_600L, range.startMs)
    }

    @Test
    fun clipNearEndSlidesBack() {
        val range = QuoteClip.forLines(195_000, 199_000, 0, 200_000, fixedLengthMs = 15_000)
        assertEquals(200_000L, range.endMs)
        assertEquals(15_000L, range.lengthMs)
    }

    @Test
    fun startNeverNegative() {
        val range = QuoteClip.forLines(100, 3_000, 0, 200_000)
        assertEquals(0L, range.startMs)
        assertEquals(QuoteClip.MIN_MS, range.lengthMs)
    }

    @Test
    fun labelFormatsMinutes() {
        assertEquals("1:05", QuoteClip.label(65_400))
    }

    @Test
    fun segmentsFollowLineStartsAndFillTheClip() {
        val range = QuoteClip.Range(9_600, 20_000)
        val parts = QuoteClip.segments(listOf(10_000, 13_000, 16_500), 0, range)
        assertEquals(listOf(0 to 3_400L, 1 to 3_500L, 2 to 3_500L), parts)
        assertEquals(range.lengthMs, parts.sumOf { it.second })
    }

    @Test
    fun segmentsStopAtClipEndAndMergeTinyParts() {
        val range = QuoteClip.Range(0, 15_000)
        val parts = QuoteClip.segments(listOf(0, 50, 9_000, 40_000), 0, range)
        assertEquals(range.lengthMs, parts.sumOf { it.second })
        assertEquals(listOf(1, 2), parts.map { it.first })
    }

    @Test
    fun samplingMergesEqualFramesAndKeepsTheClipLength() {
        val clip = QuoteClip.Range(1_000, 2_050)
        val frames = QuoteClip.sample(clip, 100) { song -> if (song < 1_500) "a" else if (song < 1_700) "b" else "c" }
        assertEquals(listOf("a" to 500L, "b" to 200L, "c" to 350L), frames)
        assertEquals(clip.lengthMs, frames.sumOf { it.second })
        assertEquals(emptyList<Pair<String, Long>>(), QuoteClip.sample(QuoteClip.Range(5, 5), 100) { "x" })
    }
}
