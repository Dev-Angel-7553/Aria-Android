package ru.muz.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackClockTest {
    private val playing = PlayerState(positionMs = 10_000, durationMs = 60_000,
        isPlaying = true, sampledAtMs = 1_000)

    @Test fun `cursor advances between samples and stops on pause or buffering`() {
        assertEquals(10_125L, playing.positionAt(1_125))
        assertEquals(10_000L, playing.copy(isPlaying = false).positionAt(1_125))
        assertEquals(10_000L, playing.copy(isBuffering = true).positionAt(1_125))
    }

    @Test fun `short backward seeks take effect immediately`() {
        val sought = playing.copy(positionMs = 9_750, discontinuity = 1, sampledAtMs = 1_125)
        assertEquals(9_750L, sought.positionAt(1_125))
        assertEquals(9_800L, sought.positionAt(1_175))
    }

    @Test fun `stale samples do not run indefinitely and track duration is respected`() {
        assertEquals(10_500L, playing.positionAt(10_000))
        assertEquals(10_050L, playing.copy(durationMs = 10_050).positionAt(1_125))
        assertEquals(10_000L, playing.positionAt(900))
    }
}
