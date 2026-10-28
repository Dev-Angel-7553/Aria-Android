package ru.muz.player

import android.content.Context
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheWriter
import androidx.media3.datasource.cache.ContentMetadata
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ru.muz.auth.TokenStore
import ru.muz.settings.AppPreferences
import ru.muz.yandex.YandexClient

object AudioCache {
    private const val MB = 1024L * 1024L

    @Volatile
    private var instance: SimpleCache? = null
    private var disabled = false

    fun cache(context: Context): SimpleCache? {
        instance?.let { return it }
        synchronized(this) {
            instance?.let { return it }
            if (disabled) return null
            val app = context.applicationContext
            val limitMb = AppPreferences(app).load().cacheSizeMb
            if (limitMb <= 0) {
                disabled = true
                return null
            }
            return SimpleCache(
                File(app.filesDir, "audio_cache"),
                LeastRecentlyUsedCacheEvictor(limitMb * MB),
                StandaloneDatabaseProvider(app),
            ).also { instance = it }
        }
    }

    fun quality(context: Context): YandexClient.AudioQuality =
        if (AppPreferences(context).load().dataSaver) YandexClient.AudioQuality.ECONOMY
        else YandexClient.AudioQuality.HIGH

    fun cacheKey(trackId: String, quality: YandexClient.AudioQuality): String =
        "yamusic:${trackId.substringBefore(':')}:${quality.name}"

    private fun upstreamFactory(context: Context): DataSource.Factory {
        val app = context.applicationContext
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("Yandex-Music-API")
            .setAllowCrossProtocolRedirects(true)
        return ResolvingDataSource.Factory(http) { dataSpec ->
            if (dataSpec.uri.scheme != "yamusic") return@Factory dataSpec
            val trackId = dataSpec.uri.lastPathSegment ?: error("В очереди нет ID трека")
            val token = TokenStore(app).token ?: error("Токен Яндекс Музыки удалён")
            val quality = quality(app)
            val direct = runBlocking(Dispatchers.IO) {
                val client = YandexClient(token)
                client.streamUrl(trackId, quality)
                    .recoverCatching { client.freshStreamUrl(trackId, quality).getOrThrow() }
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
            dataSpec.withUri(Uri.parse(direct))
        }
    }

    private fun cacheFactory(context: Context, cache: SimpleCache): CacheDataSource.Factory {
        val app = context.applicationContext
        return CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(upstreamFactory(app))
            .setCacheKeyFactory { spec ->
                if (spec.uri.scheme == "yamusic") cacheKey(spec.uri.lastPathSegment.orEmpty(), quality(app))
                else spec.key ?: spec.uri.toString()
            }
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    fun dataSourceFactory(context: Context): DataSource.Factory {
        val cache = cache(context) ?: return upstreamFactory(context)
        return cacheFactory(context, cache)
    }

    fun usedBytes(context: Context): Long = cache(context)?.cacheSpace ?: 0L

    fun clear(context: Context) {
        val cache = cache(context) ?: return
        cache.keys.toList().forEach { key -> runCatching { cache.removeResource(key) } }
    }

    fun cachedTrackIds(context: Context): Set<String> {
        val cache = cache(context) ?: return emptySet()
        val suffix = ":" + quality(context).name
        return runCatching {
            cache.keys.filter { key ->
                key.startsWith("yamusic:") && key.endsWith(suffix) && run {
                    val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
                    length > 0 && cache.isCached(key, 0, length)
                }
            }.mapTo(HashSet()) { it.removePrefix("yamusic:").removeSuffix(suffix) }
        }.getOrDefault(emptySet())
    }

    fun isCached(context: Context, trackId: String): Boolean {
        val cache = cache(context) ?: return false
        val key = cacheKey(trackId, quality(context))
        val length = ContentMetadata.getContentLength(cache.getContentMetadata(key))
        return length > 0 && cache.isCached(key, 0, length)
    }

    suspend fun saveOffline(
        context: Context,
        trackIds: List<String>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): Int = withContext(Dispatchers.IO) {
        val cache = cache(context) ?: return@withContext 0
        val factory = cacheFactory(context, cache)
        var saved = 0
        trackIds.forEachIndexed { index, id ->
            currentCoroutineContext().ensureActive()
            val ok = isCached(context, id) || runCatching {
                CacheWriter(factory.createDataSource(), DataSpec(MediaExtras.uri(id)), null, null).cache()
            }.isSuccess
            if (ok) saved++
            withContext(Dispatchers.Main) { onProgress(index + 1, trackIds.size) }
        }
        saved
    }
}
