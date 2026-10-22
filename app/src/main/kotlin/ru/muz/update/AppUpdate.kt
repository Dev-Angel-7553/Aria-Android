package ru.muz.update

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class AppRelease(
    val version: String,
    val apkUrl: String,
    val apkSize: Long,
    val notes: String,
    val pageUrl: String,
)

object AppUpdate {
    const val LATEST_URL = "https://api.github.com/repos/Dev-Angel-7553/Aria-Android/releases/latest"

    private val json = Json { ignoreUnknownKeys = true }

    fun parseLatest(body: String): AppRelease? = runCatching {
        val root = json.parseToJsonElement(body).jsonObject
        if (root.bool("draft") || root.bool("prerelease")) return null
        val tag = root.text("tag_name") ?: return null
        val asset = (root["assets"] as? JsonArray).orEmpty()
            .mapNotNull { it as? JsonObject }
            .firstOrNull { it.text("name").orEmpty().endsWith(".apk", ignoreCase = true) }
            ?: return null
        AppRelease(
            version = tag.removePrefix("v").removePrefix("V"),
            apkUrl = asset.text("browser_download_url") ?: return null,
            apkSize = asset["size"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L,
            notes = root.text("body").orEmpty().trim(),
            pageUrl = root.text("html_url").orEmpty(),
        )
    }.getOrNull()

    fun isNewer(candidate: String, current: String): Boolean = compare(candidate, current) > 0

    fun compare(a: String, b: String): Int {
        val left = parts(a)
        val right = parts(b)
        for (i in 0 until maxOf(left.size, right.size)) {
            val diff = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
            if (diff != 0) return diff
        }
        return 0
    }

    private fun parts(version: String): List<Int> =
        version.trim().removePrefix("v").substringBefore('-').substringBefore('+')
            .split('.')
            .map { part -> part.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

    private fun JsonObject.text(key: String): String? =
        this[key]?.jsonPrimitive?.content?.takeUnless { it == "null" }

    private fun JsonObject.bool(key: String): Boolean = text(key) == "true"
}
