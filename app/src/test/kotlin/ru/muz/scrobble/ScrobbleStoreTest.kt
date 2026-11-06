package ru.muz.scrobble

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ScrobbleStoreTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("scrobble-test", Context.MODE_PRIVATE)
    private lateinit var store: ScrobbleStore
    private val account = ScrobbleAccount(ScrobbleService.LASTFM, name = "user", token = "session", apiKey = "key", secret = "secret")
    private val play = ScrobblePlay("play", ScrobbleTrack("1", "Song", "Artist", 120000), 1700000000)
    @Before fun setup() { prefs.edit().clear().commit(); store = ScrobbleStore(prefs); store.connect(account) }

    @Test fun queueSurvivesRestartAndDeduplicatesTheSamePlay() {
        val entry = QueuedScrobble(account.service, account.id, play)
        store.enqueue(entry); store.enqueue(entry)
        assertEquals(listOf(entry), ScrobbleStore(prefs).pending())
        assertEquals(1, store.statuses.value.first().pending)
    }

    @Test fun switchingUsersCannotSubmitThePreviousUsersQueue() {
        store.enqueue(QueuedScrobble(account.service, account.id, play))
        store.connect(account.copy(id = "other", name = "other user"))
        assertTrue(store.pending().isEmpty())
        store.enqueue(QueuedScrobble(account.service, account.id, play))
        assertTrue(store.pending().isEmpty())
    }

    @Test fun reconnectingTheSameUserKeepsTheirQueue() {
        store.enqueue(QueuedScrobble(account.service, account.id, play))
        store.connect(account.copy(id = "new login", token = "rotated session"))
        assertEquals(account.id, store.accounts().single().id)
        assertEquals(1, store.pending().size)
    }

    @Test fun disconnectRemovesCredentialsAndOnlyThatServicesQueue() {
        store.enqueue(QueuedScrobble(account.service, account.id, play))
        val other = ScrobbleAccount(ScrobbleService.LISTENBRAINZ, name = "brainz", token = "personal")
        store.connect(other)
        store.enqueue(QueuedScrobble(other.service, other.id, play))
        store.savePending(PendingLastFm("key", "secret", "token", 1))
        store.disconnect(account.service)
        assertEquals(other, store.accounts().single())
        assertEquals(other.service, store.pending().single().service)
        assertNull(store.pendingLogin())
        val raw = prefs.getString("state", "").orEmpty()
        assertFalse(raw.contains("secret"))
    }

    @Test fun disabledAccountsDoNotReceiveNewListensAndRetriesKeepTheOriginalTime() {
        val entry = QueuedScrobble(account.service, account.id, play)
        store.enqueue(entry)
        store.retry(entry)
        val retry = store.pending().single()
        assertEquals(play.startedAt, retry.play.startedAt)
        assertEquals(1, retry.attempts)
        assertTrue(retry.nextAttemptAt > System.currentTimeMillis())
        store.retryNow(account.service)
        assertEquals(0, store.pending().single().nextAttemptAt)
        store.enable(account.service, false)
        store.enqueue(entry.copy(play = play.copy(id = "other")))
        assertEquals(1, store.pending().size)
    }
}
