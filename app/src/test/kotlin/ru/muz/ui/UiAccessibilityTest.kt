package ru.muz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import ru.muz.settings.AppSettings
import ru.muz.settings.AppStyle
import ru.muz.settings.IconPack
import ru.muz.lyrics.Lyrics
import ru.muz.lyrics.LyricLine
import ru.muz.lyrics.LyricsSettings
import ru.muz.lyrics.LyricsScreen
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w320dp-h640dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class UiAccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test
    fun dialogActionsRemainReachableWithLargeText() {
        var confirmed = false
        rule.setContent {
            MuzTheme(AppSettings(appStyle = AppStyle.GLASS, reduceMotion = true, textScalePercent = 130)) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    AriaDialog(onDismissRequest = {},
                        title = { Text("Удалить сохранённую музыку из памяти приложения?") },
                        text = { Text("Музыку понадобится сохранить заново для прослушивания без сети. ".repeat(12)) },
                        confirmButton = { TextButton(onClick = { confirmed = true }) { Text("Удалить") } },
                        dismissButton = { TextButton(onClick = {}) { Text("Отмена") } })
                }
            }
        }
        rule.onNodeWithText("Удалить").assertIsDisplayed()
        rule.onNodeWithText("Отмена").assertIsDisplayed()
        rule.runOnIdle {
            val view = android.view.inspector.WindowInspector.getGlobalWindowViews().last()
            val bitmap = android.graphics.Bitmap.createBitmap(view.width, view.height, android.graphics.Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(bitmap))
            File(System.getProperty("java.io.tmpdir"), "aria-0.41-dialog.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        }
        rule.onNodeWithText("Удалить").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun glassSwitchAndSeekBarSupportAccessibilityActions() {
        var checked by mutableStateOf(false)
        var seek = -1f
        rule.setContent {
            MuzTheme(AppSettings(appStyle = AppStyle.GLASS, reduceMotion = true)) {
                Column {
                    AriaSwitch(checked, { checked = it })
                    ThinSeekBar(0.2f, { seek = it })
                }
            }
        }
        rule.onNode(isToggleable()).assertIsOff().performClick().assertIsOn()
        rule.onNodeWithContentDescription("Позиция воспроизведения").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.SetProgress) { it(0.7f) }
        assertEquals(0.7f, seek)
    }

    @Test
    fun choosingIconPackUpdatesSelectionImmediately() {
        var settings by mutableStateOf(AppSettings(reduceMotion = true))
        rule.setContent { MuzTheme(settings) { IconPackPicker(settings) { settings = it } } }
        rule.onNodeWithText("Контурные").performClick()
        assertEquals(IconPack.OUTLINED, settings.iconPack)
        rule.onNodeWithText("Угловатые").performScrollTo().performClick()
        assertEquals(IconPack.SHARP, settings.iconPack)
    }

    @Test
    fun choosesExactCollaboratorWithPunctuationInTheirName() {
        var selected = ""
        val artists = listOf(
            ru.muz.yandex.YandexClient.Artist("1", "Tyler, The Creator", null, 0),
            ru.muz.yandex.YandexClient.Artist("2", "A & B", null, 0),
        )
        rule.setContent { MuzTheme(AppSettings(reduceMotion = true)) { ArtistPicker(artists, { selected = it.id }, {}) } }
        rule.onNodeWithText("Tyler, The Creator").assertIsDisplayed()
        rule.onNodeWithText("A & B").performClick()
        assertEquals("2", selected)
    }

    @Test
    fun filtersSmallPlaylistBeforePlaying() {
        var played = ""
        val tracks = (0..2).map { ru.muz.yandex.YandexClient.Track("$it", "", "Song $it", "Artist", "", 1000, null, false, false) }
        val playlist = ru.muz.yandex.YandexClient.Playlist("p", "", "", "", "A long playlist title that spans several lines", "", null, 3, "")
        rule.setContent {
            MuzTheme(AppSettings(reduceMotion = true)) {
                PlaylistScreen(playlist, tracks, false, false, 100, 60, ru.muz.player.PlayerState(), true, true,
                    onBack = {}, onPlay = { filtered, index -> played = filtered[index].id }, onMiniClick = {},
                    onPlayPause = {}, onNext = {}, onPrevious = {}, onTrackMore = {}, onPlaylistMore = {},
                    albumLiked = false, onToggleAlbumLike = {})
            }
        }
        rule.onNode(hasSetTextAction()).performScrollTo().performTextInput("Song 2")
        rule.onNode(hasText("Song 2") and !hasSetTextAction()).performScrollTo().performClick()
        assertEquals("2", played)
        rule.onNodeWithText("Song 0").assertDoesNotExist()
    }

    @Test
    fun fullPlayerControlsRemainReachableWithLargeText() {
        var plays = 0
        val settings = AppSettings(reduceMotion = true, appStyle = AppStyle.GLASS, highContrast = true)
        rule.setContent {
            MuzTheme(settings) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                    FullPlayerScreen(
                        state = ru.muz.player.PlayerState(title = "A long song title with several collaborators", artist = "First artist, Second artist"),
                        lyrics = Lyrics.EMPTY, lyricsLoading = false, lyricsStatus = "", lyricsError = "", showLyrics = false,
                        settings = settings, liked = false, onBack = {}, onToggleView = {}, onPlayPause = { plays++ },
                        onPrevious = {}, onNext = {}, onSeek = {}, onLike = {}, onShuffle = {}, onRepeat = {}, onRetryLyrics = {},
                        queue = emptyList(), onRemoveQueueTrack = {}, onMoveQueueTrack = { _, _ -> },
                        transferBusy = false, transferMessage = "", onQueueTrack = {}, onDownload = {}, onDownloadCover = {},
                        onOpenCover = {}, onShareLink = {}, onShareFile = {}, onArtist = {}, onCopyArtist = {},
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Играть").assertIsDisplayed().performClick()
        assertEquals(1, plays)
        rule.onNodeWithContentDescription("Текст").assertIsDisplayed()
        rule.onNodeWithText("A long song title with several collaborators").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun lyricsSeekByLineAndAllowManualScrolling() = checkLyricsInteraction(false)

    @Test
    fun modernLyricsSeekByLineAndAllowManualScrolling() = checkLyricsInteraction(true)

    private fun checkLyricsInteraction(modern: Boolean) {
        var sought = -1L
        val lyrics = Lyrics((0 until 30).map { LyricLine(it * 3000L, (it + 1) * 3000L, "Строка $it") }, true)
        rule.setContent {
            MuzTheme(AppSettings(reduceMotion = true)) {
                LyricsScreen(lyrics, 0L, { sought = it }, LyricsSettings(modern = modern), modifier = Modifier.fillMaxSize())
            }
        }
        rule.onNodeWithText("Строка 1").performClick()
        assertEquals(3000L, sought)
        rule.mainClock.autoAdvance = false
        rule.onNodeWithText("Строка 1").performTouchInput { swipeUp() }
        rule.mainClock.advanceTimeBy(250)
        rule.onNodeWithText("К текущей строке").assertExists().performClick()
    }
}
