package ru.muz.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.muz.yandex.YandexClient

class PlaylistViewTest {
    private val tracks = listOf(
        YandexClient.Track("1", "", "Zebra", "One", "Alpha", 1000L, null, false, false, addedAtMs = 200),
        YandexClient.Track("2", "", "Apple", "Two", "Beta", 1000L, null, false, false, addedAtMs = 100),
        YandexClient.Track("3", "", "Mango", "Three", "Alpha", 1000L, null, false, false),
    )

    @Test
    fun searchesAlbumsAndArtistsInSmallPlaylists() {
        assertEquals(listOf("1", "3"), playlistView(tracks, " ALPHA ", "Порядок").map { it.id })
        assertEquals(listOf("2"), playlistView(tracks, "two", "Порядок").map { it.id })
    }

    @Test
    fun sortsByActualDatesAndKeepsUnknownDatesLast() {
        assertEquals(listOf("1", "2", "3"), playlistView(tracks, "", "Сначала новые").map { it.id })
        assertEquals(listOf("2", "1", "3"), playlistView(tracks, "", "Сначала старые").map { it.id })
        assertEquals(listOf("3", "2", "1"), playlistView(tracks, "", "Обратный порядок").map { it.id })
        assertEquals(listOf("2", "3", "1"), playlistView(tracks, "", "Название").map { it.id })
    }
}
