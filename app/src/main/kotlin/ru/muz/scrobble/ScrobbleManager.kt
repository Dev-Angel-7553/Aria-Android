package ru.muz.scrobble

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ru.muz.settings.AppPreferences

internal class ScrobbleManager(
    context: Context,
    val store: ScrobbleStore = ScrobbleStore.shared(context),
    private val api: ScrobbleApi = ScrobbleApi(),
) {
    private val app = context.applicationContext
    private val mutex = Mutex()
    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var nowPlayingJob: Job? = null

    fun nowPlaying(play: ScrobblePlay): List<Pair<ScrobbleService, String>> {
        val accounts = store.accounts().filter { it.enabled }
        nowPlayingJob?.cancel()
        nowPlayingJob = io.launch {
            try {
                for (account in accounts) {
                    if (AppPreferences(app).load().privateSession) break
                    if (store.accounts().none { it.id == account.id && it.enabled }) continue
                    if (api.deliver(account, play, true) == ScrobbleApi.Delivery.Unauthorized)
                        store.accountError(account, "Доступ отозван. Подключите аккаунт заново", disable = true)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { store.reportStorageFailure() }
        }
        return accounts.map { it.service to it.id }
    }

    fun activeDestinations(destinations: List<Pair<ScrobbleService, String>>): List<Pair<ScrobbleService, String>> {
        val accounts = store.accounts().filter { it.enabled }
        return destinations.filter { (service, id) -> accounts.any { it.service == service && it.id == id } }
    }

    fun stopNowPlaying() { nowPlayingJob?.cancel() }

    fun submit(play: ScrobblePlay, destinations: List<Pair<ScrobbleService, String>>) {
        if (destinations.isEmpty() || AppPreferences(app).load().privateSession) return
        io.launch {
            try {
                if (AppPreferences(app).load().privateSession) return@launch
                destinations.forEach { (service, accountId) -> store.enqueue(QueuedScrobble(service, accountId, play)) }
                ScrobbleWorker.keepRecovery(app)
                ScrobbleWorker.schedule(app)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { store.reportStorageFailure() }
        }
    }

    suspend fun flush(): Boolean = mutex.withLock {
        if (AppPreferences(app).load().privateSession) return@withLock false
        val blocked = mutableSetOf<ScrobbleService>()
        val queue = store.pending()
        for (entry in ScrobbleService.entries.flatMap { service -> queue.filter { it.service == service }.take(50) }) {
            if (AppPreferences(app).load().privateSession) return@withLock false
            val account = store.accounts().firstOrNull { it.id == entry.accountId && it.service == entry.service && it.enabled } ?: continue
            if (account.service in blocked) continue
            if (entry.nextAttemptAt > System.currentTimeMillis()) { blocked += account.service; continue }
            when (val result = api.deliver(account, entry.play, false)) {
                ScrobbleApi.Delivery.Accepted -> store.complete(entry, true)
                ScrobbleApi.Delivery.Unauthorized -> {
                    store.accountError(account, "Доступ отозван. Подключите аккаунт заново", disable = true)
                    blocked += account.service
                }
                is ScrobbleApi.Delivery.Rejected -> store.complete(entry, false, result.message)
                ScrobbleApi.Delivery.Retry -> { store.retry(entry); blocked += account.service }
            }
        }
        val enabled = store.accounts().filter { it.enabled }.map { it.id }.toSet()
        store.pending().any { it.accountId in enabled }
    }

    companion object {
        @Volatile private var shared: ScrobbleManager? = null
        fun shared(context: Context): ScrobbleManager = shared ?: synchronized(this) {
            shared ?: ScrobbleManager(context).also { shared = it }
        }
    }
}

internal class ScrobbleWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = try {
        if (ScrobbleManager.shared(applicationContext).flush()) Result.retry() else Result.success()
    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (_: Exception) { Result.retry() }

    companion object {
        fun keepRecovery(context: Context) {
            val work = PeriodicWorkRequestBuilder<ScrobbleWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork("scrobble-recovery", ExistingPeriodicWorkPolicy.KEEP, work)
        }

        fun stopRecovery(context: Context) { WorkManager.getInstance(context.applicationContext).cancelUniqueWork("scrobble-recovery") }

        fun schedule(context: Context, force: Boolean = false) {
            val work = OneTimeWorkRequestBuilder<ScrobbleWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork("scrobble-delivery", if (force) ExistingWorkPolicy.REPLACE else ExistingWorkPolicy.KEEP, work)
        }
    }
}
