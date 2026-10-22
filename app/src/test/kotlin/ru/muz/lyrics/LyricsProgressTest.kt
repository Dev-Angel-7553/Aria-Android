package ru.muz.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsProgressTest {
    private val line = LrcParser.parse(
        "[00:10.00]<00:10.00>Верю, <00:10.50>верю, <00:11.00>верю"
    ).lines.single()

    @Test
    fun `progress is zero before the line and one after it`() {
        assertEquals(0f, line.progressAt(9_000), 0.001f)
        assertEquals(1f, line.progressAt(30_000), 0.001f)
    }

    @Test
    fun `progress never goes backwards`() {
        var previous = 0f
        var t = 9_500L
        while (t <= 12_500L) {
            val now = line.progressAt(t)
            assertTrue("progress fell at ${t}ms: $previous -> $now", now >= previous - 0.0001f)
            previous = now
            t += 25
        }
    }

    @Test
    fun `the boundary lands inside a word rather than jumping over it`() {
        val mid = line.progressAt(10_250)
        assertTrue("expected a fraction inside the first word, got $mid", mid > 0.05f && mid < 0.35f)
    }

    @Test
    fun `progress is measured in characters so a long word takes proportionally longer`() {
        val uneven = LrcParser.parse("[00:00.00]<00:00.00>я <00:01.00>довериться").lines.single()
        val afterShortWord = uneven.progressAt(1_000)
        assertTrue(
            "the 1-char word should take about 1/11 of the width, got $afterShortWord",
            afterShortWord < 0.2f
        )
    }

    @Test
    fun `a line without word timings still fills evenly over its own span`() {
        val plain = LrcParser.parse("[00:00.00]abc\n[00:10.00]def").lines.first()
        assertEquals(0.5f, plain.progressAt(5_000), 0.01f)
    }

    @Test
    fun `active line is found by position`() {
        val lyrics = LrcParser.parse(
            """
            [00:10.00]one
            [00:20.00]two
            [00:30.00]three
            """.trimIndent()
        )
        assertEquals(-1, lyrics.activeIndexAt(5_000))
        assertEquals(0, lyrics.activeIndexAt(10_000))
        assertEquals(0, lyrics.activeIndexAt(19_999))
        assertEquals(1, lyrics.activeIndexAt(20_000))
        assertEquals(2, lyrics.activeIndexAt(999_000))
    }
}
