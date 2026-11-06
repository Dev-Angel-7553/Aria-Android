package ru.muz.ui

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.muz.scrobble.*
import ru.muz.settings.AppSettings

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ScrobbleConnectionsTest {
    @get:Rule val rule = createComposeRule()
    private lateinit var store: ScrobbleStore
    @Before fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("scrobble-ui-test", Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        store = ScrobbleStore(prefs)
    }
    private fun show() { rule.setContent { MuzTheme(AppSettings(reduceMotion = true)) {
        Surface(Modifier.fillMaxSize(), color = AppBackground) { ScrobbleConnections(false, store) }
    } } }

    @Test fun lastFmRequiresTheUsersOwnApplicationKeys() {
        show()
        rule.onAllNodesWithText("Подключить")[0].performClick()
        rule.onNodeWithText("API key").assertIsDisplayed()
        rule.onNodeWithText("Shared secret").assertIsDisplayed()
        rule.onNodeWithText("Разрешить доступ в Last.fm").assertIsNotEnabled()
        rule.onNodeWithText("API key").performTextInput("own-key")
        rule.onNodeWithText("Shared secret").performTextInput("own-secret")
        rule.onNodeWithText("Разрешить доступ в Last.fm").assertIsEnabled()
        assertTrue(store.accounts().isEmpty())
        assertNull(store.pendingLogin())
    }

    @Test fun listenBrainzHasASeparatePersonalTokenFlow() {
        show()
        rule.onAllNodesWithText("Подключить")[1].performClick()
        rule.onNodeWithText("Токен ListenBrainz").assertIsDisplayed()
        rule.onNodeWithText("Shared secret").assertDoesNotExist()
    }

    @Test fun disconnectConfirmsAndRemovesOnlyTheSelectedAccount() {
        store.connect(ScrobbleAccount(ScrobbleService.LASTFM, name = "listener", token = "session"))
        store.connect(ScrobbleAccount(ScrobbleService.LISTENBRAINZ, name = "brainz", token = "token"))
        show()
        rule.onAllNodesWithText("Отключить")[0].performClick()
        assertEquals(2, store.accounts().size)
        rule.onNodeWithText("Отключить Last.fm?").assertIsDisplayed()
        rule.onAllNodesWithText("Отключить").onLast().performClick()
        rule.waitUntil { store.accounts().size == 1 }
        assertEquals(ScrobbleService.LISTENBRAINZ, store.accounts().single().service)
    }
}
