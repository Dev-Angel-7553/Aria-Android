package ru.muz.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {
    @Test fun `word starts retain their delay relative to the line and repeats`() {
        val lyrics = LrcParser.parse("[00:10.00][00:20.00]<00:10.50>one<00:11.50>")
        assertEquals(10_500L, lyrics.lines[0].words.single().startMs)
        assertEquals(11_500L, lyrics.lines[0].words.single().endMs)
        assertEquals(20_500L, lyrics.lines[1].words.single().startMs)
        assertEquals(21_500L, lyrics.lines[1].words.single().endMs)
        assertEquals(0f, lyrics.lines[0].progressAt(10_250), 0.001f)
        assertEquals(0.5f, lyrics.lines[0].progressAt(11_000), 0.001f)
    }

    @Test fun `untimed prefix and file offset are preserved`() {
        val line = LrcParser.parse("[offset:-250]\n[00:10.00]hello <00:11.00>world<00:12.00>").lines.single()
        assertEquals("hello world", line.text)
        assertEquals(9_750L, line.startMs)
        assertEquals(10_750L, line.words[1].startMs)
        assertEquals(11_750L, line.words[1].endMs)
    }

    @Test fun `plain lyrics never highlight the final line as if it were synchronized`() {
        val lyrics = LrcParser.parse("[ar:Artist]\none\ntwo")
        assertEquals(2, lyrics.lines.size)
        assertEquals(-1, lyrics.activeIndexAt(10_000))
    }

    @Test fun `backward seek before intro clears the active line`() {
        val lyrics = LrcParser.parse("[00:10.00]one\n[00:10.50]two")
        assertEquals(1, lyrics.activeIndexAt(10_600))
        assertEquals(0, lyrics.activeIndexAt(10_400))
        assertEquals(-1, lyrics.activeIndexAt(9_900))
    }

    @Test
    fun `plain lrc gives line timings and no word timings`() {
        val lyrics = LrcParser.parse(
            """
            [ar:Дора]
            [00:18.00]Верю, верю, верю
            [00:20.50]Завтра будет всё хорошо
            """.trimIndent()
        )

        assertTrue(lyrics.synced)
        assertEquals(2, lyrics.lines.size)
        assertEquals(18_000L, lyrics.lines[0].startMs)
        assertEquals("Верю, верю, верю", lyrics.lines[0].text)
        assertFalse(lyrics.lines[0].hasWordTiming)
        assertEquals(20_500L, lyrics.lines[0].endMs)
    }

    @Test
    fun `metadata rows are not lyrics`() {
        val lyrics = LrcParser.parse(
            """
            [ti:Верю]
            [ar:Дора]
            [length:02:50]
            [00:18.00]Верю
            """.trimIndent()
        )
        assertEquals(1, lyrics.lines.size)
        assertEquals("Верю", lyrics.lines[0].text)
    }

    @Test
    fun `enhanced lrc gives word timings and the text keeps its spacing`() {
        val lyrics = LrcParser.parse("[00:18.00]<00:18.00>Верю, <00:18.45>верю, <00:18.90>верю")

        val line = lyrics.lines.single()
        assertTrue(line.hasWordTiming)
        assertEquals(3, line.words.size)
        assertEquals("Верю, верю, верю", line.text)
        assertEquals(18_000L, line.words[0].startMs)
        assertEquals(18_450L, line.words[1].startMs)
        assertEquals(18_450L, line.words[0].endMs)
    }

    @Test
    fun `fractions of different lengths all read as milliseconds`() {
        val lyrics = LrcParser.parse(
            """
            [00:01.5]a
            [00:02.50]b
            [00:03.500]c
            """.trimIndent()
        )
        assertEquals(1_500L, lyrics.lines[0].startMs)
        assertEquals(2_500L, lyrics.lines[1].startMs)
        assertEquals(3_500L, lyrics.lines[2].startMs)
    }

    @Test
    fun `a repeated line becomes one entry per timestamp with word timings shifted`() {
        val lyrics = LrcParser.parse("[00:10.00][00:20.00]<00:10.00>Верю <00:10.50>снова")

        assertEquals(2, lyrics.lines.size)
        assertEquals(10_000L, lyrics.lines[0].startMs)
        assertEquals(20_000L, lyrics.lines[1].startMs)
        assertEquals(20_000L, lyrics.lines[1].words[0].startMs)
        assertEquals(20_500L, lyrics.lines[1].words[1].startMs)
    }

    @Test
    fun `text without timestamps is kept but reported unsynced`() {
        val lyrics = LrcParser.parse("Верю, верю, верю\nЗавтра будет всё хорошо")

        assertFalse(lyrics.synced)
        assertEquals(2, lyrics.lines.size)
        assertEquals("Верю, верю, верю", lyrics.lines[0].text)
    }

    @Test
    fun `lines out of order are sorted`() {
        val lyrics = LrcParser.parse("[00:20.00]second\n[00:10.00]first")
        assertEquals("first", lyrics.lines[0].text)
        assertEquals("second", lyrics.lines[1].text)
    }

    @Test
    fun `explicit blank timed lines remain as stable rhythm spacers`() {
        val lyrics = LrcParser.parse("[00:01.00]one\n[00:02.00]\n[00:03.00]three")
        assertEquals(3, lyrics.lines.size)
        assertEquals("", lyrics.lines[1].text)
        assertEquals(3_000L, lyrics.lines[1].endMs)
    }

    @Test
    fun `short and overlapping timings still produce positive line spans`() {
        val lyrics = LrcParser.parse("[00:01.00]one\n[00:01.00]repeat\n[00:01.01]next")
        assertTrue(lyrics.lines.all { it.endMs > it.startMs })
    }

    @Test
    fun `stale playback callback cannot rewind karaoke cursor`() {
        assertEquals(10_000L, stablePlaybackPosition(9_400L, 10_000L))
        assertEquals(8_000L, stablePlaybackPosition(8_000L, 10_000L))
    }

    @Test
    fun `forward playback callbacks advance stable cursor`() {
        assertEquals(10_500L, stablePlaybackPosition(10_500L, 10_000L))
    }
}
