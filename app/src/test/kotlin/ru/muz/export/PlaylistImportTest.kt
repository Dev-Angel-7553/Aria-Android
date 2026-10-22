package ru.muz.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistImportTest {
    @Test
    fun readsOwnExportBack() {
        val rows = listOf(
            PlaylistCsv.Row("Say \"Hi\"", "Tyler, The Creator", "Album", "Мне нравится", PlaylistCsv.Kind.FAVORITE, 1000, "1", "2"),
            PlaylistCsv.Row("Кукушка", "Кино", "", "Рок", PlaylistCsv.Kind.PLAYLIST, 1000, "3", "4"),
        )
        val entries = PlaylistImport.entries(String(PlaylistCsv.encode(rows), Charsets.UTF_8))
        assertEquals(2, entries.size)
        assertEquals("Say \"Hi\"", entries[0].title)
        assertEquals("Tyler, The Creator", entries[0].artist)
        assertEquals("Мне нравится", entries[0].playlist)
        assertEquals(setOf("Мне нравится", "Рок"), PlaylistImport.groups(entries, "Импорт").keys)
    }

    @Test
    fun readsExportifyStyleWithSemicolonArtists() {
        val csv = "\"Track URI\",\"Track Name\",\"Artist Name(s)\",\"Album Name\"\n" +
            "\"spotify:track:x\",\"Blinding Lights\",\"The Weeknd\",\"After Hours\"\n" +
            "\"spotify:track:y\",\"Stay\",\"The Kid LAROI;Justin Bieber\",\"F*CK LOVE 3\"\n"
        val entries = PlaylistImport.entries(csv)
        assertEquals("Blinding Lights", entries[0].title)
        assertEquals("The Kid LAROI, Justin Bieber", entries[1].artist)
    }

    @Test
    fun readsSemicolonDelimitedAndPlainLines() {
        val csv = "Исполнитель;Название\nЗемфира;Хочешь?\n"
        assertEquals(PlaylistImport.Entry("Хочешь?", "Земфира"), PlaylistImport.entries(csv).single())
        assertEquals(PlaylistImport.Entry("Numb", "Linkin Park"), PlaylistImport.splitLine("Linkin Park — Numb"))
        val plain = PlaylistImport.entries("Linkin Park - Numb\nMuse - Uprising\n")
        assertEquals(listOf("Numb", "Uprising"), plain.map { it.title })
    }

    @Test
    fun matchingIgnoresDecorationsAndCase() {
        val entry = PlaylistImport.Entry("Bohemian Rhapsody - Remastered 2011", "Queen")
        assertTrue(PlaylistImport.score(entry, "Bohemian Rhapsody", "Queen") > 0.9)
        assertTrue(PlaylistImport.score(entry, "We Will Rock You", "Queen") < PlaylistImport.MATCH_THRESHOLD)
        val feat = PlaylistImport.Entry("Peaches (feat. Daniel Caesar & Giveon)", "Justin Bieber")
        assertTrue(PlaylistImport.score(feat, "Peaches", "Justin Bieber, Daniel Caesar, Giveon") > 0.9)
        val wrongArtist = PlaylistImport.score(PlaylistImport.Entry("Numb", "Linkin Park"), "Numb", "Marina")
        assertTrue(wrongArtist < 0.9)
        assertEquals("ежик", PlaylistImport.normalize("Ёжик!"))
    }

    @Test
    fun queryUsesFirstArtistAndCleanTitle() {
        assertEquals("Justin Bieber Peaches", PlaylistImport.query(PlaylistImport.Entry("Peaches (feat. Giveon)", "Justin Bieber, Giveon")))
    }
}
