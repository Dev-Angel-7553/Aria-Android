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

class UpdateCheckException(message: String) : Exception(message)

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

    private val probe = http.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    suspend fun latest(): Result<AppRelease?> = runCatching {
        withContext(Dispatchers.IO) {
            val api = runCatching { latestFromApi() }
            api.getOrNull()?.let { return@withContext it }
            val page = runCatching { latestFromPage() }
            page.getOrNull()?.let { return@withContext it }
            if (api.isSuccess && page.isSuccess) return@withContext null
            throw UpdateCheckException(reason(api.exceptionOrNull() ?: page.exceptionOrNull()))
        }
    }

    private fun latestFromApi(): AppRelease? {
        val request = Request.Builder()
            .url(AppUpdate.LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Aria")
            .build()
        return http.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> null
                !response.isSuccessful -> throw HttpStatusException(response.code)
                else -> AppUpdate.parseLatest(response.body?.string().orEmpty())
            }
        }
    }

    private fun latestFromPage(): AppRelease? {
        val request = Request.Builder()
            .url(AppUpdate.LATEST_PAGE_URL)
            .header("User-Agent", "Aria")
            .build()
        val location = probe.newCall(request).execute().use { response ->
            when {
                response.code == 404 -> return null
                response.isRedirect -> response.header("Location").orEmpty()
                else -> throw HttpStatusException(response.code)
            }
        }
        return AppUpdate.fromReleasePage(location)
    }

    private fun reason(error: Throwable?): String = when (error) {
        is HttpStatusException -> when (error.code) {
            403, 429 -> "GitHub временно ограничил запросы, попробуйте через час"
            in 500..599 -> "GitHub сейчас недоступен"
            else -> "GitHub ответил ${error.code}"
        }
        is java.net.UnknownHostException -> "нет связи с GitHub"
        is java.net.SocketTimeoutException -> "GitHub не ответил вовремя"
        is java.io.IOException -> "нет связи с GitHub"
        else -> "ответ GitHub не распознан"
    }

    private class HttpStatusException(val code: Int) : java.io.IOException("HTTP $code")

    suspend fun download(
        context: Context,
        release: AppRelease,
        control: ru.muz.net.TransferControl = ru.muz.net.TransferControl(),
        onProgress: (Float) -> Unit,
    ): Result<File> = runCatching {
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "updates").apply { mkdirs() }
            val target = File(directory, "Aria-${release.version}.apk")
            val partial = File(directory, "Aria-${release.version}.part")
            try {
                if (target.exists()) {
                    runCatching { UpdateIntegrity.verify(target, release.apkSize, release.sha256); validatePackage(context, target) }
                        .onSuccess { return@withContext target }
                    target.delete()
                }
                val request = Request.Builder().url(release.apkUrl).header("User-Agent", "Aria").build()
                control.response(http.newCall(request)) { response ->
                    if (!response.isSuccessful) error("Не удалось скачать обновление (${response.code})")
                    val body = response.body ?: error("Пустой ответ")
                    val total = body.contentLength().takeIf { it > 0 } ?: release.apkSize
                    var done = 0L
                    var lastReported = -1
                    body.byteStream().use { input ->
                        partial.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                control.checkActive()
                                val count = input.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                done += count
                                if (total > 0) {
                                    val percent = (done * 100 / total).toInt().coerceIn(0, 100)
                                    if (percent != lastReported) {
                                        lastReported = percent
                                        withContext(Dispatchers.Main) { onProgress(percent / 100f) }
                                    }
                                }
                            }
                        }
                    }
                }
                UpdateIntegrity.verify(partial, release.apkSize, release.sha256)
                validatePackage(context, partial)
                currentCoroutineContext().ensureActive()
                control.checkActive()
                check(partial.renameTo(target)) { "Не удалось сохранить обновление" }
                target
            } finally {
                partial.delete()
            }
        }
    }.onFailure { if (it is kotlinx.coroutines.CancellationException) throw it }

    @Suppress("DEPRECATION")
    private fun validatePackage(context: Context, file: File) {
        val flags = if (android.os.Build.VERSION.SDK_INT >= 28) android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES else android.content.pm.PackageManager.GET_SIGNATURES
        val incoming = context.packageManager.getPackageArchiveInfo(file.absolutePath, flags) ?: error("Не удалось прочитать APK")
        val installed = context.packageManager.getPackageInfo(context.packageName, flags)
        require(incoming.packageName == context.packageName) { "Это обновление другого приложения" }
        fun signatures(info: android.content.pm.PackageInfo): Set<String> =
            (if (android.os.Build.VERSION.SDK_INT >= 28) info.signingInfo?.apkContentsSigners else info.signatures)
                .orEmpty().map { it.toCharsString() }.toSet()
        val expected = signatures(installed)
        require(expected.isNotEmpty() && signatures(incoming) == expected) { "Подпись обновления отличается от установленной версии" }
        val incomingVersion = if (android.os.Build.VERSION.SDK_INT >= 28) incoming.longVersionCode else incoming.versionCode.toLong()
        val installedVersion = if (android.os.Build.VERSION.SDK_INT >= 28) installed.longVersionCode else installed.versionCode.toLong()
        require(incomingVersion > installedVersion) { "Установлена такая же или более новая версия" }
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
