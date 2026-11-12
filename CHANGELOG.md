# Changelog

## 0.46

- Upload your own MP3, AAC or FLAC files into your Yandex Music playlists: from a playlist, its menu, the add button in Collection or the Collection tab hold menu, with an option to create a new playlist on the way
- Uploaded tracks are visible only to you, play like any other track and are marked as your files; the playlist refreshes by itself while Yandex processes them
- Uploads show progress, can be cancelled, retry when the Yandex upload server times out, and explain limits and unsupported files
- The modern player uses one layout for the cover and the lyrics: the same header, background and control panel, so switching views no longer changes the design
- Switch between lyrics and cover from the control panel, which now also has shuffle and repeat
- Quote videos can fill each line as it is sung, highlight the current line or reveal lines one by one, with a live preview in the editor
- New quote card styles: Half, Polaroid and Neon
- The cover view scrolls on small screens with large text

## 0.45

- Four Solar icon packs: Linear, Bold, Broken and Duotone, with filled icons for the active tab
- Pick the navigation bar: bottom, folder-style tabs on top like Telegram, or a side menu
- Hold a tab to open quick actions: the icon squeezes during the hold, then a menu offers shortcuts and the bar style
- Quote videos light up each selected line in time with the song, with a live preview in the editor
- Lyric lines and track names on quote cards are never translated, and card and stats dialogs use readable status bar icons
- The jump-to-current-line button no longer hides under the lyrics controls and fades in smoothly
- Modern lyrics open on the current line without jumping, and the player header crossfades between views
- The cookie play button keeps its shape instead of stretching and slowly turns while playing
- The cover only flies between the mini and full player once it has loaded, so no grey placeholder crosses the screen
- The mini player keeps its shadow while it appears

## 0.44

- Settings are rebuilt into compact grouped cards with short hints; animations live in Appearance, and Home and Lists share one page
- Long radio lists became chip choices, the player page is flat instead of collapsible groups, and settings pages slide in and out
- Screens slide between the home tabs, playlists and settings, and the home screen keeps its scroll position when you come back
- Player buttons, cards and tool buttons share one springy press response; mini player, dock and cover swipes follow the finger
- The mini player no longer sits under the navigation bar on playlist and settings screens, and settings lost the doubled top gap
- Snackbars no longer cover download and update panels
- Long titles, tab labels and statistics numbers shrink to fit instead of being cut off
- Playlist covers stay square, the player cover fits short screens, and the player tool row never cuts off its last button
- The lyrics menu scrolls on small screens, and lyrics fallback buttons wrap instead of overflowing
- The listening chart grows again when you switch periods
- More of the interface is translated into English

## 0.43

- Connect Last.fm with your own application API key and shared secret, then authorize access in your browser
- Connect ListenBrainz with a personal token; credentials are stored encrypted on the device and excluded from settings exports
- Submit now playing and qualified listens independently of Yandex listening reports, with persistent offline queues, retries and account-specific disconnect controls
- Private mode pauses external submissions and excludes private plays from the queue
- Lyrics try LRCMux after Yandex and LRCLIB, validate the recording metadata, and offer browser search when no text is found
- Connection settings and fallback search messages include English translations

## 0.42

- The album cover moves between the mini-player and full player with a shared transition
- Playback controls have larger touch targets, animated shapes and press feedback
- The full player uses a coordinated palette from the album cover, with theme colors when no cover is available
- The player background stops moving on pause and respects reduced motion; no particle effects
- Settings open with a cleaner category list; player options are organized into five collapsible groups
- Settings previews scroll with the page, and search opens the relevant player group without changing saved preferences

## 0.41

- Four icon packs: Rounded, Outlined, Sharp and Two-tone, with previews in Appearance
- Full playback queues survive restarts, including repeated tracks and all artist credits; temporary network failures retry with a limit
- Queue tracks can be moved or removed, with Undo for queue changes
- Choose which artist to open on collaborative tracks, including from the track menu
- Downloads show progress and can be cancelled; incomplete files are cleaned up
- Updates check the APK size, checksum, package, version and signing certificate; installation continues after granting permission
- Offline music is stored separately from the listening cache and is only removed manually
- Storage controls show music, artwork, lyrics and temporary files separately, with confirmation and Undo for saved music removal
- Search works in small playlists too; date sorting is available when Yandex supplies addition dates, and the search field handles the keyboard
- Both lyrics views pause following during manual scrolling and offer a button to return to the current line
- Dialogs share a new layout with scrolling content and wrapping actions; the player scrolls on small screens or with large text
- High contrast mode, accessible Glass switches and seek controls, larger touch targets, and updated English translations

## 0.40

- Rounded icons, calmer navigation animations and more consistent text in the player, settings and library
- Track and playlist menus have grouped actions and scroll when they do not fit on the screen
- Copy all artist names from the player or track menu; names keep their punctuation and duplicate credits are removed
- New English strings remain available when a cached language pack is older than the app

## 0.39

- The in-app "Sign in with Yandex" is removed: Yandex does not accept confirmation codes in an embedded page. Sign in with a token is back on the first screen, with the steps right under the field

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
