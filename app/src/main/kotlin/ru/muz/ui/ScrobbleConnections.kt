package ru.muz.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.muz.scrobble.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ScrobbleConnections(privateSession: Boolean, providedStore: ScrobbleStore? = null) {
    val context = LocalContext.current
    val loaded by produceState<Result<ScrobbleStore>?>(providedStore?.let { Result.success(it) }, providedStore) {
        if (providedStore == null) value = withContext(Dispatchers.IO) { runCatching { ScrobbleStore.shared(context) } }
    }
    if (loaded == null) {
        Text("Загружаем подключения…", modifier = Modifier.padding(20.dp))
        return
    }
    val store = loaded?.getOrNull()
    if (store == null) {
        Text("Не удалось открыть защищённое хранилище подключений. Попробуйте перезапустить приложение.", modifier = Modifier.padding(20.dp))
        return
    }
    val statuses by store.statuses.collectAsState()
    var selected by remember { mutableStateOf<ScrobbleService?>(null) }
    var disconnect by remember { mutableStateOf<ScrobbleService?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Скробблинг", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Text("История прослушиваний и «сейчас играет» в ваших музыкальных профилях. Короткие пропуски не отправляются.", color = SoftText, fontSize = 13.sp)
        if (privateSession) Text("Приватный режим: отправка приостановлена. Треки, прослушанные приватно, не добавляются в очередь.", color = Accent, fontSize = 13.sp)
        statuses.forEach { status ->
            Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(status.service.title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                            Text(if (status.connected) status.name else "Не подключён", color = SoftText, fontSize = 13.sp)
                        }
                        if (status.connected) {
                            Switch(status.enabled, { enabled -> scope.launch { withContext(Dispatchers.IO) { store.enable(status.service, enabled) }; ScrobbleWorker.schedule(context) } },
                                modifier = Modifier.semantics { contentDescription = "${status.service.title}: скробблинг" })
                        }
                    }
                    if (status.error.isNotBlank()) Text(status.error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                    if (status.pending > 0) Text("В очереди: ${status.pending}", color = SoftText, fontSize = 13.sp)
                    else if (status.lastSentAt > 0) Text("Последняя отправка: ${java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).format(java.util.Date(status.lastSentAt))}", color = SoftText, fontSize = 12.sp)
                    if (status.connected) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { scope.launch { withContext(Dispatchers.IO) { store.retryNow(status.service) }; ScrobbleWorker.schedule(context, force = true) } }, enabled = status.enabled && status.pending > 0 && !privateSession) { Text("Отправить очередь") }
                            TextButton(onClick = { disconnect = status.service }) { Text("Отключить") }
                            if (status.error.isNotBlank()) TextButton(onClick = { selected = status.service }) { Text("Войти заново") }
                        }
                    } else Button(onClick = { selected = status.service }) { Text("Подключить") }
                }
            }
        }
        Text("Без сети прослушивания сохраняются на устройстве и отправляются позже. Отключение сервиса удаляет его локальную очередь.", color = SoftText, fontSize = 12.sp)
    }
    selected?.let { service -> ConnectionSheet(service, store) { selected = null } }
    disconnect?.let { service ->
        AriaDialog(onDismissRequest = { disconnect = null },
            title = { Text("Отключить ${service.title}?") },
            text = { Text("Данные входа и очередь этого аккаунта будут удалены с устройства. История на сервисе сохранится.") },
            confirmButton = { TextButton(onClick = {
                scope.launch { withContext(Dispatchers.IO) { store.disconnect(service) }; if (store.accounts().isEmpty()) ScrobbleWorker.stopRecovery(context); disconnect = null }
            }) { Text("Отключить") } },
            dismissButton = { TextButton(onClick = { disconnect = null }) { Text("Отмена") } })
    }
}

@Composable
private fun ConnectionSheet(service: ScrobbleService, store: ScrobbleStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { ScrobbleApi() }
    var key by remember { mutableStateOf("") }
    var secret by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf(store.pendingLogin()?.takeIf { System.currentTimeMillis() - it.createdAt < 55 * 60_000 }) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    fun perform(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            message = ""
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { message = "Не удалось подключиться. Проверьте данные, доступ в браузере и соединение с интернетом." }
            finally { busy = false }
        }
    }
    fun openBrowser(url: String) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    AriaSheet(onDismissRequest = { if (!busy) onDismiss() }) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Подключить ${service.title}", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            if (service == ScrobbleService.LISTENBRAINZ) {
                Text("Скопируйте личный токен из настроек ListenBrainz. Он хранится зашифрованно только на этом устройстве.", color = SoftText)
                TextButton(onClick = { openBrowser("https://listenbrainz.org/settings/") }) { Text("Открыть настройки ListenBrainz") }
                OutlinedTextField(token, { token = it }, label = { Text("Токен ListenBrainz") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), enabled = !busy)
                Button(onClick = { perform {
                    val value = token.trim()
                    val name = api.listenBrainzUser(value)
                    withContext(Dispatchers.IO) { store.connect(ScrobbleAccount(service, name = name, token = value)) }
                    ScrobbleWorker.schedule(context)
                    token = ""
                    onDismiss()
                } }, enabled = token.isNotBlank() && !busy) { Text(if (busy) "Проверяем…" else "Подключить") }
            } else if (pending == null) {
                Text("Введите API key и shared secret своего приложения Last.fm, затем разрешите доступ в браузере. Пароль Last.fm вводится на сайте сервиса.", color = SoftText)
                TextButton(onClick = { openBrowser("https://www.last.fm/api/account/create") }) { Text("Получить ключи Last.fm") }
                OutlinedTextField(key, { key = it }, label = { Text("API key") }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
                OutlinedTextField(secret, { secret = it }, label = { Text("Shared secret") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    visualTransformation = PasswordVisualTransformation(), enabled = !busy)
                Button(onClick = { perform {
                    val login = PendingLastFm(key.trim(), secret.trim(), api.lastFmToken(key.trim(), secret.trim()), System.currentTimeMillis())
                    withContext(Dispatchers.IO) { store.savePending(login) }
                    pending = login
                    key = ""; secret = ""
                    openBrowser(api.authorizationUrl(login.apiKey, login.token))
                } }, enabled = key.isNotBlank() && secret.isNotBlank() && !busy) { Text(if (busy) "Подключаем…" else "Разрешить доступ в Last.fm") }
            } else {
                Text("Разрешите доступ на сайте Last.fm, вернитесь в Aria и завершите подключение.", color = SoftText)
                TextButton(onClick = { pending?.let { openBrowser(api.authorizationUrl(it.apiKey, it.token)) } }, enabled = !busy) { Text("Открыть Last.fm") }
                Button(onClick = { perform {
                    val login = pending ?: return@perform
                    require(System.currentTimeMillis() - login.createdAt < 60 * 60_000) { "Вход устарел" }
                    val (name, session) = api.lastFmSession(login.apiKey, login.secret, login.token)
                    withContext(Dispatchers.IO) { store.connect(ScrobbleAccount(service, name = name, token = session, apiKey = login.apiKey, secret = login.secret)) }
                    ScrobbleWorker.schedule(context)
                    onDismiss()
                } }, enabled = !busy) { Text(if (busy) "Проверяем…" else "Я разрешил доступ") }
                TextButton(onClick = { perform { withContext(Dispatchers.IO) { store.savePending(null) }; pending = null } }, enabled = !busy) { Text("Начать заново") }
            }
            if (message.isNotBlank()) Text(message, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }
    }
}
