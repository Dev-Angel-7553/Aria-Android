package ru.muz.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import ru.muz.auth.TokenStore
import ru.muz.text.playlistsLabel
import ru.muz.text.tracksLabel
import ru.muz.yandex.LibrarySnapshot
import ru.muz.yandex.LibrarySnapshotStore
import ru.muz.yandex.PlaySource
import ru.muz.yandex.YandexClient

internal class LibraryTree(context: Context) {
    private val app = context.applicationContext
    private val snapshots = LibrarySnapshotStore(app)
    private var client: YandexClient? = null

    companion object {
        const val ROOT = "root"
        const val WAVE = "wave"
        const val LIKED = "liked"
        const val PLAYLISTS = "playlists"
        const val CHART = "chart"
        const val PLAYLIST_PREFIX = "pl|"
        const val SEPARATOR = '#'
    }

    fun root(): MediaItem = folder(ROOT, "Aria", null)

    suspend fun children(parentId: String): List<MediaItem> {
        val library = snapshots.readLatest()
        return when {
            parentId == ROOT -> listOf(
                playableFolder(WAVE, "Моя волна", "Бесконечный поток по вашему вкусу"),
                folder(LIKED, "Мне нравится", library?.liked?.size?.let { tracksLabel(it) }),
                folder(PLAYLISTS, "Плейлисты", library?.playlists?.size?.let { playlistsLabel(it) }),
                folder(CHART, "Чарт", "Популярное сейчас"),
            )
            parentId == PLAYLISTS -> library?.playlists.orEmpty().map { playlist ->
                folder(
                    PLAYLIST_PREFIX + playlist.id,
                    playlist.title,
                    tracksLabel(playlist.trackCount),
                    playlist.coverUrl,
                )
            }
            else -> tracksOf(parentId, library).map { (track, _) ->
                MediaExtras.item(MediaExtras.track(track), PlaySource.other(), mediaId = "$parentId$SEPARATOR${track.id}")
            }
        }
    }

    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT -> root()
        mediaId.contains(SEPARATOR) -> {
            val parent = mediaId.substringBeforeLast(SEPARATOR)
            children(parent).firstOrNull { it.mediaId == mediaId }
        }
        else -> children(ROOT).firstOrNull { it.mediaId == mediaId }
            ?: children(PLAYLISTS).firstOrNull { it.mediaId == mediaId }
    }

    suspend fun playable(parentId: String): Pair<List<MediaItem>, PlaySource> {
        val library = snapshots.readLatest()
        val source = when {
            parentId == WAVE -> PlaySource.wave()
            parentId == LIKED -> PlaySource.liked()
            parentId == CHART -> PlaySource.chart()
            parentId.startsWith(PLAYLIST_PREFIX) -> {
                val playlist = library?.playlists?.firstOrNull { it.id == parentId.removePrefix(PLAYLIST_PREFIX) }
                PlaySource.playlist(playlist?.ownerUid.orEmpty(), playlist?.kind.orEmpty())
            }
            else -> PlaySource.other()
        }
        val tracks = tracksOf(parentId, library)
        return tracks.map { (track, liked) -> MediaExtras.item(MediaExtras.track(track, liked), source) } to source
    }

    private suspend fun tracksOf(parentId: String, library: LibrarySnapshot?): List<Pair<YandexClient.Track, Boolean>> {
        val likedIds = library?.liked.orEmpty().mapTo(HashSet()) { it.id }
        val tracks = when {
            parentId == LIKED -> library?.liked.orEmpty()
            parentId == CHART -> library?.chart.orEmpty().ifEmpty { api()?.chart()?.getOrNull().orEmpty() }
            parentId == WAVE -> api()?.myWave()?.getOrNull().orEmpty().ifEmpty { library?.wave.orEmpty() }
            parentId.startsWith(PLAYLIST_PREFIX) -> {
                val playlist = library?.playlists?.firstOrNull { it.id == parentId.removePrefix(PLAYLIST_PREFIX) }
                    ?: return emptyList()
                playlist.tracks.ifEmpty { api()?.playlistTracks(playlist)?.getOrNull().orEmpty() }
            }
            else -> emptyList()
        }
        return tracks.map { it to (it.id in likedIds) }
    }

    private fun api(): YandexClient? {
        client?.let { return it }
        val token = TokenStore(app).token ?: return null
        return YandexClient(token).also { client = it }
    }

    private fun folder(id: String, title: String, subtitle: String?, cover: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setArtworkUri(cover?.let(Uri::parse))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(
                        if (id.startsWith(PLAYLIST_PREFIX)) MediaMetadata.MEDIA_TYPE_PLAYLIST
                        else MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                    )
                    .build(),
            )
            .build()

    private fun playableFolder(id: String, title: String, subtitle: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                    .build(),
            )
            .build()
}
