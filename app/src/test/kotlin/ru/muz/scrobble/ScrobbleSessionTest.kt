package ru.muz.scrobble

import org.junit.Assert.*
import org.junit.Test

class ScrobbleSessionTest {
    private val track = ScrobbleTrack("1", "Song", "Artist", 180_000)
    private fun play(session: ScrobbleSession, seconds: Int, duration: Long = track.durationMs) =
        (0 until seconds).flatMap { session.tick(1000, duration, 1700000000L + it, false) }

    @Test fun halfATrackSubmitsOnce() {
        val session = ScrobbleSession { "play" }
        session.start(track)
        val events = play(session, 180)
        assertEquals(1, events.filterIsInstance<ScrobbleEvent.NowPlaying>().size)
        assertEquals(1, events.filterIsInstance<ScrobbleEvent.Submit>().size)
        assertEquals(1699999999L, events.filterIsInstance<ScrobbleEvent.Submit>().single().play.startedAt)
    }

    @Test fun longTracksSubmitAfterFourMinutes() {
        val session = ScrobbleSession()
        session.start(track.copy(durationMs = 900_000))
        assertTrue(play(session, 239, 900_000).none { it is ScrobbleEvent.Submit })
        assertTrue(play(session, 1, 900_000).any { it is ScrobbleEvent.Submit })
    }

    @Test fun shortSkipsAndUnknownDurationsDoNotSubmit() {
        for (duration in listOf(0L, 30_000L)) {
            val session = ScrobbleSession()
            session.start(track.copy(durationMs = duration))
            assertTrue(play(session, 100, duration).none { it is ScrobbleEvent.Submit })
        }
        val session = ScrobbleSession()
        session.start(track)
        assertTrue(play(session, 12).none { it is ScrobbleEvent.Submit })
    }

    @Test fun privatePlaybackIsSuppressedForTheWholePlay() {
        val session = ScrobbleSession()
        session.start(track)
        play(session, 20)
        assertTrue(session.tick(1000, 180_000, 1700000021, true).isEmpty())
        assertTrue(play(session, 180).isEmpty())
    }

    @Test fun repeatsGetSeparatePlayIdsAndPausedWallTimeIsNotCounted() {
        var next = 0
        val session = ScrobbleSession { "${next++}" }
        session.start(track)
        val first = play(session, 90).filterIsInstance<ScrobbleEvent.Submit>().single()
        session.start(track)
        val resumed = session.tick(1000, 180_000, 1700100000, false)
        assertTrue(resumed.none { it is ScrobbleEvent.Submit })
        val second = play(session, 89).filterIsInstance<ScrobbleEvent.Submit>().single()
        assertNotEquals(first.play.id, second.play.id)
    }
}
