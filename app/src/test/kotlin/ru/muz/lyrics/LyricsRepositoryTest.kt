package ru.muz.lyrics

import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import ru.muz.yandex.YandexClient

class LyricsRepositoryTest {
    private val track = YandexClient.Track("1", "", "Song", "Artist", "", 120_000, null, false, true)

    private fun client(requests: AtomicInteger, answer: (Interceptor.Chain) -> Pair<Int, String>) =
        OkHttpClient.Builder().addInterceptor { chain ->
            requests.incrementAndGet()
            val (code, body) = answer(chain)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                .code(code).message("fixture").body(body.toResponseBody()).build()
        }.build()

    private fun lrclibOnly(requests: AtomicInteger) = client(requests) { chain ->
        if (chain.request().url.host == "api.music.yandex.net") 404 to "{}"
        else 200 to """{"syncedLyrics":null,"plainLyrics":"one\ntwo"}"""
    }

    @Test fun `null synced lyrics fall back to plain text and successful results are cached`() = runBlocking {
        val requests = AtomicInteger()
        val http = lrclibOnly(requests)
        val repository = LyricsRepository(YandexClient("test", http), http = http)
        val lyrics = repository.load(track).getOrThrow()
        assertFalse(lyrics.synced)
        assertEquals(listOf("one", "two"), lyrics.lines.map { it.text })
        val before = requests.get()
        assertEquals(lyrics, repository.load(track).getOrThrow())
        assertEquals(before, requests.get())
    }

    @Test fun `lyrics stored on disk load without network`() = runBlocking {
        val dir = Files.createTempDirectory("lyrics").toFile()
        val first = lrclibOnly(AtomicInteger())
        LyricsRepository(YandexClient("test", first), dir, first).load(track).getOrThrow()
        val requests = AtomicInteger()
        val offline = client(requests) { 500 to "" }
        val lyrics = LyricsRepository(YandexClient("test", offline), dir, offline).load(track).getOrThrow()
        assertEquals(listOf("one", "two"), lyrics.lines.map { it.text })
        assertEquals(0, requests.get())
    }

    @Test fun `missing lyrics are remembered until a refresh`() = runBlocking {
        val dir = Files.createTempDirectory("lyrics").toFile()
        val requests = AtomicInteger()
        val http = client(requests) { 404 to "{}" }
        val repository = LyricsRepository(YandexClient("test", http), dir, http)
        assertTrue(repository.load(track).isFailure)
        val before = requests.get()
        assertTrue(before > 0)
        assertTrue(repository.load(track).isFailure)
        assertEquals(before, requests.get())
        assertTrue(repository.load(track, refresh = true).isFailure)
        assertTrue(requests.get() > before)
    }

    @Test fun `server errors are not remembered as missing`() = runBlocking {
        val dir = Files.createTempDirectory("lyrics").toFile()
        val requests = AtomicInteger()
        val http = client(requests) { 503 to "" }
        val repository = LyricsRepository(YandexClient("test", http), dir, http)
        assertTrue(repository.load(track).isFailure)
        val before = requests.get()
        assertTrue(repository.load(track).isFailure)
        assertTrue(requests.get() > before)
    }
}
