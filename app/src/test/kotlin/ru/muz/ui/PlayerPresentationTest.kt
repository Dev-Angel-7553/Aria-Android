package ru.muz.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.muz.lyrics.Lyrics
import ru.muz.player.PlayerState
import ru.muz.player.RepeatMode
import ru.muz.settings.AppSettings
import ru.muz.settings.AppStyle
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w360dp-h800dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PlayerPresentationTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun transportControlsStayUsableOnNarrowScreens() {
        var state by mutableStateOf(PlayerState(title = "Тихий город"))
        var skipped = 0
        rule.setContent {
            MuzTheme(AppSettings(reduceMotion = true)) {
                Box(Modifier.width(280.dp)) {
                    PlayerTransport(state,
                        { state = state.copy(isPlaying = !state.isPlaying) }, {}, { skipped++ },
                        { state = state.copy(shuffle = !state.shuffle) },
                        { state = state.copy(repeat = RepeatMode.ONE) })
                }
            }
        }
        rule.onNodeWithContentDescription("Играть").assertIsDisplayed().performClick()
        rule.onNodeWithContentDescription("Пауза").assertIsDisplayed()
        rule.onNodeWithContentDescription("Следующий").performClick()
        rule.onNodeWithContentDescription("Перемешать").performClick().assertIsSelected()
        rule.onNodeWithContentDescription("Повтор выключен").performClick()
        rule.onNodeWithContentDescription("Повтор одного трека").assertIsSelected()
        assertEquals(1, skipped)
        rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
            assertTrue(node.boundsInRoot.width >= 48f)
            assertTrue(node.boundsInRoot.height >= 48f)
        }
    }

    @Test
    fun artworkTransitionCanBeReversedAndReopened() {
        var expanded by mutableStateOf(false)
        rule.setContent {
            MuzTheme(AppSettings(animatePlayerTransitions = true)) {
                PlayerScene(expanded) { full ->
                    Box(Modifier.fillMaxSize(), contentAlignment = if (full) Alignment.Center else Alignment.BottomStart) {
                        Box(Modifier.size(if (full) 240.dp else 48.dp).playerArtworkTransition("track", null)) {
                            Artwork(null, Modifier.fillMaxSize(), 20.dp)
                        }
                        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.align(Alignment.TopCenter)) {
                            Text(if (full) "Свернуть" else "Раскрыть")
                        }
                    }
                }
            }
        }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Раскрыть").performClick()
        rule.mainClock.advanceTimeBy(160)
        rule.runOnIdle { expanded = false }
        rule.mainClock.autoAdvance = true
        rule.waitForIdle()
        rule.onNodeWithText("Раскрыть").assertIsDisplayed().performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Свернуть").assertIsDisplayed()
    }

    @Test
    fun playerSettingsKeepValuesAndSearchOpensThePage() {
        var settings by mutableStateOf(AppSettings(reduceMotion = true, appStyle = AppStyle.CLASSIC))
        rule.setContent { MuzTheme(settings) {
            SettingsScreen(settings, null, PlayerState(), true, true, { settings = it }, {}, {}, {}, {}, {}, {})
        } }
        rule.onNodeWithText("Плеер").performScrollTo().performClick()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Свайпы"))
        rule.onNodeWithText("Свайпы").performClick()
        assertFalse(settings.swipeMiniPlayer)
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Акцент от обложки"))
        rule.onNodeWithText("Акцент от обложки").assertIsDisplayed()
        rule.onNodeWithContentDescription("Назад").performClick()
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("обложк")
        rule.onNodeWithText("Цвет обложки в фоне").performScrollTo().performClick()
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(hasText("Акцент от обложки"))
        rule.onNodeWithText("Акцент от обложки").assertIsDisplayed()
        assertFalse(settings.swipeMiniPlayer)
    }

    @Test
    fun captureFullPlayerAndSettings() {
        var settingsVisible by mutableStateOf(false)
        val settings = AppSettings(reduceMotion = true, appStyle = AppStyle.CLASSIC, themeMode = ru.muz.settings.ThemeMode.DARK)
        rule.setContent { MuzTheme(settings) { androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = AppBackground) {
            if (settingsVisible) {
                SettingsScreen(settings, null, PlayerState(), true, true, {}, {}, {}, {}, {}, {}, {})
            } else {
                FullPlayerScreen(
                    state = PlayerState(mediaId = "preview", title = "Тихий город", artist = "Северный свет", positionMs = 74000, durationMs = 224000),
                    lyrics = Lyrics.EMPTY, lyricsLoading = false, lyricsStatus = "", lyricsError = "", showLyrics = false,
                    settings = settings, liked = true, onBack = {}, onToggleView = {}, onPlayPause = {},
                    onPrevious = {}, onNext = {}, onSeek = {}, onLike = {}, onShuffle = {}, onRepeat = {}, onRetryLyrics = {},
                    queue = emptyList(), onRemoveQueueTrack = {}, onMoveQueueTrack = { _, _ -> },
                    transferBusy = false, transferMessage = "", onQueueTrack = {}, onDownload = {}, onDownloadCover = {},
                    onOpenCover = {}, onShareLink = {}, onShareFile = {}, onArtist = {}, onCopyArtist = {},
                )
            }
        } } }
        rule.waitForIdle()
        screenshot("aria-player")
        rule.runOnIdle { settingsVisible = true }
        rule.waitForIdle()
        screenshot("aria-settings")
        rule.onNodeWithText("Плеер").performScrollTo().performClick()
        screenshot("aria-player-settings")
    }

    private fun screenshot(name: String) {
        rule.runOnIdle {
            val view = android.view.inspector.WindowInspector.getGlobalWindowViews().last()
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            File(System.getProperty("java.io.tmpdir"), "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }
}
