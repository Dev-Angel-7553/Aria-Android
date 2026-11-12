package ru.muz.yandex

import java.util.Collections
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackUploadTest {
    private class Seen(val method: String, val url: String, val auth: String?, val client: String?, val body: String)

    private fun client(answer: (Request, Int) -> Pair<Int, String>): Pair<YandexClient, MutableList<Seen>> {
        val seen = Collections.synchronizedList(mutableListOf<Seen>())
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val buffer = Buffer()
            request.body?.writeTo(buffer)
            seen += Seen(request.method, request.url.toString(), request.header("Authorization"), request.header("X-Yandex-Music-Client"), buffer.readUtf8())
            val (code, body) = if (request.url.encodedPath.endsWith("/account/status")) {
                200 to """{"result":{"account":{"uid":"7","login":"rita","displayName":"Рита"}}}"""
            } else answer(request, seen.size)
            Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(code).message("x")
                .body(body.toResponseBody("application/json".toMediaType())).build()
        }.build()
        return YandexClient("token", http, pause = {}) to seen
    }

    private fun file(name: String = "Песня.mp3", bytes: ByteArray = ByteArray(1000) { 1 }) =
        UploadFile(name, TrackUpload.mimeFor(name, null), bytes.size.toLong()) { bytes.inputStream() }

    @Test
    fun asksForTargetThenPostsFileAsMultipart() = runBlocking {
        val (yandex, seen) = client { request, _ ->
            when {
                request.url.encodedPath.endsWith("/loader/upload-url") ->
                    200 to """{"result":{"post-target":"https://upload.music.yandex.net/ugc/abc","ugc-track-id":"u-42"}}"""
                request.url.host == "upload.music.yandex.net" -> 200 to "CREATED"
                else -> 404 to "{}"
            }
        }
        var progress = 0f
        val id = yandex.uploadTrack("1005", file(), onProgress = { progress = it }).getOrThrow()
        assertEquals("u-42", id)
        assertEquals(1f, progress)
        val target = seen.first { it.url.contains("/loader/upload-url") }
        assertEquals("POST", target.method)
        assertTrue(target.url.contains("playlist-id=7%3A1005"))
        assertTrue(target.url.contains("uid=7"))
        assertEquals("OAuth token", target.auth)
        val upload = seen.first { it.url.startsWith("https://upload.music.yandex.net/") }
        assertEquals("OAuth token", upload.auth)
        assertTrue(upload.body.contains("name=\"file\"; filename=\"Песня.mp3\""))
    }

    @Test
    fun fallsBackToWebHostAndNeverSendsTokenOutsideYandex() = runBlocking {
        val (yandex, seen) = client { request, _ ->
            when {
                request.url.host == "api.music.yandex.net" -> 405 to """{"error":"method GET is not supported"}"""
                request.url.host == "api.music.yandex.ru" -> 200 to """{"post-target":"https://evil.example.com/up","ugc-track-id":"9"}"""
                else -> 200 to "ok"
            }
        }
        yandex.uploadTrack("3000", file()).getOrThrow()
        val web = seen.first { it.url.startsWith("https://api.music.yandex.ru/") }
        assertEquals("YandexMusicWebNext/1.0.0", web.client)
        assertNull(seen.first { it.url.startsWith("https://evil.example.com/") }.auth)
    }

    @Test
    fun retriesTemporaryServerErrors() = runBlocking {
        var uploads = 0
        val (yandex, _) = client { request, _ ->
            when {
                request.url.encodedPath.endsWith("/loader/upload-url") -> 200 to """{"result":{"post-target":"https://upload.music.yandex.net/x"}}"""
                else -> { uploads++; if (uploads < 3) 504 to "upstream request timeout" else 200 to "ok" }
            }
        }
        assertTrue(yandex.uploadTrack("1", file()).isSuccess)
        assertEquals(3, uploads)
    }

    @Test
    fun explainsLimitsAndBadFiles() = runBlocking {
        val (yandex, _) = client { _, _ -> 200 to """{"result":{"result":"too-many-files"}}""" }
        val limit = yandex.uploadTrack("1", file()).exceptionOrNull() as TrackUploadException
        assertEquals(TrackUploadException.Reason.LIMIT, limit.reason)
        val format = yandex.uploadTrack("1", file("clip.wav")).exceptionOrNull() as TrackUploadException
        assertEquals(TrackUploadException.Reason.FORMAT, format.reason)
        val big = UploadFile("big.flac", "audio/flac", TrackUpload.MAX_BYTES + 1) { ByteArray(0).inputStream() }
        assertEquals(TrackUploadException.Reason.TOO_LARGE, (yandex.uploadTrack("1", big).exceptionOrNull() as TrackUploadException).reason)
        assertEquals(TrackUploadException.Reason.FORMAT, TrackUpload.failure(415, "").reason)
    }

    @Test
    fun recognisesYandexHostsAndMimeTypes() {
        assertTrue(TrackUpload.isYandex("https://upload.music.yandex.net/a"))
        assertFalse(TrackUpload.isYandex("http://upload.music.yandex.net/a"))
        assertFalse(TrackUpload.isYandex("https://yandex.net.evil.com/a"))
        assertEquals("audio/flac", TrackUpload.mimeFor("a.FLAC", null))
        assertEquals("audio/x-custom", TrackUpload.mimeFor("a.mp3", "audio/x-custom"))
    }
}
