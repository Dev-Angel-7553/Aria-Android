package ru.muz.export

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistCsvTest {
    private fun row(
        title: String = "Песня",
        artist: String = "Артист",
        album: String = "Альбом",
        playlist: String = "Мне нравится",
        trackId: String = "123",
        albumId: String = "45",
    ) = PlaylistCsv.Row(title, artist, album, playlist, PlaylistCsv.Kind.FAVORITE, 185_000, trackId, albumId)

    @Test
    fun headerAndRowUseCrlfAndServiceColumnNames() {
        val text = PlaylistCsv.render(listOf(row()))
        assertEquals(
            "Track name,Artist name,Album,Playlist name,Type,Duration,Yandex Music URL\r\n" +
                "Песня,Артист,Альбом,Мне нравится,Favorite,3:05,https://music.yandex.ru/album/45/track/123\r\n",
            text,
        )
    }

    @Test
    fun quotesCommasQuotesAndLineBreaks() {
        assertEquals("\"Tyler, The Creator\"", PlaylistCsv.escape("Tyler, The Creator"))
        assertEquals("\"Say \"\"Hi\"\"\"", PlaylistCsv.escape("Say \"Hi\""))
        assertEquals("\"a\nb\"", PlaylistCsv.escape("a\nb"))
        assertEquals("\" padded \"", PlaylistCsv.escape(" padded "))
        assertEquals("plain", PlaylistCsv.escape("plain"))
    }

    @Test
    fun encodedFileStartsWithUtf8Bom() {
        val bytes = PlaylistCsv.encode(emptyList())
        assertEquals(0xEF.toByte(), bytes[0])
        assertEquals(0xBB.toByte(), bytes[1])
        assertEquals(0xBF.toByte(), bytes[2])
        assertTrue(String(bytes, Charsets.UTF_8).removePrefix("﻿").startsWith("Track name,"))
    }

    @Test
    fun durationAndLinks() {
        assertEquals("", PlaylistCsv.formatDuration(0))
        assertEquals("0:59", PlaylistCsv.formatDuration(59_900))
        assertEquals("1:02:03", PlaylistCsv.formatDuration(3_723_000))
        assertEquals("https://music.yandex.ru/track/9", PlaylistCsv.trackUrl("9", ""))
        assertEquals("https://music.yandex.ru/album/7/track/9", PlaylistCsv.trackUrl("9:7", ""))
        assertEquals("", PlaylistCsv.trackUrl("", ""))
    }

    @Test
    fun fileNameIsSafe() {
        val date = LocalDate.of(2026, 10, 22)
        assertEquals("AC_DC_ лучшее 2026-10-22.csv", PlaylistCsv.fileName("AC/DC: лучшее", date))
        assertEquals("Плейлист 2026-10-22.csv", PlaylistCsv.fileName("  ", date))
    }
}
