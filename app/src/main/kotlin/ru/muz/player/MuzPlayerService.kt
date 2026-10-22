package ru.muz.player

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import ru.muz.MainActivity
import ru.muz.auth.TokenStore
import ru.muz.settings.AppPreferences
import ru.muz.yandex.YandexClient

class MuzPlayerService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Yandex-Music-API")
            .setAllowCrossProtocolRedirects(true)
        val resolvingFactory = ResolvingDataSource.Factory(httpFactory) { dataSpec ->
            if (dataSpec.uri.scheme != "yamusic") return@Factory dataSpec
            val trackId = dataSpec.uri.lastPathSegment ?: error("В очереди нет ID трека")
            val token = TokenStore(this).token ?: error("Токен Яндекс Музыки удалён")
            val quality = if (AppPreferences(this).load().dataSaver) {
                YandexClient.AudioQuality.ECONOMY
            } else {
                YandexClient.AudioQuality.HIGH
            }
            val direct = runBlocking(Dispatchers.IO) {
                val client = YandexClient(token)
                client.streamUrl(trackId, quality)
                    .recoverCatching {
                        client.freshStreamUrl(trackId, quality).getOrThrow()
                    }
                    .recoverCatching {
                        val fallback = if (quality == YandexClient.AudioQuality.HIGH) {
                            YandexClient.AudioQuality.ECONOMY
                        } else {
                            YandexClient.AudioQuality.HIGH
                        }
                        client.streamUrl(trackId, fallback).getOrThrow()
                    }
                    .getOrThrow()
            }
            dataSpec.withUri(android.net.Uri.parse(direct))
        }
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(resolvingFactory))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        val activityIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            activityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaSession.Builder(this, player!!)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onDestroy() {
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }
}
