package ru.muz.ui

import java.util.Locale
import ru.muz.yandex.YandexClient

internal fun playlistView(tracks: List<YandexClient.Track>, query: String, sort: String): List<YandexClient.Track> {
    val needle = query.trim()
    val filtered = tracks.filter { needle.isEmpty() || it.title.contains(needle, true) || it.artist.contains(needle, true) || it.album.contains(needle, true) }
    return when (sort) {
        "Название" -> filtered.sortedBy { it.title.lowercase(Locale.ROOT) }
        "Исполнитель" -> filtered.sortedBy { it.artist.lowercase(Locale.ROOT) }
        "Сначала новые" -> filtered.sortedByDescending { it.addedAtMs }
        "Сначала старые" -> filtered.sortedBy { if (it.addedAtMs > 0) it.addedAtMs else Long.MAX_VALUE }
        "Обратный порядок" -> filtered.reversed()
        else -> filtered
    }
}
