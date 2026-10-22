package ru.muz.lyrics

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import ru.muz.yandex.YandexClient

class LyricsRepositoryTest {
    private val track = YandexClient.Track("1", "", "Song", "Artist", "", 120_000, null, false, true)

    @Test fun `null synced lyrics fall back to plain text and successful results are cached`() = runBlocking {
        var requests = 0
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            requests++
            val yandex = chain.request().url.host == "api.music.yandex.net"
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(if (yandex) 404 else 200).message("fixture")
                .body((if (yandex) "{}" else """{"syncedLyrics":null,"plainLyrics":"one\ntwo"}""").toResponseBody())
                .build()
        }.build()
        val repository = LyricsRepository(YandexClient("test", http), http)
        val lyrics = repository.load(track).getOrThrow()
        assertFalse(lyrics.synced)
        assertEquals(listOf("one", "two"), lyrics.lines.map { it.text })
        val before = requests
        assertEquals(lyrics, repository.load(track).getOrThrow())
        assertEquals(before, requests)
    }

    @Test fun `cancelled lyrics request never becomes an error result or starts fallback`() = runBlocking {
        var requests = 0
        val http = OkHttpClient.Builder().addInterceptor {
            requests++
            throw CancellationException("track changed")
        }.build()
        val repository = LyricsRepository(YandexClient("test", http), http)
        try {
            repository.load(track)
            fail("Cancellation should propagate")
        } catch (_: CancellationException) {
            assertEquals(1, requests)
        }
    }
}
