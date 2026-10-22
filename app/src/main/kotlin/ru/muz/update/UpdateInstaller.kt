package ru.muz.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object UpdateInstaller {
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun currentVersion(context: Context): String =
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull().orEmpty()

    suspend fun latest(): Result<AppRelease?> = runCatching {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url(AppUpdate.LATEST_URL)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "Aria")
                .build()
            http.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> null
                    !response.isSuccessful -> error("GitHub ответил ${response.code}")
                    else -> AppUpdate.parseLatest(response.body?.string().orEmpty())
                }
            }
        }
    }

    suspend fun download(context: Context, release: AppRelease, onProgress: (Float) -> Unit): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            directory.listFiles()?.forEach { it.delete() }
            val target = File(directory, "Aria-${release.version}.apk")
            val request = Request.Builder().url(release.apkUrl).header("User-Agent", "Aria").build()
            http.newCall(request).execute().use { response ->
                if (!response.isSuccessful) error("Не удалось скачать обновление (${response.code})")
                val body = response.body ?: error("Пустой ответ")
                val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
                var done = 0L
                var lastReported = -1
                body.byteStream().use { input ->
                    target.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            done += count
                            if (total > 0) {
                                val percent = (done * 100 / total).toInt()
                                if (percent != lastReported) {
                                    lastReported = percent
                                    withContext(Dispatchers.Main) { onProgress(percent / 100f) }
                                }
                            }
                        }
                    }
                }
            }
            target
        }
    }

    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openPage(context: Context, url: String) {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
