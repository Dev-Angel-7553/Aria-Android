package ru.muz.i18n

import android.content.Context
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

data class LanguageInfo(val code: String, val name: String, val version: Int)

object I18n {
    const val SYSTEM = "system"
    const val SOURCE = "ru"
    private const val REMOTE = "https://raw.githubusercontent.com/Dev-Angel-7553/Aria-Android/main/app/src/main/assets/i18n/"

    var language by mutableStateOf(SOURCE)
        private set
    var languages by mutableStateOf(listOf(LanguageInfo(SOURCE, "Русский", 0)))
        private set
    private var table by mutableStateOf<Map<String, String>>(emptyMap())
    private var templates: List<Pair<Regex, String>> = emptyList()
    private val memo = LruCache<String, String>(1200)
    private val cyrillic = Regex("[А-Яа-яЁё]")
    private val json = Json { ignoreUnknownKeys = true }
    private val http by lazy {
        OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS).build()
    }

    fun t(text: String): String {
        if (language == SOURCE || text.isEmpty()) return text
        val current = table
        if (current.isEmpty() || !cyrillic.containsMatchIn(text)) return text
        memo.get(text)?.let { return it }
        val result = current[text] ?: current[text.trim()]?.let { text.replace(text.trim(), it) } ?: matchTemplate(text) ?: text
        memo.put(text, result)
        return result
    }

    private fun matchTemplate(text: String): String? {
        for ((pattern, target) in templates) {
            val match = pattern.matchEntire(text) ?: continue
            var out = target
            match.groupValues.drop(1).forEachIndexed { index, value -> out = out.replace("{$index}", t(value)) }
            return out
        }
        return null
    }

    fun resolve(preference: String): String {
        val wanted = if (preference == SYSTEM || preference.isBlank()) Locale.getDefault().language else preference
        return when {
            wanted == SOURCE -> SOURCE
            languages.any { it.code == wanted } -> wanted
            preference == SYSTEM && wanted in setOf("uk", "be", "kk", "ky", "uz", "tg", "hy", "az", "ka") -> SOURCE
            else -> "en"
        }
    }

    fun loadSync(context: Context, preference: String) {
        readIndex(context)
        apply(context, resolve(preference))
    }

    suspend fun refresh(context: Context, preference: String) = withContext(Dispatchers.IO) {
        runCatching {
            val index = download("index.json") ?: return@runCatching
            val remote = parseIndex(index)
            if (remote.isEmpty()) return@runCatching
            File(dir(context), "index.json").writeText(index)
            val known = languages.associateBy { it.code }
            languages = remote
            val target = resolve(preference)
            val info = remote.firstOrNull { it.code == target }
            if (info != null && target != SOURCE) {
                val cached = File(dir(context), "$target.json")
                val cachedVersion = cached.takeIf { it.exists() }?.let { packVersion(it.readText()) } ?: -1
                if (info.version > cachedVersion || info.version > (known[target]?.version ?: -1)) {
                    download("$target.json")?.let { body ->
                        if (parsePack(body).isNotEmpty()) cached.writeText(body)
                    }
                }
            }
            withContext(Dispatchers.Main) { apply(context, target) }
        }
    }

    fun select(context: Context, preference: String) = apply(context, resolve(preference))

    private fun apply(context: Context, code: String) {
        if (code == SOURCE) {
            language = SOURCE
            table = emptyMap()
            templates = emptyList()
            memo.evictAll()
            return
        }
        val text = File(dir(context), "$code.json").takeIf { it.exists() }?.readText()
            ?.takeIf { parsePack(it).isNotEmpty() }
            ?: runCatching { context.assets.open("i18n/$code.json").bufferedReader().use { it.readText() } }.getOrNull()
            ?: return
        val strings = parsePack(text)
        if (strings.isEmpty()) return
        templates = strings.filterKeys { it.contains("{0}") }.map { (key, value) -> pattern(key) to value }
            .sortedByDescending { it.first.pattern.length }
        memo.evictAll()
        table = strings
        language = code
    }

    private fun pattern(key: String): Regex {
        val parts = Regex("\\{\\d+\\}").split(key)
        return Regex(parts.joinToString("(.+?)") { Regex.escape(it) }, RegexOption.DOT_MATCHES_ALL)
    }

    private fun readIndex(context: Context) {
        val text = File(dir(context), "index.json").takeIf { it.exists() }?.readText()
            ?: runCatching { context.assets.open("i18n/index.json").bufferedReader().use { it.readText() } }.getOrNull()
            ?: return
        parseIndex(text).takeIf { it.isNotEmpty() }?.let { languages = it }
    }

    private fun parseIndex(text: String): List<LanguageInfo> = runCatching {
        json.parseToJsonElement(text).jsonObject["languages"]!!.jsonArray.mapNotNull { item ->
            val obj = item.jsonObject
            val code = obj["code"]?.jsonPrimitive?.content ?: return@mapNotNull null
            LanguageInfo(code, obj["name"]?.jsonPrimitive?.content ?: code, obj["version"]?.jsonPrimitive?.intOrNull ?: 0)
        }
    }.getOrDefault(emptyList())

    private fun parsePack(text: String): Map<String, String> = runCatching {
        val strings = json.parseToJsonElement(text).jsonObject["strings"] as JsonObject
        strings.mapValues { it.value.jsonPrimitive.content }
    }.getOrDefault(emptyMap())

    private fun packVersion(text: String): Int = runCatching {
        json.parseToJsonElement(text).jsonObject["version"]?.jsonPrimitive?.intOrNull ?: 0
    }.getOrDefault(0)

    private fun download(name: String): String? = runCatching {
        http.newCall(Request.Builder().url(REMOTE + name).build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    }.getOrNull()

    private fun dir(context: Context): File = File(context.filesDir, "i18n").apply { mkdirs() }
}
