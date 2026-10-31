package ru.muz.text

import java.util.Locale

internal object ArtistCredit {
    fun copyText(value: String): String = value.replace(Regex("\\s+"), " ").trim()

    fun names(values: List<String>): String = values
        .map(::copyText)
        .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) }
        .distinctBy { it.lowercase(Locale.ROOT) }
        .joinToString(", ")
}
