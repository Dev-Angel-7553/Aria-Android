package ru.muz.ui

import org.junit.Test
import org.junit.Assert.assertEquals
import ru.muz.yandex.YandexClient

class QueueViewTest {
    private fun track(id: String, title: String, artist: String) =
        YandexClient.Track(id, "", title, artist, "", 1000, null, false, false)

    private val queue = listOf(
        track("1", "Zebra", "Mia"),
        track("2", "apple", "Zed"),
        track("3", "Mango", "Abe"),
    )

    @Test
    fun keepsOrderWithoutQuery() {
        assertEquals(listOf(0, 1, 2), queueView(queue, "", QueueSort.ORDER).map { it.index })
    }

    @Test
    fun filtersByTitleOrArtistIgnoringCase() {
        assertEquals(listOf(1), queueView(queue, " APP ", QueueSort.ORDER).map { it.index })
        assertEquals(listOf(0, 1), queueView(queue, "z", QueueSort.ORDER).map { it.index })
    }

    @Test
    fun sortsButKeepsOriginalIndexes() {
        assertEquals(listOf(1, 2, 0), queueView(queue, "", QueueSort.TITLE).map { it.index })
        assertEquals(listOf(2, 0, 1), queueView(queue, "", QueueSort.ARTIST).map { it.index })
    }

    @Test
    fun cyclesSortModes() {
        assertEquals(QueueSort.TITLE, QueueSort.ORDER.next())
        assertEquals(QueueSort.ORDER, QueueSort.ARTIST.next())
    }
}
