package ru.muz.yandex

import ru.muz.net.readText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import okhttp3.Request
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

class YandexClient(
    private val token: String,
    private val http: OkHttpClient = sharedHttp,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    private val json = Json { ignoreUnknownKeys = true }
    private var account: Account? = null
    private val streamCache = ConcurrentHashMap<String, String>()
    private val trackCache = ConcurrentHashMap<String, Track>()

    @Volatile var lastWaveBatchId: String = ""
        private set

    fun seedTracks(tracks: Iterable<Track>) {
        cacheTracks(tracks.associateBy { it.id.substringBefore(':') })
    }

    suspend fun accountInfo(): Result<Account> = runCatching {
        val result = apiGet("account/status").resultObject()
        val data = result.obj("account") ?: error("Яндекс ответил без данных аккаунта")
        Account(
            uid = data.string("uid") ?: error("Яндекс ответил без ID аккаунта"),
            login = data.string("login").orEmpty(),
            displayName = data.string("displayName")
                ?: data.string("fullName")
                ?: data.string("login")
                ?: "Яндекс Музыка",
            serviceAvailable = data.primitive("serviceAvailable")?.booleanOrNull ?: true,
            hasPlus = result.obj("plus")?.primitive("hasPlus")?.booleanOrNull
                ?: (data.primitive("serviceAvailable")?.booleanOrNull ?: true),
        ).also { account = it }
    }

    fun seedAccount(value: Account) {
        if (account == null) account = value
    }

    suspend fun accountLogin(): Result<String> = accountInfo().map { it.login }

    suspend fun likedTracks(): Result<List<Track>> = runCatching {
        val uid = requireAccount().uid
        val response = apiGet("users/$uid/likes/tracks")
        val shorts = response.likedTrackElements()
        val embedded = shorts.mapNotNull { item ->
            val obj = item.asObject() ?: return@mapNotNull null
            (obj.obj("track") ?: obj).asTrack()?.let { it.copy(addedAtMs = obj.addedAtMs().takeIf { time -> time > 0 } ?: it.addedAtMs) }
        }
        val ids = shorts.mapNotNull { item ->
            val obj = item.asObject()
            val track = obj?.obj("track")
            val id = obj?.string("id") ?: obj?.string("trackId") ?: track?.string("id")
                ?: item.asPrimitive()?.content?.takeUnless { it == "null" }
            val albumId = obj?.string("albumId") ?: track?.string("albumId")
                ?: track?.obj("album")?.string("id")
            id?.let { value ->
                if (!albumId.isNullOrBlank() && ':' !in value) "$value:$albumId" else value
            }
        }
        if (embedded.isNotEmpty() && (ids.isEmpty() || embedded.size == shorts.size)) {
            return@runCatching embedded.distinctBy { it.id }
        }
        val fetched = fetchTracks(ids)
        val dates = shorts.mapNotNull { value ->
            val obj = value.asObject() ?: return@mapNotNull null
            val id = obj.string("id") ?: obj.string("trackId") ?: obj.obj("track")?.string("id") ?: return@mapNotNull null
            id.substringBefore(':') to obj.addedAtMs()
        }.toMap()
        val result = (embedded + fetched).distinctBy { it.id }.map { it.copy(addedAtMs = dates[it.id.substringBefore(':')] ?: it.addedAtMs) }
        lastLikedRequested = ids.distinct().size
        lastLikedLoaded = result.size
        result
    }

    @Volatile var lastLikedRequested: Int = 0
        private set
    @Volatile var lastLikedLoaded: Int = 0
        private set

    suspend fun playlists(): Result<List<Playlist>> = runCatching {
        val uid = requireAccount().uid
        val collected = linkedMapOf<String, Playlist>()
        var primaryFailure: Throwable? = null

        fun collect(elements: Iterable<JsonElement>) {
            elements.mapNotNull { item -> item.playlistObject()?.asPlaylist() }
                .forEach { playlist -> collected[playlist.stableKey()] = playlist }
        }

        runCatching { requestWithRetry { apiGet("users/$uid/playlists/list") } }
            .onSuccess { collect(it.playlistElements()) }
            .onFailure { primaryFailure = it }

        if (collected.isEmpty()) {
            val kinds = runCatching {
                requestWithRetry { apiGet("users/$uid/playlists/list/kinds") }.playlistKinds()
            }.getOrDefault(emptyList())
            for ((index, kind) in kinds.withIndex()) {
                if (index > 0) pause(PLAYLIST_REQUEST_INTERVAL_MS)
                runCatching {
                    requestWithRetry { apiGet("users/$uid/playlists/$kind") }
                }.onSuccess { response ->
                    val values = response.playlistElements()
                    if (values.isNotEmpty()) collect(values)
                    else response.resultObject().playlistObject()?.asPlaylist()?.let {
                        collected[it.stableKey()] = it
                    }
                }
            }
        }

        if (collected.isEmpty()) {
            runCatching {
                requestWithRetry {
                    apiGet("users/$uid/likes/playlists", query = mapOf("rich" to "true"))
                }
            }.onSuccess { collect(it.playlistElements()) }
        }

        if (collected.isEmpty() && primaryFailure != null) throw primaryFailure!!
        collected.values.toList()
    }

    suspend fun likedAlbums(): Result<List<Album>> = runCatching {
        val uid = requireAccount().uid
        val response = apiGet(
            path = "users/$uid/likes/albums",
            query = mapOf("rich" to "true"),
        )
        val result = response.resultObject()
        val values = response.resultArray().ifEmpty {
            result.array("albums") ?: result.array("results") ?: JsonArray(emptyList())
        }
        values.mapNotNull { item ->
            val obj = item.asObject() ?: return@mapNotNull null
            (obj.obj("album") ?: obj).asAlbum()
        }.distinctBy { it.id }
    }

    suspend fun personalPlaylists(): Result<List<Playlist>> = runCatching {
        val generated = coroutineScope {
            PERSONAL_PLAYLISTS.map { type ->
                async(Dispatchers.IO) {
                    runCatching {
                        val generated = apiGet("playlists/personal/$type").resultObject()
                        generated.generatedPlaylist(source = type)
                    }.getOrNull()
                }
            }.awaitAll().filterNotNull().distinctBy { it.id }
        }
        if (generated.isNotEmpty()) return@runCatching generated

        val feed = runCatching { apiGet("feed").resultObject() }.getOrNull()
        val fromFeed = feed?.array("generatedPlaylists")
            ?.mapNotNull { it.asObject()?.generatedPlaylist() }
            .orEmpty()
            .distinctBy { it.id }
        if (fromFeed.isNotEmpty()) fromFeed else playlists().getOrThrow()
    }

    suspend fun myWave(queue: String? = null): Result<List<Track>> = stationTracks(PlaySource.WAVE_STATION, queue)

    suspend fun stationTracks(station: String, queue: String? = null): Result<List<Track>> = runCatching {
        val result = apiGet(
            path = "rotor/station/$station/tracks",
            query = buildMap {
                put("settings2", "true")
                queue?.takeIf { it.isNotBlank() }?.let { put("queue", it.substringBefore(':')) }
            },
        ).resultObject()
        result.string("batchId")?.let { lastWaveBatchId = it }
        val sequence = result.waveTrackElements()
        sequence.mapNotNull { item ->
            val obj = item.asObject() ?: return@mapNotNull null
            (obj.obj("track") ?: obj).asTrack()?.let { it.copy(addedAtMs = obj.addedAtMs().takeIf { time -> time > 0 } ?: it.addedAtMs) }
        }.distinctBy { it.id }
    }

    suspend fun chart(): Result<List<Track>> = runCatching {
        val result = apiGet("landing3/chart").resultObject()
        (result.obj("chart") ?: result).trackList()
    }

    suspend fun playlistTracks(playlist: Playlist): Result<List<Track>> = runCatching {
        playlist.tracks.ifEmpty {
            val obj = when {
                playlist.id.startsWith("album:") -> apiGet("albums/${playlist.id.removePrefix("album:")}/with-tracks").resultObject()
                playlist.source.isNotBlank() -> apiGet("playlists/personal/${playlist.source}").resultObject()
                    .obj("data") ?: error("Персональная подборка пока недоступна")
                playlist.uuid.isNotBlank() -> apiGet("playlist/${playlist.uuid}").resultObject()
                else -> apiGet("users/${playlist.ownerUid}/playlists/${playlist.kind}").resultObject()
            }
            if (playlist.id.startsWith("album:")) obj.albumTrackList() else obj.trackList()
        }
    }

    suspend fun setLikedAlbum(albumId: String, liked: Boolean): Result<Boolean> = runCatching {
        val uid = requireAccount().uid
        val action = if (liked) "add-multiple" else "remove"
        apiPost("users/$uid/likes/albums/$action", mapOf("album-ids" to albumId))
        liked
    }

    suspend fun setLiked(trackId: String, liked: Boolean): Result<Boolean> = runCatching {
        val uid = requireAccount().uid
        val action = if (liked) "add-multiple" else "remove"
        apiPost("users/$uid/likes/tracks/$action", mapOf("track-ids" to trackId))
        liked
    }

    suspend fun reportPlay(event: ListenEvent.PlayAudio): Result<Unit> = runCatching {
        val now = Instant.now().toString()
        apiPostElement(
            "play-audio",
            listOf(
                "track-id" to event.trackId,
                "from-cache" to "False",
                "from" to event.from,
                "play-id" to event.playId,
                "uid" to requireAccount().uid,
                "timestamp" to now,
                "track-length-seconds" to seconds(event.trackLengthSeconds),
                "total-played-seconds" to seconds(event.playedSeconds),
                "end-position-seconds" to seconds(event.endPositionSeconds),
                "album-id" to event.albumId,
                "playlist-id" to event.playlistId,
                "client-now" to now,
            ),
        )
        Unit
    }

    suspend fun waveFeedback(event: ListenEvent, station: String = PlaySource.WAVE_STATION): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("timestamp", Instant.now().toString())
            when (event) {
                is ListenEvent.WaveStarted -> {
                    put("type", "radioStarted")
                    put("from", event.from)
                }
                is ListenEvent.WaveTrackStarted -> {
                    put("type", "trackStarted")
                    put("trackId", waveTrackId(event.trackId, event.albumId))
                }
                is ListenEvent.WaveTrackEnded -> {
                    put("type", if (event.skipped) "skip" else "trackFinished")
                    put("trackId", waveTrackId(event.trackId, event.albumId))
                    put("totalPlayedSeconds", event.playedSeconds)
                }
                is ListenEvent.PlayAudio -> error("play-audio is not a wave feedback")
            }
        }
        val query = buildMap { lastWaveBatchId.takeIf { it.isNotBlank() }?.let { put("batch-id", it) } }
        apiPostJson("rotor/station/$station/feedback", query, body.toString())
        Unit
    }

    private fun waveTrackId(trackId: String, albumId: String): String =
        if (albumId.isBlank()) trackId else "$trackId:$albumId"

    private fun seconds(value: Double): String = String.format(java.util.Locale.ROOT, "%.3f", value.coerceAtLeast(0.0))

    suspend fun setStationSettings(station: String, mood: String, diversity: String, language: String): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("moodEnergy", mood)
            put("diversity", diversity)
            put("language", language)
            put("type", "rotor")
        }
        runCatching { apiPostJson("rotor/station/$station/settings3", emptyMap(), body.toString()) }
            .recoverCatching {
                apiPost(
                    "rotor/station/$station/settings2",
                    mapOf("moodEnergy" to mood, "diversity" to diversity, "language" to language, "type" to "rotor"),
                )
            }.getOrThrow()
        Unit
    }

    suspend fun setDisliked(trackId: String, disliked: Boolean): Result<Boolean> = runCatching {
        val uid = requireAccount().uid
        val action = if (disliked) "add-multiple" else "remove"
        apiPost("users/$uid/dislikes/tracks/$action", mapOf("track-ids" to trackId))
        disliked
    }

    suspend fun stationDislike(station: String, trackId: String, albumId: String, playedSeconds: Double): Result<Unit> = runCatching {
        val body = buildJsonObject {
            put("type", "dislike")
            put("timestamp", Instant.now().toString())
            put("trackId", waveTrackId(trackId.substringBefore(':'), albumId))
            put("totalPlayedSeconds", playedSeconds)
        }
        val query = buildMap { lastWaveBatchId.takeIf { it.isNotBlank() }?.let { put("batch-id", it) } }
        apiPostJson("rotor/station/$station/feedback", query, body.toString())
        Unit
    }

    data class CreatedPlaylist(val kind: String, val revision: Int, val ownerUid: String)

    suspend fun createPlaylist(title: String): Result<CreatedPlaylist> = runCatching {
        val uid = requireAccount().uid
        val result = apiPostElement(
            "users/$uid/playlists/create",
            listOf("title" to title.take(120), "visibility" to "private"),
        ).jsonObject.resultObject()
        CreatedPlaylist(
            kind = result.string("kind") ?: error("Яндекс не вернул плейлист"),
            revision = result.primitive("revision")?.intOrNull ?: 1,
            ownerUid = uid,
        )
    }

    fun isOwnPlaylist(playlist: Playlist): Boolean {
        val uid = account?.uid ?: return false
        return playlist.ownerUid == uid && playlist.kind.toIntOrNull()?.let { it != LIKES_KIND } == true &&
            playlist.source.isBlank() && !playlist.id.startsWith("album:") && !playlist.id.startsWith("artist:")
    }

    suspend fun uploadTrack(
        playlistKind: String,
        file: UploadFile,
        control: ru.muz.net.TransferControl = ru.muz.net.TransferControl(),
        onProgress: (Float) -> Unit = {},
    ): Result<String?> = runCatching {
        TrackUpload.check(file)
        val uid = requireAccount().uid
        var failure: TrackUploadException? = null
        for (attempt in 0..TrackUpload.RETRY_PAUSES_MS.size) {
            if (attempt > 0) {
                pause(TrackUpload.RETRY_PAUSES_MS[attempt - 1])
                control.checkActive()
            }
            try {
                return@runCatching uploadOnce(uid, playlistKind, file, control, onProgress)
            } catch (error: TrackUploadException) {
                if (error.reason != TrackUploadException.Reason.TEMPORARY) throw error
                failure = error
            }
        }
        throw failure ?: TrackUploadException("Не удалось загрузить файл", TrackUploadException.Reason.TEMPORARY)
    }

    private suspend fun uploadOnce(
        uid: String,
        kind: String,
        file: UploadFile,
        control: ru.muz.net.TransferControl,
        onProgress: (Float) -> Unit,
    ): String? = withContext(Dispatchers.IO) {
        val target = uploadTarget(uid, kind, file.name, control)
        val part = TrackUpload.body(file, { sent -> if (file.size > 0) onProgress((sent.toFloat() / file.size).coerceIn(0f, 1f)) }, control::checkActive)
        val body = okhttp3.MultipartBody.Builder()
            .setType(okhttp3.MultipartBody.FORM)
            .addFormDataPart("file", file.name, part)
            .build()
        val request = Request.Builder().url(target.postTarget).post(body)
            .header("User-Agent", USER_AGENT)
            .apply { if (TrackUpload.isYandex(target.postTarget)) header("Authorization", "OAuth $token") }
            .build()
        val (code, reply) = control.response(uploadHttp.newCall(request)) { it.code to it.body?.string().orEmpty() }
        if (code >= 300 || reply.contains("UNSUPPORTED_MEDIA_TYPE", ignoreCase = true)) throw TrackUpload.failure(code, reply)
        onProgress(1f)
        target.ugcTrackId
    }

    private suspend fun uploadTarget(uid: String, kind: String, name: String, control: ru.muz.net.TransferControl): UploadTarget {
        val playlistId = "$uid:$kind"
        val fields = listOf("uid" to uid, "playlist-id" to playlistId, "playlistId" to playlistId, "path" to name)
        val codes = mutableListOf<Int>()
        for ((base, web) in listOf(API to false, WEB_API to true)) {
            val url = "$base/loader/upload-url".toHttpUrl().newBuilder()
                .apply { fields.forEach { (key, value) -> addQueryParameter(key, value) } }
                .build()
            val form = FormBody.Builder().apply { fields.forEach { (key, value) -> add(key, value) } }.build()
            val request = Request.Builder().url(url).post(form)
                .header("Authorization", "OAuth $token")
                .header("User-Agent", USER_AGENT)
                .header("Accept-Language", "ru")
                .apply {
                    if (web) {
                        header("X-Yandex-Music-Client", WEB_CLIENT)
                        header("X-Requested-With", "XMLHttpRequest")
                        header("X-Yandex-Music-Without-Invocation-Info", "1")
                        header("Origin", "https://music.yandex.ru")
                        header("Referer", "https://music.yandex.ru/")
                        header("Accept", "application/json")
                    } else {
                        header("X-Yandex-Music-Client", ANDROID_CLIENT)
                    }
                }
                .build()
            val reply = try {
                control.response(http.newCall(request)) { it.code to it.body?.string().orEmpty() }
            } catch (cancel: kotlinx.coroutines.CancellationException) {
                throw cancel
            } catch (_: java.io.IOException) {
                continue
            }
            TrackUpload.parseTarget(reply.second)?.let { return it }
            codes += reply.first
        }
        throw when {
            codes.isEmpty() -> TrackUploadException("Нет связи с Яндексом", TrackUploadException.Reason.TEMPORARY)
            codes.all { it == 401 || it == 403 } -> TrackUploadException("Яндекс не принял вход. Войдите заново", TrackUploadException.Reason.AUTH)
            codes.all { it in TrackUpload.TEMPORARY } -> TrackUploadException("Сервер загрузки Яндекса не ответил вовремя", TrackUploadException.Reason.TEMPORARY)
            else -> TrackUploadException("Яндекс не выдал адрес для загрузки (HTTP ${codes.last()})", TrackUploadException.Reason.ENDPOINT)
        }
    }

    private val uploadHttp: OkHttpClient by lazy {
        http.newBuilder()
            .writeTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(120, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    suspend fun appendTracks(playlist: CreatedPlaylist, at: Int, tracks: List<Track>): Result<CreatedPlaylist> = runCatching {
        if (tracks.isEmpty()) return@runCatching playlist
        val diff = kotlinx.serialization.json.buildJsonArray {
            add(buildJsonObject {
                put("op", "insert")
                put("at", at)
                put("tracks", kotlinx.serialization.json.buildJsonArray {
                    tracks.forEach { track ->
                        add(buildJsonObject {
                            put("id", track.id.substringBefore(':'))
                            put("albumId", track.albumId.ifBlank { track.id.substringAfter(':', "") })
                        })
                    }
                })
            })
        }
        val result = apiPostElement(
            "users/${playlist.ownerUid}/playlists/${playlist.kind}/change-relative",
            listOf("diff" to diff.toString(), "revision" to playlist.revision.toString()),
        ).jsonObject.resultObject()
        playlist.copy(revision = result.primitive("revision")?.intOrNull ?: (playlist.revision + 1))
    }

    suspend fun search(query: String): Result<SearchResults> = runCatching {
        if (query.isBlank()) return@runCatching SearchResults()
        val result = apiGet(
            path = "search",
            query = mapOf("text" to query.trim(), "type" to "all", "page" to "0", "nocorrect" to "false"),
        ).resultObject()
        SearchResults(
            tracks = result.obj("tracks")?.array("results")
                .orEmpty().mapNotNull { it.asObject()?.asTrack() },
            playlists = result.obj("playlists")?.array("results")
                .orEmpty().mapNotNull { it.asObject()?.asPlaylist() },
            albums = result.obj("albums")?.array("results")
                .orEmpty().mapNotNull { it.asObject()?.asAlbum() },
            artists = result.obj("artists")?.array("results")
                .orEmpty().mapNotNull { it.asObject()?.asArtist() },
        )
    }

    suspend fun searchTracks(query: String): Result<List<Track>> = search(query).map { it.tracks }

    suspend fun albumTracks(album: Album): Result<List<Track>> = runCatching {
        val result = apiGet("albums/${album.id}/with-tracks").resultObject()
        result.albumTrackList()
    }

    suspend fun artistTracks(artist: Artist): Result<List<Track>> = runCatching {
        val result = apiGet(
            path = "artists/${artist.id}/tracks",
            query = mapOf("page" to "0", "page-size" to "100"),
        ).resultObject()
        result.array("tracks").orEmpty().mapNotNull { it.asObject()?.asTrack() }
    }

    suspend fun streamUrl(trackId: String, quality: AudioQuality = AudioQuality.HIGH): Result<String> =
        runCatching {
            resolveStreamUrl(trackId, quality, useCache = true)
        }.recoverCatching { firstFailure ->
            if (firstFailure.message.orEmpty().contains("0x41c", ignoreCase = true) ||
                firstFailure.message.orEmpty().contains("1052") ||
                firstFailure.message.orEmpty().contains("устар", ignoreCase = true)
            ) {
                resolveStreamUrl(trackId, quality, useCache = false)
            } else {
                throw firstFailure
            }
        }

    suspend fun freshStreamUrl(trackId: String, quality: AudioQuality = AudioQuality.HIGH): Result<String> =
        runCatching { resolveStreamUrl(trackId, quality, useCache = false) }

    private suspend fun resolveStreamUrl(trackId: String, quality: AudioQuality, useCache: Boolean): String {
            val normalizedId = trackId.substringBefore(':')
            if (useCache) streamCache["$normalizedId:${quality.name}"]?.let { return it }
            val feeds = apiGet("tracks/$normalizedId/download-info").resultArray()
            val candidates = feeds.mapNotNull { it.asObject() }.filter {
                it.primitive("preview")?.booleanOrNull != true
            }
            val preferred = candidates.filter { it.string("codec") == "mp3" }.ifEmpty { candidates }
            val feed = when (quality) {
                AudioQuality.HIGH -> preferred.maxByOrNull { it.primitive("bitrateInKbps")?.intOrNull ?: 0 }
                AudioQuality.ECONOMY -> preferred.minByOrNull { it.primitive("bitrateInKbps")?.intOrNull ?: Int.MAX_VALUE }
            } ?: error("Яндекс не отдал доступный аудиопоток")
            return (feed.string("directLink")?.takeIf { it.isNotBlank() } ?: run {
                val infoUrl = feed.string("downloadInfoUrl") ?: error("Яндекс не отдал ссылку подписи")
                buildDirectStreamUrl(getText(infoUrl, authorized = true))
            }).also { streamCache["$normalizedId:${quality.name}"] = it }
    }

    suspend fun lyrics(track: Track): Result<String> = runCatching {
        val signed = lyricSign(track.id)
        val info = apiGet(
            path = "tracks/${track.id}/lyrics",
            query = mapOf("format" to "LRC", "timeStamp" to signed.timestamp.toString(), "sign" to signed.value),
        ).resultObject()
        val url = info.string("downloadUrl") ?: error("У Яндекса нет синхронизированного текста")
        getText(url, authorized = true)
    }

    private suspend fun requireAccount(): Account = account ?: accountInfo().getOrThrow()

    private suspend fun fetchTracks(ids: List<String>): List<Track> {
        val requested = ids.filter(String::isNotBlank).distinct()
        if (requested.isEmpty()) return emptyList()
        val missing = requested.filterNot(trackCache::containsKey)
        if (missing.isNotEmpty()) {
            for ((index, chunk) in missing.chunked(TRACK_BATCH_SIZE).withIndex()) {
                if (index > 0) pause(TRACK_REQUEST_INTERVAL_MS)
                cacheTracks(fetchTrackChunk(chunk))
            }
            val unresolved = missing.filter { id ->
                trackCache[id] == null && trackCache[id.substringBefore(':')] == null
            }
            for (chunk in unresolved.chunked(TRACK_RETRY_BATCH_SIZE)) {
                pause(TRACK_RETRY_INTERVAL_MS)
                cacheTracks(fetchTrackChunk(chunk))
            }
            val stubborn = missing.filter { id ->
                trackCache[id] == null && trackCache[id.substringBefore(':')] == null
            }
            for (chunk in stubborn.chunked(5)) {
                pause(TRACK_RETRY_INTERVAL_MS)
                cacheTracks(fetchTrackChunk(chunk))
            }
        }
        return requested.mapNotNull { trackCache[it] ?: trackCache[it.substringBefore(':')] }
    }

    private fun cacheTracks(tracks: Map<String, Track>) {
        tracks.forEach { (key, track) ->
            val id = track.id.substringBefore(':')
            trackCache[id] = track
            trackCache[key] = track
            if (track.albumId.isNotBlank()) trackCache["$id:${track.albumId}"] = track
        }
    }

    private suspend fun fetchTrackChunk(chunk: List<String>): Map<String, Track> {
        if (chunk.isEmpty()) return emptyMap()
        suspend fun tryRequest(block: suspend () -> JsonArray): JsonArray =
            runCatching { requestWithRetry(block = block) }.getOrDefault(JsonArray(emptyList()))

        val found = linkedMapOf<String, Track>()
        fun collect(values: JsonArray) {
            values.mapNotNull { item ->
                val obj = item.asObject() ?: return@mapNotNull null
                val source = obj.obj("track") ?: obj
                val track = source.asTrack() ?: return@mapNotNull null
                listOfNotNull(source.string("realId"), source.string("id"), track.id)
                    .map { it.substringBefore(':') }
                    .distinct()
                    .map { it to track }
            }.flatten().forEach { (key, track) -> found[key] = track }
        }
        fun unresolved(): List<String> = chunk.filter { it.substringBefore(':') !in found }

        val legacyIds = chunk.map { it.substringBefore(':') }
        collect(tryRequest {
            apiGet(
                path = "tracks",
                query = mapOf("trackIds" to legacyIds.joinToString(","), "with-positions" to "true"),
            ).trackElements()
        })
        unresolved().takeIf { it.isNotEmpty() }?.let { ids ->
            collect(tryRequest {
                apiPostElement(
                    path = "tracks",
                    fields = ids.map { "track-ids" to it } + ("with-positions" to "true"),
                ).trackElements()
            })
        }
        unresolved().takeIf { it.isNotEmpty() }?.let { ids ->
            val normalized = ids.map { it.substringBefore(':') }.distinct()
            collect(tryRequest {
                apiPostElement(
                    path = "tracks",
                    fields = normalized.map { "track-ids" to it } + ("with-positions" to "true"),
                ).trackElements()
            })
        }
        unresolved().takeIf { it.isNotEmpty() }?.let { ids ->
            collect(tryRequest {
                apiPostElement(
                    path = "tracks",
                    form = mapOf(
                        "track-ids" to ids.joinToString(","),
                        "with-positions" to "true",
                    ),
                ).trackElements()
            })
        }
        return found
    }

    private suspend fun <T> requestWithRetry(
        attempts: Int = NETWORK_ATTEMPTS,
        block: suspend () -> T,
    ): T {
        var failure: Throwable? = null
        repeat(attempts.coerceAtLeast(1)) { attempt ->
            try {
                return block()
            } catch (error: Throwable) {
                failure = error
                if (!error.isRetryableRequestFailure() || attempt == attempts - 1) throw error
                pause((NETWORK_RETRY_BASE_MS shl attempt).coerceAtMost(NETWORK_RETRY_MAX_MS))
            }
        }
        throw failure ?: IllegalStateException("Не удалось выполнить запрос")
    }

    private suspend fun apiGet(path: String, query: Map<String, String> = emptyMap()): JsonObject {
        val builder = "$API/${path.trimStart('/')}".toHttpUrl().newBuilder()
        query.forEach { (key, value) -> builder.addQueryParameter(key, value) }
        return getJson(builder.build().toString(), authorized = true)
    }

    private suspend fun apiPost(path: String, form: Map<String, String>): JsonObject = withContext(Dispatchers.IO) {
        val body = FormBody.Builder().apply { form.forEach { (key, value) -> add(key, value) } }.build()
        val request = baseRequest("$API/${path.trimStart('/')}", authorized = true).post(body).build()
        val text = execute(request)
        json.parseToJsonElement(text).jsonObject
    }

    private suspend fun apiPostElement(path: String, form: Map<String, String>): JsonElement =
        apiPostElement(path, form.entries.map { it.key to it.value })

    private suspend fun apiPostElement(path: String, fields: List<Pair<String, String>>): JsonElement =
        withContext(Dispatchers.IO) {
            val body = FormBody.Builder().apply { fields.forEach { (key, value) -> add(key, value) } }.build()
            val request = baseRequest("$API/${path.trimStart('/')}", authorized = true).post(body).build()
            json.parseToJsonElement(execute(request))
        }

    private suspend fun apiPostJson(path: String, query: Map<String, String>, body: String): String =
        withContext(Dispatchers.IO) {
            val url = "$API/${path.trimStart('/')}".toHttpUrl().newBuilder()
                .apply { query.forEach { (key, value) -> addQueryParameter(key, value) } }
                .build()
            val request = baseRequest(url.toString(), authorized = true)
                .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()
            execute(request)
        }

    private suspend fun getJson(url: String, authorized: Boolean): JsonObject = withContext(Dispatchers.IO) {
        val body = execute(url, authorized)
        json.parseToJsonElement(body).jsonObject
    }

    private suspend fun getText(url: String, authorized: Boolean): String = withContext(Dispatchers.IO) {
        execute(url, authorized)
    }

    private suspend fun execute(url: String, authorized: Boolean): String {
        val request = baseRequest(url, authorized).build()
        return execute(request)
    }

    private fun baseRequest(url: String, authorized: Boolean): Request.Builder = Request.Builder().url(url)
            .header("User-Agent", USER_AGENT)
            .header("X-Yandex-Music-Client", ANDROID_CLIENT)
            .header("Accept-Language", "ru")
            .apply { if (authorized) header("Authorization", "OAuth $token") }

    private suspend fun execute(request: Request): String {
        return http.newCall(request).readText().let { response ->
            val body = response.body
            val apiError = body.extractYandexError()
            when {
                response.code == 401 -> throw YandexHttpException(response.code, "Токен отклонён: войдите заново")
                response.code == 403 -> throw YandexHttpException(response.code, "Яндекс ограничил доступ к этому контенту")
                response.code == 404 -> throw YandexHttpException(response.code, "Контент не найден")
                response.code == 410 -> throw YandexHttpException(response.code, "Ссылка успела устареть, попробуйте ещё раз")
                !response.isSuccessful -> throw YandexHttpException(
                    response.code,
                    apiError ?: "Яндекс ответил ${response.code}",
                )
                apiError != null -> error(apiError)
                else -> body
            }
        }
    }

    private fun JsonObject.addedAtMs(): Long = sequenceOf("timestamp", "addedAt", "added").mapNotNull { string(it) }
        .mapNotNull { raw -> runCatching { Instant.parse(raw).toEpochMilli() }.getOrNull() }.firstOrNull() ?: 0L

    private fun JsonObject.asTrack(): Track? {
        val id = string("id") ?: string("trackId") ?: return null
        val title = string("title") ?: string("name") ?: return null
        val artists = ru.muz.text.ArtistCredit.names(
            array("artists").orEmpty().mapNotNull { it.asObject()?.string("name") ?: it.asPrimitive()?.content },
        ).ifBlank {
            ru.muz.text.ArtistCredit.names(listOf(string("artist") ?: obj("artist")?.string("name").orEmpty()))
        }
        val artistId = array("artists")?.firstOrNull()?.asObject()?.string("id")
            ?: obj("artist")?.string("id")
            ?: string("artistId").orEmpty()
        val album = array("albums")?.firstOrNull()?.asObject() ?: obj("album")
        val cover = string("coverUri")
            ?: album?.string("coverUri")
            ?: obj("cover")?.string("uri")
            ?: string("ogImage")
        return Track(
            id = id,
            artistId = artistId,
            addedAtMs = addedAtMs(),
            artists = array("artists").orEmpty().mapNotNull { value ->
                val obj = value.asObject() ?: return@mapNotNull null
                val name = obj.string("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                Artist(obj.string("id").orEmpty(), name, coverUrl(obj.string("coverUri")), 0)
            }.distinctBy { it.id.ifBlank { it.name.lowercase() } },
            albumId = string("albumId") ?: album?.string("id").orEmpty(),
            title = title,
            artist = artists,
            album = album?.string("title").orEmpty(),
            durationMs = primitive("durationMs")?.longOrNull ?: 0L,
            coverUrl = coverUrl(cover),
            explicit = primitive("contentWarning")?.content == "explicit",
            lyricsAvailable = obj("lyricsInfo")?.let {
                it.primitive("hasAvailableSyncLyrics")?.booleanOrNull == true ||
                    it.primitive("hasAvailableTextLyrics")?.booleanOrNull == true
            } ?: false,
            loudness = obj("r128")?.primitive("i")?.content?.toFloatOrNull() ?: Float.NaN,
            ugc = string("trackSource").equals("UGC", ignoreCase = true),
        )
    }

    private fun JsonObject.asPlaylist(source: String = ""): Playlist? {
        val ownerUid = string("uid") ?: obj("owner")?.string("uid").orEmpty()
        val kind = string("kind").orEmpty()
        val title = string("title") ?: string("ogTitle") ?: return null
        val cover = obj("cover")
        val uri = cover?.string("uri") ?: cover?.array("itemsUri")?.firstOrNull()?.asPrimitive()?.content
            ?: string("ogImage")
        return Playlist(
            id = string("playlistUuid") ?: listOf(ownerUid, kind, source).joinToString(":"),
            ownerUid = ownerUid,
            kind = kind,
            uuid = string("playlistUuid").orEmpty(),
            title = title,
            description = string("descriptionFormatted") ?: string("description").orEmpty(),
            coverUrl = coverUrl(uri),
            trackCount = primitive("trackCount")?.longOrNull?.toInt()
                ?: array("tracks")?.size ?: 0,
            source = source,
            tracks = trackList(),
        )
    }

    private fun JsonObject.generatedPlaylist(source: String = ""): Playlist? {
        val data = obj("data") ?: obj("playlist") ?: obj("generatedPlaylist")
        return (data ?: this).asPlaylist(source)
    }

    private fun JsonObject.asAlbum(): Album? {
        val id = string("id") ?: return null
        val title = string("title") ?: return null
        val artist = array("artists").orEmpty().mapNotNull { it.asObject()?.string("name") }.joinToString(", ")
        return Album(
            id = id,
            title = title,
            artist = artist,
            coverUrl = coverUrl(string("coverUri") ?: string("ogImage")),
            trackCount = primitive("trackCount")?.longOrNull?.toInt() ?: 0,
        )
    }

    private fun JsonObject.asArtist(): Artist? {
        val id = string("id") ?: return null
        val cover = obj("cover")
        return Artist(
            id = id,
            name = string("name") ?: return null,
            coverUrl = coverUrl(cover?.string("uri") ?: string("ogImage")),
            trackCount = primitive("trackCount")?.longOrNull?.toInt()
                ?: primitive("tracksCount")?.longOrNull?.toInt() ?: 0,
        )
    }

    private fun JsonObject.trackList(): List<Track> = array("tracks").orEmpty().mapNotNull { item ->
        val obj = item.asObject() ?: return@mapNotNull null
        (obj.obj("track") ?: obj).asTrack()?.let { it.copy(addedAtMs = obj.addedAtMs().takeIf { time -> time > 0 } ?: it.addedAtMs) }
    }

    private fun JsonObject.albumTrackList(): List<Track> {
        val direct = array("tracks") ?: obj("tracks")?.array("results")
        if (direct != null) {
            return direct.mapNotNull { item ->
                val obj = item.asObject() ?: return@mapNotNull null
                (obj.obj("track") ?: obj).asTrack()?.let { it.copy(addedAtMs = obj.addedAtMs().takeIf { time -> time > 0 } ?: it.addedAtMs) }
            }
        }
        return array("volumes").orEmpty().flatMap { volume ->
            (volume as? JsonArray).orEmpty().mapNotNull { item ->
                val obj = item.asObject() ?: return@mapNotNull null
                (obj.obj("track") ?: obj).asTrack()?.let { it.copy(addedAtMs = obj.addedAtMs().takeIf { time -> time > 0 } ?: it.addedAtMs) }
            }
        }
    }

    data class Account(val uid: String, val login: String, val displayName: String, val serviceAvailable: Boolean, val hasPlus: Boolean = true)
    data class Track(
        val id: String, val albumId: String, val title: String, val artist: String, val album: String,
        val durationMs: Long, val coverUrl: String?, val explicit: Boolean, val lyricsAvailable: Boolean,
        val artistId: String = "",
        val loudness: Float = Float.NaN,
        val artists: List<Artist> = emptyList(),
        val addedAtMs: Long = 0,
        val ugc: Boolean = false,
    )
    data class Playlist(
        val id: String, val ownerUid: String, val kind: String, val uuid: String, val title: String,
        val description: String, val coverUrl: String?, val trackCount: Int, val source: String,
        val tracks: List<Track> = emptyList(),
    )
    data class Album(val id: String, val title: String, val artist: String, val coverUrl: String?, val trackCount: Int)
    data class Artist(val id: String, val name: String, val coverUrl: String?, val trackCount: Int)
    data class SearchResults(
        val tracks: List<Track> = emptyList(),
        val playlists: List<Playlist> = emptyList(),
        val albums: List<Album> = emptyList(),
        val artists: List<Artist> = emptyList(),
    )
    enum class AudioQuality { HIGH, ECONOMY }

    private data class LyricSign(val timestamp: Long, val value: String)

    companion object {
        private const val API = "https://api.music.yandex.net"
        private const val WEB_API = "https://api.music.yandex.ru"
        private const val WEB_CLIENT = "YandexMusicWebNext/1.0.0"
        private const val LIKES_KIND = 3
        private const val SIGN_SALT = "XGRlBW9FXlekgbPrRHuSiA"
        private const val LYRIC_SIGN_KEY = "p93jhgh689SBReK6ghtw62"
        private const val USER_AGENT = "Yandex-Music-API"
        private const val ANDROID_CLIENT = "YandexMusicAndroid/24023621"
        private const val TRACK_BATCH_SIZE = 100
        private const val TRACK_RETRY_BATCH_SIZE = 25
        private const val TRACK_REQUEST_INTERVAL_MS = 450L
        private const val TRACK_RETRY_INTERVAL_MS = 650L
        private const val PLAYLIST_REQUEST_INTERVAL_MS = 160L
        private const val NETWORK_ATTEMPTS = 4
        private const val NETWORK_RETRY_BASE_MS = 500L
        private const val NETWORK_RETRY_MAX_MS = 4_000L
        private val PERSONAL_PLAYLISTS = listOf(
            "playlistOfTheDay",
            "origin",
            "recentTracks",
            "neverHeard",
            "missedLikes",
            "podcasts",
        )
        private val sharedHttp = OkHttpClient.Builder().followRedirects(true).followSslRedirects(true).build()
    }

    private fun lyricSign(trackId: String): LyricSign {
        val numericId = trackId.substringBefore(':')
        val timestamp = System.currentTimeMillis() / 1000
        val mac = javax.crypto.Mac.getInstance("HmacSHA256")
        mac.init(javax.crypto.spec.SecretKeySpec(LYRIC_SIGN_KEY.toByteArray(), "HmacSHA256"))
        val bytes = mac.doFinal("$numericId$timestamp".toByteArray())
        return LyricSign(timestamp, java.util.Base64.getEncoder().encodeToString(bytes))
    }
}

private class YandexHttpException(val statusCode: Int, message: String) : IOException(message)

private fun Throwable.isRetryableRequestFailure(): Boolean = when (this) {
    is YandexHttpException -> statusCode == 408 || statusCode == 425 || statusCode == 429 || statusCode >= 500
    is IOException -> true
    else -> false
}

private fun String.extractYandexError(): String? {
    val code = Regex("\\\"(?:code|errorCode)\\\"\\s*:\\s*\\\"?([^\\\",} ]+)").find(this)
        ?.groupValues?.getOrNull(1)
    val message = Regex("\\\"(?:message|error)\\\"\\s*:\\s*\\\"([^\\\"]+)").find(this)
        ?.groupValues?.getOrNull(1)
    return when {
        code.equals("0x41c", ignoreCase = true) || code == "1052" -> "Не удалось получить аудиопоток"
        message?.contains("cover", ignoreCase = true) == true &&
            message.contains("exist", ignoreCase = true) -> "Не удалось загрузить часть данных"
        message != null -> message
        else -> null
    }
}

internal fun buildDirectStreamUrl(xml: String): String {
    fun value(name: String): String? {
        val tag = Regex("(?s)<$name(?:\\s[^>]*)?>(.*?)</$name>").find(xml)?.groupValues?.get(1)?.trim()
        if (!tag.isNullOrBlank()) return tag
        return Regex("\\b$name=\"([^\"]*)\"").find(xml)?.groupValues?.get(1)?.trim()
    }
    val host = value("host") ?: error("Яндекс не отдал адрес аудиосервера")
    val path = value("path") ?: error("Яндекс не отдал путь аудиопотока")
    val s = value("s") ?: error("Яндекс не отдал ключ аудиопотока")
    val ts = value("ts") ?: error("Яндекс не отдал время подписи аудиопотока")
    val sign = MessageDigest.getInstance("MD5")
        .digest(("XGRlBW9FXlekgbPrRHuSiA" + path.removePrefix("/") + s).toByteArray())
        .joinToString("") { "%02x".format(it) }
    return "https://$host/get-mp3/$sign/$ts$path"
}

private fun coverUrl(uri: String?): String? = uri?.takeIf { it.isNotBlank() }?.let { raw ->
    val replaced = raw.replace("%%", "600x600")
    when {
        replaced.startsWith("https://") || replaced.startsWith("http://") -> replaced
        else -> "https://$replaced"
    }
}
private fun JsonObject.resultObject(): JsonObject = obj("result") ?: this
private fun JsonObject.resultArray(): JsonArray = array("result") ?: JsonArray(emptyList())
private fun JsonElement?.playlistObject(): JsonObject? {
    val value = asObject() ?: return null
    return value.obj("playlist") ?: value.obj("data") ?: value
}

private fun YandexClient.Playlist.stableKey(): String = when {
    uuid.isNotBlank() -> "uuid:$uuid"
    ownerUid.isNotBlank() || kind.isNotBlank() -> "owner:$ownerUid:$kind"
    else -> id
}

private fun JsonObject.playlistElements(): JsonArray {
    resultArray().takeIf { it.isNotEmpty() }?.let { return it }
    val result = resultObject()
    return result.array("playlists")
        ?: result.array("results")
        ?: result.obj("library")?.array("playlists")
        ?: array("playlists")
        ?: array("results")
        ?: obj("library")?.array("playlists")
        ?: JsonArray(emptyList())
}

private fun JsonObject.playlistKinds(): List<String> {
    val result = resultObject()
    val values = resultArray().takeIf { it.isNotEmpty() }
        ?: result.array("kinds")
        ?: result.array("playlistKinds")
        ?: array("kinds")
        ?: JsonArray(emptyList())
    return values.mapNotNull { item ->
        item.asPrimitive()?.content?.takeUnless { it == "null" }
            ?: item.asObject()?.string("kind")
    }.filter(String::isNotBlank).distinct()
}
private fun JsonElement.trackElements(): JsonArray {
    if (this is JsonArray) return this
    val objectValue = this as? JsonObject ?: return JsonArray(emptyList())
    return objectValue.trackElements()
}

private fun JsonObject.trackElements(): JsonArray {
    resultArray().takeIf { it.isNotEmpty() }?.let { return it }
    val result = resultObject()
    return result.array("tracks")
        ?: result.array("results")
        ?: array("tracks")
        ?: JsonArray(emptyList())
}
internal fun JsonObject.likedTrackElements(): JsonArray {
    val result = resultObject()
    return result.obj("library")?.array("tracks")
        ?: result.array("tracks")
        ?: result.array("likedTracks")
        ?: obj("library")?.array("tracks")
        ?: array("tracks")
        ?: array("likedTracks")
        ?: JsonArray(emptyList())
}

internal fun JsonObject.waveTrackElements(): JsonArray {
    val result = resultObject()
    return result.array("sequence")
        ?: result.obj("result")?.array("sequence")
        ?: result.array("tracks")
        ?: result.array("results")
        ?: JsonArray(emptyList())
}
private fun JsonObject.obj(name: String): JsonObject? = this[name].asObject()
private fun JsonObject.array(name: String): JsonArray? = this[name] as? JsonArray
private fun JsonObject.primitive(name: String): JsonPrimitive? = this[name].asPrimitive()
private fun JsonObject.string(name: String): String? = primitive(name)?.content?.takeUnless { it == "null" }
private fun JsonElement?.asObject(): JsonObject? = this as? JsonObject
private fun JsonElement?.asPrimitive(): JsonPrimitive? = this as? JsonPrimitive
