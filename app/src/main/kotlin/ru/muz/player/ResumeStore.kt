package ru.muz.player

import android.content.Context
import android.util.AtomicFile
import androidx.media3.common.MediaItem
import java.io.File

internal class ResumeStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("playback_resume", Context.MODE_PRIVATE)
    private val file = AtomicFile(File(context.filesDir, "playback_queue.json"))

    @Synchronized
    fun save(items: List<MediaItem>, index: Int, positionMs: Long) {
        if (items.isNotEmpty() && index !in items.indices) return
        val tracks = items.map { item ->
            val metadata = item.mediaMetadata
            val extras = metadata.extras
            PlaybackController.YandexTrack(MediaExtras.trackId(item), metadata.title?.toString().orEmpty(),
                metadata.artist?.toString().orEmpty(), metadata.artworkUri?.toString(), extras?.getString(MediaExtras.ALBUM_ID).orEmpty(),
                extras?.getLong(MediaExtras.DURATION) ?: 0L, extras?.getBoolean(MediaExtras.LIKED) ?: false,
                extras?.getFloat(MediaExtras.LOUDNESS, Float.NaN) ?: Float.NaN, ArtistMetadata.decode(extras?.getString(MediaExtras.ARTISTS).orEmpty()))
        }
        val value = ResumeCodec.encode(ResumeSnapshot(tracks, items.map { MediaExtras.source(it.mediaMetadata.extras) }, index, positionMs))
        val output = file.startWrite()
        try {
            output.write(value.encodeToByteArray())
            file.finishWrite(output)
            prefs.edit().remove("queue").apply()
        } catch (error: Throwable) {
            file.failWrite(output)
            throw error
        }
    }

    fun savePosition(positionMs: Long, trackId: String) {
        prefs.edit().putLong("position", positionMs.coerceAtLeast(0)).putString("position_track", trackId).apply()
    }

    @Synchronized
    fun read(): ResumeSnapshot? = runCatching {
        val raw = if (file.baseFile.exists()) file.readFully().decodeToString() else prefs.getString("queue", null) ?: return null
        val snapshot = ResumeCodec.decode(raw) ?: return null
        val positionTrack = prefs.getString("position_track", null)
        val position = if (positionTrack == null || positionTrack == snapshot.tracks[snapshot.index].id) prefs.getLong("position", snapshot.positionMs) else snapshot.positionMs
        snapshot.copy(positionMs = position.coerceAtLeast(0))
    }.getOrNull()
}
