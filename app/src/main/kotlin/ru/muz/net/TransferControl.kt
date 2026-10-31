package ru.muz.net

import java.util.concurrent.CancellationException
import okhttp3.Call
import okhttp3.Response

class TransferControl {
    private val calls = mutableSetOf<Call>()
    @Volatile private var cancelled = false

    fun cancel() {
        val active = synchronized(calls) { cancelled = true; calls.toList() }
        active.forEach { it.cancel() }
    }

    fun checkActive() {
        if (cancelled) throw CancellationException("Загрузка отменена")
    }

    suspend fun <T> response(call: Call, block: suspend (Response) -> T): T {
        synchronized(calls) { checkActive(); calls.add(call) }
        try {
            return call.execute().use { checkActive(); block(it) }
        } finally {
            synchronized(calls) { calls.remove(call) }
        }
    }
}
