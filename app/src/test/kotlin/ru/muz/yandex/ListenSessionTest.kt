package ru.muz.yandex

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListenSessionTest {
    private var ids = 0
    private val session = ListenSession(newPlayId = { "play-${++ids}" })

    private fun play(seconds: Int, startPosition: Long = 0) {
        var position = startPosition
        repeat(seconds) {
            position += 1_000
            session.tick(1_000, position)
        }
    }

    @Test
    fun quickSkipIsNotReportedToHistory() {
        session.start("10:20", "", 200_000, PlaySource.liked())
        play(12)
        assertEquals(emptyList<ListenEvent>(), session.start("11", "21", 180_000, PlaySource.liked()))
    }

    @Test
    fun thirtySecondsCountAsListen() {
        session.start("10", "20", 200_000, PlaySource.playlist("1", "3"))
        play(31)
        val events = session.finish()
        val listen = events.single() as ListenEvent.PlayAudio
        assertEquals("10", listen.trackId)
        assertEquals("20", listen.albumId)
        assertEquals("1:3", listen.playlistId)
        assertEquals("play-1", listen.playId)
        assertEquals(31.0, listen.playedSeconds, 0.001)
        assertEquals(200.0, listen.trackLengthSeconds, 0.001)
    }

    @Test
    fun albumIdIsTakenFromCompositeTrackId() {
        session.start("10:20", "", 100_000, PlaySource.search())
        play(40)
        assertEquals("20", (session.finish().single() as ListenEvent.PlayAudio).albumId)
    }

    @Test
    fun shortTrackPlayedToTheEndCounts() {
        session.start("5", "6", 20_000, PlaySource.chart())
        play(19)
        assertTrue(session.finish().single() is ListenEvent.PlayAudio)
    }

    @Test
    fun waveAnnouncesSessionOnceAndSendsSkipOrFinish() {
        val wave = PlaySource.wave()
        val first = session.start("1", "9", 60_000, wave)
        assertEquals(
            listOf(ListenEvent.WaveStarted(wave.from), ListenEvent.WaveTrackStarted("1", "9")),
            first,
        )
        play(5)
        val second = session.start("2", "9", 60_000, wave)
        assertEquals(
            listOf(
                ListenEvent.WaveTrackEnded("1", "9", 5.0, skipped = true),
                ListenEvent.WaveTrackStarted("2", "9"),
            ),
            second,
        )
        play(59)
        val end = session.finish()
        assertTrue(end[0] is ListenEvent.PlayAudio)
        assertEquals(ListenEvent.WaveTrackEnded("2", "9", 59.0, skipped = false), end[1])
    }

    @Test
    fun newWaveStartIsAnnouncedAgain() {
        session.start("1", "9", 60_000, PlaySource.wave())
        val events = session.start("1", "9", 60_000, PlaySource.wave())
        assertTrue(events.any { it is ListenEvent.WaveStarted })
    }

    @Test
    fun repeatOneLoopIsANewListen() {
        session.start("7", "8", 40_000, PlaySource.liked())
        play(39)
        val looped = session.tick(1_000, 500)
        val listen = looped.single() as ListenEvent.PlayAudio
        assertEquals("play-1", listen.playId)
        play(35, startPosition = 500)
        assertEquals("play-2", (session.finish().single() as ListenEvent.PlayAudio).playId)
    }

    @Test
    fun seekingBackMidTrackIsNotALoop() {
        session.start("7", "8", 200_000, PlaySource.liked())
        play(60)
        assertEquals(emptyList<ListenEvent>(), session.tick(1_000, 0))
    }
}
