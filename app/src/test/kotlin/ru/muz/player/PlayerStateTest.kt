package ru.muz.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerStateTest {
    @Test
    fun `duration is formatted the way the reference player shows it`() {
        assertEquals("0:24", formatDuration(24_000))
        assertEquals("2:50", formatDuration(170_000))
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9_400))
    }

    @Test
    fun `an hour long track gets an hours field`() {
        assertEquals("1:02:03", formatDuration(3_723_000))
    }

    @Test
    fun `a negative position does not produce a negative clock`() {
        assertEquals("0:00", formatDuration(-1))
    }

    @Test
    fun `progress is zero while the duration is still unknown`() {
        assertEquals(0f, PlayerState(positionMs = 5_000, durationMs = 0).progress, 0.0001f)
        assertEquals(0f, PlayerState(positionMs = 5_000, durationMs = -1).progress, 0.0001f)
    }

    @Test
    fun `progress is clamped when the position runs past the duration`() {
        assertEquals(1f, PlayerState(positionMs = 200_000, durationMs = 170_000).progress, 0.0001f)
    }

    @Test
    fun `progress is the fraction played`() {
        assertEquals(0.5f, PlayerState(positionMs = 85_000, durationMs = 170_000).progress, 0.0001f)
    }
}
