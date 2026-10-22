package ru.muz.yandex

import android.content.Context
import android.util.AtomicFile
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal class LibrarySnapshotStore(context: Context) {
    private val file = AtomicFile(File(context.filesDir, "yandex_library_snapshot_v1.json"))
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun read(accountUid: String): LibrarySnapshot? = readMatching(accountUid)

    suspend fun readLatest(): LibrarySnapshot? = readMatching(null)

    private suspend fun readMatching(accountUid: String?): LibrarySnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            if (!file.baseFile.exists()) return@runCatching null
            val root = json.parseToJsonElement(file.readFully().decodeToString()).jsonObject
            val uid = root.string("uid")
            if (accountUid != null && uid != accountUid) return@runCatching null
            LibrarySnapshot(
                uid = uid,
                syncedAtMs = root.long("syncedAtMs"),
                liked = root.array("liked").mapNotNull { it.asObject()?.toTrack() },
                albums = root.array("albums").mapNotNull { it.asObject()?.toAlbum() },
                playlists = root.array("playlists").mapNotNull { it.asObject()?.toPlaylist() },
                wave = root.array("wave").mapNotNull { it.asObject()?.toTrack() },
                chart = root.array("chart").mapNotNull { it.asObject()?.toTrack() },
            )
        }.getOrNull()
    }

    suspend fun write(
        account: YandexClient.Account,
        liked: List<YandexClient.Track>,
        albums: List<YandexClient.Album>,
        playlists: List<YandexClient.Playlist>,
        wave: List<YandexClient.Track>,
        chart: List<YandexClient.Track>,
    ) = withContext(Dispatchers.IO) {
        val value = buildJsonObject {
            put("v", 1)
            put("uid", account.uid)
            put("syncedAtMs", System.currentTimeMillis())
            put("liked", liked.asJsonArray { it.toJson() })
            put("albums", albums.asJsonArray { it.toJson() })
            put("playlists", playlists.asJsonArray { it.toJson() })
            put("wave", wave.asJsonArray { it.toJson() })
            put("chart", chart.asJsonArray { it.toJson() })
        }.toString().encodeToByteArray()
        runCatching {
            val output = file.startWrite()
            try {
                output.write(value)
                file.finishWrite(output)
            } catch (error: Throwable) {
                file.failWrite(output)
                throw error
            }
        }
    }

    companion object {
        fun isFresh(snapshot: LibrarySnapshot?, intervalMinutes: Int, nowMs: Long = System.currentTimeMillis()): Boolean {
            if (snapshot == null || intervalMinutes <= 0) return false
            val intervalMs = intervalMinutes * 60_000L
            return nowMs - snapshot.syncedAtMs in 0 until intervalMs
        }
    }
}

internal data class LibrarySnapshot(
    val uid: String,
    val syncedAtMs: Long,
    val liked: List<YandexClient.Track>,
    val albums: List<YandexClient.Album>,
    val playlists: List<YandexClient.Playlist>,
    val wave: List<YandexClient.Track>,
    val chart: List<YandexClient.Track>,
)

private fun <T> List<T>.asJsonArray(transform: (T) -> JsonObject): JsonArray = buildJsonArray {
    this@asJsonArray.forEach { add(transform(it)) }
}

private fun YandexClient.Track.toJson() = buildJsonObject {
    put("id", id); put("albumId", albumId); put("title", title); put("artist", artist)
    put("album", album); put("durationMs", durationMs); put("coverUrl", coverUrl?.let(::JsonPrimitive) ?: JsonNull)
    put("explicit", explicit); put("lyricsAvailable", lyricsAvailable); put("artistId", artistId)
}

private fun YandexClient.Album.toJson() = buildJsonObject {
    put("id", id); put("title", title); put("artist", artist); put("coverUrl", coverUrl?.let(::JsonPrimitive) ?: JsonNull)
    put("trackCount", trackCount)
}

private fun YandexClient.Playlist.toJson() = buildJsonObject {
    put("id", id); put("ownerUid", ownerUid); put("kind", kind); put("uuid", uuid); put("title", title)
    put("description", description); put("coverUrl", coverUrl?.let(::JsonPrimitive) ?: JsonNull)
    put("trackCount", trackCount); put("source", source)
}

private fun JsonObject.toTrack(): YandexClient.Track? {
    val id = string("id")
    if (id.isBlank()) return null
    return YandexClient.Track(
        id = id, albumId = string("albumId"), title = string("title"), artist = string("artist"),
        album = string("album"), durationMs = long("durationMs"), coverUrl = nullableString("coverUrl"),
        explicit = boolean("explicit"), lyricsAvailable = boolean("lyricsAvailable"), artistId = string("artistId"),
    )
}

private fun JsonObject.toAlbum(): YandexClient.Album? {
    val id = string("id")
    if (id.isBlank()) return null
    return YandexClient.Album(id, string("title"), string("artist"), nullableString("coverUrl"), long("trackCount").toInt())
}

private fun JsonObject.toPlaylist(): YandexClient.Playlist? {
    val id = string("id")
    if (id.isBlank()) return null
    return YandexClient.Playlist(
        id, string("ownerUid"), string("kind"), string("uuid"), string("title"), string("description"),
        nullableString("coverUrl"), long("trackCount").toInt(), string("source"),
    )
}

private fun JsonElement.asObject(): JsonObject? = this as? JsonObject
private fun JsonObject.array(key: String): JsonArray = (this[key] as? JsonArray) ?: JsonArray(emptyList())
private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.content.orEmpty()
private fun JsonObject.nullableString(key: String): String? = this[key]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() && it != "null" }
private fun JsonObject.long(key: String): Long = this[key]?.jsonPrimitive?.longOrNull ?: 0L
private fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull ?: false
