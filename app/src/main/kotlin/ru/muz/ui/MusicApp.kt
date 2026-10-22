package ru.muz.ui

import android.app.Activity
import android.view.WindowManager
import android.os.SystemClock
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import ru.muz.export.PlaylistCsv
import ru.muz.yandex.ListenEvent
import ru.muz.yandex.ListenSession
import ru.muz.yandex.PlaySource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import android.content.ClipboardManager
import android.content.ClipData
import ru.muz.auth.TokenStore
import ru.muz.lyrics.Lyrics
import ru.muz.lyrics.LyricsRepository
import ru.muz.player.PlaybackController
import ru.muz.player.RepeatMode
import ru.muz.settings.AppSettings
import ru.muz.yandex.YandexClient
import ru.muz.yandex.LibrarySnapshotStore
import ru.muz.download.TrackTransfer
import ru.muz.stats.ListeningStats
import ru.muz.stats.ListeningStatsSnapshot

internal enum class MainTab(
    val title: String,
    val active: ImageVector,
    val inactive: ImageVector,
) {
    HOME("Главная", Icons.Filled.Home, Icons.Outlined.Home),
    LIBRARY("Коллекция", Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic),
    SEARCH("Поиск", Icons.Filled.Search, Icons.Outlined.Search),
    STATS("Статистика", Icons.Filled.Equalizer, Icons.Outlined.Equalizer),
}

@Composable
fun MusicApp(
    playback: PlaybackController,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tokenStore = remember { TokenStore(context) }
    val librarySnapshotStore = remember { ru.muz.yandex.LibrarySnapshotStore(context) }
    val listeningStats = remember { ListeningStats(context) }
    val player by playback.state.collectAsState()

    var api by remember { mutableStateOf<YandexClient?>(null) }
    val lyricsRepository = remember(api) { api?.let(::LyricsRepository) }
    var lyricsTrackId by remember { mutableStateOf("") }
    var lyricsGeneration by remember { mutableStateOf(0) }
    var account by remember { mutableStateOf<YandexClient.Account?>(null) }
    var liked by remember { mutableStateOf<List<YandexClient.Track>>(emptyList()) }
    var likedAlbums by remember { mutableStateOf<List<YandexClient.Album>>(emptyList()) }
    var playlists by remember { mutableStateOf<List<YandexClient.Playlist>>(emptyList()) }
    var wave by remember { mutableStateOf<List<YandexClient.Track>>(emptyList()) }
    var chart by remember { mutableStateOf<List<YandexClient.Track>>(emptyList()) }
    var searchResults by remember { mutableStateOf(YandexClient.SearchResults()) }
    var selectedPlaylist by remember { mutableStateOf<YandexClient.Playlist?>(null) }
    var selectedCollectionTitle by remember { mutableStateOf<String?>(null) }
    var selectedTracks by remember { mutableStateOf<List<YandexClient.Track>>(emptyList()) }
    var queue by remember { mutableStateOf<List<YandexClient.Track>>(emptyList()) }
    var lyrics by remember { mutableStateOf(Lyrics.EMPTY) }
    var lyricsLoading by remember { mutableStateOf(false) }
    var lyricsStatus by remember { mutableStateOf("") }
    var lyricsError by remember { mutableStateOf("") }
    var lyricsRequestKey by remember { mutableStateOf(0) }
    var likedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var tab by remember { mutableStateOf(MainTab.HOME) }
    var showPlayer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var collectionMessage by remember { mutableStateOf("") }
    var transferMessage by remember { mutableStateOf("") }
    var transferDiagnostic by remember { mutableStateOf("") }
    var transferBusy by remember { mutableStateOf(false) }
    var statsSnapshot by remember { mutableStateOf(ListeningStatsSnapshot()) }
    var actionTrack by remember { mutableStateOf<YandexClient.Track?>(null) }
    var actionPlaylist by remember { mutableStateOf<YandexClient.Playlist?>(null) }
    var artistLoading by remember { mutableStateOf(false) }
    var waveSession by remember { mutableStateOf(false) }
    var waveLoadingMore by remember { mutableStateOf(false) }
    var lastWaveContinuation by remember { mutableStateOf("") }
    var exportBusy by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val listenSession = remember { ListenSession() }
    var playSource by remember { mutableStateOf(PlaySource.other()) }
    val reportScope = remember { CoroutineScope(SupervisorJob() + Dispatchers.IO) }
    val latestPlayer by rememberUpdatedState(player)

    val background = AppBackground
    val currentTrack = remember(player.mediaId, player.title, player.artist, player.durationMs, queue, liked, selectedTracks, searchResults) {
        (queue + selectedTracks + liked + searchResults.tracks)
            .firstOrNull { it.id == player.mediaId }
            ?: player.takeIf { it.mediaId.isNotBlank() && it.title.isNotBlank() }?.let {
                YandexClient.Track(it.mediaId, "", it.title, it.artist, "", it.durationMs, it.coverUrl, false, true)
            }
    }

    DisposableEffect(settings.keepScreenOn) {
        val window = (context as? Activity)?.window
        if (settings.keepScreenOn) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            if (settings.keepScreenOn) {
                window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    DisposableEffect(listeningStats) {
        onDispose { listeningStats.flush() }
    }

    fun updateSettings(value: AppSettings) {
        onSettingsChange(value)
    }

    fun toast(text: String) {
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            snackbar.showSnackbar(text)
        }
    }

    fun sendListenEvents(events: List<ListenEvent>) {
        val client = api ?: return
        if (events.isEmpty() || !settings.reportListening) return
        reportScope.launch {
            events.forEach { event ->
                when (event) {
                    is ListenEvent.PlayAudio -> client.reportPlay(event)
                    else -> client.waveFeedback(event)
                }
            }
        }
    }

    fun applyLibrarySnapshot(snapshot: ru.muz.yandex.LibrarySnapshot) {
        liked = snapshot.liked
        likedIds = snapshot.liked.mapTo(mutableSetOf()) { it.id }
        likedAlbums = snapshot.albums
        playlists = snapshot.playlists
        wave = snapshot.wave
        chart = snapshot.chart
    }

    suspend fun loadHomeData(
        client: YandexClient,
        owner: YandexClient.Account? = account,
    ) = coroutineScope {
        var likedError: Throwable? = null
        var playlistsError: Throwable? = null
        val jobs = listOf(
            launch {
                client.likedTracks().fold(
                    onSuccess = { value -> liked = value; likedIds = value.mapTo(mutableSetOf()) { it.id } },
                    onFailure = { likedError = it },
                )
            },
            launch {
                client.playlists().fold(
                    onSuccess = { playlists = it },
                    onFailure = { playlistsError = it },
                )
            },
            launch {
                client.likedAlbums().fold(
                    onSuccess = { likedAlbums = it },
                    onFailure = { },
                )
            },
            launch {
                client.myWave().fold(
                    onSuccess = { wave = it },
                    onFailure = { },
                )
            },
            launch {
                client.chart().fold(
                    onSuccess = { chart = it },
                    onFailure = { },
                )
            },
        )
        jobs.joinAll()
        val albumsFromTracks = liked.mapNotNull { track ->
            if (track.albumId.isBlank() || track.album.isBlank()) null else {
                YandexClient.Album(
                    id = track.albumId,
                    title = track.album,
                    artist = track.artist,
                    coverUrl = track.coverUrl,
                    trackCount = 0,
                )
            }
        }
        likedAlbums = (likedAlbums + albumsFromTracks).distinctBy { it.id }
        if (likedError == null && owner != null) {
            librarySnapshotStore.write(owner, liked, likedAlbums, playlists, wave, chart)
        }
        collectionMessage = when {
            likedError != null && liked.isEmpty() && playlistsError != null && playlists.isEmpty() ->
                "Не удалось загрузить коллекцию"
            likedError != null && liked.isEmpty() -> "Не удалось загрузить любимые треки"
            playlistsError != null && playlists.isEmpty() && liked.isEmpty() -> "Не удалось загрузить плейлисты"
            else -> ""
        }
        message = collectionMessage
    }

    fun connect(rawToken: String) {
        val clean = rawToken.trim()
        if (clean.isBlank()) {
            message = "Вставьте OAuth-токен"
            return
        }
        busy = true
        message = "Подключаем Яндекс Музыку…"
        scope.launch {
            val client = YandexClient(clean)
            val info = client.accountInfo().getOrElse {
                message = it.message ?: "Не удалось войти"
                busy = false
                return@launch
            }
            api = client
            account = info
            tokenStore.save(clean)
            val cached = librarySnapshotStore.read(info.uid)
            cached?.let(::applyLibrarySnapshot)
            cached?.liked?.let(client::seedTracks)
            if (LibrarySnapshotStore.isFresh(cached, settings.syncIntervalMinutes)) {
                message = ""
                busy = false
                return@launch
            }
            message = if (info.serviceAvailable) "Синхронизируем изменения…" else "Аккаунт подключён, но подписка не подтверждена"
            loadHomeData(client, info)
            busy = false
        }
    }

    fun refreshHome() {
        val client = api ?: return
        busy = true
        message = "Обновляем рекомендации…"
        scope.launch {
            loadHomeData(client, account)
            if (message.isNotBlank() && listOf(liked, playlists, wave, chart).any { it.isNotEmpty() }) {
                message = ""
            }
            busy = false
        }
    }

    fun openPlaylist(value: YandexClient.Playlist) {
        val client = api ?: return
        selectedPlaylist = value
        selectedTracks = value.tracks
        busy = true
        scope.launch {
            client.playlistTracks(value).fold(
                onSuccess = { selectedTracks = it },
                onFailure = { toast(it.message ?: "Не удалось открыть плейлист") },
            )
            busy = false
        }
    }

    fun likedPlaylist() = YandexClient.Playlist(
        id = "liked",
        ownerUid = account?.uid.orEmpty(),
        kind = "liked",
        uuid = "",
        title = "Мне нравится",
        description = "Любимые треки",
        coverUrl = liked.firstNotNullOfOrNull { it.coverUrl },
        trackCount = liked.size,
        source = "",
        tracks = liked,
    )

    fun openLiked() {
        if (liked.isEmpty()) return
        selectedCollectionTitle = null
        selectedPlaylist = likedPlaylist()
        selectedTracks = liked
        collectionMessage = ""
        busy = false
    }

    fun toggleAlbumLike(value: YandexClient.Playlist) {
        val client = api ?: return
        val albumId = value.id.removePrefix("album:")
        if (albumId == value.id || albumId.isBlank()) return
        val album = YandexClient.Album(
            id = albumId,
            title = value.title,
            artist = value.description,
            coverUrl = value.coverUrl,
            trackCount = value.trackCount.coerceAtLeast(selectedTracks.size),
        )
        val wasLiked = likedAlbums.any { it.id == albumId }
        likedAlbums = if (wasLiked) likedAlbums.filterNot { it.id == albumId }
        else listOf(album) + likedAlbums
        scope.launch {
            client.setLikedAlbum(albumId, !wasLiked).onFailure {
                likedAlbums = if (wasLiked) listOf(album) + likedAlbums
                else likedAlbums.filterNot { it.id == albumId }
                toast("Не удалось изменить любимые альбомы")
            }
        }
    }

    fun playTracks(
        tracks: List<YandexClient.Track>,
        index: Int,
        source: PlaySource,
        isWave: Boolean = false,
    ) {
        if (tracks.isEmpty()) return
        playSource = source
        waveSession = isWave
        lastWaveContinuation = ""
        queue = tracks
        playback.playQueue(
            tracks.map { PlaybackController.YandexTrack(it.id, it.title, it.artist, it.coverUrl) },
            index,
        )
        showLyrics = settings.showLyricsByDefault
        if (settings.openPlayerOnTrack) showPlayer = true
    }

    fun downloadTrack(target: YandexClient.Track?, shareFile: Boolean) {
        val client = api ?: return
        val track = target ?: return
        transferBusy = true
        transferDiagnostic = ""
        transferMessage = if (shareFile) "Готовим аудиофайл…" else "Скачиваем трек…"
        scope.launch {
            TrackTransfer.download(context, client, track, forSharing = shareFile).fold(
                onSuccess = { uri ->
                    if (shareFile) {
                        TrackTransfer.shareFile(context, uri, track)
                        transferMessage = "Аудиофайл готов"
                    } else {
                        transferMessage = "Сохранено в Music/Aria"
                    }
                },
                onFailure = {
                    transferMessage = "Не удалось скачать трек"
                    transferDiagnostic = it.stackTraceToString()
                },
            )
            transferBusy = false
        }
    }

    fun downloadCover(target: YandexClient.Track?) {
        api ?: return
        val track = target ?: return
        transferBusy = true
        transferDiagnostic = ""
        transferMessage = "Сохраняем обложку…"
        scope.launch {
            TrackTransfer.downloadCover(context, track).fold(
                onSuccess = { transferMessage = "Обложка сохранена в Pictures/Aria" },
                onFailure = {
                    transferMessage = "Не удалось сохранить обложку"
                    transferDiagnostic = it.stackTraceToString()
                },
            )
            transferBusy = false
        }
    }

    fun openArtist(name: String, artistId: String = "") {
        val client = api ?: return
        if (name.isBlank()) return
        showPlayer = false
        selectedCollectionTitle = name
        selectedPlaylist = YandexClient.Playlist(
            id = "artist:$artistId:$name", ownerUid = "", kind = "", uuid = "",
            title = name, description = "Треки исполнителя", coverUrl = currentTrack?.coverUrl,
            trackCount = 0, source = "",
        )
        selectedTracks = emptyList()
        artistLoading = true
        scope.launch {
            val artist = if (artistId.isNotBlank()) {
                YandexClient.Artist(artistId, name, currentTrack?.coverUrl, 0)
            } else {
                val results = client.search(name).getOrDefault(YandexClient.SearchResults())
                results.artists.firstOrNull { it.name.equals(name, true) } ?: results.artists.firstOrNull()
            }
            if (artist == null) {
                toast("Исполнитель не найден")
            } else {
                client.artistTracks(artist).fold(
                    onSuccess = { selectedTracks = it },
                    onFailure = { toast(it.message ?: "Не удалось открыть исполнителя") },
                )
            }
            artistLoading = false
        }
    }

    fun toggleLike(target: YandexClient.Track? = currentTrack) {
        val client = api ?: return
        val track = target ?: return
        val newValue = track.id !in likedIds
        likedIds = if (newValue) likedIds + track.id else likedIds - track.id
        liked = if (newValue) listOf(track) + liked.filterNot { it.id == track.id }
        else liked.filterNot { it.id == track.id }
        if (selectedPlaylist?.id == "liked") {
            selectedTracks = if (newValue) listOf(track) + selectedTracks.filterNot { it.id == track.id }
            else selectedTracks.filterNot { it.id == track.id }
            selectedPlaylist = selectedPlaylist?.copy(trackCount = selectedTracks.size, tracks = selectedTracks)
        }
        scope.launch {
            client.setLiked(track.id, newValue).onFailure {
                likedIds = if (newValue) likedIds - track.id else likedIds + track.id
                liked = if (newValue) liked.filterNot { it.id == track.id }
                else listOf(track) + liked.filterNot { it.id == track.id }
                if (selectedPlaylist?.id == "liked") {
                    selectedTracks = if (newValue) selectedTracks.filterNot { it.id == track.id }
                    else listOf(track) + selectedTracks.filterNot { it.id == track.id }
                    selectedPlaylist = selectedPlaylist?.copy(trackCount = selectedTracks.size, tracks = selectedTracks)
                }
                toast(it.message ?: "Не удалось изменить лайк")
            }
        }
    }

    suspend fun tracksFor(value: YandexClient.Playlist): List<YandexClient.Track> {
        if (value.id == "liked") return liked
        if (selectedPlaylist?.id == value.id && selectedTracks.isNotEmpty()) return selectedTracks
        return api?.playlistTracks(value)?.getOrDefault(value.tracks).orEmpty()
    }

    fun sourceFor(value: YandexClient.Playlist): PlaySource = when {
        value.id == "liked" -> PlaySource.liked()
        value.id.startsWith("album:") -> PlaySource.album()
        value.id.startsWith("artist:") -> PlaySource.artist()
        else -> PlaySource.playlist(value.ownerUid, value.kind)
    }

    fun exportCsv(targets: List<YandexClient.Playlist>, fileTitle: String, share: Boolean) {
        if (exportBusy || targets.isEmpty()) return
        exportBusy = true
        scope.launch {
            try {
                if (targets.size > 1) toast("Собираем ${ru.muz.text.playlistsLabel(targets.size)}…")
                val rows = targets.flatMap { playlist ->
                    val kind = when {
                        playlist.id == "liked" -> PlaylistCsv.Kind.FAVORITE
                        playlist.id.startsWith("album:") -> PlaylistCsv.Kind.ALBUM
                        else -> PlaylistCsv.Kind.PLAYLIST
                    }
                    val title = if (playlist.id == selectedPlaylist?.id) selectedCollectionTitle ?: playlist.title else playlist.title
                    tracksFor(playlist).map { track ->
                        PlaylistCsv.Row(
                            title = track.title,
                            artist = track.artist,
                            album = track.album,
                            playlist = title,
                            kind = kind,
                            durationMs = track.durationMs,
                            trackId = track.id,
                            albumId = track.albumId,
                        )
                    }
                }
                if (rows.isEmpty()) {
                    toast("Нет треков для экспорта")
                    return@launch
                }
                val bytes = withContext(Dispatchers.Default) { PlaylistCsv.encode(rows) }
                val name = PlaylistCsv.fileName(fileTitle)
                if (share) {
                    TrackTransfer.shareExport(context, name, "text/csv", bytes)
                        .onFailure { toast("Не удалось подготовить CSV") }
                } else {
                    TrackTransfer.saveExport(context, name, "text/csv", bytes).fold(
                        onSuccess = { where -> toast("${ru.muz.text.tracksLabel(rows.size)} → $where") },
                        onFailure = { toast("Не удалось сохранить CSV") },
                    )
                }
            } finally {
                exportBusy = false
            }
        }
    }

    fun exportCollection(share: Boolean) {
        val targets = buildList {
            if (liked.isNotEmpty()) add(likedPlaylist())
            addAll(playlists)
        }
        if (targets.isEmpty()) toast("Коллекция пока пуста") else exportCsv(targets, "Коллекция Яндекс Музыки", share)
    }

    LaunchedEffect(Unit) {
        tokenStore.token?.let(::connect)
        statsSnapshot = listeningStats.snapshot()
    }
    LaunchedEffect(player.mediaId) {
            transferMessage = ""
            transferDiagnostic = ""
        if (player.mediaId.isNotBlank()) {
            listeningStats.recordStart(player.mediaId, player.title, player.artist)
            statsSnapshot = listeningStats.snapshot()
            val track = queue.firstOrNull { it.id == player.mediaId }
            sendListenEvents(
                listenSession.start(
                    trackId = player.mediaId,
                    albumId = track?.albumId.orEmpty(),
                    durationMs = track?.durationMs ?: player.durationMs,
                    source = playSource,
                ),
            )
        } else {
            sendListenEvents(listenSession.finish())
        }
    }
    LaunchedEffect(player.isPlaying, player.mediaId) {
        if (!player.isPlaying || player.mediaId.isBlank()) return@LaunchedEffect
        var last = SystemClock.elapsedRealtime()
        while (true) {
            delay(1_000)
            val now = SystemClock.elapsedRealtime()
            val state = latestPlayer
            sendListenEvents(listenSession.tick((now - last).coerceIn(0, 5_000), state.positionMs, state.durationMs))
            last = now
        }
    }
    DisposableEffect(listenSession) {
        onDispose { sendListenEvents(listenSession.finish()) }
    }
    LaunchedEffect(player.mediaId, queue, settings.preloadArtwork) {
        if (!settings.preloadArtwork || queue.isEmpty()) return@LaunchedEffect
        val current = queue.indexOfFirst { it.id == player.mediaId }
        queue.drop((current + 1).coerceAtLeast(0)).take(3).mapNotNull { it.coverUrl }.forEach { url ->
            context.imageLoader.enqueue(
                ImageRequest.Builder(context).data(url).memoryCacheKey(url).diskCacheKey(url).build(),
            )
        }
    }
    LaunchedEffect(player.isPlaying, player.mediaId, player.artist) {
        if (player.isPlaying && player.mediaId.isNotBlank()) {
            while (true) {
                kotlinx.coroutines.delay(5_000)
                listeningStats.addListening(
                    durationMs = 5_000,
                    trackId = player.mediaId,
                    title = player.title,
                    artist = player.artist,
                )
                statsSnapshot = listeningStats.snapshot()
            }
        }
    }
    LaunchedEffect(player.mediaId, currentTrack, lyricsRepository, showPlayer, showLyrics, lyricsRequestKey) {
        val repository = lyricsRepository
        val track = currentTrack
        val generation = ++lyricsGeneration
        if (repository != null && track != null && showPlayer && showLyrics) {
            lyricsTrackId = track.id
            lyrics = Lyrics.EMPTY
            lyricsLoading = true
            lyricsStatus = "Проверяем Яндекс Музыку…"
            lyricsError = ""
            try {
                val result = repository.load(track) {
                    if (generation == lyricsGeneration) lyricsStatus = it
                }
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                result.fold(
                    onSuccess = { lyrics = it },
                    onFailure = { lyricsError = it.message ?: "Текст для этого трека не найден" },
                )
            } finally {
                if (generation == lyricsGeneration) lyricsLoading = false
            }
        }
    }
    LaunchedEffect(player.mediaId, settings.preloadNextTracks, queue) {
        if (settings.preloadNextTracks && api != null && queue.isNotEmpty()) {
            val current = queue.indexOfFirst { it.id == player.mediaId }
            val quality = if (settings.dataSaver) {
                YandexClient.AudioQuality.ECONOMY
            } else {
                YandexClient.AudioQuality.HIGH
            }
            queue.drop((current + 1).coerceAtLeast(0)).take(2).forEach { next ->
                launch(Dispatchers.IO) { api?.streamUrl(next.id, quality) }
            }
        }
    }
    LaunchedEffect(api, settings.syncIntervalMinutes) {
        val interval = settings.syncIntervalMinutes
        if (api != null && interval > 0) {
            while (true) {
                kotlinx.coroutines.delay(interval * 60_000L)
                refreshHome()
            }
        }
    }
    LaunchedEffect(player.mediaId, waveSession, queue.size) {
        val client = api
        val currentIndex = queue.indexOfFirst { it.id == player.mediaId }
        val continuation = player.mediaId
        if (
            waveSession && client != null && continuation.isNotBlank() &&
            currentIndex >= 0 && currentIndex >= queue.lastIndex - 2 &&
            !waveLoadingMore && lastWaveContinuation != continuation
        ) {
            lastWaveContinuation = continuation
            waveLoadingMore = true
            client.myWave(continuation).fold(
                onSuccess = { nextBatch ->
                    val existingIds = queue.mapTo(hashSetOf()) { it.id.substringBefore(':') }
                    val fresh = nextBatch.filter { existingIds.add(it.id.substringBefore(':')) }
                    fresh.forEach { track ->
                        playback.addToQueue(
                            PlaybackController.YandexTrack(track.id, track.title, track.artist, track.coverUrl),
                        )
                    }
                    if (fresh.isNotEmpty()) {
                        queue = queue + fresh
                        wave = (wave + fresh).distinctBy { it.id.substringBefore(':') }
                    }
                },
                onFailure = { },
            )
            waveLoadingMore = false
        }
    }

    BackHandler(enabled = showSettings || showPlayer || selectedPlaylist != null) {
        when {
            showSettings -> showSettings = false
            showPlayer -> showPlayer = false
            selectedPlaylist != null -> {
                selectedPlaylist = null
                selectedCollectionTitle = null
                selectedTracks = emptyList()
            }
        }
    }

    val hasMini = player.title.isNotBlank()
    val snackbarBottom = when {
        api == null -> 16.dp
        showPlayer -> 150.dp
        showSettings || selectedPlaylist != null -> if (hasMini) 88.dp else 16.dp
        else -> if (hasMini) 168.dp else 92.dp
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalNowPlaying provides NowPlaying(player.mediaId, player.isPlaying),
    ) {
        Box(Modifier.fillMaxSize()) {
            Surface(Modifier.fillMaxSize(), color = background) {
                when {
                    api == null -> LoginScreen(busy, message, ::connect)

                    showSettings -> SettingsScreen(
                        settings = settings,
                        account = account,
                        player = player,
                        swipeMiniPlayer = settings.swipeMiniPlayer,
                        showMiniPlayerProgress = settings.showMiniPlayerProgress,
                        onChange = ::updateSettings,
                        onBack = { showSettings = false },
                        onLogout = {
                            sendListenEvents(listenSession.finish())
                            tokenStore.forget()
                            api = null
                            account = null
                            liked = emptyList()
                            likedAlbums = emptyList()
                            playlists = emptyList()
                            wave = emptyList()
                            chart = emptyList()
                            showSettings = false
                            message = "Токен удалён с устройства"
                        },
                        onMiniClick = {
                            showSettings = false
                            showPlayer = true
                        },
                        onPlayPause = playback::togglePlayPause,
                        onNext = playback::skipNext,
                        onPrevious = playback::skipPrevious,
                        onSyncNow = ::refreshHome,
                        syncBusy = busy,
                        exportBusy = exportBusy,
                        onExportCollection = ::exportCollection,
                    )

                    showPlayer -> FullPlayerScreen(
                        state = player,
                        lyrics = if (lyricsTrackId == player.mediaId) lyrics else Lyrics.EMPTY,
                        lyricsLoading = lyricsLoading || (showLyrics && lyricsTrackId != player.mediaId),
                        lyricsStatus = lyricsStatus,
                        lyricsError = if (lyricsTrackId == player.mediaId) lyricsError else "",
                        showLyrics = showLyrics,
                        settings = settings,
                        liked = currentTrack?.id in likedIds,
                        onBack = { showPlayer = false },
                        onToggleView = { showLyrics = !showLyrics },
                        onPlayPause = playback::togglePlayPause,
                        onPrevious = playback::skipPrevious,
                        onNext = playback::skipNext,
                        onSeek = playback::seekTo,
                        onLike = { toggleLike() },
                        onShuffle = { playback.setShuffle(!player.shuffle) },
                        onRepeat = {
                            val next = when (player.repeat) {
                                RepeatMode.OFF -> RepeatMode.ALL
                                RepeatMode.ALL -> RepeatMode.ONE
                                RepeatMode.ONE -> RepeatMode.OFF
                            }
                            playback.setRepeat(next)
                        },
                        onRetryLyrics = { lyricsRequestKey++ },
                        queue = queue,
                        transferBusy = transferBusy,
                        transferMessage = transferMessage,
                        onCopyTransferLog = {
                            if (transferDiagnostic.isNotBlank()) {
                                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                                    ?.setPrimaryClip(ClipData.newPlainText("Лог загрузки", transferDiagnostic))
                                transferMessage = "Диагностический лог скопирован"
                            }
                        },
                        onQueueTrack = playback::skipTo,
                        onDownload = { downloadTrack(currentTrack, false) },
                        onShareLink = { currentTrack?.let { TrackTransfer.shareLink(context, it) } },
                        onShareFile = { downloadTrack(currentTrack, true) },
                        onArtist = { openArtist(player.artist, currentTrack?.artistId.orEmpty()) },
                        onDownloadCover = { downloadCover(currentTrack) },
                        onOpenCover = { currentTrack?.let { TrackTransfer.openCover(context, it) } },
                        onCopyArtist = {
                            val artist = player.artist.trim()
                            if (artist.isNotBlank()) {
                                (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? ClipboardManager)
                                    ?.setPrimaryClip(ClipData.newPlainText("Артист", artist))
                                toast("Имя артиста скопировано")
                            }
                        },
                    )

                    selectedPlaylist != null -> PlaylistScreen(
                        playlist = selectedPlaylist!!.copy(title = selectedCollectionTitle ?: selectedPlaylist!!.title),
                        tracks = selectedTracks,
                        busy = busy || artistLoading,
                        compact = settings.compactLists,
                        listScalePercent = settings.listScalePercent,
                        listPageSize = settings.listPageSize,
                        swipeMiniPlayer = settings.swipeMiniPlayer,
                        showMiniPlayerProgress = settings.showMiniPlayerProgress,
                        player = player,
                        onBack = {
                            selectedPlaylist = null
                            selectedCollectionTitle = null
                            selectedTracks = emptyList()
                        },
                        onPlay = { tracks, index -> playTracks(tracks, index, sourceFor(selectedPlaylist!!)) },
                        onMiniClick = { showPlayer = true },
                        onPlayPause = playback::togglePlayPause,
                        onNext = playback::skipNext,
                        onPrevious = playback::skipPrevious,
                        onTrackMore = { actionTrack = it },
                        onPlaylistMore = { actionPlaylist = it },
                        albumLiked = selectedPlaylist!!.id.removePrefix("album:") in likedAlbums.map { it.id },
                        onToggleAlbumLike = { toggleAlbumLike(selectedPlaylist!!) },
                        exportBusy = exportBusy,
                        onExportCsv = { share ->
                            val playlist = selectedPlaylist!!
                            exportCsv(listOf(playlist), selectedCollectionTitle ?: playlist.title, share)
                        },
                    )

                    else -> MainShell(
                        tab = tab,
                        onTab = { tab = it },
                        player = player,
                        liked = liked,
                        likedAlbums = likedAlbums,
                        playlists = playlists,
                        wave = wave,
                        chart = chart,
                        searchResults = searchResults,
                        busy = busy,
                        message = message,
                        collectionMessage = collectionMessage,
                        compact = settings.compactLists,
                        listScalePercent = settings.listScalePercent,
                        listPageSize = settings.listPageSize,
                        showNavigationLabels = settings.showNavigationLabels,
                        swipeMiniPlayer = settings.swipeMiniPlayer,
                        showMiniPlayerProgress = settings.showMiniPlayerProgress,
                        account = account,
                        stats = statsSnapshot,
                        onSettings = { showSettings = true },
                        onPlaylist = ::openPlaylist,
                        onOpenLiked = ::openLiked,
                        onSearch = { query ->
                            val client = api ?: return@MainShell
                            busy = true
                            scope.launch {
                                client.search(query).fold(
                                    onSuccess = { searchResults = it },
                                    onFailure = { toast(it.message ?: "Поиск не удался") },
                                )
                                busy = false
                            }
                        },
                        onPlayLiked = { playTracks(liked, it, PlaySource.liked()) },
                        onPlayLibrary = { tracks, index -> playTracks(tracks, index, PlaySource.liked()) },
                        onPlayWave = { playTracks(wave, 0, PlaySource.wave(), isWave = true) },
                        onPlayChart = { playTracks(chart, it, PlaySource.chart()) },
                        onRefresh = ::refreshHome,
                        onPlaySearch = { playTracks(searchResults.tracks, it, PlaySource.search()) },
                        keepSearchHistory = settings.keepSearchHistory,
                        onMiniClick = { showPlayer = true },
                        onPlayPause = playback::togglePlayPause,
                        onNext = playback::skipNext,
                        onPrevious = playback::skipPrevious,
                        onTrackMore = { actionTrack = it },
                        onPlaylistMore = { actionPlaylist = it },
                        onAlbum = { album ->
                            selectedCollectionTitle = album.title
                            selectedPlaylist = YandexClient.Playlist(
                                id = "album:${album.id}", ownerUid = "", kind = "", uuid = "",
                                title = album.title, description = album.artist, coverUrl = album.coverUrl,
                                trackCount = album.trackCount, source = "",
                            )
                            selectedTracks = emptyList()
                            busy = true
                            scope.launch {
                                api?.albumTracks(album)?.fold(
                                    onSuccess = { selectedTracks = it },
                                    onFailure = { toast(it.message ?: "Не удалось открыть альбом") },
                                )
                                busy = false
                            }
                        },
                        onArtist = { artist ->
                            selectedCollectionTitle = artist.name
                            selectedPlaylist = YandexClient.Playlist(
                                id = "artist:${artist.id}", ownerUid = "", kind = "", uuid = "",
                                title = artist.name, description = "Популярные треки исполнителя", coverUrl = artist.coverUrl,
                                trackCount = artist.trackCount, source = "",
                            )
                            selectedTracks = emptyList()
                            busy = true
                            scope.launch {
                                api?.artistTracks(artist)?.fold(
                                    onSuccess = { selectedTracks = it },
                                    onFailure = { toast(it.message ?: "Не удалось открыть исполнителя") },
                                )
                                busy = false
                            }
                        },
                    )
                }
            }
            SnackbarHost(
                snackbar,
                Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = snackbarBottom),
            )
        }
        actionTrack?.let { track ->
            TrackActionsSheet(
                track = track,
                liked = track.id in likedIds,
                busy = transferBusy,
                onDismiss = { actionTrack = null },
                onPlayNext = {
                    playback.playNext(PlaybackController.YandexTrack(track.id, track.title, track.artist, track.coverUrl))
                    val current = queue.indexOfFirst { it.id == player.mediaId }
                    queue = queue.toMutableList().apply { add((current + 1).coerceIn(0, size), track) }
                    actionTrack = null
                    toast("Будет играть следующим")
                },
                onAddToQueue = {
                    playback.addToQueue(PlaybackController.YandexTrack(track.id, track.title, track.artist, track.coverUrl))
                    queue = queue + track
                    actionTrack = null
                    toast("Добавлено в очередь")
                },
                onToggleLike = { toggleLike(track); actionTrack = null },
                onDownload = { downloadTrack(track, false); actionTrack = null },
                onShareLink = { TrackTransfer.shareLink(context, track); actionTrack = null },
                    onShareFile = { downloadTrack(track, true); actionTrack = null },
                    onDownloadCover = { downloadCover(track); actionTrack = null },
                    onOpenCover = { TrackTransfer.openCover(context, track); actionTrack = null },
            )
        }
        actionPlaylist?.let { playlist ->
            PlaylistActionsSheet(
                playlist = playlist,
                busy = busy || transferBusy,
                onDismiss = { actionPlaylist = null },
                onOpen = {
                    actionPlaylist = null
                    if (selectedPlaylist?.id != playlist.id) openPlaylist(playlist)
                },
                onPlay = {
                    actionPlaylist = null
                    scope.launch {
                        val tracks = tracksFor(playlist)
                        if (tracks.isNotEmpty()) playTracks(tracks, 0, sourceFor(playlist)) else toast("В плейлисте нет доступных треков")
                    }
                },
                onDownload = {
                    actionPlaylist = null
                    scope.launch {
                        val tracks = tracksFor(playlist)
                        transferBusy = true
                        toast("Скачиваем плейлист…")
                        transferMessage = "Скачиваем плейлист: 0 из ${tracks.size}"
                        var saved = 0
                        tracks.forEach { track ->
                            TrackTransfer.download(context, api ?: return@launch, track, false).onSuccess { saved++ }
                            transferMessage = "Скачиваем плейлист: ${saved} из ${tracks.size}"
                        }
                        transferBusy = false
                        transferMessage = "Плейлист сохранён: ${ru.muz.text.tracksLabel(saved)}"
                        toast(transferMessage)
                    }
                },
                onShare = { TrackTransfer.sharePlaylistLink(context, playlist); actionPlaylist = null },
                exportBusy = exportBusy,
                onExportCsv = { share ->
                    actionPlaylist = null
                    exportCsv(listOf(playlist), playlist.title, share)
                },
            )
        }
    }
}
