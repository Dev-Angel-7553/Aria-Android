package ru.muz.scrobble

import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.*
import okhttp3.FormBody
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import ru.muz.net.readText

internal class ScrobbleApi(
    private val http: OkHttpClient = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS).callTimeout(18, TimeUnit.SECONDS).build(),
) {
    sealed interface Delivery {
        data object Accepted : Delivery
        data object Retry : Delivery
        data object Unauthorized : Delivery
        data class Rejected(val message: String) : Delivery
    }

    suspend fun listenBrainzUser(token: String): String {
        val response = http.newCall(Request.Builder().url("https://api.listenbrainz.org/1/validate-token")
            .header("Authorization", "Token ${token.trim()}").build()).readText()
        require(response.isSuccessful) { "Не удалось проверить токен ListenBrainz" }
        val obj = Json.parseToJsonElement(response.body).jsonObject
        require(obj["valid"]?.jsonPrimitive?.booleanOrNull == true) { "Токен ListenBrainz недействителен" }
        return obj["user_name"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
            ?: error("ListenBrainz не вернул имя аккаунта")
    }

    suspend fun lastFmToken(key: String, secret: String): String = lastFm(
        mapOf("method" to "auth.getToken", "api_key" to key), secret,
    )["token"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() } ?: error("Last.fm не вернул токен входа")

    suspend fun lastFmSession(key: String, secret: String, token: String): Pair<String, String> {
        val session = lastFm(mapOf("method" to "auth.getSession", "api_key" to key, "token" to token), secret)["session"]?.jsonObject
            ?: error("Не удалось подключить Last.fm")
        val name = session["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val sk = session["key"]?.jsonPrimitive?.contentOrNull.orEmpty()
        require(name.isNotBlank() && sk.isNotBlank()) { "Last.fm вернул пустой аккаунт" }
        return name to sk
    }

    fun authorizationUrl(key: String, token: String): String = "https://www.last.fm/api/auth/".toHttpUrl().newBuilder()
        .addQueryParameter("api_key", key).addQueryParameter("token", token).build().toString()

    private suspend fun lastFm(parameters: Map<String, String>, secret: String): JsonObject {
        val response = http.newCall(lastFmRequest(parameters, secret)).readText()
        require(response.isSuccessful) { "Last.fm временно недоступен. Попробуйте позже" }
        val obj = Json.parseToJsonElement(response.body).jsonObject
        val code = obj["error"]?.jsonPrimitive?.intOrNull
        require(code == null) {
            when (code) {
                14 -> "Сначала разрешите доступ в браузере"
                4, 10, 13, 26 -> "Проверьте API key и shared secret Last.fm"
                15 -> "Вход устарел. Начните подключение заново"
                else -> "Не удалось подключить Last.fm. Попробуйте позже"
            }
        }
        return obj
    }

    suspend fun deliver(account: ScrobbleAccount, play: ScrobblePlay, nowPlaying: Boolean): Delivery {
        return try {
            val request = if (account.service == ScrobbleService.LASTFM) {
                lastFmRequest(lastFmTrackParameters(account, play, nowPlaying), account.secret)
            } else Request.Builder().url("https://api.listenbrainz.org/1/submit-listens")
                .header("Authorization", "Token ${account.token}")
                .post(listenBrainzPayload(play, nowPlaying).toString().toRequestBody("application/json".toMediaType())).build()
            val response = http.newCall(request).readText()
            if (response.code == 401 || response.code == 403) return Delivery.Unauthorized
            if (response.code == 429 || response.code >= 500) return Delivery.Retry
            if (!response.isSuccessful) return Delivery.Rejected("Сервис отклонил прослушивание")
            val obj = Json.parseToJsonElement(response.body).jsonObject
            if (account.service == ScrobbleService.LISTENBRAINZ) {
                if (obj["status"]?.jsonPrimitive?.contentOrNull == "ok") Delivery.Accepted else Delivery.Retry
            } else {
                when (obj["error"]?.jsonPrimitive?.intOrNull) {
                    4, 9, 10, 13, 26 -> Delivery.Unauthorized
                    11, 16, 29 -> Delivery.Retry
                    null -> {
                        if (nowPlaying && obj["nowplaying"] is JsonObject) Delivery.Accepted
                        else {
                            val accepted = obj["scrobbles"]?.jsonObject?.get("@attr")?.jsonObject?.get("accepted")?.jsonPrimitive?.intOrNull
                            if (accepted != null && accepted > 0) Delivery.Accepted
                            else if (accepted == 0) Delivery.Rejected("Last.fm не засчитал прослушивание")
                            else Delivery.Retry
                        }
                    }
                    else -> Delivery.Rejected("Last.fm отклонил прослушивание")
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { Delivery.Retry }
    }

    private fun lastFmRequest(parameters: Map<String, String>, secret: String): Request {
        val body = FormBody.Builder()
        (parameters + mapOf("api_sig" to signature(parameters, secret), "format" to "json")).forEach { (k, v) -> body.add(k, v) }
        return Request.Builder().url("https://ws.audioscrobbler.com/2.0/")
            .header("User-Agent", "Aria Android").post(body.build()).build()
    }

    companion object {
        fun signature(parameters: Map<String, String>, secret: String): String {
            val raw = parameters.filterKeys { it !in setOf("format", "callback", "api_sig") }.toSortedMap()
                .entries.joinToString("") { it.key + it.value } + secret
            return MessageDigest.getInstance("MD5").digest(raw.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
        }

        fun lastFmTrackParameters(account: ScrobbleAccount, play: ScrobblePlay, nowPlaying: Boolean): Map<String, String> = buildMap {
            put("method", if (nowPlaying) "track.updateNowPlaying" else "track.scrobble")
            put("api_key", account.apiKey)
            put("sk", account.token)
            put("artist", play.track.artist)
            put("track", play.track.title)
            if (play.track.durationMs > 0) put("duration", (play.track.durationMs / 1000).toString())
            if (play.track.album.isNotBlank()) put("album", play.track.album)
            if (!nowPlaying) put("timestamp", play.startedAt.toString())
        }

        fun listenBrainzPayload(play: ScrobblePlay, nowPlaying: Boolean): JsonObject = buildJsonObject {
            put("listen_type", if (nowPlaying) "playing_now" else "single")
            putJsonArray("payload") {
                add(buildJsonObject {
                    if (!nowPlaying) put("listened_at", play.startedAt)
                    putJsonObject("track_metadata") {
                        put("artist_name", play.track.artist)
                        put("track_name", play.track.title)
                        if (play.track.album.isNotBlank()) put("release_name", play.track.album)
                        putJsonObject("additional_info") {
                            put("submission_client", "Aria")
                            put("media_player", "Aria")
                            if (play.track.durationMs > 0) put("duration_ms", play.track.durationMs)
                            if (play.track.artists.isNotEmpty()) putJsonArray("artist_names") { play.track.artists.forEach { add(JsonPrimitive(it)) } }
                        }
                    }
                })
            }
        }
    }
}
