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
}
