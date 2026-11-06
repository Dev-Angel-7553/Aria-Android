package ru.muz.scrobble

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.muz.settings.AppPreferences

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScrobbleManagerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var store: ScrobbleStore
    private val lastfm = ScrobbleAccount(ScrobbleService.LASTFM, name = "last", token = "session")
    private val brainz = ScrobbleAccount(ScrobbleService.LISTENBRAINZ, name = "brainz", token = "token")
    private val play = ScrobblePlay("play", ScrobbleTrack("1", "Song", "Artist", 120000), 1700000000)

    @Before fun setup() {
        AppPreferences(context).save(AppPreferences(context).load().copy(privateSession = false))
        val prefs = context.getSharedPreferences("manager-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = ScrobbleStore(prefs)
        store.connect(lastfm); store.connect(brainz)
        store.enqueue(QueuedScrobble(lastfm.service, lastfm.id, play))
        store.enqueue(QueuedScrobble(brainz.service, brainz.id, play))
    }

    @After fun cleanup() { AppPreferences(context).save(AppPreferences(context).load().copy(privateSession = false)) }

    @Test fun failingOneServiceDoesNotBlockTheOther() = runBlocking {
        val api = ScrobbleApi(OkHttpClient.Builder().addInterceptor { chain ->
            val failed = chain.request().url.host == "ws.audioscrobbler.com"
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(if (failed) 503 else 200)
                .message("fixture").body("""{"status":"ok"}""".toResponseBody()).build()
        }.build())
        assertTrue(ScrobbleManager(context, store, api).flush())
        assertEquals(lastfm.service, store.pending().single().service)
        assertEquals(1, store.pending().single().attempts)
        assertTrue(store.statuses.value.first { it.service == brainz.service }.lastSentAt > 0)
    }

    @Test fun privateModeDoesNotSendOrDiscardQueuedPublicListens() = runBlocking {
        AppPreferences(context).save(AppPreferences(context).load().copy(privateSession = true))
        val api = ScrobbleApi(OkHttpClient.Builder().addInterceptor { error("Private mode must not send network requests") }.build())
        assertFalse(ScrobbleManager(context, store, api).flush())
        assertEquals(2, store.pending().size)
    }
}
