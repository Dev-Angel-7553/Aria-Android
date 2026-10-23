package ru.muz.player

import org.junit.Assert.assertEquals
import org.junit.Test

class LoudnessTest {
    @Test
    fun loudTrackIsTurnedDown() {
        assertEquals(-6f, Loudness.gainDb(-8f), 0.001f)
    }

    @Test
    fun quietTrackIsLeftAlone() {
        assertEquals(0f, Loudness.gainDb(-20f), 0.001f)
    }

    @Test
    fun cutIsLimited() {
        assertEquals(Loudness.MAX_CUT_DB, Loudness.gainDb(-1f), 0.001f)
    }

    @Test
    fun brokenValuesAreIgnored() {
        assertEquals(0f, Loudness.gainDb(Float.NaN), 0.001f)
        assertEquals(0f, Loudness.gainDb(3f), 0.001f)
        assertEquals(0f, Loudness.gainDb(-90f), 0.001f)
    }
}
