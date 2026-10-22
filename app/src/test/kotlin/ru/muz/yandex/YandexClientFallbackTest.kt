package ru.muz.yandex

import java.util.Collections
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YandexClientFallbackTest {
    @Test
    fun `library falls back to post when old track request returns cover error`() = runBlocking {
        val requests = Collections.synchronizedList(mutableListOf<Pair<String, String>>())
        val contentType = "application/json".toMediaType()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requests += request.method to request.url.encodedPath
            val (code, body) = when {
                request.url.encodedPath.endsWith("/account/status") -> 200 to
                    """{"result":{"account":{"uid":"1","login":"rita","displayName":"Рита"}}}"""
                request.url.encodedPath.endsWith("/users/1/likes/tracks") -> 200 to
                    """{"result":{"library":{"tracks":[{"id":"11","albumId":"2"}]}}}"""
                request.url.encodedPath.endsWith("/tracks") && request.method == "GET" -> 400 to
                    """{"error":{"message":"cover doesn't exist"}}"""
                request.url.encodedPath.endsWith("/tracks") && request.method == "POST" -> 200 to
                    """{"result":[{"id":"11","title":"Трек","artists":[{"name":"Артист"}],"albums":[{"id":"2","title":"Альбом"}]}]}"""
                else -> 404 to "{}"
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Error")
                .body(body.toResponseBody(contentType))
                .build()
        }.build()

        val tracks = YandexClient("test-token", http, pause = {}).likedTracks().getOrThrow()

        assertEquals(listOf("11"), tracks.map { it.id })
        assertTrue(requests.contains("GET" to "/tracks"))
        assertTrue(requests.contains("POST" to "/tracks"))
    }

    @Test
    fun `large library loads every sequential batch beyond three hundred tracks`() = runBlocking {
        val trackRequests = Collections.synchronizedList(mutableListOf<List<String>>())
        val contentType = "application/json".toMediaType()
        val ids = (1..350).map(Int::toString)
        val shorts = ids.joinToString(",") { "{\"id\":\"$it\"}" }
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = when {
                request.url.encodedPath.endsWith("/account/status") ->
                    """{"result":{"account":{"uid":"1","login":"rita","displayName":"Рита"}}}"""
                request.url.encodedPath.endsWith("/users/1/likes/tracks") ->
                    """{"result":{"library":{"tracks":[$shorts]}}}"""
                request.url.encodedPath.endsWith("/tracks") -> {
                    val batch = request.url.queryParameter("trackIds").orEmpty().split(',').filter(String::isNotBlank)
                    trackRequests += batch
                    val tracks = batch.joinToString(",") {
                        "{\"id\":\"$it\",\"title\":\"Трек $it\",\"artists\":[{\"name\":\"Артист\"}]}"
                    }
                    """{"result":[$tracks]}"""
                }
                else -> "{}"
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody(contentType))
                .build()
        }.build()

        val tracks = YandexClient("test-token", http, pause = {}).likedTracks().getOrThrow()

        assertEquals(350, tracks.size)
        assertEquals(listOf(100, 100, 100, 50), trackRequests.map { it.size })
    }

    @Test
    fun `liked albums accept rich album wrappers`() = runBlocking {
        val contentType = "application/json".toMediaType()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = when {
                request.url.encodedPath.endsWith("/account/status") ->
                    """{"result":{"account":{"uid":"1","login":"rita","displayName":"Рита"}}}"""
                request.url.encodedPath.endsWith("/users/1/likes/albums") ->
                    """{"result":[{"id":"like-1","album":{"id":"77","title":"Альбом","artists":[{"name":"Артист"}],"trackCount":12}}]}"""
                else -> "{}"
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(200)
                .message("OK")
                .body(body.toResponseBody(contentType))
                .build()
        }.build()

        val albums = YandexClient("test-token", http, pause = {}).likedAlbums().getOrThrow()

        assertEquals(listOf("77"), albums.map { it.id })
        assertEquals("Альбом", albums.single().title)
    }

    @Test
    fun `library retries the same batch after throttling beyond six hundred tracks`() = runBlocking {
        val contentType = "application/json".toMediaType()
        val ids = (1..1_205).map(Int::toString)
        val shorts = ids.joinToString(",") { "{\"id\":\"$it\"}" }
        var throttledBatchAttempts = 0
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val batch = request.url.queryParameter("trackIds")
                .orEmpty().split(',').filter(String::isNotBlank)
            val (code, body) = when {
                request.url.encodedPath.endsWith("/account/status") -> 200 to
                    """{"result":{"account":{"uid":"1","login":"rita","displayName":"Рита"}}}"""
                request.url.encodedPath.endsWith("/users/1/likes/tracks") -> 200 to
                    """{"result":{"library":{"tracks":[$shorts]}}}"""
                request.method == "GET" && batch.firstOrNull() == "601" && throttledBatchAttempts++ == 0 ->
                    429 to """{"error":{"message":"too many requests"}}"""
                request.url.encodedPath.endsWith("/tracks") && request.method == "GET" -> {
                    val tracks = batch.joinToString(",") {
                        "{\"id\":\"$it\",\"title\":\"Трек $it\",\"artists\":[{\"name\":\"Артист\"}]}"
                    }
                    200 to """{"result":[$tracks]}"""
                }
                else -> 404 to "{}"
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Error")
                .body(body.toResponseBody(contentType))
                .build()
        }.build()

        val tracks = YandexClient("test-token", http, pause = {}).likedTracks().getOrThrow()

        assertEquals(1_205, tracks.size)
        assertEquals(2, throttledBatchAttempts)
    }

    @Test
    fun `playlists fall back to kinds and unwrap playlist objects`() = runBlocking {
        val requestedPaths = Collections.synchronizedList(mutableListOf<String>())
        val contentType = "application/json".toMediaType()
        val http = OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            requestedPaths += request.url.encodedPath
            val (code, body) = when {
                request.url.encodedPath.endsWith("/account/status") -> 200 to
                    """{"result":{"account":{"uid":"1","login":"rita","displayName":"Рита"}}}"""
                request.url.encodedPath.endsWith("/users/1/playlists/list") -> 503 to
                    """{"error":{"message":"temporarily unavailable"}}"""
                request.url.encodedPath.endsWith("/users/1/playlists/list/kinds") -> 200 to
                    """{"result":[7,9]}"""
                request.url.encodedPath.endsWith("/users/1/playlists/7") -> 200 to
                    """{"result":{"playlist":{"uid":"1","kind":7,"title":"Дорога","trackCount":18}}}"""
                request.url.encodedPath.endsWith("/users/1/playlists/9") -> 200 to
                    """{"result":{"uid":"1","kind":9,"title":"Тихое","trackCount":11}}"""
                else -> 404 to "{}"
            }
            Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(code)
                .message(if (code == 200) "OK" else "Error")
                .body(body.toResponseBody(contentType))
                .build()
        }.build()

        val playlists = YandexClient("test-token", http, pause = {}).playlists().getOrThrow()

        assertEquals(listOf("Дорога", "Тихое"), playlists.map { it.title })
        assertTrue(requestedPaths.contains("/users/1/playlists/list/kinds"))
    }
}
