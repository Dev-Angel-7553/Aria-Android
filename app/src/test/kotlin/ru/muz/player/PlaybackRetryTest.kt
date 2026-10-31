package ru.muz.player

import org.junit.Assert.*
import org.junit.Test

class PlaybackRetryTest {
    @Test
    fun retriesTransientFailuresOnlyThreeTimesWithBackoff() {
        assertEquals(listOf(1000L, 2000L, 4000L, null), (0..3).map { PlaybackRetry.delayMs(2001, it) })
        assertNotNull(PlaybackRetry.delayMs(2004, 0, 503))
        assertNotNull(PlaybackRetry.delayMs(2004, 0, 429))
        assertNull(PlaybackRetry.delayMs(2004, 0, 401))
        assertNull(PlaybackRetry.delayMs(2004, 0, 403))
        assertNull(PlaybackRetry.delayMs(2004, 0, 404))
        assertNull(PlaybackRetry.delayMs(4005, 0))
    }
}
