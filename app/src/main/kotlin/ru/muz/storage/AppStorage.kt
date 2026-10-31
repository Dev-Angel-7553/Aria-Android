package ru.muz.storage

import android.content.Context
import coil.imageLoader
import java.io.File
import ru.muz.player.AudioCache

internal data class StorageSnapshot(
    val listeningBytes: Long = 0,
    val artworkBytes: Long = 0,
    val temporaryBytes: Long = 0,
    val lyricsBytes: Long = 0,
    val offline: List<AudioCache.OfflineEntry> = emptyList(),
)

internal object AppStorage {
    private val temporaryFolders = listOf("shared_music", "updates")

    fun snapshot(context: Context): StorageSnapshot = StorageSnapshot(
        listeningBytes = AudioCache.usedBytes(context),
        artworkBytes = context.imageLoader.diskCache?.size ?: 0L,
        temporaryBytes = temporaryFolders.sumOf { size(File(context.cacheDir, it)) },
        lyricsBytes = size(File(context.cacheDir, "lyrics")),
        offline = AudioCache.offlineEntries(context),
    )

    fun clear(context: Context, category: String) {
        when (category) {
            "listening" -> AudioCache.clear(context)
            "artwork" -> { context.imageLoader.memoryCache?.clear(); context.imageLoader.diskCache?.clear() }
            "temporary" -> temporaryFolders.forEach { File(context.cacheDir, it).deleteRecursively() }
            "lyrics" -> File(context.cacheDir, "lyrics").deleteRecursively()
        }
    }

    private fun size(file: File): Long = if (!file.exists()) 0L else if (file.isFile) file.length()
        else file.listFiles().orEmpty().sumOf(::size)
}
