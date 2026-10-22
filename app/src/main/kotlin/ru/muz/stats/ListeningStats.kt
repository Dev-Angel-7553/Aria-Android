package ru.muz.stats

import android.content.Context
import java.time.LocalDate
import java.time.LocalTime
import org.json.JSONArray
import org.json.JSONObject

const val NO_STATS_ARTIST = "Пока нет данных"

data class ArtistStat(val name: String, val listenedMs: Long, val starts: Int)

data class TrackStat(
    val id: String,
    val title: String,
    val artist: String,
    val listenedMs: Long,
    val starts: Int,
)

data class DayStat(val epochDay: Long, val listenedMs: Long, val starts: Int)

data class ListeningStatsSnapshot(
    val listenedMs: Long = 0,
    val trackStarts: Int = 0,
    val topArtist: String = NO_STATS_ARTIST,
    val uniqueArtists: Int = 0,
    val uniqueTracks: Int = 0,
    val activeDays: Int = 0,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val todayMs: Long = 0,
    val weekMs: Long = 0,
    val monthMs: Long = 0,
    val averageActiveDayMs: Long = 0,
    val longestDay: DayStat? = null,
    val topArtists: List<ArtistStat> = emptyList(),
    val topTracks: List<TrackStat> = emptyList(),
    val recentDays: List<DayStat> = emptyList(),
    val hourHistogram: List<Long> = List(24) { 0L },
) {
    val hasData: Boolean get() = listenedMs > 0 || trackStarts > 0
}
interface StatsStore {
    fun read(): String?
    fun write(value: String)
}

interface StatsClock {
    fun epochDay(): Long
    fun hour(): Int
}

internal object SystemStatsClock : StatsClock {
    override fun epochDay(): Long = LocalDate.now().toEpochDay()
    override fun hour(): Int = LocalTime.now().hour
}

private class TrackAcc(var title: String, var artist: String, var ms: Long, var starts: Int)

class ListeningStats(
    private val store: StatsStore,
    private val clock: StatsClock = SystemStatsClock,
) {
    private val artists = HashMap<String, TrackAcc>()
    private val tracks = HashMap<String, TrackAcc>()
    private val days = HashMap<Long, LongArray>()
    private val hours = LongArray(24)

    private var carryMs = 0L
    private var carryStarts = 0
    private var legacyDays = 0
    private var loaded = false
    private var dirty = false
    private var unpersistedListeningMs = 0L

    constructor(context: Context) : this(PrefsStatsStore(context))

    fun recordStart() = recordStart("", "", "")

    fun addListening(durationMs: Long, artist: String) = addListening(durationMs, "", "", artist)

    @Synchronized
    fun recordStart(trackId: String, title: String, artist: String) {
        load()
        val day = days.getOrPut(clock.epochDay()) { LongArray(2) }
        day[1]++
        if (artist.isNotBlank()) artistAcc(artist).starts++
        if (trackId.isNotBlank()) trackAcc(trackId, title, artist).starts++
        dirty = true
        persist()
    }

    @Synchronized
    fun addListening(durationMs: Long, trackId: String, title: String, artist: String) {
        if (durationMs <= 0) return
        load()
        val day = days.getOrPut(clock.epochDay()) { LongArray(2) }
        day[0] += durationMs
        hours[clock.hour().coerceIn(0, 23)] += durationMs
        if (artist.isNotBlank()) artistAcc(artist).ms += durationMs
        if (trackId.isNotBlank()) trackAcc(trackId, title, artist).ms += durationMs
        dirty = true
        unpersistedListeningMs += durationMs
        if (unpersistedListeningMs >= PERSIST_INTERVAL_MS) persist()
    }

    @Synchronized
    fun flush() {
        if (loaded && dirty) persist()
    }

    private fun artistAcc(name: String) = artists.getOrPut(name) { TrackAcc(name, name, 0L, 0) }

    private fun trackAcc(id: String, title: String, artist: String) =
        tracks.getOrPut(id) { TrackAcc(title, artist, 0L, 0) }.also {
            if (title.isNotBlank()) it.title = title
            if (artist.isNotBlank()) it.artist = artist
        }

    @Synchronized
    fun snapshot(): ListeningStatsSnapshot {
        load()
        val today = clock.epochDay()
        val totalMs = carryMs + days.values.sumOf { it[0] }
        val totalStarts = carryStarts + days.values.sumOf { it[1].toInt() }
        val activeDays = legacyDays + days.count { it.value[0] > 0 || it.value[1] > 0 }
        val topArtistList = artists.values
            .sortedWith(compareByDescending<TrackAcc> { it.ms }.thenByDescending { it.starts })
            .map { ArtistStat(it.title, it.ms, it.starts) }
        val topTrackList = tracks.entries
            .sortedWith(compareByDescending<Map.Entry<String, TrackAcc>> { it.value.ms }
                .thenByDescending { it.value.starts })
            .map { TrackStat(it.key, it.value.title, it.value.artist, it.value.ms, it.value.starts) }
        val longest = days.entries
            .filter { it.value[0] > 0 }
            .maxByOrNull { it.value[0] }
            ?.let { DayStat(it.key, it.value[0], it.value[1].toInt()) }
        return ListeningStatsSnapshot(
            listenedMs = totalMs,
            trackStarts = totalStarts,
            topArtist = topArtistList.firstOrNull()?.name ?: NO_STATS_ARTIST,
            uniqueArtists = artists.size,
            uniqueTracks = tracks.size,
            activeDays = activeDays,
            currentStreak = currentStreak(today),
            bestStreak = maxOf(bestStreak(), currentStreak(today)),
            todayMs = days[today]?.get(0) ?: 0L,
            weekMs = rangeMs(today - 6, today),
            monthMs = rangeMs(today - 29, today),
            averageActiveDayMs = if (activeDays > 0) totalMs / activeDays else 0L,
            longestDay = longest,
            topArtists = topArtistList.take(TOP_SHOWN),
            topTracks = topTrackList.take(TOP_SHOWN),
            recentDays = recentDays(today, RECENT_DAYS),
            hourHistogram = hours.toList(),
        )
    }

    private fun rangeMs(from: Long, to: Long): Long =
        days.entries.filter { it.key in from..to }.sumOf { it.value[0] }

    private fun currentStreak(today: Long): Int {
        val start = when {
            days[today].isActive() -> today
            days[today - 1].isActive() -> today - 1
            else -> return 0
        }
        var streak = 0
        var day = start
        while (days[day].isActive()) {
            streak++
            day--
        }
        return streak
    }

    private fun bestStreak(): Int {
        val active = days.filterValues { it[0] > 0 || it[1] > 0 }.keys.sorted()
        var best = 0
        var run = 0
        var previous: Long? = null
        for (day in active) {
            run = if (previous != null && day == previous + 1) run + 1 else 1
            if (run > best) best = run
            previous = day
        }
        return best
    }

    private fun LongArray?.isActive(): Boolean = this != null && (this[0] > 0 || this[1] > 0)

    private fun recentDays(today: Long, count: Int): List<DayStat> =
        (today - count + 1..today).map { day ->
            val value = days[day]
            DayStat(day, value?.get(0) ?: 0L, value?.get(1)?.toInt() ?: 0)
        }

    private fun load() {
        if (loaded) return
        loaded = true
        val raw = store.read() ?: return
        runCatching {
            val json = JSONObject(raw)
            carryMs = json.optLong("carryMs")
            carryStarts = json.optInt("carryStarts")
            legacyDays = json.optInt("legacyDays")
            json.optJSONObject("days")?.let { obj ->
                obj.keys().forEach { key ->
                    val pair = obj.optJSONArray(key) ?: return@forEach
                    key.toLongOrNull()?.let { days[it] = longArrayOf(pair.optLong(0), pair.optLong(1)) }
                }
            }
            json.optJSONObject("artists")?.let { obj ->
                obj.keys().forEach { key ->
                    val pair = obj.optJSONArray(key) ?: return@forEach
                    artists[key] = TrackAcc(key, key, pair.optLong(0), pair.optInt(1))
                }
            }
            json.optJSONObject("tracks")?.let { obj ->
                obj.keys().forEach { key ->
                    val value = obj.optJSONArray(key) ?: return@forEach
                    tracks[key] = TrackAcc(
                        value.optString(2),
                        value.optString(3),
                        value.optLong(0),
                        value.optInt(1),
                    )
                }
            }
            json.optJSONArray("hours")?.let { array ->
                for (i in 0 until minOf(24, array.length())) hours[i] = array.optLong(i)
            }
        }.onFailure {
        }
    }

    private fun persist() {
        if (!dirty) return
        prune()
        val json = JSONObject()
        json.put("v", 2)
        json.put("carryMs", carryMs)
        json.put("carryStarts", carryStarts)
        json.put("legacyDays", legacyDays)
        json.put("days", JSONObject().apply {
            days.forEach { (day, value) -> put(day.toString(), JSONArray().put(value[0]).put(value[1])) }
        })
        json.put("artists", JSONObject().apply {
            artists.forEach { (name, acc) -> put(name, JSONArray().put(acc.ms).put(acc.starts)) }
        })
        json.put("tracks", JSONObject().apply {
            tracks.forEach { (id, acc) ->
                put(id, JSONArray().put(acc.ms).put(acc.starts).put(acc.title).put(acc.artist))
            }
        })
        json.put("hours", JSONArray().apply { hours.forEach { put(it) } })
        store.write(json.toString())
        dirty = false
        unpersistedListeningMs = 0L
    }

    private fun prune() {
        if (days.size > MAX_DAYS) {
            days.keys.sorted().dropLast(MAX_DAYS).forEach { key ->
                val value = days.remove(key) ?: return@forEach
                carryMs += value[0]
                carryStarts += value[1].toInt()
                if (value[0] > 0 || value[1] > 0) legacyDays++
            }
        }
        if (artists.size > MAX_ARTISTS) {
            artists.entries.sortedBy { it.value.ms }.take(artists.size - MAX_ARTISTS)
                .forEach { artists.remove(it.key) }
        }
        if (tracks.size > MAX_TRACKS) {
            tracks.entries.sortedBy { it.value.ms }.take(tracks.size - MAX_TRACKS)
                .forEach { tracks.remove(it.key) }
        }
    }

    companion object {
        const val MAX_DAYS = 400
        const val MAX_ARTISTS = 300
        const val MAX_TRACKS = 300
        const val TOP_SHOWN = 10
        const val RECENT_DAYS = 14
        const val PERSIST_INTERVAL_MS = 30_000L

        @Volatile
        private var shared: ListeningStats? = null

        fun shared(context: Context): ListeningStats =
            shared ?: synchronized(this) {
                shared ?: ListeningStats(context.applicationContext).also { shared = it }
            }
    }
}

internal class PrefsStatsStore(context: Context) : StatsStore {
    private val prefs = context.getSharedPreferences("listening_stats", Context.MODE_PRIVATE)

    override fun read(): String? {
        prefs.getString(KEY_BLOB, null)?.let { return it }
        val time = prefs.getLong(KEY_TIME, 0L)
        val starts = prefs.getInt(KEY_STARTS, 0)
        val days = prefs.getInt(KEY_DAYS, 0)
        val artistsRaw = prefs.getString(KEY_ARTISTS, null)
        if (time == 0L && starts == 0 && days == 0 && artistsRaw.isNullOrBlank()) return null
        val json = JSONObject()
        json.put("v", 2)
        json.put("carryMs", time)
        json.put("carryStarts", starts)
        json.put("legacyDays", days)
        json.put("days", JSONObject())
        json.put("hours", JSONArray().apply { repeat(24) { put(0L) } })
        json.put("tracks", JSONObject())
        val artists = JSONObject()
        if (!artistsRaw.isNullOrBlank()) {
            runCatching {
                val old = JSONObject(artistsRaw)
                old.keys().forEach { name ->
                    artists.put(name, JSONArray().put(old.optLong(name)).put(0))
                }
            }
        }
        json.put("artists", artists)
        val blob = json.toString()
        prefs.edit()
            .putString(KEY_BLOB, blob)
            .remove(KEY_TIME)
            .remove(KEY_STARTS)
            .remove(KEY_ARTISTS)
            .remove(KEY_DAYS)
            .remove(KEY_LAST_DAY)
            .apply()
        return blob
    }

    override fun write(value: String) {
        prefs.edit().putString(KEY_BLOB, value).apply()
    }

    private companion object {
        const val KEY_BLOB = "history_v2"
        const val KEY_TIME = "listened_ms"
        const val KEY_STARTS = "track_starts"
        const val KEY_ARTISTS = "artist_ms"
        const val KEY_DAYS = "active_days"
        const val KEY_LAST_DAY = "last_active_day"
    }
}
