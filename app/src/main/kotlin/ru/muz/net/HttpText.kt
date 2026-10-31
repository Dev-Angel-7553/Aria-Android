package ru.muz.net

import java.io.IOException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response

internal data class HttpText(val code: Int, val body: String) {
    val isSuccessful: Boolean get() = code in 200..299
}

internal suspend fun Call.readText(): HttpText = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, error: IOException) {
            continuation.resumeWith(Result.failure(error))
        }

        override fun onResponse(call: Call, response: Response) {
            val result = runCatching { response.use { HttpText(it.code, it.body?.string().orEmpty()) } }
            continuation.resumeWith(result)
        }
    })
}
