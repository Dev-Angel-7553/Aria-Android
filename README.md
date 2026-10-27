<p align="center">
  <img src="docs/icon.png" width="112" alt="Aria">
</p>

**English** · [Русский](README.ru.md) · [Website](https://aria.dev-angel-7553.dev/) · [Desktop version](https://github.com/Dev-Angel-7553/Aria-Desktop)

# Aria

An unofficial Yandex Music client for Android. It started as a way to get a proper lyrics screen: the active line stays in place while the text fills in word by word as it is sung. Then it grew into everything else.

## Features

- home with My Wave and the chart, collection, search, playlists, albums, artists
- karaoke lyrics: the active line stays on top, distant lines are blurred, pauses in the song are shown as dots; if Yandex has no lyrics, they are looked up on LRCLIB
- quote cards for stories: long-press a line on the lyrics screen. A card can be saved as an image or as a video with that part of the song playing
- background playback, controls from the notification shade and the headset
- listens are counted by Yandex: they go into history and My Wave learns from skips; there is a private mode
- My Wave settings (mood, character, language), wave by track and by artist, "Don't recommend"
- loudness normalization between tracks based on Yandex data
- export a playlist or the whole collection to CSV, import CSV from Spotify, TuneMyMusic, Exportify
- offline cache, sleep timer, playback speed, silence skipping
- equalizer with presets, smooth transitions between tracks
- home screen widgets, a like button in the notification, Android Auto
- downloading tracks and covers
- own listening statistics: week, month, year, tops with covers, comparison with the previous period, a summary image
- a quick settings tile: play and pause
- background that pulses with the music, lyrics cached and found in advance
- four progress bar styles (line, wave, capsule, thin with a dot), player controls that take their color from the cover, search and sorting in the queue
- interface styles: Classic, Material You, Glass, Minimal. Glass is liquid glass: panel edges bend what is under them, the rim catches light and follows the tilt of the phone (Android 13+, blur on 12, a solid panel on older versions)
- tab bar that can be dragged across: the bubble follows the finger, the highlight flows between tabs
- almost everything is configurable: theme, any accent color, font, text size, home blocks, tabs, player buttons, play button shape, double tap action, playback speed with or without pitch change; settings can be saved to a file

## Gestures

| Where | What to do | What happens |
| --- | --- | --- |
| cover in the player | double tap | like, pause or next track, as chosen in the settings |
| lyrics screen | tap a line | seek to it |
| lyrics screen | long-press a line | quote card |
| lyrics screen | leave it alone for a few seconds | the control panel hides, a tap brings it back |
| cover in the player | swipe down | collapse the player |
| cover in the player | swipe up | queue |
| tab bar | drag across the tabs | the tab under the finger opens on release |

Tap to seek, double tap and vibration can be turned off in the settings.

## Installation

The ready-made APK is in [Releases](../../releases). Android 8.0 and newer. After that the app finds new versions on its own and offers to update.

You need a Yandex Music OAuth token to sign in. How to get it is explained right on the login screen: open the Yandex page, sign in, copy the address and paste it into the app. The token is stored only on the phone, in encrypted storage, and is sent only to Yandex. There is no token in the repository or in the APK.

## Build

You need JDK 17 and the Android SDK with platform 35.

```bash
./gradlew assembleDebug
```

The APK appears in `app/build/outputs/apk/debug/`. Tests: `./gradlew testDebugUnitTest`.

Every push to `main` is built in GitHub Actions and published as release `v<versionName>`. A new version in `app/build.gradle.kts` makes a new release, the same version updates the APK in the existing one.

### Signing

For updates to install over the previous version, releases must be signed with the same key. The key is not stored in the repository, CI takes it from secrets:

| Secret | What it is |
| --- | --- |
| `ARIA_KEYSTORE_BASE64` | the keystore in base64 |
| `ARIA_KEYSTORE_PASSWORD` | keystore password |
| `ARIA_KEY_ALIAS` | key alias |
| `ARIA_KEY_PASSWORD` | key password |

Locally the same values are passed as environment variables, the path to the file goes in `ARIA_KEYSTORE`. Without them a release build is signed with the debug key.

## CSV export

In the playlist menu or in Settings → Account and data → Export. The file has one row per track: title, artist, album, playlist, duration and a Yandex Music link. The file is saved to `Downloads/Aria`.

## Stack

Kotlin, Jetpack Compose, Material 3, Media3 (ExoPlayer + MediaSession), OkHttp, kotlinx.serialization, Coil, WorkManager.

## Disclaimer

The project is not affiliated with Yandex. The Yandex Music API is unofficial and may change at any moment. You need your own Yandex Plus subscription to use it.
