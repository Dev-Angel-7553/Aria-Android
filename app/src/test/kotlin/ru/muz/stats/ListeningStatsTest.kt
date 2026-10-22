package ru.muz.stats

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ListeningStatsTest {
    @Test
    fun `aggregates listening by day hour artist and track`() {
        val store = MemoryStore()
        val clock = MutableClock(day = 100, hour = 2)
        val stats = ListeningStats(store, clock)

        stats.recordStart("one", "Первая", "Артист А")
        stats.addListening(60_000, "one", "Первая", "Артист А")
        clock.day = 101
        clock.hour = 14
        stats.recordStart("one", "Первая", "Артист А")
        stats.addListening(120_000, "one", "Первая", "Артист А")
        clock.day = 103
        clock.hour = 20
        stats.recordStart("two", "Вторая", "Артист Б")
        stats.addListening(30_000, "two", "Вторая", "Артист Б")

        val snapshot = stats.snapshot()
        assertEquals(210_000, snapshot.listenedMs)
        assertEquals(3, snapshot.trackStarts)
        assertEquals(3, snapshot.activeDays)
        assertEquals(2, snapshot.uniqueArtists)
        assertEquals(2, snapshot.uniqueTracks)
        assertEquals(30_000, snapshot.todayMs)
        assertEquals(210_000, snapshot.weekMs)
        assertEquals(60_000, snapshot.hourHistogram[2])
        assertEquals(120_000, snapshot.hourHistogram[14])
        assertEquals(30_000, snapshot.hourHistogram[20])
        assertEquals("Артист А", snapshot.topArtist)
        assertEquals("one", snapshot.topTracks.first().id)
    }

    @Test
    fun `flush persists a partial listening interval and reload restores it`() {
        val store = MemoryStore()
        val clock = MutableClock(day = 7, hour = 9)
        val stats = ListeningStats(store, clock)

        stats.addListening(5_000, "id", "Песня", "Исполнитель")
        assertEquals(null, store.value)
        stats.flush()

        val restored = ListeningStats(store, clock).snapshot()
        assertEquals(5_000, restored.listenedMs)
        assertEquals("Песня", restored.topTracks.single().title)
        assertEquals("Исполнитель", restored.topArtists.single().name)
        assertEquals(5_000, restored.hourHistogram[9])
    }

    @Test
    fun `streaks preserve the best run and allow an unopened today`() {
        val clock = MutableClock(day = 40, hour = 12)
        val stats = ListeningStats(MemoryStore(), clock)

        listOf(34L, 35L, 36L, 39L).forEach { day ->
            clock.day = day
            stats.recordStart("track-$day", "Песня", "Артист")
        }
        clock.day = 40

        val snapshot = stats.snapshot()
        assertEquals(1, snapshot.currentStreak)
        assertEquals(3, snapshot.bestStreak)
    }

    @Test
    fun `rankings use listening time then starts and keep track metadata`() {
        val clock = MutableClock(day = 1, hour = 1)
        val stats = ListeningStats(MemoryStore(), clock)

        stats.recordStart("a", "Старое имя", "Первый")
        stats.addListening(30_000, "a", "Новое имя", "Первый")
        stats.recordStart("b", "Вторая", "Второй")
        stats.recordStart("b", "Вторая", "Второй")
        stats.addListening(30_000, "b", "Вторая", "Второй")

        val snapshot = stats.snapshot()
        assertEquals("Второй", snapshot.topArtists.first().name)
        assertEquals(2, snapshot.topArtists.first().starts)
        assertEquals("b", snapshot.topTracks.first().id)
        assertEquals("Новое имя", snapshot.topTracks.first { it.id == "a" }.title)
    }

    @Test
    fun `pruning bounds detailed days without reducing lifetime totals`() {
        val store = MemoryStore()
        val clock = MutableClock(day = 1, hour = 0)
        val stats = ListeningStats(store, clock)

        repeat(ListeningStats.MAX_DAYS + 1) { index ->
            clock.day = index.toLong()
            stats.addListening(ListeningStats.PERSIST_INTERVAL_MS, "one", "Песня", "Артист")
        }

        val snapshot = stats.snapshot()
        assertEquals((ListeningStats.MAX_DAYS + 1) * ListeningStats.PERSIST_INTERVAL_MS, snapshot.listenedMs)
        assertEquals(ListeningStats.MAX_DAYS + 1, snapshot.activeDays)
        val savedDays = JSONObject(store.value!!).getJSONObject("days")
        assertEquals(ListeningStats.MAX_DAYS, savedDays.length())
        assertTrue(!savedDays.has("0"))
    }

    private class MemoryStore(var value: String? = null) : StatsStore {
        override fun read(): String? = value
        override fun write(value: String) {
            this.value = value
        }
    }

    private class MutableClock(var day: Long, var hour: Int) : StatsClock {
        override fun epochDay(): Long = day
        override fun hour(): Int = hour
    }
}
