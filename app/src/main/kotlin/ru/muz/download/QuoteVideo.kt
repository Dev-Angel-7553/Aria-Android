package ru.muz.download

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

object QuoteVideo {
    @OptIn(UnstableApi::class)
    suspend fun render(
        context: Context,
        image: File,
        audioUrl: String,
        clip: QuoteClip.Range,
        output: File,
        onProgress: (Int) -> Unit,
    ): File = withContext(Dispatchers.Main) {
        output.parentFile?.mkdirs()
        output.delete()
        val picture = EditedMediaItem.Builder(
            MediaItem.Builder()
                .setUri(Uri.fromFile(image))
                .setMimeType(MimeTypes.IMAGE_PNG)
                .setImageDurationMs(clip.lengthMs)
                .build(),
        ).setFrameRate(30).build()
        val sound = EditedMediaItem.Builder(
            MediaItem.Builder()
                .setUri(audioUrl)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build(),
                )
                .build(),
        ).setRemoveVideo(true).build()
        val composition = Composition.Builder(
            EditedMediaItemSequence(listOf(picture)),
            EditedMediaItemSequence(listOf(sound)),
        ).build()
        val handler = Handler(Looper.getMainLooper())
        suspendCancellableCoroutine { continuation ->
            val holder = ProgressHolder()
            lateinit var transformer: Transformer
            val poll = object : Runnable {
                override fun run() {
                    if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                    handler.postDelayed(this, 200)
                }
            }
            transformer = Transformer.Builder(context)
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .setAudioMimeType(MimeTypes.AUDIO_AAC)
                .setLooper(Looper.getMainLooper())
                .addListener(object : Transformer.Listener {
                    override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                        handler.removeCallbacks(poll)
                        onProgress(100)
                        if (continuation.isActive) continuation.resume(output)
                    }

                    override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                        handler.removeCallbacks(poll)
                        output.delete()
                        if (continuation.isActive) continuation.resumeWithException(exportException)
                    }
                })
                .build()
            continuation.invokeOnCancellation {
                handler.post {
                    handler.removeCallbacks(poll)
                    transformer.cancel()
                    output.delete()
                }
            }
            transformer.start(composition, output.absolutePath)
            handler.post(poll)
        }
    }
}
