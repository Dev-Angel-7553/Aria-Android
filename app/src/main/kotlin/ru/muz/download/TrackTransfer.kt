package ru.muz.download

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import ru.muz.yandex.YandexClient

object TrackTransfer {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(2, TimeUnit.MINUTES)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun download(
        context: Context,
        client: YandexClient,
        track: YandexClient.Track,
        forSharing: Boolean,
    ): Result<Uri> = runCatching {
        withContext(Dispatchers.IO) {
            val stream = resolveStream(client, track.id)
            val fileName = safeName("${track.artist} — ${track.title}.mp3")
            if (forSharing) {
                val directory = File(context.cacheDir, "shared_music").apply { mkdirs() }
                val file = File(directory, fileName)
                FileOutputStream(file).use { output -> copy(client, track.id, stream, output) }
                FileProvider.getUriForFile(context, "${context.packageName}.files", file)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(context, client, track.id, stream, fileName)
            } else {
                val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Aria")
                    .apply { mkdirs() }
                val file = File(directory, fileName)
                FileOutputStream(file).use { output -> copy(client, track.id, stream, output) }
                Uri.fromFile(file)
            }
        }
    }

    fun shareLink(context: Context, track: YandexClient.Track) {
        val id = track.id.substringBefore(':')
        val link = if (track.albumId.isBlank()) "https://music.yandex.ru/track/$id"
        else "https://music.yandex.ru/album/${track.albumId}/track/$id"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "${track.artist} — ${track.title}")
            putExtra(Intent.EXTRA_TEXT, link)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться треком"))
    }

    fun shareFile(context: Context, uri: Uri, track: YandexClient.Track) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/mpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, "${track.artist} — ${track.title}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Отправить аудиофайл"))
    }

    fun openCover(context: Context, track: YandexClient.Track) {
        track.coverUrl?.let { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
    }

    fun sharePlaylistLink(context: Context, playlist: YandexClient.Playlist) {
        val link = when {
            playlist.uuid.isNotBlank() -> "https://music.yandex.ru/playlists/${playlist.uuid}"
            playlist.ownerUid.isNotBlank() && playlist.kind.isNotBlank() ->
                "https://music.yandex.ru/users/${playlist.ownerUid}/playlists/${playlist.kind}"
            else -> "https://music.yandex.ru"
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, playlist.title)
            putExtra(Intent.EXTRA_TEXT, link)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться плейлистом"))
    }

    suspend fun downloadCover(context: Context, track: YandexClient.Track): Result<Uri> = runCatching {
        withContext(Dispatchers.IO) {
            val url = track.coverUrl ?: error("Не удалось сохранить обложку")
            val name = safeName("${track.artist} — ${track.title}.jpg")
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Aria")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Не удалось создать файл обложки")
            try {
                context.contentResolver.openOutputStream(uri)?.use { output -> copyImage(url, output) }
                    ?: error("Не удалось открыть файл обложки")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    context.contentResolver.update(uri, done, null, null)
                }
                uri
            } catch (error: Throwable) {
                context.contentResolver.delete(uri, null, null)
                throw error
            }
        }
    }

    suspend fun saveExport(context: Context, fileName: String, mimeType: String, bytes: ByteArray): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val name = safeName(fileName)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val folder = "${Environment.DIRECTORY_DOWNLOADS}/Aria"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, folder)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Не удалось создать файл в Загрузках")
                try {
                    resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Не удалось открыть файл")
                    resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                } catch (error: Throwable) {
                    resolver.delete(uri, null, null)
                    throw error
                }
                "Загрузки/Aria/$name"
            } else {
                val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: error("Хранилище недоступно")
                directory.mkdirs()
                File(directory, name).writeBytes(bytes)
                "Android/data/${context.packageName}/files/Documents/$name"
            }
        }
    }

    suspend fun shareExport(
        context: Context,
        fileName: String,
        mimeType: String,
        bytes: ByteArray,
        chooserTitle: String = "Отправить CSV",
    ): Result<Unit> = runCatching {
        val uri = withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "shared_music").apply { mkdirs() }
            val file = File(directory, safeName(fileName))
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TITLE, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    suspend fun savePicture(context: Context, fileName: String, bytes: ByteArray): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val name = safeName(fileName)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Aria")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Не удалось создать картинку")
            try {
                resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: error("Не удалось открыть файл")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                }
            } catch (error: Throwable) {
                resolver.delete(uri, null, null)
                throw error
            }
            "Pictures/Aria/$name"
        }
    }

    private suspend fun saveToMediaStore(context: Context, client: YandexClient, trackId: String, url: String, fileName: String): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MUSIC}/Aria")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("Не удалось создать файл")
        try {
            context.contentResolver.openOutputStream(uri)?.use { copy(client, trackId, url, it) }
                ?: error("Не удалось открыть файл")
            values.clear()
            values.put(MediaStore.Audio.Media.IS_PENDING, 0)
            context.contentResolver.update(uri, values, null, null)
            return uri
        } catch (error: Throwable) {
            context.contentResolver.delete(uri, null, null)
            throw error
        }
    }

    private suspend fun resolveStream(client: YandexClient, trackId: String): String {
        var failure: Throwable? = null
        for (quality in listOf(YandexClient.AudioQuality.HIGH, YandexClient.AudioQuality.ECONOMY)) {
            client.streamUrl(trackId, quality).onSuccess { url ->
                return url
            }.onFailure { error ->
                failure = error
                client.freshStreamUrl(trackId, quality).onSuccess { url -> return url }.onFailure { failure = it }
            }
        }
        throw (failure ?: IllegalStateException("Аудиопоток недоступен")).let(::friendlyDownloadError)
    }

    private suspend fun copy(client: YandexClient, trackId: String, initialUrl: String, output: java.io.OutputStream) {
        var last: Throwable? = null
        repeat(3) { attempt ->
            var wroteBytes = false
            try {
                val url = if (attempt == 0) initialUrl else {
                    client.freshStreamUrl(trackId, YandexClient.AudioQuality.HIGH).getOrElse {
                        client.freshStreamUrl(trackId, YandexClient.AudioQuality.ECONOMY).getOrThrow()
                    }
                }
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Aria/0.18")
                    .header("Accept", "audio/mpeg,audio/*;q=0.9,*/*;q=0.1")
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val detail = response.body?.string()?.take(280).orEmpty()
                        if (detail.contains("0x41c", ignoreCase = true) || detail.contains("1052")) {
                            error("Яндекс вернул 0x41c: скачивание этого трека ограничено")
                        }
                        error(downloadHttpError(response.code, detail))
                    }
                    val body = response.body ?: error("Аудиопоток пуст")
                    if (body.contentLength() == 0L) error("Аудиопоток пуст")
                    body.byteStream().use { input ->
                        val buffer = ByteArray(32 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count > 0) {
                                wroteBytes = true
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                }
                return
            } catch (error: Throwable) {
                last = error
                if (wroteBytes || attempt == 2) throw friendlyDownloadError(error)
                kotlinx.coroutines.delay((250L shl attempt).coerceAtMost(1_500L))
            }
        }
        throw friendlyDownloadError(last ?: IllegalStateException("Не удалось скачать аудиофайл"))
    }

    private fun downloadHttpError(code: Int, body: String): String = when (code) {
        401, 403 -> "Не удалось скачать трек. Откройте его в плеере или повторите позже"
        404, 410 -> "Не удалось скачать трек. Ссылка устарела, повторите позже"
        408, 429 -> "Не удалось скачать трек. Сервер временно занят"
        in 500..599 -> "Не удалось скачать трек. Сервер временно недоступен"
        else -> "Не удалось скачать трек"
    }

    private fun copyImage(url: String, output: java.io.OutputStream) {
        var last: Throwable? = null
        val candidates = listOf(
            url,
            url.replace("600x600", "300x300"),
            url.replace("600x600", "orig"),
        ).distinct()
        for (candidate in candidates) {
            try {
                val request = Request.Builder()
                    .url(candidate)
                    .header("User-Agent", "Aria/0.18")
                    .header("Accept", "image/avif,image/webp,image/jpeg,image/*;q=0.8,*/*;q=0.1")
                    .build()
                http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) error("Обложка недоступна (${response.code})")
                    val body = response.body ?: error("Обложка пуста")
                    var wrote = 0L
                    body.byteStream().use { input ->
                        val buffer = ByteArray(16 * 1024)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            if (count > 0) {
                                output.write(buffer, 0, count)
                                wrote += count
                            }
                        }
                    }
                    if (wrote == 0L) error("Обложка пуста")
                }
                return
            } catch (error: Throwable) {
                last = error
            }
        }
        throw (last ?: IllegalStateException("Не удалось сохранить обложку"))
    }

    private fun friendlyDownloadError(error: Throwable): Throwable {
        val message = error.message.orEmpty()
        if (message.contains("0x41c", ignoreCase = true) || message.contains("1052")) {
            return IllegalStateException(
                "Не удалось скачать трек. Попробуйте ещё раз или откройте его в плеере",
                error,
            )
        }
        return error
    }

    private fun safeName(value: String): String = value
        .map { if (it in INVALID_FILE_NAME_CHARS) '_' else it }
        .joinToString("")
        .take(120)

    private val INVALID_FILE_NAME_CHARS = charArrayOf('\\', '/', ':', '*', '?', '"', '<', '>', '|')
}
