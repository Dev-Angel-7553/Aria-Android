package ru.muz.export

import java.time.LocalDate
import java.util.Locale

object PlaylistCsv {
    enum class Kind(val label: String) { PLAYLIST("Playlist"), FAVORITE("Favorite"), ALBUM("Album") }

    data class Row(
        val title: String,
        val artist: String,
        val album: String,
        val playlist: String,
        val kind: Kind,
        val durationMs: Long,
        val trackId: String,
        val albumId: String,
    )

    val HEADER = listOf(
        "Track name",
        "Artist name",
        "Album",
        "Playlist name",
        "Type",
        "Duration",
        "Yandex Music URL",
    )

    fun encode(rows: List<Row>): ByteArray = ("\uFEFF" + render(rows)).toByteArray(Charsets.UTF_8)

    fun render(rows: List<Row>): String = buildString {
        appendRow(HEADER)
        rows.forEach { row ->
            appendRow(
                listOf(
                    row.title,
                    row.artist,
                    row.album,
                    row.playlist,
                    row.kind.label,
                    formatDuration(row.durationMs),
                    trackUrl(row.trackId, row.albumId),
                ),
            )
        }
    }

    private fun StringBuilder.appendRow(fields: List<String>) {
        fields.joinTo(this, separator = ",", transform = ::escape)
        append("\r\n")
    }

    fun escape(value: String): String {
        val clean = value.replace("\u0000", "")
        val needsQuotes = clean.any { it == ',' || it == '"' || it == '\n' || it == '\r' } ||
            clean != clean.trim()
        return if (needsQuotes) "\"" + clean.replace("\"", "\"\"") + "\"" else clean
    }

    fun formatDuration(durationMs: Long): String {
        if (durationMs <= 0) return ""
        val totalSeconds = durationMs / 1000
        val hours = totalSeconds / 3600
        val minutes = totalSeconds % 3600 / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }

    fun trackUrl(trackId: String, albumId: String): String {
        val id = trackId.substringBefore(':').trim()
        if (id.isEmpty()) return ""
        val album = albumId.ifBlank { trackId.substringAfter(':', "") }.trim()
        return if (album.isEmpty()) "https://music.yandex.ru/track/$id"
        else "https://music.yandex.ru/album/$album/track/$id"
    }

    fun fileName(title: String, date: LocalDate = LocalDate.now()): String {
        val safe = title.map { if (it in INVALID_FILE_NAME_CHARS || it.isISOControl()) '_' else it }
            .joinToString("")
            .trim()
            .trim('.')
            .take(80)
            .ifBlank { "Плейлист" }
        return "$safe $date.csv"
    }

    private val INVALID_FILE_NAME_CHARS = charArrayOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
}
