package ru.muz.ui

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.muz.settings.AppSettings
import ru.muz.settings.AppTab
import ru.muz.yandex.YandexClient

class AriaWaveTest {
    @Test
    fun betaTabIsOffByDefault() {
        assertFalse(AppTab.ARIA_WAVE in AppSettings().tabs)
    }

    @Test
    fun mixFiltersDuplicatesAndSeparatesArtists() {
        val result = makeAriaWave(
            tracks = listOf(track("1:1", "A"), track("1:2", "A"), track("2", "A"), track("3", "B"), track("4", "C")),
            recent = emptySet(), previous = emptySet(), saved = emptySet(), available = null,
            hideExplicit = false, mode = AriaWaveMode.MIX, random = Random(5),
        )
        assertEquals(4, result.size)
        assertEquals(4, result.map { it.id.substringBefore(':') }.toSet().size)
        assertTrue(result.zipWithNext().all { (first, second) -> first.artist != second.artist })
    }

    @Test
    fun modesRespectRecentHistoryAndOfflineAvailability() {
        val tracks = listOf(track("1", "A"), track("2", "B"), track("3", "C", explicit = true))
        val rediscover = makeAriaWave(
            tracks, recent = setOf("1:10"), previous = emptySet(), saved = setOf("1"),
            available = null, hideExplicit = true, mode = AriaWaveMode.REDISCOVER,
        )
        val offline = makeAriaWave(
            tracks, recent = emptySet(), previous = emptySet(), saved = setOf("1", "3"),
            available = setOf("1", "2", "3"), hideExplicit = true, mode = AriaWaveMode.OFFLINE,
        )
        assertEquals(listOf("2"), rediscover.map { it.id })
        assertEquals(listOf("1"), offline.map { it.id })
    }

    private fun track(id: String, artist: String, explicit: Boolean = false) = YandexClient.Track(
        id = id, albumId = "", title = "Track $id", artist = artist, album = "",
        durationMs = 200_000L, coverUrl = null, explicit = explicit, lyricsAvailable = false,
    )
}
