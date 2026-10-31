package ru.muz.player

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.muz.yandex.YandexClient

internal object ArtistMetadata {
    fun encode(artists: List<YandexClient.Artist>): String = buildJsonArray {
        artists.forEach { artist -> add(buildJsonObject {
            put("id", artist.id)
            put("name", artist.name)
            put("cover", artist.coverUrl.orEmpty())
        }) }
    }.toString()

    fun decode(raw: String): List<YandexClient.Artist> = runCatching {
        (Json.parseToJsonElement(raw) as JsonArray).map {
            val entry = it.jsonObject
            YandexClient.Artist(entry["id"]?.jsonPrimitive?.content.orEmpty(), entry["name"]?.jsonPrimitive?.content.orEmpty(),
                entry["cover"]?.jsonPrimitive?.content?.ifBlank { null }, 0)
        }.filter { it.name.isNotBlank() }.distinctBy { it.id.ifBlank { it.name.lowercase() } }
    }.getOrDefault(emptyList())
}
