package ru.muz.text

import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistCreditTest {
    @Test
    fun `credits preserve punctuation inside artist names`() {
        assertEquals("Tyler, The Creator, Florence + The Machine", ArtistCredit.names(listOf("Tyler, The Creator", "Florence + The Machine")))
    }

    @Test
    fun `credits normalize whitespace and remove duplicate and missing names`() {
        assertEquals("Дайте танк (!), Земфира", ArtistCredit.names(listOf(" Дайте  танк (!) ", "", "null", "дайте танк (!)", "Земфира")))
    }

    @Test
    fun `copying keeps all collaborators without splitting their names`() {
        assertEquals("A & B, C feat. D", ArtistCredit.copyText("  A & B,\n C feat. D  "))
    }

    @Test
    fun `empty credits stay empty`() {
        assertEquals("", ArtistCredit.names(listOf(" ", "null")))
        assertEquals("", ArtistCredit.copyText("\n  "))
    }
}
