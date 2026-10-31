package ru.muz.net

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CancellationException
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test

class TransferControlTest {
    @Test
    fun cancellationStopsApiRequestWhileResolvingAudioUrl() = runBlocking {
        val entered = CountDownLatch(1)
        val finish = CountDownLatch(1)
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            entered.countDown()
            finish.await(5, TimeUnit.SECONDS)
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK").body("ok".toResponseBody()).build()
        }.build()
        val call = client.newCall(Request.Builder().url("https://example.com/audio").build())
        val task = async(Dispatchers.IO) { call.readText() }
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            task.cancelAndJoin()
            assertTrue(call.isCanceled())
        } finally { finish.countDown(); client.dispatcher.executorService.shutdown() }
    }

    @Test
    fun cancelledTransferRejectsFurtherRequests() = runBlocking {
        val control = TransferControl()
        control.cancel()
        val call = OkHttpClient().newCall(Request.Builder().url("https://example.com/audio").build())
        try {
            control.response(call) { fail("Cancelled transfer started") }
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertFalse(call.isExecuted())
        }
    }
}
