package ru.muz.yandex

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import ru.muz.player.ArtistMetadata

class ArtistAndDateTest {
    @Test
    fun preservesCollaboratorsWithoutSplittingPunctuationAndReadsPlaylistDates() = runBlocking {
        val client = YandexClient("token", OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body(
                """{"result":{"tracks":[{"timestamp":"2026-10-31T12:00:00Z","track":{"id":"7","title":"Song","artists":[{"id":"1","name":"Tyler, The Creator"},{"id":"2","name":"A & B"},{"id":"1","name":"Tyler, The Creator"}]}}]}}""".toResponseBody()
            ).build()
        }.build())
        val playlist = YandexClient.Playlist("p", "1", "2", "", "List", "", null, 1, "")
        val track = client.playlistTracks(playlist).getOrThrow().single()
        assertEquals(listOf("1", "2"), track.artists.map { it.id })
        assertEquals("Tyler, The Creator, A & B", track.artist)
        assertEquals(1_793_448_000_000L, track.addedAtMs)
        val restored = ArtistMetadata.decode(ArtistMetadata.encode(track.artists))
        assertEquals(track.artists, restored)
        assertTrue(ArtistMetadata.decode("corrupted").isEmpty())
    }
}
