package ru.muz.lyrics

import java.nio.file.Files
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import ru.muz.yandex.YandexClient

class InternetLyricsTest {
    private val track = YandexClient.Track("internet", "", "Song", "Artist", "Album", 120000, null, false, false)
    private val body = """{"track":{"title":"Song","artist":"Artist","duration":120},"meta":{"level":"line"},"lines":[{"text":"First line","start":1000},{"text":"Last line","start":2000}]}"""

    @Test fun decodesSyncedAndPlainWithoutInventingTimes() {
        val text = InternetLyrics.decode(body, track)!!
        assertTrue(LrcParser.parse(text).synced)
        assertEquals(1000L, LrcParser.parse(text).lines.first().startMs)
        val plain = InternetLyrics.decode(body.replace("\"line\"", "\"none\""), track)!!
        assertFalse(LrcParser.parse(plain).synced)
        assertEquals("First line", LrcParser.parse(plain).lines.first().text)
    }

    @Test fun omittedZeroTimestampsKeepTheFirstLine() {
        val text = InternetLyrics.decode(body.replace(",\"start\":1000", ""), track)!!
        assertEquals(0L, LrcParser.parse(text).lines.first().startMs)
        assertEquals(2, LrcParser.parse(text).lines.size)
    }

    @Test fun rejectsAnotherArtistAndWrongRecordingDuration() {
        assertNull(InternetLyrics.decode(body.replace("\"Artist\"", "\"Someone else\""), track))
        assertNull(InternetLyrics.decode(body.replace("\"duration\":120", "\"duration\":240"), track))
        assertNull(InternetLyrics.decode(body.replace("\"Song\"", "\"Song live\""), track))
    }

    @Test fun internetFallbackRunsOnlyAfterPrimarySourcesAndIsCached() = runBlocking {
        val hosts = mutableListOf<String>()
        val stages = mutableListOf<String>()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            synchronized(hosts) { hosts += chain.request().url.host }
            val response = if (chain.request().url.host == "api.lrcmux.dev") 200 to body else 404 to "{}"
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(response.first).message("fixture").body(response.second.toResponseBody()).build()
        }.build()
        val repository = LyricsRepository(YandexClient("test", http), Files.createTempDirectory("internet-lyrics").toFile(), http)
        assertTrue(repository.load(track, onStage = { stages += it }).getOrThrow().synced)
        assertEquals("api.lrcmux.dev", hosts.last())
        assertTrue(stages.last().contains("интернете"))
        val before = hosts.size
        repository.load(track).getOrThrow()
        assertEquals(before, hosts.size)
    }

    @Test fun transientFallbackFailuresAreNotCachedAsMissing() = runBlocking {
        var fallbackRequests = 0
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val fallback = chain.request().url.host == "api.lrcmux.dev"
            if (fallback) fallbackRequests++
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(if (fallback) 503 else 404)
                .message("fixture").body("{}".toResponseBody()).build()
        }.build()
        val repository = LyricsRepository(YandexClient("test", http), Files.createTempDirectory("internet-lyrics").toFile(), http)
        assertTrue(repository.load(track).isFailure)
        assertTrue(repository.load(track).isFailure)
        assertEquals(2, fallbackRequests)
    }
}
