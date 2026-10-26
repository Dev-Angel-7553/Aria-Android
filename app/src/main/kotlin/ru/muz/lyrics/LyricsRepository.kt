package ru.muz.lyrics

import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.muz.yandex.YandexClient

class LyricsRepository(
    private val yandex: YandexClient,
    private val dir: File? = null,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inflight = ConcurrentHashMap<String, Deferred<Found>>()
    private val cache = object : LinkedHashMap<String, Lyrics>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Lyrics>?) = size > 60
    }

    private sealed interface Found {
        data class Text(val value: String) : Found
        data object Missing : Found
        data object Failed : Found
    }

    suspend fun load(
        track: YandexClient.Track,
        refresh: Boolean = false,
        onStage: (String) -> Unit = {},
    ): Result<Lyrics> = runCatching {
        if (!refresh) {
            remembered(track.id)?.let { return@runCatching it }
            if (withContext(Dispatchers.IO) { knownMissing(track.id) }) error("Текст для этого трека не найден")
        }
        onStage("Ищем текст в Яндексе и LRCLIB…")
        when (val found = shared(track, refresh).await()) {
            is Found.Text -> parse(found.value)?.also { remember(track.id, it) } ?: error("Текст для этого трека пуст")
            Found.Missing -> error("Текст для этого трека не найден")
            Found.Failed -> error("Не получилось загрузить текст, проверьте интернет")
        }
    }.onFailure {
        if (it is CancellationException) throw it
    }

    fun prefetch(tracks: List<YandexClient.Track>) {
        tracks.forEach { track ->
            if (synchronized(cache) { cache.containsKey(track.id) }) return@forEach
            scope.launch {
                val stored = readStored(track.id)
                if (stored != null) {
                    parse(stored)?.let { remember(track.id, it) }
                } else if (!knownMissing(track.id)) {
                    val found = runCatching { shared(track, false).await() }.getOrNull()
                    if (found is Found.Text) parse(found.value)?.let { remember(track.id, it) }
                }
            }
        }
    }

    private suspend fun remembered(id: String): Lyrics? {
        synchronized(cache) { cache[id] }?.let { return it }
        val stored = withContext(Dispatchers.IO) { readStored(id) } ?: return null
        return parse(stored)?.also { remember(id, it) }
    }

    private fun remember(id: String, lyrics: Lyrics) {
        synchronized(cache) { cache[id] = lyrics }
    }

    private fun shared(track: YandexClient.Track, refresh: Boolean): Deferred<Found> {
        if (!refresh) inflight[track.id]?.let { return it }
        val job = scope.async(start = CoroutineStart.LAZY) { fetch(track) }
        if (refresh) {
            inflight[track.id] = job
        } else {
            inflight.putIfAbsent(track.id, job)?.let { return it }
        }
        job.invokeOnCompletion { inflight.remove(track.id, job) }
        job.start()
        return job
    }

    private suspend fun fetch(track: YandexClient.Track): Found = coroutineScope {
        val fromYandex = async {
            withTimeoutOrNull(8_000) { yandex.lyrics(track).getOrNull() }?.takeIf { parse(it) != null }
        }
        val fromLrcLib = async {
            withTimeoutOrNull(10_000) {
                runCatching { searchLrcLib(track) }.map { result ->
                    val synced = result?.get("syncedLyrics")?.jsonPrimitive?.contentOrNull.orEmpty()
                    val plain = result?.get("plainLyrics")?.jsonPrimitive?.contentOrNull.orEmpty()
                    synced.ifBlank { plain }.takeIf { it.isNotBlank() && parse(it) != null }
                }
            }
        }
        val yandexText = fromYandex.await()
        if (yandexText != null) {
            fromLrcLib.cancel()
            store(track.id, yandexText)
            return@coroutineScope Found.Text(yandexText)
        }
        val lrc = fromLrcLib.await()
        val text = lrc?.getOrNull()
        when {
            text != null -> Found.Text(text).also { store(track.id, text) }
            lrc != null && lrc.isSuccess -> Found.Missing.also { storeMissing(track.id) }
            else -> Found.Failed
        }
    }

    private fun parse(text: String): Lyrics? =
        LrcParser.parse(text).takeIf { it.lines.any { line -> line.text.isNotBlank() } }

    private fun fileFor(id: String, extension: String): File? =
        dir?.let { File(it, id.replace(Regex("[^A-Za-z0-9_-]"), "_") + "." + extension) }

    private fun readStored(id: String): String? = runCatching {
        fileFor(id, "lrc")?.takeIf { it.isFile }?.readText()
    }.getOrNull()

    private fun knownMissing(id: String): Boolean {
        val file = fileFor(id, "none") ?: return false
        return file.isFile && System.currentTimeMillis() - file.lastModified() < MISSING_TTL_MS
    }

    private fun store(id: String, text: String) {
        runCatching {
            val folder = dir ?: return
            folder.mkdirs()
            fileFor(id, "none")?.delete()
            fileFor(id, "lrc")?.writeText(text)
            trim(folder)
        }
    }

    private fun storeMissing(id: String) {
        runCatching {
            dir?.mkdirs()
            fileFor(id, "none")?.writeText("")
        }
    }

    private fun trim(folder: File) {
        val files = folder.listFiles() ?: return
        if (files.size <= MAX_FILES) return
        files.sortedBy { it.lastModified() }.take(files.size - MAX_FILES + 50).forEach { it.delete() }
    }

    private suspend fun searchLrcLib(track: YandexClient.Track): JsonObject? = withContext(Dispatchers.IO) {
        val exactUrl = "https://lrclib.net/api/get".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", clean(track.title))
            .addQueryParameter("artist_name", track.artist)
            .apply {
                if (track.album.isNotBlank()) addQueryParameter("album_name", track.album)
                if (track.durationMs > 0) addQueryParameter("duration", (track.durationMs / 1000).toString())
            }
            .build()
        request(exactUrl.toString())?.let { body ->
            val exact = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            if (exact != null && exact.hasText()) return@withContext exact
        }

        val searchUrl = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
            .addQueryParameter("track_name", clean(track.title))
            .addQueryParameter("artist_name", track.artist)
            .build()
        val array = request(searchUrl.toString())?.let { body ->
            runCatching { json.parseToJsonElement(body) as? JsonArray }.getOrNull()
        }
        array?.map { it.jsonObject }?.filter { it.hasText() }?.minByOrNull {
            val duration = it["duration"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull() ?: 0.0
            kotlin.math.abs(duration - track.durationMs / 1000.0)
        }
    }

    private fun JsonObject.hasText(): Boolean =
        !this["syncedLyrics"]?.jsonPrimitive?.contentOrNull.isNullOrBlank() ||
            !this["plainLyrics"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()

    private fun request(url: String): String? {
        val request = Request.Builder().url(url).header("User-Agent", "Aria (Android)").build()
        return http.newCall(request).execute().use { response ->
            when {
                response.isSuccessful -> response.body?.string()
                response.code == 429 || response.code >= 500 -> throw java.io.IOException("LRCLIB ${response.code}")
                else -> null
            }
        }
    }

    private fun clean(value: String): String = value
        .replace(Regex("\\s*[\\[(].*?[\\])]\\s*"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private companion object {
        const val MAX_FILES = 600
        const val MISSING_TTL_MS = 12L * 60 * 60 * 1000
    }
}
