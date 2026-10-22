package ru.muz.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import ru.muz.MainActivity
import ru.muz.R
import ru.muz.auth.TokenStore
import ru.muz.yandex.YandexClient

class MuzPlayerService : MediaLibraryService() {
    private var player: ExoPlayer? = null
    private var session: MediaLibrarySession? = null
    private var tracker: ListenTracker? = null
    private lateinit var tree: LibraryTree
    private lateinit var resume: ResumeStore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val likedOverrides = HashMap<String, Boolean>()

    private val toggleLike = SessionCommand(MediaExtras.CMD_TOGGLE_LIKE, Bundle.EMPTY)
    private val setLiked = SessionCommand(MediaExtras.CMD_SET_LIKED, Bundle.EMPTY)
    private val sleepCommand = SessionCommand(MediaExtras.CMD_SLEEP, Bundle.EMPTY)
    private val silenceCommand = SessionCommand(MediaExtras.CMD_SKIP_SILENCE, Bundle.EMPTY)
    private var sleepJob: Job? = null
    private var sleepEndsAt = 0L

    override fun onCreate() {
        super.onCreate()
        tree = LibraryTree(this)
        resume = ResumeStore(this)
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(AudioCache.dataSourceFactory(this)))
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exo
        val prefs = ru.muz.settings.AppPreferences(this).load()
        exo.skipSilenceEnabled = prefs.skipSilence
        exo.setPlaybackSpeed(prefs.playbackSpeedPercent / 100f)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaLibrarySession.Builder(this, exo, Callback())
            .setSessionActivity(pendingIntent)
            .build()
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this).build().apply {
                setSmallIcon(R.drawable.ic_stat_aria)
            },
        )
        exo.addListener(object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateLikeButton()
                saveQueue()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!isPlaying) resume.savePosition(exo.currentPosition)
            }

            override fun onEvents(player: Player, events: Player.Events) {
                if (events.containsAny(
                        Player.EVENT_MEDIA_METADATA_CHANGED,
                        Player.EVENT_IS_PLAYING_CHANGED,
                        Player.EVENT_MEDIA_ITEM_TRANSITION,
                    )
                ) {
                    val metadata = player.mediaMetadata
                    ru.muz.widget.WidgetUpdater.push(
                        this@MuzPlayerService,
                        metadata.title?.toString().orEmpty(),
                        metadata.artist?.toString().orEmpty(),
                        metadata.artworkUri?.toString(),
                        player.isPlaying,
                    )
                }
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && sleepEndsAt == MediaExtras.SLEEP_END_OF_TRACK.toLong()) {
                    exo.pauseAtEndOfMediaItems = false
                    sleepEndsAt = 0L
                    broadcastSleep()
                }
            }
        })
        tracker = ListenTracker(this, exo).also { it.start() }
        updateLikeButton()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        sleepJob?.cancel()
        player?.let { resume.savePosition(it.currentPosition) }
        tracker?.release()
        tracker = null
        session?.release()
        player?.release()
        session = null
        player = null
        scope.cancel()
        super.onDestroy()
    }

    private fun currentLiked(): Boolean {
        val item = player?.currentMediaItem ?: return false
        val id = MediaExtras.trackId(item)
        return likedOverrides[id] ?: item.mediaMetadata.extras?.getBoolean(MediaExtras.LIKED, false) ?: false
    }

    private fun updateLikeButton() {
        val current = session ?: return
        if (player?.currentMediaItem == null) {
            current.setCustomLayout(ImmutableList.of())
            return
        }
        val liked = currentLiked()
        val button = CommandButton.Builder()
            .setDisplayName(if (liked) "Убрать из «Мне нравится»" else "Нравится")
            .setIconResId(if (liked) R.drawable.ic_heart else R.drawable.ic_heart_outline)
            .setSessionCommand(toggleLike)
            .build()
        current.setCustomLayout(ImmutableList.of(button))
    }

    private fun saveQueue() {
        val exo = player ?: return
        val items = (0 until exo.mediaItemCount).map(exo::getMediaItemAt)
        resume.save(items, exo.currentMediaItemIndex, exo.currentPosition)
    }

    private fun setSleep(minutes: Int) {
        val exo = player ?: return
        if (minutes == MediaExtras.SLEEP_QUERY) {
            broadcastSleep()
            return
        }
        sleepJob?.cancel()
        sleepJob = null
        exo.volume = 1f
        exo.pauseAtEndOfMediaItems = false
        sleepEndsAt = when {
            minutes == MediaExtras.SLEEP_END_OF_TRACK -> {
                exo.pauseAtEndOfMediaItems = true
                MediaExtras.SLEEP_END_OF_TRACK.toLong()
            }
            minutes > 0 -> {
                val durationMs = minutes * 60_000L
                val endsAt = android.os.SystemClock.elapsedRealtime() + durationMs
                sleepJob = scope.launch {
                    val fadeMs = 12_000L
                    delay((durationMs - fadeMs).coerceAtLeast(0))
                    val steps = 24
                    for (step in steps downTo 1) {
                        exo.volume = step / steps.toFloat()
                        delay(fadeMs / steps)
                    }
                    exo.pause()
                    exo.volume = 1f
                    sleepEndsAt = 0L
                    sleepJob = null
                    broadcastSleep()
                }
                endsAt
            }
            else -> 0L
        }
        broadcastSleep()
    }

    private fun broadcastSleep() {
        val args = Bundle().apply { putLong(MediaExtras.ARG_ENDS_AT, sleepEndsAt) }
        session?.broadcastCustomCommand(SessionCommand(MediaExtras.CMD_SLEEP_STATE, Bundle.EMPTY), args)
    }

    private fun broadcastLike(trackId: String, liked: Boolean) {
        val args = Bundle().apply {
            putString(MediaExtras.ARG_TRACK, trackId)
            putBoolean(MediaExtras.ARG_LIKED, liked)
        }
        session?.broadcastCustomCommand(SessionCommand(MediaExtras.CMD_LIKE_CHANGED, Bundle.EMPTY), args)
    }

    private fun toggleCurrentLike() {
        val item = player?.currentMediaItem ?: return
        val id = MediaExtras.trackId(item)
        val newValue = !currentLiked()
        likedOverrides[id] = newValue
        updateLikeButton()
        broadcastLike(id, newValue)
        val token = TokenStore(this).token ?: return
        scope.launch(Dispatchers.IO) {
            val ok = YandexClient(token).setLiked(id, newValue).isSuccess
            if (!ok) {
                launch(Dispatchers.Main) {
                    likedOverrides[id] = !newValue
                    updateLikeButton()
                    broadcastLike(id, !newValue)
                }
            }
        }
    }

    private fun <T> future(block: suspend () -> T): ListenableFuture<T> {
        val result = SettableFuture.create<T>()
        scope.launch {
            runCatching { block() }
                .onSuccess { result.set(it) }
                .onFailure { result.setException(it) }
        }
        return result
    }

    private suspend fun resolve(items: List<MediaItem>, startIndex: Int, startPositionMs: Long): MediaSession.MediaItemsWithStartPosition {
        val single = items.singleOrNull()
        if (single != null && single.localConfiguration == null) {
            val mediaId = single.mediaId
            val parent = when {
                mediaId.contains(LibraryTree.SEPARATOR) -> mediaId.substringBeforeLast(LibraryTree.SEPARATOR)
                mediaId == LibraryTree.WAVE -> LibraryTree.WAVE
                else -> null
            }
            if (parent != null) {
                val (expanded, _) = tree.playable(parent)
                val target = mediaId.substringAfterLast(LibraryTree.SEPARATOR, "")
                val index = expanded.indexOfFirst { it.mediaId == target }.coerceAtLeast(0)
                if (expanded.isNotEmpty()) return MediaSession.MediaItemsWithStartPosition(expanded, index, C.TIME_UNSET)
            }
        }
        val playable = items.map { item ->
            if (item.localConfiguration != null) item
            else item.buildUpon().setUri(MediaExtras.uri(MediaExtras.trackId(item))).build()
        }
        return MediaSession.MediaItemsWithStartPosition(playable, startIndex, startPositionMs)
    }

    private inner class Callback : MediaLibrarySession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): MediaSession.ConnectionResult {
            val commands = MediaSession.ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()
                .add(toggleLike)
                .add(setLiked)
                .add(sleepCommand)
                .add(silenceCommand)
                .build()
            return MediaSession.ConnectionResult.accept(commands, MediaSession.ConnectionResult.DEFAULT_PLAYER_COMMANDS)
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            when (customCommand.customAction) {
                MediaExtras.CMD_TOGGLE_LIKE -> toggleCurrentLike()
                MediaExtras.CMD_SLEEP -> setSleep(args.getInt(MediaExtras.ARG_MINUTES, 0))
                MediaExtras.CMD_SKIP_SILENCE -> player?.skipSilenceEnabled = args.getBoolean(MediaExtras.ARG_ENABLED, false)
                MediaExtras.CMD_SET_LIKED -> {
                    val id = args.getString(MediaExtras.ARG_TRACK).orEmpty()
                    if (id.isNotBlank()) {
                        likedOverrides[id] = args.getBoolean(MediaExtras.ARG_LIKED, false)
                        updateLikeButton()
                    }
                }
            }
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
        }

        override fun onGetLibraryRoot(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<MediaItem>> =
            Futures.immediateFuture(LibraryResult.ofItem(tree.root(), params))

        override fun onGetChildren(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            parentId: String,
            page: Int,
            pageSize: Int,
            params: LibraryParams?,
        ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> = future {
            val all = tree.children(parentId)
            val from = (page * pageSize).coerceAtMost(all.size)
            val to = (from + pageSize).coerceAtMost(all.size)
            LibraryResult.ofItemList(ImmutableList.copyOf(all.subList(from, to)), params)
        }

        override fun onGetItem(
            session: MediaLibrarySession,
            browser: MediaSession.ControllerInfo,
            mediaId: String,
        ): ListenableFuture<LibraryResult<MediaItem>> = future {
            tree.item(mediaId)?.let { LibraryResult.ofItem(it, null) }
                ?: LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        }

        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> = future {
            resolve(mediaItems, 0, C.TIME_UNSET).mediaItems.toMutableList()
        }

        override fun onSetMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
            startIndex: Int,
            startPositionMs: Long,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = future {
            resolve(mediaItems, startIndex, startPositionMs)
        }

        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> = future {
            val saved = resume.read() ?: error("Нечего продолжать")
            val items = saved.tracks.mapIndexed { index, track -> MediaExtras.item(track, saved.sources[index]) }
            MediaSession.MediaItemsWithStartPosition(items, saved.index, saved.positionMs)
        }
    }
}
