package ru.muz.scrobble

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
internal data class ScrobbleAccount(
    val service: ScrobbleService, val id: String = UUID.randomUUID().toString(), val name: String,
    val token: String, val apiKey: String = "", val secret: String = "", val enabled: Boolean = true,
    val error: String = "", val lastSentAt: Long = 0,
)

@Serializable
internal data class PendingLastFm(val apiKey: String, val secret: String, val token: String, val createdAt: Long)

@Serializable
internal data class QueuedScrobble(
    val service: ScrobbleService, val accountId: String, val play: ScrobblePlay,
    val attempts: Int = 0, val nextAttemptAt: Long = 0,
)

internal data class ScrobbleStatus(
    val service: ScrobbleService, val name: String = "", val connected: Boolean = false,
    val enabled: Boolean = false, val pending: Int = 0, val error: String = "", val lastSentAt: Long = 0,
)

@Serializable
private data class StoredScrobbles(
    val accounts: List<ScrobbleAccount> = emptyList(), val queue: List<QueuedScrobble> = emptyList(),
    val pendingLastFm: PendingLastFm? = null,
)

internal class ScrobbleStore(private val prefs: SharedPreferences) {
    private val json = Json { ignoreUnknownKeys = true }
    private var data = prefs.getString("state", null)?.let { runCatching { json.decodeFromString<StoredScrobbles>(it) }.getOrNull() }
        ?: StoredScrobbles()
    private val state = MutableStateFlow(status())
    val statuses: StateFlow<List<ScrobbleStatus>> = state

    @Synchronized fun accounts(): List<ScrobbleAccount> = data.accounts.toList()
    @Synchronized fun pending(): List<QueuedScrobble> = data.queue.toList()
    @Synchronized fun pendingLogin(): PendingLastFm? = data.pendingLastFm

    @Synchronized fun savePending(value: PendingLastFm?) { write(data.copy(pendingLastFm = value)) }

    @Synchronized fun connect(account: ScrobbleAccount) {
        val old = data.accounts.firstOrNull { it.service == account.service }
        if (old != null && old.name.equals(account.name, ignoreCase = true)) {
            write(data.copy(accounts = data.accounts.filterNot { it.service == account.service } + account.copy(id = old.id),
                pendingLastFm = if (account.service == ScrobbleService.LASTFM) null else data.pendingLastFm))
        } else {
            write(data.copy(accounts = data.accounts.filterNot { it.service == account.service } + account,
                queue = data.queue.filterNot { it.service == account.service },
                pendingLastFm = if (account.service == ScrobbleService.LASTFM) null else data.pendingLastFm))
        }
    }

    @Synchronized fun disconnect(service: ScrobbleService) {
        write(data.copy(accounts = data.accounts.filterNot { it.service == service }, queue = data.queue.filterNot { it.service == service },
            pendingLastFm = if (service == ScrobbleService.LASTFM) null else data.pendingLastFm))
    }

    @Synchronized fun enable(service: ScrobbleService, enabled: Boolean) {
        write(data.copy(accounts = data.accounts.map { if (it.service == service) it.copy(enabled = enabled) else it }))
    }

    @Synchronized fun enqueue(value: QueuedScrobble) {
        if (data.queue.any { it.service == value.service && it.accountId == value.accountId && it.play.id == value.play.id }) return
        val account = data.accounts.firstOrNull { it.service == value.service && it.id == value.accountId && it.enabled } ?: return
        if (data.queue.count { it.service == value.service } >= 2000) {
            accountError(account, "Очередь заполнена. Подключитесь к сети и отправьте накопленные прослушивания")
            return
        }
        write(data.copy(queue = data.queue + value))
    }

    @Synchronized fun complete(value: QueuedScrobble, accepted: Boolean, error: String = "") {
        write(data.copy(queue = data.queue.filterNot { it.service == value.service && it.accountId == value.accountId && it.play.id == value.play.id },
            accounts = data.accounts.map {
                if (it.service == value.service && it.id == value.accountId) it.copy(error = error,
                    lastSentAt = if (accepted) System.currentTimeMillis() else it.lastSentAt) else it
            }))
    }

    @Synchronized fun retryNow(service: ScrobbleService) {
        write(data.copy(queue = data.queue.map { if (it.service == service) it.copy(nextAttemptAt = 0) else it }))
    }

    @Synchronized fun retry(value: QueuedScrobble) {
        val attempts = value.attempts + 1
        val delay = minOf(6 * 60 * 60_000L, 30_000L * (1L shl attempts.coerceAtMost(10)))
        write(data.copy(queue = data.queue.map {
            if (it.service == value.service && it.accountId == value.accountId && it.play.id == value.play.id)
                it.copy(attempts = attempts, nextAttemptAt = System.currentTimeMillis() + delay) else it
        }))
    }

    @Synchronized fun accountError(account: ScrobbleAccount, message: String, disable: Boolean = false) {
        write(data.copy(accounts = data.accounts.map {
            if (it.service == account.service && it.id == account.id && it.token == account.token) it.copy(error = message, enabled = it.enabled && !disable) else it
        }))
    }

    private fun status(): List<ScrobbleStatus> = ScrobbleService.entries.map { service ->
        val account = data.accounts.firstOrNull { it.service == service }
        ScrobbleStatus(service, account?.name.orEmpty(), account != null, account?.enabled == true,
            data.queue.count { it.service == service }, account?.error.orEmpty(), account?.lastSentAt ?: 0)
    }

    fun reportStorageFailure() {
        state.value = state.value.map { if (it.connected) it.copy(error = "Не удалось сохранить данные скробблинга. Проверьте свободное место") else it }
    }

    private fun write(value: StoredScrobbles) {
        check(prefs.edit().putString("state", json.encodeToString(value)).commit()) { "Не удалось сохранить подключение" }
        data = value
        state.value = status()
    }

    companion object {
        @Volatile private var shared: ScrobbleStore? = null
        fun shared(context: Context): ScrobbleStore = shared ?: synchronized(this) {
            shared ?: ScrobbleStore(EncryptedSharedPreferences.create(
                context.applicationContext, "scrobble_accounts",
                MasterKey.Builder(context.applicationContext).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )).also { shared = it }
        }
    }
}
