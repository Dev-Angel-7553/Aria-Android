package ru.muz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import ru.muz.update.AppRelease
import ru.muz.settings.HomeSection
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.AssistChip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import ru.muz.player.PlayerState
import ru.muz.yandex.YandexClient
import ru.muz.stats.ListeningStatsSnapshot

@Composable
internal fun MainShell(
    tab: MainTab,
    onTab: (MainTab) -> Unit,
    player: PlayerState,
    liked: List<YandexClient.Track>,
    likedAlbums: List<YandexClient.Album>,
    playlists: List<YandexClient.Playlist>,
    wave: List<YandexClient.Track>,
    chart: List<YandexClient.Track>,
    searchResults: YandexClient.SearchResults,
    busy: Boolean,
    message: String,
    collectionMessage: String,
    compact: Boolean,
    listScalePercent: Int,
    listPageSize: Int,
    showNavigationLabels: Boolean,
    swipeMiniPlayer: Boolean,
    showMiniPlayerProgress: Boolean,
    account: YandexClient.Account?,
    stats: ListeningStatsSnapshot,
    statsSource: ru.muz.stats.ListeningStats? = null,
    waveSubtitle: String = "",
    onWaveSettings: () -> Unit = {},
    onPlayStatTracks: (List<ru.muz.stats.TrackStat>, Int) -> Unit = { _, _ -> },
    update: AppRelease? = null,
    updateProgress: Float? = null,
    onUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    tabs: List<MainTab> = MainTab.entries,
    homeSections: List<HomeSection> = HomeSection.entries,
    onImportCsv: () -> Unit = {},
    onSettings: () -> Unit,
    onPlaylist: (YandexClient.Playlist) -> Unit,
    onSearch: (String) -> Unit,
    onOpenLiked: () -> Unit,
    onPlayLiked: (Int) -> Unit,
    onPlayLibrary: (List<YandexClient.Track>, Int) -> Unit,
    onPlayWave: () -> Unit,
    onPlayChart: (Int) -> Unit,
    onRefresh: () -> Unit,
    onPlaySearch: (Int) -> Unit,
    keepSearchHistory: Int,
    onMiniClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTrackMore: (YandexClient.Track) -> Unit,
    onPlaylistMore: (YandexClient.Playlist) -> Unit,
    onAlbum: (YandexClient.Album) -> Unit,
    onArtist: (YandexClient.Artist) -> Unit,
    haptics: Boolean = true,
) {
    val backdrop = rememberGlassBackdrop()
    val glass = LocalAppStyle.current == ru.muz.settings.AppStyle.GLASS
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Column {
                PlaybackErrorBanner(player.error)
                AnimatedVisibility(player.title.isNotBlank()) {
                    CompactPlayer(
                        player, onMiniClick, onPlayPause, onNext, onPrevious,
                        swipeMiniPlayer, showMiniPlayerProgress,
                        backdrop = if (glass) backdrop else null,
                    )
                }
                AriaTabBar(tabs, tab, onTab, showNavigationLabels, if (glass) backdrop else null, haptics)
            }
        },
    ) { padding ->
        androidx.compose.foundation.layout.Box(Modifier.fillMaxSize().glassSource(if (glass) backdrop else null)) {
        if (glass) MovingCoverBackground(Accent, strength = 0.55f)
        androidx.compose.animation.AnimatedContent(
            targetState = tab,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(tween(animationDuration(240))) +
                    androidx.compose.animation.scaleIn(tween(animationDuration(240)), initialScale = 0.985f)) togetherWith
                    androidx.compose.animation.fadeOut(tween(animationDuration(150)))
            },
            label = "tab-content",
        ) { current ->
        when (current) {
            MainTab.HOME -> HomeScreen(
                account, liked, wave, chart, busy, message, padding,
                stats, onSettings, onRefresh, onPlaylist, onPlayLiked, onPlayWave, onPlayChart,
                onTrackMore,
                onPlaylistMore,
                update, updateProgress, onUpdate, onDismissUpdate,
                homeSections, playlists, waveSubtitle, onWaveSettings,
            )
            MainTab.LIBRARY -> LibraryScreen(
                liked, likedAlbums, playlists, busy, collectionMessage, compact, listScalePercent, listPageSize, padding,
                onSettings, onRefresh, onPlaylist, onOpenLiked, onPlayLibrary,
                onTrackMore,
                onPlaylistMore,
                onAlbum,
                onImportCsv,
            )
            MainTab.SEARCH -> SearchScreen(
                searchResults, compact, listScalePercent, listPageSize, keepSearchHistory, busy, padding, onSettings, onSearch,
                onPlaylist, onPlaySearch,
                onTrackMore,
                onPlaylistMore,
                onAlbum,
                onArtist,
            )
            MainTab.STATS -> StatisticsScreen(stats = stats, padding = padding, source = statsSource, onPlayTracks = onPlayStatTracks)
        }
        }
        }
    }
}

@Composable
private fun HomeScreen(
    account: YandexClient.Account?,
    liked: List<YandexClient.Track>,
    wave: List<YandexClient.Track>,
    chart: List<YandexClient.Track>,
    busy: Boolean,
    message: String,
    padding: PaddingValues,
    stats: ListeningStatsSnapshot,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onPlaylist: (YandexClient.Playlist) -> Unit,
    onPlay: (Int) -> Unit,
    onPlayWave: () -> Unit,
    onPlayChart: (Int) -> Unit,
    onTrackMore: (YandexClient.Track) -> Unit,
    onPlaylistMore: (YandexClient.Playlist) -> Unit,
    update: AppRelease?,
    updateProgress: Float?,
    onUpdate: () -> Unit,
    onDismissUpdate: () -> Unit,
    sections: List<HomeSection>,
    playlists: List<YandexClient.Playlist>,
    waveSubtitle: String = "",
    onWaveSettings: () -> Unit = {},
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp + padding.calculateBottomPadding()),
    ) {
        if (update != null) {
            item { UpdateBanner(update, updateProgress, onUpdate, onDismissUpdate) }
        }
        item {
            ScreenHeader("Для вас", account?.displayName) {
                Row {
                    IconButton(onClick = onRefresh, enabled = !busy) {
                        Icon(Icons.Filled.Refresh, "Обновить")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, "Настройки")
                    }
                }
            }
        }
        if (busy) {
            item {
                if (liked.isNotEmpty() || wave.isNotEmpty() || chart.isNotEmpty()) {
                    LoadingRibbon("Обновляем рекомендации")
                } else {
                    LoadingCards()
                }
            }
        }
        if (message.isNotBlank()) {
            item {
                Text(
                    message,
                    color = SoftText,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        if (sections.isEmpty()) {
            item { EmptyHint("Все блоки главной скрыты. Включить их можно в «Настройки → Главная и вкладки»") }
        }
        sections.forEach { section ->
            when (section) {
                HomeSection.WAVE -> item(key = "home-wave") { MyWaveCard(wave.size, waveSubtitle, onWaveSettings, if (wave.isEmpty()) onRefresh else onPlayWave) }
                HomeSection.RECENT -> {
                    if (liked.isNotEmpty()) item(key = "home-recent-title") { SectionTitle("Недавно добавленное") }
                    itemsIndexed(liked.take(12), key = { _, it -> "recent-${it.id}" }) { index, track ->
                        TrackRow(track, compact = false, onMore = onTrackMore) { onPlay(index) }
                    }
                }
                HomeSection.PLAYLISTS -> if (playlists.isNotEmpty()) {
                    item(key = "home-playlists-title") { SectionTitle("Ваши плейлисты") }
                    item(key = "home-playlists") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(playlists.take(20), key = { "home-pl-${it.id}" }) { PlaylistCard(it, onPlaylist, onPlaylistMore) }
                        }
                    }
                }
                HomeSection.CHART -> {
                    if (chart.isNotEmpty()) item(key = "home-chart-title") { SectionTitle("Чарт Яндекс Музыки") }
                    itemsIndexed(chart.take(20), key = { _, it -> "chart-${it.id}" }) { index, track ->
                        TrackRow(track, compact = false, onMore = onTrackMore) { onPlayChart(index) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    liked: List<YandexClient.Track>,
    likedAlbums: List<YandexClient.Album>,
    playlists: List<YandexClient.Playlist>,
    busy: Boolean,
    message: String,
    compact: Boolean,
    listScalePercent: Int,
    listPageSize: Int,
    padding: PaddingValues,
    onSettings: () -> Unit,
    onRefresh: () -> Unit,
    onPlaylist: (YandexClient.Playlist) -> Unit,
    onOpenLiked: () -> Unit,
    onPlay: (List<YandexClient.Track>, Int) -> Unit,
    onTrackMore: (YandexClient.Track) -> Unit,
    onPlaylistMore: (YandexClient.Playlist) -> Unit,
    onAlbum: (YandexClient.Album) -> Unit,
    onImportCsv: () -> Unit,
) {
    var filter by remember { mutableStateOf("Все") }
    var sort by remember { mutableStateOf("Добавленные") }
    var selectedArtist by remember { mutableStateOf<String?>(null) }
    var visibleCount by remember(liked, filter, sort, selectedArtist, listPageSize) { mutableStateOf(listPageSize) }
    var visiblePlaylistCount by remember(playlists, listPageSize) { mutableStateOf(listPageSize) }
    val artists = remember(liked) { liked.groupBy { it.artist }.filterKeys { it.isNotBlank() } }
    val shown = remember(liked, filter, sort, selectedArtist) {
        val filtered = when (filter) {
            "С текстом" -> liked.filter { it.lyricsAvailable }
            "Явные" -> liked.filter { it.explicit }
            else -> liked
        }
        val base = selectedArtist?.let { artist -> filtered.filter { it.artist == artist } } ?: filtered
        when (sort) {
            "Название" -> base.sortedBy { it.title.lowercase() }
            "Исполнитель" -> base.sortedBy { it.artist.lowercase() }
            "Длительность" -> base.sortedByDescending { it.durationMs }
            else -> base
        }
    }
    val visible = remember(shown, visibleCount) { shown.take(visibleCount) }
    LazyColumn(
        Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp + padding.calculateBottomPadding()),
    ) {
        item {
            ScreenHeader("Коллекция", "Ваши треки и плейлисты") {
                Row {
                    IconButton(onClick = onImportCsv) {
                        Icon(Icons.Filled.FileUpload, "Импорт плейлиста из CSV")
                    }
                    IconButton(onClick = onRefresh, enabled = !busy) {
                        Icon(Icons.Filled.Refresh, "Обновить коллекцию")
                    }
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, "Настройки")
                    }
                }
            }
        }
        if (busy) {
            item {
                if (liked.isEmpty() && playlists.isEmpty()) LoadingCards()
                else LoadingRibbon("Обновляем коллекцию")
            }
        }
        if (message.isNotBlank()) {
            item {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text(message, color = SoftText)
                    TextButton(onClick = onRefresh, enabled = !busy) { Text("Попробовать снова") }
                }
            }
        }
        item {
            CollectionHero(liked.size) {
                if (liked.isNotEmpty()) onOpenLiked()
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf("Все", "С текстом", "Явные")) { value ->
                    FilterChip(selected = filter == value, onClick = { filter = value }, label = { Text(value) })
                }
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf("Добавленные", "Название", "Исполнитель", "Длительность")) { value ->
                    FilterChip(selected = sort == value, onClick = { sort = value }, label = { Text(value) })
                }
            }
        }
        if (artists.isNotEmpty()) {
            item { SectionTitle("Исполнители · ${artists.size}") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(artists.entries.sortedByDescending { it.value.size }.take(12)) { entry ->
                        FilterChip(
                            selected = selectedArtist == entry.key,
                            onClick = { selectedArtist = if (selectedArtist == entry.key) null else entry.key },
                            label = { Text("${entry.key} · ${entry.value.size}") },
                        )
                    }
                }
            }
        }
        if (likedAlbums.isNotEmpty()) {
            item { SectionTitle("Альбомы · ${likedAlbums.size}") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(likedAlbums, key = { "liked-album-${it.id}" }) { album ->
                        AlbumCard(album, onAlbum)
                    }
                }
            }
        }
        item { SectionTitle("Плейлисты") }
        if (playlists.isEmpty()) {
            item { EmptyHint("Созданные в Яндекс Музыке плейлисты появятся здесь") }
        }
        items(playlists.take(visiblePlaylistCount), key = { it.id }) { PlaylistListRow(it, onPlaylist, onPlaylistMore) }
        if (visiblePlaylistCount < playlists.size) {
            item {
                TextButton(
                    onClick = { visiblePlaylistCount = (visiblePlaylistCount + listPageSize).coerceAtMost(playlists.size) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                ) { Text("Показать ещё плейлисты · ${playlists.size - visiblePlaylistCount}") }
            }
        }
        item { SectionTitle("Треки · ${shown.size}") }
        if (!busy && shown.isEmpty()) {
            item { EmptyHint("Любимые треки появятся здесь после обновления") }
        }
        itemsIndexed(visible, key = { _, it -> it.id }) { index, track ->
            TrackRow(track, compact, listScalePercent, onTrackMore) { onPlay(shown, index) }
        }
        if (visible.size < shown.size) {
            item {
                TextButton(
                    onClick = { visibleCount = (visibleCount + listPageSize).coerceAtMost(shown.size) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                ) { Text("Показать ещё · ${shown.size - visible.size}") }
            }
        }
    }
}

@Composable
private fun SearchScreen(
    results: YandexClient.SearchResults,
    compact: Boolean,
    listScalePercent: Int,
    listPageSize: Int,
    keepSearchHistory: Int,
    busy: Boolean,
    padding: PaddingValues,
    onSettings: () -> Unit,
    onSearch: (String) -> Unit,
    onPlaylist: (YandexClient.Playlist) -> Unit,
    onPlay: (Int) -> Unit,
    onTrackMore: (YandexClient.Track) -> Unit,
    onPlaylistMore: (YandexClient.Playlist) -> Unit,
    onAlbum: (YandexClient.Album) -> Unit,
    onArtist: (YandexClient.Artist) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var visibleCount by remember(results, listPageSize) { mutableStateOf(listPageSize) }
    val context = LocalContext.current
    val historyPrefs = remember { context.getSharedPreferences("search_history", android.content.Context.MODE_PRIVATE) }
    val historySeparator = "\u001F"
    var recentQueries by remember {
        mutableStateOf(
            historyPrefs.getString("queries_ordered", null)?.split(historySeparator)
                ?.filter(String::isNotBlank)
                ?: historyPrefs.getStringSet("queries", emptySet()).orEmpty().toList(),
        )
    }
    fun persistHistory(values: List<String>) {
        historyPrefs.edit()
            .putString("queries_ordered", values.joinToString(historySeparator))
            .remove("queries")
            .apply()
    }
    val focusManager: FocusManager = LocalFocusManager.current
    fun submitSearch(raw: String) {
        val value = raw.trim()
        if (value.isBlank()) return
        query = value
        val updated = listOf(value) + recentQueries.filterNot { it.equals(value, ignoreCase = true) }
        recentQueries = updated.take(keepSearchHistory.coerceAtLeast(0))
        persistHistory(recentQueries)
        onSearch(value)
        focusManager.clearFocus()
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(top = padding.calculateTopPadding()),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp + padding.calculateBottomPadding()),
    ) {
        item {
            ScreenHeader("Поиск", "Треки и плейлисты Яндекс Музыки") {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Filled.Settings, "Настройки")
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                label = { Text("Что хотите послушать?") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    submitSearch(query)
                }),
                trailingIcon = if (query.isNotEmpty()) {
                    { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Очистить") } }
                } else null,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(28.dp * LocalCornerRadiusScale.current),
            )
        }
        if (recentQueries.isNotEmpty() && query.isBlank()) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.History, null, tint = SoftText, modifier = Modifier.size(18.dp))
                    Text("Недавние", color = SoftText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp).weight(1f))
                    TextButton(onClick = {
                        recentQueries = emptyList()
                        historyPrefs.edit().remove("queries_ordered").remove("queries").apply()
                    }) {
                        Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp))
                        Text("Очистить", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            items(recentQueries, key = { "recent-$it" }) { value ->
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AssistChip(
                        onClick = { submitSearch(value) },
                        label = { Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Filled.History, null, modifier = Modifier.size(16.dp)) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = {
                        recentQueries = recentQueries.filterNot { it == value }
                        persistHistory(recentQueries)
                    }) {
                        Icon(Icons.Filled.Close, "Удалить запрос", tint = SoftText, modifier = Modifier.size(18.dp))
                    }
                }
            }
        } else if (query.length >= 2 && results.tracks.isEmpty() && results.artists.isEmpty() && results.playlists.isEmpty()) {
            val suggestions = recentQueries.filter { it.contains(query.trim(), ignoreCase = true) }
            if (suggestions.isNotEmpty()) {
                item { Text("Предложения", color = SoftText, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) }
                items(suggestions, key = { "suggestion-$it" }) { value ->
                    TextButton(onClick = { submitSearch(value) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
                        Icon(Icons.Filled.History, null, tint = SoftText)
                        Text(value, modifier = Modifier.padding(start = 10.dp).weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        if (busy) item { LoadingCards() }
        if (results.artists.isNotEmpty()) {
            item { SectionTitle("Исполнители") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(results.artists, key = { it.id }) { ArtistCard(it, onArtist) }
                }
            }
        }
        if (results.albums.isNotEmpty()) {
            item { SectionTitle("Альбомы") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(results.albums, key = { it.id }) { AlbumCard(it, onAlbum) }
                }
            }
        }
        if (results.playlists.isNotEmpty()) {
            item { SectionTitle("Плейлисты") }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(results.playlists, key = { it.id }) {
                        PlaylistCard(it, onPlaylist, onPlaylistMore)
                    }
                }
            }
        }
        if (results.tracks.isNotEmpty()) item { SectionTitle("Треки") }
        val visibleSearchTracks = results.tracks.take(visibleCount)
        itemsIndexed(visibleSearchTracks, key = { _, it -> it.id }) { index, track ->
            TrackRow(track, compact, listScalePercent, onTrackMore) { onPlay(index) }
        }
        if (results.tracks.size > visibleSearchTracks.size) {
            item {
                TextButton(
                    onClick = { visibleCount = (visibleCount + listPageSize).coerceAtMost(results.tracks.size) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                ) { Text("Показать ещё · ${results.tracks.size - visibleSearchTracks.size}") }
            }
        }
        if (!busy && results.tracks.isEmpty() && results.playlists.isEmpty() && results.albums.isEmpty() && results.artists.isEmpty()) {
            item { EmptyHint("Введите исполнителя, трек или название плейлиста") }
        }
    }
}

@Composable
internal fun PlaylistScreen(
    playlist: YandexClient.Playlist,
    tracks: List<YandexClient.Track>,
    busy: Boolean,
    compact: Boolean,
    listScalePercent: Int,
    listPageSize: Int,
    player: PlayerState,
    swipeMiniPlayer: Boolean,
    showMiniPlayerProgress: Boolean,
    onBack: () -> Unit,
    onPlay: (List<YandexClient.Track>, Int) -> Unit,
    onMiniClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onTrackMore: (YandexClient.Track) -> Unit,
    onPlaylistMore: (YandexClient.Playlist) -> Unit,
    albumLiked: Boolean,
    onToggleAlbumLike: () -> Unit,
    exportBusy: Boolean = false,
    onExportCsv: (share: Boolean) -> Unit = {},
    offlineBusy: Boolean = false,
    onSaveOffline: () -> Unit = {},
) {
    var exportMenu by remember { mutableStateOf(false) }
    var query by remember(playlist.id) { mutableStateOf("") }
    var sort by remember(playlist.id) { mutableStateOf("Порядок") }
    var visibleCount by remember(playlist.id, listPageSize) { mutableStateOf(listPageSize) }
    val shown = remember(tracks, query, sort) {
        val filtered = query.trim().takeIf { it.isNotEmpty() }?.let { needle ->
            tracks.filter {
                it.title.contains(needle, ignoreCase = true) ||
                    it.artist.contains(needle, ignoreCase = true) ||
                    it.album.contains(needle, ignoreCase = true)
            }
        } ?: tracks
        when (sort) {
            "Название" -> filtered.sortedBy { it.title.lowercase() }
            "Исполнитель" -> filtered.sortedBy { it.artist.lowercase() }
            else -> filtered
        }
    }
    val isAlbum = playlist.id.startsWith("album:")
    val visible = remember(shown, visibleCount) { shown.take(visibleCount) }
    val isLiked = playlist.id == "liked"
    val isSynthetic = isAlbum || isLiked || playlist.id.startsWith("artist:")
    val screenLabel = when {
        isLiked -> "Коллекция"
        isAlbum -> "Альбом"
        playlist.id.startsWith("artist:") -> "Исполнитель"
        else -> "Плейлист"
    }
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            Column {
                PlaybackErrorBanner(player.error)
                AnimatedVisibility(player.title.isNotBlank()) {
                    CompactPlayer(
                        player, onMiniClick, onPlayPause, onNext, onPrevious,
                        swipeMiniPlayer, showMiniPlayerProgress,
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(scaffoldPadding),
            contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
        ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Назад") }
                Text(screenLabel, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Box {
                    IconButton(onClick = { exportMenu = true }, enabled = tracks.isNotEmpty()) {
                        Icon(Icons.Filled.FileDownload, "Сохранить или экспортировать")
                    }
                    DropdownMenu(expanded = exportMenu, onDismissRequest = { exportMenu = false }) {
                        DropdownMenuItem(
                            enabled = !exportBusy,
                            text = { Text("Сохранить CSV в Загрузки") },
                            leadingIcon = { Icon(Icons.Filled.FileDownload, null) },
                            onClick = { exportMenu = false; onExportCsv(false) },
                        )
                        DropdownMenuItem(
                            text = { Text("Отправить CSV") },
                            leadingIcon = { Icon(Icons.Filled.Share, null) },
                            onClick = { exportMenu = false; onExportCsv(true) },
                        )
                        DropdownMenuItem(
                            text = { Text(if (offlineBusy) "Сохраняем для офлайна…" else "Сохранить для офлайна") },
                            leadingIcon = { Icon(Icons.Filled.DownloadForOffline, null) },
                            enabled = !offlineBusy,
                            onClick = { exportMenu = false; onSaveOffline() },
                        )
                    }
                }
                if (isAlbum) {
                    IconButton(onClick = onToggleAlbumLike) {
                        Icon(
                            if (albumLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            if (albumLiked) "Убрать альбом из любимых" else "Добавить альбом в любимые",
                            tint = if (albumLiked) Accent else SoftText,
                        )
                    }
                } else if (!isSynthetic) {
                    IconButton(onClick = { onPlaylistMore(playlist) }) { Icon(Icons.Filled.MoreVert, "Ещё") }
                }
            }
        }
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Artwork(playlist.coverUrl, Modifier.height(230.dp).fillMaxWidth(0.7f), 28.dp)
                Text(
                    playlist.title,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(top = 20.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    ru.muz.text.tracksLabel(playlist.trackCount.coerceAtLeast(tracks.size)),
                    color = SoftText,
                    modifier = Modifier.padding(top = 6.dp),
                )
                if (playlist.description.isNotBlank()) {
                    Text(
                        playlist.description,
                        color = SoftText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Row(
                    Modifier.padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = { if (shown.isNotEmpty()) onPlay(shown, 0) },
                        enabled = shown.isNotEmpty(),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, null)
                        Spacer(Modifier.padding(horizontal = 3.dp))
                        Text("Слушать", fontWeight = FontWeight.Bold)
                    }
                    IconButton(
                        onClick = {
                            if (shown.isNotEmpty()) {
                                val shuffled = shown.shuffled()
                                onPlay(shuffled, 0)
                            }
                        },
                        enabled = shown.isNotEmpty(),
                    ) {
                        Icon(Icons.Filled.Shuffle, "Перемешать")
                    }
                }
            }
        }
        if (busy) item { LoadingCards() }
        if (tracks.size >= 20) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { query = "" }) { Icon(Icons.Filled.Close, "Очистить") } }
                    } else null,
                    label = { Text("Найти в ${if (isLiked) "любимых треках" else "списке"}") },
                )
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf("Порядок", "Название", "Исполнитель")) { value ->
                    FilterChip(selected = sort == value, onClick = { sort = value }, label = { Text(value) })
                }
            }
        }
        if (!busy && shown.isEmpty()) item { EmptyHint(if (query.isBlank()) "Здесь пока нет треков" else "Ничего не найдено") }
        itemsIndexed(visible, key = { _, it -> it.id }) { index, track ->
            TrackRow(track, compact, listScalePercent, onMore = onTrackMore) { onPlay(shown, index) }
        }
        if (visible.size < shown.size) {
            item {
                TextButton(onClick = { visibleCount = (visibleCount + listPageSize).coerceAtMost(shown.size) }, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                    Text("Показать ещё · ${shown.size - visible.size}")
                }
            }
        }
        }
    }
}

@Composable
private fun UpdateBanner(
    release: AppRelease,
    progress: Float?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(20.dp * LocalCornerRadiusScale.current))
            .background(scheme.primaryContainer)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.SystemUpdate, null, tint = scheme.onPrimaryContainer)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Доступна Aria ${release.version}", fontWeight = FontWeight.Bold, color = scheme.onPrimaryContainer)
                Text(
                    release.notes.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.trimStart('-', '*', ' ')
                        ?: "Новая версия на GitHub",
                    color = scheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (progress != null) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            )
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Позже", color = scheme.onPrimaryContainer) }
                Button(onClick = onUpdate) { Text("Обновить") }
            }
        }
    }
}
