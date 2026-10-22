package ru.muz.text

import org.junit.Assert.assertEquals
import org.junit.Test

class RussianPluralTest {
    @Test
    fun tracks() {
        assertEquals("0 треков", tracksLabel(0))
        assertEquals("1 трек", tracksLabel(1))
        assertEquals("2 трека", tracksLabel(2))
        assertEquals("4 трека", tracksLabel(4))
        assertEquals("5 треков", tracksLabel(5))
        assertEquals("11 треков", tracksLabel(11))
        assertEquals("12 треков", tracksLabel(12))
        assertEquals("14 треков", tracksLabel(14))
        assertEquals("21 трек", tracksLabel(21))
        assertEquals("22 трека", tracksLabel(22))
        assertEquals("111 треков", tracksLabel(111))
        assertEquals("1001 трек", tracksLabel(1001))
    }

    @Test
    fun days() {
        assertEquals("1 день", daysLabel(1))
        assertEquals("3 дня", daysLabel(3))
        assertEquals("7 дней", daysLabel(7))
    }
}
