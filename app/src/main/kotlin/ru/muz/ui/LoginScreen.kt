package ru.muz.ui

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import ru.muz.auth.TokenInput

@Composable
internal fun LoginScreen(
    busy: Boolean,
    message: String,
    onConnect: (String) -> Unit,
    language: String = ru.muz.i18n.I18n.SYSTEM,
    onLanguage: (String) -> Unit = {},
) {
    var webLogin by remember { mutableStateOf(false) }
    var manual by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var raw by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var showOther by remember { mutableStateOf(false) }
    val token = TokenInput.extract(raw)
    val extracted = raw.contains("access_token=") && token.isNotBlank()
    fun open(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }
    fun paste() {
        val clip = (context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager)?.primaryClip
        clip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { raw = it }
    }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            LanguageChips(language, onLanguage)
        }
        Spacer(Modifier.height(18.dp))
        AndroidView(
            factory = { android.widget.ImageView(it).apply { setImageResource(ru.muz.R.mipmap.ic_launcher) } },
            modifier = Modifier.size(84.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text("Aria", fontSize = 44.sp, fontWeight = FontWeight.Black)
        Text("Клиент Яндекс Музыки с текстами песен", color = SoftText, fontSize = 15.sp)
        Spacer(Modifier.height(26.dp))
        Feature(Icons.Filled.Lyrics, "Текст по словам", "Строка заливается, пока её поют, дальние строки размыты")
        Feature(Icons.Filled.GraphicEq, "Моя волна, чарт и офлайн", "Сохранённое играет и без сети")
        Feature(Icons.Filled.Palette, "Всё под себя", "Стили, цвета, жесты, эквалайзер")
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = { webLogin = true },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp * LocalCornerRadiusScale.current),
        ) {
            if (busy) {
                MuzLoadingIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Войти через Яндекс", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
        Text(
            "Откроется обычная страница входа Яндекса прямо в приложении, токен подставится сам",
            color = SoftText,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
        )
        if (message.isNotBlank() && !manual) {
            Text(message, color = SoftText, modifier = Modifier.padding(top = 12.dp))
        }
        TextButton(onClick = { manual = !manual }, modifier = Modifier.padding(top = 10.dp)) {
            Text(if (manual) "Скрыть ручной вход" else "У меня уже есть токен")
            Icon(if (manual) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, modifier = Modifier.padding(start = 4.dp).size(18.dp))
        }
        androidx.compose.animation.AnimatedVisibility(manual) {
        Column {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = raw,
            onValueChange = { raw = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Токен или адрес со страницы Яндекса") },
            singleLine = true,
            visualTransformation = if (visible || raw.startsWith("http")) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (!busy) onConnect(token) }),
            trailingIcon = {
                Row {
                    if (raw.isNotEmpty()) {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (visible) "Скрыть" else "Показать")
                        }
                    } else {
                        IconButton(onClick = ::paste) { Icon(Icons.Filled.ContentPaste, "Вставить") }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp * LocalCornerRadiusScale.current),
        )
        if (extracted) {
            Text("Токен найден в адресе", color = Accent, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp, start = 4.dp))
        }
        Button(
            onClick = { onConnect(token) },
            enabled = !busy && token.isNotBlank(),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(52.dp),
        ) {
            if (busy) {
                MuzLoadingIndicator(modifier = Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Войти по токену", fontWeight = FontWeight.Bold)
            }
        }
        if (message.isNotBlank()) {
            Text(message, color = SoftText, modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(Modifier.height(28.dp))
        Column(
            Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(24.dp * LocalCornerRadiusScale.current))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(18.dp),
        ) {
            Text("Как получить токен", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            GuideStep(1, "Откройте страницу входа Яндекса и войдите в свой аккаунт, как обычно.") {
                FilledTonalButton(onClick = { open(TokenInput.OAUTH_URL) }, modifier = Modifier.padding(top = 8.dp)) {
                    Icon(Icons.Filled.OpenInNew, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Открыть страницу входа")
                }
            }
            GuideStep(2, "После входа браузер перейдёт на адрес вида music.yandex.ru/#access_token=… Быстро скопируйте этот адрес целиком, пока страница не обновилась.")
            GuideStep(3, "Вернитесь сюда и нажмите «Вставить» в поле выше. Aria сама достанет токен из адреса.")
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp).clickable { showOther = !showOther },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Не получается? Другие способы", color = Accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(if (showOther) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = Accent)
            }
            if (showOther) {
                Text(
                    "Есть готовые помощники, которые показывают токен сразу после входа:",
                    color = SoftText,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
                GuideLink("Приложение Yandex Music Token для Android") { open(TokenInput.ANDROID_APP) }
                GuideLink("Расширение для Chrome") { open(TokenInput.EXTENSION_CHROME) }
                GuideLink("Расширение для Firefox") { open(TokenInput.EXTENSION_FIREFOX) }
            }
            TextButton(onClick = { open(TokenInput.GUIDE_URL) }, modifier = Modifier.padding(top = 6.dp)) {
                Icon(Icons.Filled.MenuBook, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Подробная инструкция")
            }
        }
        }
        }
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.Lock, null, tint = SoftText, modifier = Modifier.size(16.dp).padding(top = 2.dp))
            Text(
                "Токен хранится зашифрованно только на этом телефоне и отправляется только в Яндекс. Пароль Aria не видит и не спрашивает.",
                color = SoftText,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
    if (webLogin) YandexWebLogin(onToken = { webLogin = false; onConnect(it) }, onDismiss = { webLogin = false })
}

@Composable
private fun Feature(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, text: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.padding(start = 14.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(text, color = SoftText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun LanguageChips(selected: String, onSelect: (String) -> Unit) {
    val current = ru.muz.i18n.I18n.language
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)).padding(3.dp),
    ) {
        listOf("ru" to "RU", "en" to "EN").forEach { (code, label) ->
            val active = current == code
            Box(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (active) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                    .clickable { onSelect(code) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                androidx.compose.material3.Text(label, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun YandexWebLogin(onToken: (String) -> Unit, onDismiss: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, "Закрыть") }
                Column(Modifier.weight(1f)) {
                    Text("Вход в Яндекс", fontWeight = FontWeight.Bold)
                    Text("oauth.yandex.ru", color = SoftText, fontSize = 12.sp)
                }
                if (loading) MuzLoadingIndicator(modifier = Modifier.padding(end = 12.dp).size(22.dp))
            }
            Text(
                "Если Яндекс не принимает код или пароль, закройте это окно и войдите по токену",
                color = SoftText,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp),
            )
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    android.webkit.WebView(context).apply web@{
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.javaScriptCanOpenWindowsAutomatically = true
                        settings.userAgentString = settings.userAgentString
                            .replace("; wv)", ")")
                            .replace(Regex("Version/\\d+(\\.\\d+)* "), "")
                        android.webkit.CookieManager.getInstance().apply {
                            setAcceptCookie(true)
                            setAcceptThirdPartyCookies(this@web, true)
                        }
                        var done = false
                        fun check(url: String?): Boolean {
                            if (done || url == null || !url.contains("access_token=")) return false
                            val token = TokenInput.extract(url)
                            if (!TokenInput.looksValid(token)) return false
                            done = true
                            post { onToken(token) }
                            return true
                        }
                        webViewClient = object : android.webkit.WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: android.webkit.WebView, request: android.webkit.WebResourceRequest): Boolean =
                                check(request.url.toString())

                            override fun onPageStarted(view: android.webkit.WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                loading = true
                                check(url)
                            }

                            override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                                loading = false
                                check(url)
                            }

                            override fun doUpdateVisitedHistory(view: android.webkit.WebView, url: String?, isReload: Boolean) {
                                check(url)
                            }
                        }
                        loadUrl(TokenInput.OAUTH_URL)
                    }
                },
            )
        }
    }
}

@Composable
private fun GuideStep(number: Int, text: String, extra: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(Accent),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 13.sp)
        }
        Column(Modifier.padding(start = 12.dp).weight(1f)) {
            Text(text, fontSize = 14.sp)
            extra?.invoke()
        }
    }
}

@Composable
private fun GuideLink(title: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.OpenInNew, null, tint = SoftText, modifier = Modifier.size(18.dp))
    }
}
