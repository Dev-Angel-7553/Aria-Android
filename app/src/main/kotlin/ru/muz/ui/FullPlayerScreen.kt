package ru.muz.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.imageLoader
import androidx.compose.animation.animateColorAsState
import android.graphics.drawable.BitmapDrawable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs
import ru.muz.lyrics.Lyrics
import ru.muz.lyrics.LyricsScreen
import ru.muz.lyrics.LyricsSettings
import ru.muz.player.PlayerState
import ru.muz.player.RepeatMode as PlayerRepeatMode
import ru.muz.player.formatDuration
import ru.muz.settings.AppSettings
import ru.muz.settings.LyricsAlignment
import ru.muz.settings.PlayerLayout
import ru.muz.yandex.YandexClient
import ru.muz.settings.PlayerAction
import androidx.compose.material3.LocalContentColor
import ru.muz.theme.ColorMath
import androidx.compose.ui.graphics.toArgb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullPlayerScreen(
    state: PlayerState,
    lyrics: Lyrics,
    lyricsLoading: Boolean,
    lyricsStatus: String,
    lyricsError: String,
    showLyrics: Boolean,
    settings: AppSettings,
    liked: Boolean,
    onBack: () -> Unit,
    onToggleView: () -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onRetryLyrics: () -> Unit,
    queue: List<YandexClient.Track>,
    transferBusy: Boolean,
    transferMessage: String,
    onCopyTransferLog: () -> Unit = {},
    onQueueTrack: (Int) -> Unit,
    onDownload: () -> Unit,
    onDownloadCover: () -> Unit,
    onOpenCover: () -> Unit,
    onShareLink: () -> Unit,
    onShareFile: () -> Unit,
    onArtist: () -> Unit,
    onCopyArtist: () -> Unit,
    tools: PlayerTools = PlayerTools(),
) {
    val position by rememberPlaybackPosition(state)
    var quoteStart by remember(state.mediaId) { mutableStateOf<Int?>(null) }
    var pendingQuote by remember(state.mediaId) { mutableStateOf(false) }
    LaunchedEffect(pendingQuote, lyrics) {
        if (pendingQuote && lyrics.lines.isNotEmpty()) {
            pendingQuote = false
            quoteStart = lyrics.activeIndexAt(position).coerceAtLeast(0)
        }
    }
    val onQuote = {
        if (lyrics.lines.isNotEmpty()) {
            quoteStart = lyrics.activeIndexAt(position).coerceAtLeast(0)
        } else {
            pendingQuote = true
            if (!showLyrics) onToggleView()
        }
    }
    val reduceMotion = LocalReduceMotion.current
    var showQueue by remember { mutableStateOf(false) }
    var playerDrag by remember(state.mediaId) { mutableFloatStateOf(0f) }
    val playerOffset by animateDpAsState(
        targetValue = (if (reduceMotion) 0f else playerDrag / 5f).coerceIn(-34f, 34f).dp,
        animationSpec = tween(animationDuration(180)),
        label = "full-player-swipe",
    )
    val playerSwipeModifier = if (settings.fullPlayerSwipe && !showLyrics) {
        Modifier.pointerInput(state.mediaId, showLyrics) {
            detectHorizontalDragGestures(
                onDragStart = { playerDrag = 0f },
                onHorizontalDrag = { change, amount -> change.consume(); playerDrag += amount },
                onDragEnd = {
                    when {
                        playerDrag < -100f -> onNext()
                        playerDrag > 100f -> onPrevious()
                    }
                    playerDrag = 0f
                },
                onDragCancel = { playerDrag = 0f },
            )
        }
    } else Modifier
    val context = LocalContext.current
    var coverTint by remember(state.coverUrl) { mutableStateOf(fallbackCoverTint(state.coverUrl)) }
    LaunchedEffect(state.coverUrl) {
        val url = state.coverUrl ?: return@LaunchedEffect
        coverTint = withContext(Dispatchers.IO) {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).size(96).build()
            val result = context.imageLoader.execute(request) as? SuccessResult
            val bitmap = (result?.drawable as? BitmapDrawable)?.bitmap
                ?: return@withContext fallbackCoverTint(url)
            val small = android.graphics.Bitmap.createScaledBitmap(bitmap, 32, 32, true)
            val pixels = IntArray(32 * 32)
            small.getPixels(pixels, 0, 32, 0, 0, 32, 32)
            if (small !== bitmap) small.recycle()
            ColorMath.dominantVibrant(pixels)?.let { Color(it) } ?: fallbackCoverTint(url)
        }
    }
    val screenBackground = AppBackground
    val coverAccentColor = remember(coverTint, screenBackground) {
        Color(ColorMath.playerAccent(coverTint.toArgb(), screenBackground.toArgb()))
    }
    val animatedTint by animateColorAsState(coverTint, tween(playerTransitionDuration(400)), label = "cover-tint")
    val animatedAccent by animateColorAsState(coverAccentColor, tween(playerTransitionDuration(400)), label = "cover-accent")
    val backdrop = if (settings.coverColorBackground || settings.playerBlurBackground) {
        Modifier.background(
            Brush.verticalGradient(
                listOf(
                    animatedTint.copy(alpha = if (settings.coverColorBackground) 0.28f else 0.13f),
                    AppBackground,
                    AppBackground,
                ),
            ),
        )
    } else Modifier.background(AppBackground)

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val like = {
        if (settings.haptics) haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
        onLike()
    }
    val modern = settings.lyricsModern
    var touches by remember { mutableStateOf(0) }
    var dockVisible by remember { mutableStateOf(true) }
    LaunchedEffect(touches, state.isPlaying, showLyrics, settings.lyricsAutoHide, modern) {
        dockVisible = true
        if (showLyrics && modern && settings.lyricsAutoHide && state.isPlaying) {
            kotlinx.coroutines.delay(3_500)
            dockVisible = false
        }
    }
    var lyricsMenu by remember { mutableStateOf(false) }
    var menuSleep by remember { mutableStateOf(false) }
    var menuSpeed by remember { mutableStateOf(false) }
    val sleepLabel = rememberSleepLabel(tools.sleepEndsAt)
    val glassStyle = LocalAppStyle.current == ru.muz.settings.AppStyle.GLASS
    val playerGlass = rememberGlassBackdrop()
    val glassDock = if (glassStyle && modern) playerGlass else null
    val coverBackground = settings.coverColorBackground || glassStyle
    androidx.compose.runtime.CompositionLocalProvider(
        LocalPlayerAccent provides if (settings.coverAccent) animatedAccent else null,
        LocalOnPlayerAccent provides if (settings.coverAccent) Color(ColorMath.contentColorFor(animatedAccent.toArgb())) else null,
    ) {
      Box(Modifier.fillMaxSize()) {
      Box(Modifier.fillMaxSize().glassSource(glassDock)) {
        if (coverBackground) {
            MovingCoverBackground(
                animatedTint,
                strength = if (showLyrics && modern) 1f else 0.62f,
                pulse = if (settings.pulseBackground && state.isPlaying) settings.pulseStrength / 100f else 0f,
            )
        }
        Column(
            Modifier.fillMaxSize().then(if (coverBackground) Modifier else backdrop).statusBarsPadding().navigationBarsPadding()
            .offset(x = playerOffset).graphicsLayer {
                alpha = if (reduceMotion) 1f else 1f - (abs(playerDrag) / 900f).coerceIn(0f, 0.14f)
            }.then(playerSwipeModifier)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                        if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) touches++
                    }
                }
            },
        ) {
            if (showLyrics && modern) {
                ModernLyricsHeader(
                    state = state,
                    liked = liked,
                    sleepLabel = sleepLabel,
                    onBack = onBack,
                    onCover = onToggleView,
                    onArtist = onArtist,
                    onLike = like,
                    onMore = { lyricsMenu = true },
                    onCancelSleep = { tools.onSleep(0) },
                )
            } else {
                PlayerHeader(showLyrics, state, onBack, onToggleView, onArtist, sleepLabel) { tools.onSleep(0) }
            }
            PlayerCrossfade(showLyrics, Modifier.weight(1f)) { lyricsVisible ->
                Column(Modifier.fillMaxSize()) {
                    if (lyricsVisible) {
                        val lyricsPage = when {
                            lyricsLoading -> 0
                            lyricsError.isNotBlank() -> 1
                            lyrics.lines.isEmpty() -> 2
                            else -> 3
                        }
                      Box(Modifier.weight(1f).fillMaxWidth()) {
                        PlayerCrossfade(lyricsPage, Modifier.fillMaxSize()) { page ->
                            Column(Modifier.fillMaxSize()) {
                                when {
                                    page == 0 -> LyricsLoading(lyricsStatus, Modifier.weight(1f))
                                    page == 1 -> LyricsError(lyricsError, onRetryLyrics, onToggleView, Modifier.weight(1f))
                                    page == 2 -> LyricsEmpty(
                                        onRetry = onRetryLyrics,
                                        onOpenPlayer = onToggleView,
                                        modifier = Modifier.weight(1f),
                                    )
                                    else -> LyricsScreen(
                                        lyrics = lyrics,
                                        positionMs = (position + settings.lyricsOffsetMs).coerceAtLeast(0),
                                        discontinuity = state.discontinuity,
                                        onSeek = { onSeek((it - settings.lyricsOffsetMs).coerceAtLeast(0)) },
                                        onLongPressLine = { quoteStart = it },
                                        settings = lyricsSettingsFor(settings),
                                        modifier = Modifier.weight(1f),
                                        background = Color.Transparent,
                                    )
                                }
                            }
                        }
                        if (modern && glassDock == null) {
                            DockOverlay(dockVisible, Modifier.align(Alignment.BottomCenter), onShow = { touches++ }) {
                                ModernLyricsDock(
                                    state = state,
                                    label = if (settings.privateSession) "Приватно" else "",
                                    onPlayPause = onPlayPause,
                                    onPrevious = onPrevious,
                                    onNext = onNext,
                                    onSeek = onSeek,
                                    onCover = onToggleView,
                                    onQueue = { showQueue = true },
                                    accentControls = settings.accentControls,
                                )
                            }
                        }
                      }
                        if (!modern) {
                            LyricsDock(
                                state = state,
                                liked = liked,
                                swipeEnabled = settings.swipeMiniPlayer,
                                onPlayPause = onPlayPause,
                                onPrevious = onPrevious,
                                onNext = onNext,
                                onSeek = onSeek,
                                onLike = like,
                                onQueue = { showQueue = true },
                                onDismiss = onBack,
                            )
                        }
                    } else {
                        StandardPlayer(
                            state, liked, onPlayPause, onPrevious, onNext, onSeek, like, onShuffle, onRepeat,
                            onQueue = { showQueue = true },
                            transferBusy = transferBusy,
                            transferMessage = transferMessage,
                            onCopyTransferLog = onCopyTransferLog,
                            onDownload = onDownload,
                            onDownloadCover = onDownloadCover,
                            onOpenCover = onOpenCover,
                            onShareLink = onShareLink,
                            onShareFile = onShareFile,
                            onArtist = onArtist,
                            onCopyArtist = onCopyArtist,
                            tools = tools,
                            onQuote = onQuote,
                            artworkPercent = settings.playerArtworkPercent,
                            layout = settings.playerLayout,
                            onDismiss = onBack,
                            gesturesEnabled = settings.fullPlayerSwipe,
                            doubleTapLike = settings.doubleTapLike,
                            doubleTapAction = settings.doubleTapAction,
                            privateLabel = if (settings.privateSession) "Приватно" else "",
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
      }
        androidx.compose.animation.AnimatedVisibility(
            glassDock != null && showLyrics,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = androidx.compose.animation.fadeIn(tween(playerTransitionDuration(320))),
            exit = androidx.compose.animation.fadeOut(tween(playerTransitionDuration(200))),
        ) {
            DockOverlay(
                dockVisible,
                Modifier.navigationBarsPadding().pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(androidx.compose.ui.input.pointer.PointerEventPass.Initial)
                            if (event.type == androidx.compose.ui.input.pointer.PointerEventType.Press) touches++
                        }
                    }
                },
                onShow = { touches++ },
                scrim = false,
            ) {
                ModernLyricsDock(
                    state = state,
                    label = if (settings.privateSession) "Приватно" else "",
                    onPlayPause = onPlayPause,
                    onPrevious = onPrevious,
                    onNext = onNext,
                    onSeek = onSeek,
                    onCover = onToggleView,
                    onQueue = { showQueue = true },
                    accentControls = settings.accentControls,
                    backdrop = playerGlass,
                )
            }
        }
      }
    }
    if (lyricsMenu) {
        ModalBottomSheet(onDismissRequest = { lyricsMenu = false }) {
            PlayerSheetAction(Icons.Filled.Image, "Обложка и управление") { lyricsMenu = false; onToggleView() }
            PlayerSheetAction(Icons.Filled.QueueMusic, "Очередь") { lyricsMenu = false; showQueue = true }
            PlayerSheetAction(Icons.Filled.FormatQuote, "Карточка с цитатой") { lyricsMenu = false; onQuote() }
            PlayerSheetAction(Icons.Filled.Bedtime, if (sleepLabel != null) "Таймер сна: $sleepLabel" else "Таймер сна") { lyricsMenu = false; menuSleep = true }
            PlayerSheetAction(Icons.Filled.Speed, "Скорость") { lyricsMenu = false; menuSpeed = true }
            PlayerSheetAction(Icons.Filled.InsertLink, "Поделиться ссылкой") { lyricsMenu = false; onShareLink() }
            PlayerSheetAction(PlayerAction.TRACK_WAVE.icon(), "Волна по треку") { lyricsMenu = false; tools.onTrackWave() }
            PlayerSheetAction(PlayerAction.DISLIKE.icon(), "Не рекомендовать") { lyricsMenu = false; tools.onDislike() }
            PlayerSheetAction(PlayerAction.PRIVATE.icon(), if (tools.privateSession) "Выключить приватный режим" else "Приватный режим") { lyricsMenu = false; tools.onPrivate(!tools.privateSession) }
            PlayerSheetAction(Icons.Filled.Person, "К исполнителю") { lyricsMenu = false; onArtist() }
            androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
        }
    }
    if (menuSleep) SleepTimerSheet(tools.sleepEndsAt, tools.onSleep) { menuSleep = false }
    if (menuSpeed) SpeedSheet(state.playbackSpeed, tools.skipSilence, tools.onSpeed, tools.onSkipSilence, tools.speedChangesPitch, tools.onSpeedChangesPitch) { menuSpeed = false }
    quoteStart?.let { start ->
        QuoteEditor(
            lines = lyrics.lines,
            initial = start,
            title = state.title,
            artist = state.artist,
            coverUrl = state.coverUrl,
            music = QuoteMusic(
                link = tools.trackLink,
                resolveAudio = tools.resolveAudio,
                durationMs = state.durationMs,
                positionMs = state.positionMs,
                offsetMs = settings.lyricsOffsetMs,
                synced = lyrics.synced,
            ),
            onDismiss = { quoteStart = null },
        )
    }
    if (showQueue) {
        ModalBottomSheet(onDismissRequest = { showQueue = false }) {
            Text(
                "Очередь · ${queue.size}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            val currentIndex = queue.indexOfFirst { it.id == state.mediaId }.coerceAtLeast(0)
            val queueState = androidx.compose.foundation.lazy.rememberLazyListState(
                initialFirstVisibleItemIndex = (currentIndex - 2).coerceAtLeast(0),
            )
            var queryText by remember { mutableStateOf("") }
            var queueSort by remember { mutableStateOf(QueueSort.ORDER) }
            val entries = remember(queue, queryText, queueSort) { queueView(queue, queryText, queueSort) }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                androidx.compose.material3.OutlinedTextField(
                    value = queryText,
                    onValueChange = { queryText = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    placeholder = { Text("Поиск по очереди") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    shape = RoundedCornerShape(28.dp),
                )
                androidx.compose.material3.TextButton(onClick = { queueSort = queueSort.next() }) { Text(queueSort.label) }
            }
            LazyColumn(Modifier.fillMaxWidth().height(480.dp), state = queueState) {
                itemsIndexed(entries, key = { _, entry -> "${entry.index}:${entry.track.id}" }) { _, entry ->
                    val index = entry.index
                    val track = entry.track
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            onQueueTrack(index)
                            showQueue = false
                        }.padding(horizontal = 20.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(track.coverUrl, Modifier.size(48.dp), 9.dp)
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(
                                track.title,
                                color = if (track.id == state.mediaId) Accent else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(track.artist, color = SoftText, fontSize = 13.sp, maxLines = 1)
                        }
                        if (track.id == state.mediaId) {
                            NowPlayingBars(state.isPlaying, Modifier.size(width = 18.dp, height = 16.dp))
                        } else {
                            Text(formatDuration(track.durationMs), color = SoftText, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerHeader(
    showLyrics: Boolean,
    state: PlayerState,
    onBack: () -> Unit,
    onToggleView: () -> Unit,
    onArtist: () -> Unit,
    sleepLabel: String?,
    onCancelSleep: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Свернуть") }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(if (showLyrics) "Текст песни" else "Сейчас играет", fontWeight = FontWeight.Bold)
            if (sleepLabel != null) {
                Text(
                    "$sleepLabel · отменить",
                    color = PlayerAccent,
                    fontSize = 12.sp,
                    maxLines = 1,
                    modifier = Modifier.clickable(onClick = onCancelSleep),
                )
            } else if (showLyrics) {
                Text(
                    listOf(state.artist, state.title).filter { it.isNotBlank() }.joinToString(" — "),
                    color = SoftText,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onArtist),
                )
            }
        }
        IconButton(onClick = onToggleView) {
            Icon(
                if (showLyrics) Icons.Filled.Headphones else Icons.Filled.Lyrics,
                if (showLyrics) "Показать плеер" else "Показать текст песни",
            )
        }
    }
}

private fun fallbackCoverTint(url: String?): Color {
    val hash = url.orEmpty().hashCode()
    val red = 0.34f + ((hash ushr 16) and 0x7f) / 255f
    val green = 0.34f + ((hash ushr 8) and 0x7f) / 255f
    val blue = 0.34f + (hash and 0x7f) / 255f
    return Color(red.coerceAtMost(0.82f), green.coerceAtMost(0.82f), blue.coerceAtMost(0.82f))
}

@Composable
private fun LyricsLoading(status: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MuzLoadingIndicator(Modifier.size(52.dp))
            Text(
                status.ifBlank { "Проверяем Яндекс Музыку…" },
                color = SoftText,
                modifier = Modifier.padding(top = 18.dp),
            )
        }
    }
}

@Composable
private fun LyricsError(
    message: String,
    onRetry: () -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(message, color = SoftText, textAlign = TextAlign.Center)
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) { Text("Искать ещё раз") }
                androidx.compose.material3.TextButton(onClick = onOpenPlayer) { Text("Открыть плеер") }
            }
        }
    }
}

@Composable
private fun LyricsEmpty(onRetry: () -> Unit, onOpenPlayer: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(Modifier.padding(horizontal = 30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            NoLyricsArt()
            Text("Текст не найден", fontWeight = FontWeight.Bold, fontSize = 20.sp, modifier = Modifier.padding(top = 12.dp))
            Text(
                "Ни в тегах, ни в сети текста для этого трека нет. Можно искать ещё раз или просто слушать.",
                color = SoftText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRetry) { Text("Искать ещё раз") }
                androidx.compose.material3.TextButton(onClick = onOpenPlayer) { Text("Открыть плеер") }
            }
        }
    }
}

@Composable
private fun LyricsDock(
    state: PlayerState,
    liked: Boolean,
    swipeEnabled: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onLike: () -> Unit,
    onQueue: () -> Unit,
    onDismiss: () -> Unit,
) {
    val reduceMotion = LocalReduceMotion.current
    var drag by remember(state.mediaId) { mutableFloatStateOf(0f) }
    var verticalDrag by remember(state.mediaId) { mutableFloatStateOf(0f) }
    val dockOffset by animateDpAsState(
        targetValue = (if (reduceMotion) 0f else drag / 5f).coerceIn(-30f, 30f).dp,
        animationSpec = tween(animationDuration(180)),
        label = "lyrics-dock-swipe",
    )
    val dockVerticalOffset by animateDpAsState(
        targetValue = (if (reduceMotion) 0f else verticalDrag / 5f).coerceIn(-26f, 26f).dp,
        animationSpec = tween(animationDuration(180)),
        label = "lyrics-dock-vertical-swipe",
    )
    val swipe = if (swipeEnabled) {
        Modifier.pointerInput(state.mediaId) {
            detectDragGestures(
                onDragStart = { drag = 0f; verticalDrag = 0f },
                onDrag = { change, amount ->
                    change.consume()
                    drag += amount.x
                    verticalDrag += amount.y
                },
                onDragEnd = {
                    if (abs(drag) >= abs(verticalDrag)) {
                        when {
                            drag < -72f -> onNext()
                            drag > 72f -> onPrevious()
                        }
                    } else {
                        when {
                            verticalDrag < -90f -> onQueue()
                            verticalDrag > 90f -> onDismiss()
                        }
                    }
                    drag = 0f
                    verticalDrag = 0f
                },
                onDragCancel = { drag = 0f; verticalDrag = 0f },
            )
        }
    } else Modifier
    Column(
        Modifier.fillMaxWidth().offset(x = dockOffset, y = dockVerticalOffset).graphicsLayer {
            alpha = if (reduceMotion) 1f else 1f - (maxOf(abs(drag), abs(verticalDrag)) / 700f).coerceIn(0f, 0.16f)
            scaleX = if (reduceMotion) 1f else 1f - (maxOf(abs(drag), abs(verticalDrag)) / 7000f).coerceIn(0f, 0.035f)
            scaleY = scaleX
        }.then(swipe)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PlayerCrossfade(state.coverUrl, Modifier.size(50.dp)) { url -> Artwork(url, Modifier.fillMaxSize(), 9.dp) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(state.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(state.artist, color = SoftText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onLike) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Нравится",
                    tint = if (liked) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Accent).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                PlaybackIcon(state, Modifier.size(26.dp), MaterialTheme.colorScheme.onPrimary)
            }
        }
        PlaybackTimeline(state, onSeek)
    }
}

@Composable
private fun StandardPlayer(
    state: PlayerState,
    liked: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit,
    transferBusy: Boolean,
    transferMessage: String,
    onCopyTransferLog: () -> Unit,
    onDownload: () -> Unit,
    onDownloadCover: () -> Unit,
    onOpenCover: () -> Unit,
    onShareLink: () -> Unit,
    onShareFile: () -> Unit,
    onArtist: () -> Unit,
    onCopyArtist: () -> Unit,
    tools: PlayerTools,
    onQuote: () -> Unit,
    artworkPercent: Int,
    layout: PlayerLayout,
    onDismiss: () -> Unit,
    gesturesEnabled: Boolean,
    doubleTapLike: Boolean = true,
    doubleTapAction: ru.muz.settings.DoubleTapAction = ru.muz.settings.DoubleTapAction.LIKE,
    privateLabel: String = "",
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = 20.dp)) {
        if (privateLabel.isNotBlank()) {
            Text(
                "Приватный режим: прослушивания не уходят в Яндекс",
                color = SoftText,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            )
        }
        if (layout == PlayerLayout.COVER_FIRST) {
            PlayerArtwork(
                settingsSwipe = gesturesEnabled,
                state = state, artworkPercent = artworkPercent,
                onSwipeUp = onQueue, onSwipeDown = onDismiss, modifier = Modifier.weight(1f),
                onDoubleTap = if (doubleTapLike) ({
                    when (doubleTapAction) {
                        ru.muz.settings.DoubleTapAction.LIKE -> if (!liked) onLike()
                        ru.muz.settings.DoubleTapAction.PLAY_PAUSE -> onPlayPause()
                        ru.muz.settings.DoubleTapAction.NEXT -> onNext()
                    }
                }) else null,
                showHeart = doubleTapAction == ru.muz.settings.DoubleTapAction.LIKE,
            )
        }
            PlayerDetails(
            state, liked, onPlayPause, onPrevious, onNext, onSeek, onLike, onShuffle, onRepeat,
            onQueue, transferBusy, transferMessage, onCopyTransferLog, onDownload, onDownloadCover, onOpenCover, onShareLink, onShareFile, onArtist, onCopyArtist,
            tools, onQuote,
        )
        if (layout == PlayerLayout.CONTROLS_FIRST) {
            PlayerArtwork(
                settingsSwipe = gesturesEnabled,
                state = state, artworkPercent = artworkPercent,
                onSwipeUp = onQueue, onSwipeDown = onDismiss, modifier = Modifier.weight(1f),
                onDoubleTap = if (doubleTapLike) ({
                    when (doubleTapAction) {
                        ru.muz.settings.DoubleTapAction.LIKE -> if (!liked) onLike()
                        ru.muz.settings.DoubleTapAction.PLAY_PAUSE -> onPlayPause()
                        ru.muz.settings.DoubleTapAction.NEXT -> onNext()
                    }
                }) else null,
                showHeart = doubleTapAction == ru.muz.settings.DoubleTapAction.LIKE,
            )
        }
    }
}

@Composable
private fun PlayerArtwork(
    state: PlayerState,
    artworkPercent: Int,
    settingsSwipe: Boolean,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    modifier: Modifier = Modifier,
    onDoubleTap: (() -> Unit)? = null,
    showHeart: Boolean = true,
) {
    val reduceMotion = LocalReduceMotion.current
    var verticalDrag by remember(state.mediaId) { mutableFloatStateOf(0f) }
    var hearts by remember { mutableStateOf(0) }
    val heart = remember { androidx.compose.animation.core.Animatable(0f) }
    val heartDuration = playerTransitionDuration(700)
    LaunchedEffect(hearts) {
        if (hearts == 0) return@LaunchedEffect
        heart.snapTo(0f)
        heart.animateTo(1f, tween(heartDuration))
    }
    val doubleTap = if (onDoubleTap != null) {
        Modifier.pointerInput(state.mediaId) {
            detectTapGestures(onDoubleTap = {
                if (showHeart) hearts++
                onDoubleTap()
            })
        }
    } else Modifier
    val artworkOffset by animateDpAsState(
        targetValue = (if (reduceMotion) 0f else verticalDrag / 5f).coerceIn(-34f, 34f).dp,
        animationSpec = tween(animationDuration(180)),
        label = "artwork-vertical-swipe",
    )
    val gesture = if (settingsSwipe) {
        Modifier.pointerInput(state.mediaId) {
            detectVerticalDragGestures(
                onDragStart = { verticalDrag = 0f },
                onVerticalDrag = { change, amount -> change.consume(); verticalDrag += amount },
                onDragEnd = {
                    when {
                        verticalDrag < -110f -> onSwipeUp()
                        verticalDrag > 110f -> onSwipeDown()
                    }
                    verticalDrag = 0f
                },
                onDragCancel = { verticalDrag = 0f },
            )
        }
    } else Modifier
    Box(
        modifier.fillMaxWidth().offset(y = artworkOffset).graphicsLayer {
            alpha = if (reduceMotion) 1f else 1f - (abs(verticalDrag) / 850f).coerceIn(0f, 0.16f)
            scaleX = if (reduceMotion) 1f else 1f - (abs(verticalDrag) / 6000f).coerceIn(0f, 0.035f)
            scaleY = scaleX
        }.then(gesture),
        contentAlignment = Alignment.Center,
    ) {
        PlayerCrossfade(state.coverUrl,
            Modifier.fillMaxWidth((artworkPercent / 100f).coerceIn(0.7f, 1f)).aspectRatio(1f).then(doubleTap),
        ) { url -> Artwork(url, Modifier.fillMaxSize(), 30.dp) }
        if (heart.value in 0.001f..0.999f) {
            val k = heart.value
            Icon(
                Icons.Filled.Favorite,
                null,
                tint = Color.White.copy(alpha = (1f - k) * 0.95f),
                modifier = Modifier.size(110.dp).graphicsLayer {
                    val sc = 0.6f + 0.6f * kotlin.math.sin(k * kotlin.math.PI.toFloat() * 0.6f)
                    scaleX = sc
                    scaleY = sc
                },
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = state.isBuffering,
            enter = androidx.compose.animation.fadeIn(tween(playerTransitionDuration(180))),
            exit = androidx.compose.animation.fadeOut(tween(playerTransitionDuration(180))),
        ) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)), contentAlignment = Alignment.Center) {
                MorphLoading(Modifier.size(40.dp), PlayerAccent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerDetails(
    state: PlayerState,
    liked: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onLike: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onQueue: () -> Unit,
    transferBusy: Boolean,
    transferMessage: String,
    onCopyTransferLog: () -> Unit,
    onDownload: () -> Unit,
    onDownloadCover: () -> Unit,
    onOpenCover: () -> Unit,
    onShareLink: () -> Unit,
    onShareFile: () -> Unit,
    onArtist: () -> Unit,
    onCopyArtist: () -> Unit,
    tools: PlayerTools,
    onQuote: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                PlayerCrossfade(state.title) { title ->
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.artist,
                        color = SoftText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable(onClick = onArtist),
                    )
                    IconButton(onClick = onCopyArtist, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.ContentCopy, "Скопировать имя артиста", tint = SoftText, modifier = Modifier.size(16.dp))
                    }
                }
            }
            IconButton(onClick = onLike) {
                Icon(
                    if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    "Нравится",
                    tint = if (liked) PlayerAccent else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        PlaybackErrorBanner(state.error)
        PlaybackTimeline(state, onSeek)
        if (transferBusy) MuzWavyProgress(Modifier.fillMaxWidth().padding(top = 6.dp))
        if (transferMessage.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(transferMessage, color = SoftText, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                if (!transferBusy && transferMessage.contains("Не удалось", ignoreCase = true)) {
                    IconButton(onClick = onCopyTransferLog, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Filled.ContentCopy, "Скопировать диагностический лог", tint = SoftText, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
        var moreActions by remember { mutableStateOf(false) }
        var showSleep by remember { mutableStateOf(false) }
        var showSpeed by remember { mutableStateOf(false) }
        fun enabled(action: PlayerAction) = when (action) {
            PlayerAction.DOWNLOAD, PlayerAction.SHARE_FILE, PlayerAction.SAVE_COVER -> !transferBusy
            else -> true
        }
        fun run(action: PlayerAction) {
            when (action) {
                PlayerAction.QUEUE -> onQueue()
                PlayerAction.DOWNLOAD -> onDownload()
                PlayerAction.LINK -> onShareLink()
                PlayerAction.SHARE_FILE -> onShareFile()
                PlayerAction.SAVE_COVER -> onDownloadCover()
                PlayerAction.OPEN_COVER -> onOpenCover()
                PlayerAction.SLEEP -> showSleep = true
                PlayerAction.SPEED -> showSpeed = true
                PlayerAction.QUOTE -> onQuote()
                PlayerAction.COPY_ARTIST -> onCopyArtist()
                PlayerAction.TRACK_WAVE -> tools.onTrackWave()
                PlayerAction.DISLIKE -> tools.onDislike()
                PlayerAction.PRIVATE -> tools.onPrivate(!tools.privateSession)
            }
        }
        val inRow = tools.actions.take(5)
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            inRow.forEach { action ->
                val active = (action == PlayerAction.SLEEP && tools.sleepEndsAt != 0L) ||
                    (action == PlayerAction.PRIVATE && tools.privateSession) ||
                    (action == PlayerAction.SPEED && kotlin.math.abs(state.playbackSpeed - 1f) > 0.01f)
                IconButton(onClick = { run(action) }, enabled = enabled(action)) {
                    Icon(action.icon(), action.title(), tint = if (active) PlayerAccent else LocalContentColor.current)
                }
            }
            IconButton(onClick = { moreActions = true }) { Icon(Icons.Filled.MoreHoriz, "Ещё") }
        }
        if (moreActions) {
            ModalBottomSheet(onDismissRequest = { moreActions = false }) {
                PlayerAction.entries.filter { it !in inRow }.forEach { action ->
                    PlayerSheetAction(action.icon(), action.title(), enabled(action)) { moreActions = false; run(action) }
                }
                androidx.compose.foundation.layout.Spacer(Modifier.height(24.dp))
            }
        }
        if (showSleep) SleepTimerSheet(tools.sleepEndsAt, tools.onSleep) { showSleep = false }
        if (showSpeed) {
            SpeedSheet(state.playbackSpeed, tools.skipSilence, tools.onSpeed, tools.onSkipSilence, tools.speedChangesPitch, tools.onSpeedChangesPitch) { showSpeed = false }
        }
        Row(
            Modifier.fillMaxWidth().height(84.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onShuffle) {
                Icon(Icons.Filled.Shuffle, "Перемешать", tint = if (state.shuffle) PlayerAccent else SoftText)
            }
            IconButton(onClick = onPrevious) { Icon(Icons.Filled.SkipPrevious, "Предыдущий", modifier = Modifier.size(34.dp)) }
            Box(
                Modifier.size(64.dp).clip(playShape(LocalPlayShape.current)).background(PlayerAccent).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                PlaybackIcon(state, Modifier.size(38.dp), OnPlayerAccent)
            }
            IconButton(onClick = onNext) { Icon(Icons.Filled.SkipNext, "Следующий", modifier = Modifier.size(34.dp)) }
            IconButton(onClick = onRepeat) {
                Icon(
                    if (state.repeat == PlayerRepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                    "Повтор",
                    tint = if (state.repeat != PlayerRepeatMode.OFF) PlayerAccent else SoftText,
                )
            }
        }
    }
}

@Composable
private fun PlayerSheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    androidx.compose.material3.ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.fillMaxWidth().then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f },
    )
}
