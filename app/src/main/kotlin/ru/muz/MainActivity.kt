package ru.muz

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ru.muz.player.PlaybackController
import ru.muz.settings.AppPreferences
import ru.muz.ui.MuzTheme
import ru.muz.ui.MusicApp
import ru.muz.yandex.LibrarySyncScheduler

class MainActivity : ComponentActivity() {
    private lateinit var playback: PlaybackController
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        playback = PlaybackController(this)
        LibrarySyncScheduler.schedule(this, AppPreferences(this).load().syncIntervalMinutes)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)

        setContent {
            val preferences = remember { AppPreferences(this) }
            var settings by remember { mutableStateOf(preferences.load()) }
            var scheduledSync by remember { mutableStateOf(settings.syncIntervalMinutes) }
            LaunchedEffect(settings) {
                delay(400)
                preferences.save(settings)
                if (settings.syncIntervalMinutes != scheduledSync) {
                    scheduledSync = settings.syncIntervalMinutes
                    LibrarySyncScheduler.schedule(this@MainActivity, settings.syncIntervalMinutes)
                }
            }
            val latest by rememberUpdatedState(settings)
            DisposableEffect(Unit) { onDispose { preferences.save(latest) } }
            MuzTheme(settings) {
                MusicApp(
                    playback = playback,
                    settings = settings,
                    onSettingsChange = { value -> settings = value },
                )
            }
        }
    }

    override fun onDestroy() {
        playback.release()
        super.onDestroy()
    }
}
