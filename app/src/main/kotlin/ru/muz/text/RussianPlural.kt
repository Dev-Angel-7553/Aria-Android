package ru.muz.text

fun plural(count: Int, one: String, few: String, many: String): String {
    val n = kotlin.math.abs(count) % 100
    val last = n % 10
    return when {
        n in 11..14 -> many
        last == 1 -> one
        last in 2..4 -> few
        else -> many
    }
}

fun tracksLabel(count: Int): String = "$count ${plural(count, "трек", "трека", "треков")}"

fun playlistsLabel(count: Int): String = "$count ${plural(count, "плейлист", "плейлиста", "плейлистов")}"

fun daysLabel(count: Int): String = "$count ${plural(count, "день", "дня", "дней")}"
