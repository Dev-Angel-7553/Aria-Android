package ru.muz.export

import java.text.Normalizer

object PlaylistImport {
    data class Entry(val title: String, val artist: String, val album: String = "", val playlist: String = "")

    private val TITLE = listOf("track name", "track title", "title", "song", "song name", "name", "track", "название", "трек", "песня")
    private val ARTIST = listOf("artist name(s)", "artist names", "artist name", "artists", "artist", "исполнитель", "исполнители", "артист")
    private val ALBUM = listOf("album name", "album title", "album", "альбом")
    private val PLAYLIST = listOf("playlist name", "playlist", "плейлист")

    fun parseCsv(raw: String): List<List<String>> {
        val text = raw.removePrefix("﻿")
        val delimiter = detectDelimiter(text)
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val field = StringBuilder()
        var quoted = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                quoted && c == '"' && i + 1 < text.length && text[i + 1] == '"' -> { field.append('"'); i++ }
                c == '"' && (quoted || field.isEmpty()) -> quoted = !quoted
                !quoted && c == delimiter -> { row += field.toString(); field.clear() }
                !quoted && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row += field.toString(); field.clear()
                    if (row.any { it.isNotBlank() }) rows += row.toList()
                    row.clear()
                }
                else -> field.append(c)
            }
            i++
        }
        if (field.isNotEmpty() || row.isNotEmpty()) {
            row += field.toString()
            if (row.any { it.isNotBlank() }) rows += row.toList()
        }
        return rows
    }

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty()
        return listOf(',', ';', '\t').maxByOrNull { delimiter -> firstLine.count { it == delimiter } }
            ?.takeIf { delimiter -> firstLine.count { it == delimiter } > 0 } ?: ','
    }

    fun entries(raw: String): List<Entry> {
        val rows = parseCsv(raw)
        if (rows.isEmpty()) return emptyList()
        val header = rows.first().map { it.trim().lowercase() }
        fun column(names: List<String>): Int =
            names.firstNotNullOfOrNull { name -> header.indexOf(name).takeIf { it >= 0 } } ?: -1
        val title = column(TITLE)
        val artist = column(ARTIST)
        if (title >= 0) {
            val album = column(ALBUM)
            val playlist = column(PLAYLIST)
            return rows.drop(1).mapNotNull { row ->
                val name = row.getOrNull(title)?.trim().orEmpty()
                if (name.isBlank()) null
                else Entry(
                    title = name,
                    artist = cleanArtists(row.getOrNull(artist)?.trim().orEmpty()),
                    album = row.getOrNull(album)?.trim().orEmpty(),
                    playlist = row.getOrNull(playlist)?.trim().orEmpty(),
                )
            }
        }
        return rows.mapNotNull { row ->
            if (row.size >= 2 && row[0].isNotBlank() && row[1].isNotBlank()) {
                Entry(title = row[1].trim(), artist = cleanArtists(row[0].trim()))
            } else {
                splitLine(row.firstOrNull().orEmpty())
            }
        }
    }

    fun splitLine(line: String): Entry? {
        val clean = line.trim()
        if (clean.isBlank()) return null
        val separator = listOf(" — ", " – ", " - ").firstOrNull { clean.contains(it) } ?: return Entry(clean, "")
        val artist = clean.substringBefore(separator).trim()
        val title = clean.substringAfter(separator).trim()
        return if (title.isBlank()) Entry(clean, "") else Entry(title, cleanArtists(artist))
    }

    private fun cleanArtists(value: String): String = value.replace(';', ',').split(',')
        .map { it.trim() }.filter { it.isNotBlank() }.joinToString(", ")

    fun groups(entries: List<Entry>, fallbackName: String): Map<String, List<Entry>> =
        entries.groupBy { it.playlist.ifBlank { fallbackName } }

    fun query(entry: Entry): String = listOf(entry.artist.substringBefore(','), stripDecorations(entry.title))
        .filter { it.isNotBlank() }.joinToString(" ")

    fun normalize(value: String): String {
        val lower = Normalizer.normalize(value.lowercase().replace('ё', 'е'), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
        return lower.replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim().replace(Regex("\\s+"), " ")
    }

    fun stripDecorations(title: String): String = title
        .replace(Regex("\\s*[\\(\\[][^\\)\\]]*(feat|ft\\.|with|remaster|version|edit|live|mono|stereo|deluxe|bonus)[^\\)\\]]*[\\)\\]]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("\\s+-\\s+(\\d{4}\\s+)?remaster(ed)?.*$", RegexOption.IGNORE_CASE), "")
        .trim()

    private fun tokens(value: String): Set<String> = normalize(value).split(' ').filter { it.isNotBlank() }.toSet()

    private fun similarity(a: String, b: String): Double {
        val left = normalize(stripDecorations(a))
        val right = normalize(stripDecorations(b))
        if (left.isEmpty() || right.isEmpty()) return 0.0
        if (left == right) return 1.0
        val ta = tokens(left)
        val tb = tokens(right)
        val common = ta.intersect(tb).size.toDouble()
        val jaccard = common / (ta.union(tb).size.toDouble())
        val contains = if (left.contains(right) || right.contains(left)) 0.85 else 0.0
        return maxOf(jaccard, contains)
    }

    fun score(entry: Entry, title: String, artist: String): Double {
        val titleScore = similarity(entry.title, title)
        if (entry.artist.isBlank()) return titleScore * 0.9
        val wanted = entry.artist.split(',').map { normalize(it) }.filter { it.isNotBlank() }
        val found = artist.split(',').map { normalize(it) }.filter { it.isNotBlank() }
        val artistScore = if (wanted.any { w -> found.any { f -> f == w || f.contains(w) || w.contains(f) } }) 1.0
        else similarity(entry.artist, artist)
        return titleScore * 0.65 + artistScore * 0.35
    }

    const val MATCH_THRESHOLD = 0.62
}
