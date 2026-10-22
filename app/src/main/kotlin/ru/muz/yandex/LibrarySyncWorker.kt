package ru.muz.yandex

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import ru.muz.auth.TokenStore

internal class LibrarySyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val token = TokenStore(applicationContext).token ?: return Result.success()
        val client = YandexClient(token)
        val account = client.accountInfo().getOrElse { return Result.retry() }
        LibrarySnapshotStore(applicationContext).read(account.uid)?.liked?.let(client::seedTracks)
        val liked = client.likedTracks().getOrElse { return Result.retry() }
        val secondary = coroutineScope {
            val albums = async { client.likedAlbums().getOrDefault(emptyList()) }
            val playlists = async { client.playlists().getOrDefault(emptyList()) }
            val wave = async { client.myWave().getOrDefault(emptyList()) }
            val chart = async { client.chart().getOrDefault(emptyList()) }
            Secondary(albums.await(), playlists.await(), wave.await(), chart.await())
        }
        LibrarySnapshotStore(applicationContext).write(
            account = account,
            liked = liked,
            albums = secondary.albums,
            playlists = secondary.playlists,
            wave = secondary.wave,
            chart = secondary.chart,
        )
        return Result.success()
    }

    private data class Secondary(
        val albums: List<YandexClient.Album>,
        val playlists: List<YandexClient.Playlist>,
        val wave: List<YandexClient.Track>,
        val chart: List<YandexClient.Track>,
    )
}

internal object LibrarySyncScheduler {
    private const val WORK_NAME = "yandex-library-sync"

    fun schedule(context: Context, intervalMinutes: Int) {
        val manager = WorkManager.getInstance(context)
        if (intervalMinutes <= 0) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val interval = intervalMinutes.coerceAtLeast(15).toLong()
        val request = PeriodicWorkRequestBuilder<LibrarySyncWorker>(interval, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        manager.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}
