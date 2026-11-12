package ru.muz.yandex

import java.io.InputStream
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source

class UploadFile(
    val name: String,
    val mimeType: String,
    val size: Long,
    val open: () -> InputStream,
)

data class UploadTarget(val postTarget: String, val ugcTrackId: String?)

class TrackUploadException(message: String, val reason: Reason) : Exception(message) {
    enum class Reason { LIMIT, FORMAT, TOO_LARGE, AUTH, TEMPORARY, ENDPOINT, REJECTED }
}

object TrackUpload {
    const val MAX_BYTES = 200L * 1024 * 1024
    val EXTENSIONS = setOf("mp3", "m4a", "aac", "flac")
    val MIME_TYPES = arrayOf("audio/mpeg", "audio/mp3", "audio/mp4", "audio/x-m4a", "audio/aac", "audio/flac", "audio/x-flac", "audio/*")
    internal val RETRY_PAUSES_MS = listOf(5_000L, 20_000L)
    internal val TEMPORARY = setOf(500, 502, 503, 504)
    private val YANDEX_DOMAINS = listOf("yandex.ru", "yandex.net", "yandex.com")
    private val json = Json { ignoreUnknownKeys = true }

    fun accepts(name: String): Boolean = name.substringAfterLast('.', "").lowercase() in EXTENSIONS

    fun mimeFor(name: String, reported: String?): String {
        if (!reported.isNullOrBlank() && reported != "application/octet-stream") return reported
        return when (name.substringAfterLast('.', "").lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            else -> "application/octet-stream"
        }
    }

    fun check(file: UploadFile) {
        if (!accepts(file.name)) {
            throw TrackUploadException("Яндекс принимает только MP3, AAC и FLAC", TrackUploadException.Reason.FORMAT)
        }
        if (file.size > MAX_BYTES) {
            throw TrackUploadException("Файл больше 200 МБ", TrackUploadException.Reason.TOO_LARGE)
        }
    }

    fun isYandex(url: String): Boolean {
        val parsed = url.toHttpUrlOrNull() ?: return false
        if (!parsed.isHttps) return false
        val host = parsed.host.lowercase()
        return YANDEX_DOMAINS.any { host == it || host.endsWith(".$it") }
    }

    fun parseTarget(body: String): UploadTarget? {
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val payload = (root["result"] as? JsonObject) ?: root
        val status = ((payload["result"] ?: root["result"]) as? JsonPrimitive)?.content.orEmpty()
        if (status.uppercase().replace('-', '_') == "TOO_MANY_FILES") {
            throw TrackUploadException("В аккаунте уже 10 000 своих треков, Яндекс больше не принимает", TrackUploadException.Reason.LIMIT)
        }
        val target = (payload["post-target"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() } ?: return null
        val id = (payload["ugc-track-id"] as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
        return UploadTarget(target, id)
    }

    fun failure(code: Int, body: String): TrackUploadException {
        val text = body.trim().take(200)
        return when {
            code in TEMPORARY -> TrackUploadException("Сервер загрузки Яндекса не ответил вовремя", TrackUploadException.Reason.TEMPORARY)
            code == 415 || text.contains("UNSUPPORTED_MEDIA_TYPE", ignoreCase = true) ->
                TrackUploadException("Яндекс не смог прочитать файл. Подойдёт обычный MP3", TrackUploadException.Reason.FORMAT)
            code == 413 -> TrackUploadException("Файл больше 200 МБ", TrackUploadException.Reason.TOO_LARGE)
            code == 401 || code == 403 -> TrackUploadException("Яндекс не принял вход. Войдите заново", TrackUploadException.Reason.AUTH)
            code in setOf(404, 405, 410, 501) -> TrackUploadException("Яндекс изменил способ загрузки (HTTP $code)", TrackUploadException.Reason.ENDPOINT)
            else -> TrackUploadException("Яндекс отклонил файл (HTTP $code)", TrackUploadException.Reason.REJECTED)
        }
    }

    internal fun body(file: UploadFile, onProgress: (Long) -> Unit, checkActive: () -> Unit): RequestBody = object : RequestBody() {
        override fun contentType(): MediaType? = file.mimeType.toMediaTypeOrNull()
        override fun contentLength(): Long = if (file.size > 0) file.size else -1
        override fun writeTo(sink: BufferedSink) {
            file.open().source().use { source ->
                val buffer = okio.Buffer()
                var sent = 0L
                while (true) {
                    checkActive()
                    val read = source.read(buffer, 64 * 1024L)
                    if (read < 0) break
                    sink.write(buffer, read)
                    sent += read
                    onProgress(sent)
                }
            }
        }
    }
}
