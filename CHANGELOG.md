# Changelog

## 0.38

- Menus in Glass, including the three dots menu in the player, are translucent like the other sheets: their rows no longer have a solid background
- Settings: every section opens at the top, and going back returns to the place in the main list where you were

## 0.37

- Sign in with Yandex inside the app accepts confirmation codes: the sign in page now keeps Yandex cookies between its own domains and presents itself as a normal browser
- A hint in the sign in window points to the token sign in if Yandex still refuses

## 0.36

- English. Language packs live in the repository and are downloaded from GitHub, so new languages appear without updating the app. The language is chosen in Appearance and on the sign in screen
- New sign in screen: "Sign in with Yandex" opens the usual Yandex page inside the app and picks up the token by itself, manual sign in with a token is still there
- Works without network: the app opens with the saved library instead of the sign in screen, playlists opened before are available, saved tracks play and the rest are dimmed, everything syncs when the network comes back
- Without an active Plus subscription saved tracks still play, the app explains what is not available
- More liked tracks load and the app tells how many Yandex did not return

## 0.35

- More liked tracks load: tracks that Yandex returns under a new id are matched, and the ones it did not return are asked for again in small batches. If some still do not come, the app says how many loaded out of how many
- The menu on the lyrics screen has everything again: download, save and open the cover, send the audio file, copy the artist, sound and equalizer, next to speed, sleep timer and the wave by track
- Bottom sheets in Glass look like glass even where the system turns off blur: more translucent, with a light top and a rim
- Distant lyrics lines blur more softly: the nearest lines are almost sharp, the blur grows gradually and changes more smoothly
- The blur of lines that are not playing is no longer cut off at the left edge
- "No vocals" and "Vocals" modes are removed for now: without a neural network they did not separate the voice well enough

## 0.34

- Sound: a five band equalizer with presets (Bass, Vocal, Acoustic, Electronic, Rock, Headphones, Quiet night) in Settings → Sound and offline and in the player menu
- Voice modes while listening: "No vocals" removes what sits in the center of the mix, usually the voice, and keeps the bass; "Vocals" brings the voice forward and turns the rest down. It works on the fly, nothing is downloaded. It is not a full separation, on live and mono recordings the voice can stay
- Smooth transition between tracks, up to 12 seconds: the end of a track fades out and the next one fades in
- Bottom sheets in Glass are translucent with the screen behind them blurred (Android 12+ with blur enabled in the system)
- Mini player: swipe up opens the player, swipe down hides it until the next track; it is tinted with the cover color, can be turned off
- The APK is much smaller: unused code and resources are stripped from release builds

## 0.33

- Fixed a crash when opening the "Player" page of the settings in Glass
- In Glass the settings header is a floating glass panel instead of a black bar, and lists in settings, playlists, albums and artists scroll under the glass header and the mini player
- "Colors from the song" in Appearance: the accent and tints of the whole app are taken from the cover of what is playing and change smoothly with the track

## 0.32

- Glass is clearer: much less blur and milky tint, stronger bending at the edges, a slight lens zoom in the middle and a soft shade along the bottom edge, so it reads as a piece of glass instead of frosted plastic
- The lyrics screen in Glass has a floating glass control panel over the lyrics and the moving cover background
- Release notes are shown in full on the update card and in the settings, with a "more" button for long lists

## 0.31

- Glass style now covers the whole app: playlists, albums, artists and settings have the same moving background and the glass mini player, not only the main tabs
- Switches in Glass look like iOS: green track, white thumb that stretches while pressed
- The slider thumb in Glass is the plain white iOS one, the quote card size slider follows the style too

## 0.30

- Lyrics stay sharp while you scroll them, so you can see where you are, and blur returns a few seconds after you let go. The switch is in the lyrics settings
- Glass panels (tab bar, mini player) no longer show a piece from the top of the screen instead of what is actually under them. The position of a panel over the content is now measured on every frame instead of once

## 0.29

- Glass is now real liquid glass on Android 13 and newer: the edges of the panels bend and slightly split the colors of what is under them, the rim catches light and the highlight follows the tilt of the phone. Android 12 gets frosted blur, older versions a solid panel
- Glass has its own look: thin custom tab icons, capsule progress bar, iOS style sliders with a round thumb, larger titles, a soft colored backdrop
- Drag across the tab bar to switch tabs: the bubble follows the finger and the tab under it opens on release
- The tab highlight flows between tabs: the front edge runs ahead and the back edge catches up
- The progress bar is drawn from scratch, so the thumb and the filled part always match. Four styles: line, wave, capsule, thin with a dot
- Sliders in the settings follow the chosen style
- Style cards in the settings show a small live version of the interface
- Tabs change with a soft fade, cards and tiles react to touch
- Sparks around the cover are gone

## 0.28

- Interface styles: Classic, Material You, Glass and Minimal. Picking a style sets up colors, shapes and effects in one tap, everything can still be tuned by hand
- Glass: the tab bar and the mini player float over the content and blur what is under them, the strength is adjustable. Real blur needs Android 12 or newer
- New tab bar: the selection slides between tabs and stretches a little on the way, icons pop on tap, light vibration
- New rounded tab icons
- "Cookie" shape for the play button
- Settings: style cards at the top, quick toggles for private mode, data saver and animations, colored section icons

## 0.27

- Lyrics load much faster: Yandex and LRCLIB are searched at the same time, results are kept on disk, and lyrics for the next tracks in the queue are found in advance
- Tracks without lyrics are remembered for a while, so the search does not hang on them every time; "Search again" still checks anew
- The background pulses with the music, the strength is adjustable
- Sparks fly out from under the cover on strong beats
- New loading animation: a soft shape that slowly turns and breathes
- Double tap on the cover can like, pause or skip to the next track

## 0.26

- Wavy progress bar: the played part of the track runs as a wave while music plays, can be turned off
- The play button on the lyrics screen takes its color from the cover
- Lyrics can fill from top to bottom instead of left to right
- Playback speed has a "Change pitch" switch
- Search and sorting in the queue
- New "Lyrics not found" screen

## 0.25

- Fixed the whole lyrics block twitching when the active line changes
- Statistics show seconds for listening shorter than a minute

## 0.24

- Smoother lyrics scrolling
- Fixed a stray fill on the bottom line of the lyrics
- Player controls now sit over the lyrics instead of resizing them
- Live lyrics preview in the settings

## 0.23

- New lyrics screen: blurred distant lines, soft word-by-word fill, wave scroll, dots for pauses, controls that hide on their own
- Statistics by week, month, year and all time with tops, comparison with the previous period, a summary image, export and reset
- My Wave settings, wave by track and by artist, "Don't recommend"
- Loudness normalization between tracks
- Private mode, hiding explicit tracks, double tap to like
- Quick settings tile with play and pause

## 0.22

- Token guide on the login screen
- Redesigned quote cards with video
