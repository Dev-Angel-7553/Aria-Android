package ru.muz.ui

import ru.muz.yandex.YandexClient

internal enum class QueueSort(val label: String) {
    ORDER("По порядку"),
    TITLE("По названию"),
    ARTIST("По исполнителю"),
    ;

    fun next(): QueueSort = entries[(ordinal + 1) % entries.size]
}

internal data class QueueEntry(val index: Int, val track: YandexClient.Track)

internal fun queueView(queue: List<YandexClient.Track>, query: String, sort: QueueSort): List<QueueEntry> {
    val needle = query.trim().lowercase()
    val entries = queue.mapIndexed { index, track -> QueueEntry(index, track) }
        .filter { needle.isEmpty() || it.track.title.lowercase().contains(needle) || it.track.artist.lowercase().contains(needle) }
    return when (sort) {
        QueueSort.ORDER -> entries
        QueueSort.TITLE -> entries.sortedBy { it.track.title.lowercase() }
        QueueSort.ARTIST -> entries.sortedBy { it.track.artist.lowercase() }
    }
}
