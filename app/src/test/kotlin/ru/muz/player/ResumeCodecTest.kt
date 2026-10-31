package ru.muz.player

import org.junit.Assert.*
import org.junit.Test
import ru.muz.yandex.PlaySource
import ru.muz.yandex.YandexClient

class ResumeCodecTest {
    @Test
    fun restoresWholeLargeQueueWithDuplicatesAndCollaborators() {
        val artists = listOf(YandexClient.Artist("a", "Tyler, The Creator", null, 0), YandexClient.Artist("b", "A & B", null, 0))
        val tracks = (0 until 2500).map { PlaybackController.YandexTrack((it % 1100).toString(), "Song $it", "Artists", null, artists = artists) }
        val source = PlaySource(from = "radio", playlistId = "p", wave = true, station = "artist:a", sessionId = "session")
        val snapshot = ResumeSnapshot(tracks, List(tracks.size) { source }, 1750, 25_000L)
        val restored = ResumeCodec.decode(ResumeCodec.encode(snapshot))!!
        assertEquals(2500, restored.tracks.size)
        assertEquals(1750, restored.index)
        assertEquals("Song 2499", restored.tracks.last().title)
        assertEquals(listOf("Tyler, The Creator", "A & B"), restored.tracks.first().artists.map { it.name })
        assertEquals(source, restored.sources.last())
        assertEquals(25_000L, restored.positionMs)
    }

    @Test
    fun readsOldSnapshotsAndClampsDamagedPosition() {
        val result = ResumeCodec.decode("""{"index":99,"position":-5,"items":[{"id":"1","title":"Old","artist":"A","from":"library"}]}""")!!
        assertEquals(0, result.index)
        assertEquals(0L, result.positionMs)
        assertTrue(result.tracks.single().artists.isEmpty())
        assertEquals("library", result.sources.single().from)
        assertNull(ResumeCodec.decode("broken"))
        assertNull(ResumeCodec.decode("""{"items":[]}"""))
        assertNull(ResumeCodec.decode("""{"items":[{"title":"Missing ID"}]}"""))
    }
}
