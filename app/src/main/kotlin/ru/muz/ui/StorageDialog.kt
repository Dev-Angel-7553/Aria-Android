package ru.muz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.muz.player.AudioCache
import ru.muz.storage.AppStorage
import ru.muz.storage.StorageSnapshot

internal val LocalStorageBusy = staticCompositionLocalOf { false }

@Composable
internal fun StorageDialog(onDismiss: () -> Unit, onChanged: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf(StorageSnapshot()) }
    var loading by remember { mutableStateOf(true) }
    var clearing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmation by remember { mutableStateOf<Pair<String, String>?>(null) }
    var pendingIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val snackbar = remember { SnackbarHostState() }
    val busy = LocalStorageBusy.current || clearing
    suspend fun refresh() {
        snapshot = withContext(Dispatchers.IO) { AppStorage.snapshot(context) }
        onChanged()
    }
    LaunchedEffect(Unit) {
        try { refresh() } catch (failure: Exception) { error = failure.message } finally { loading = false }
    }
    fun remove(ids: Set<String>) {
        if (ids.isEmpty() || pendingIds.isNotEmpty()) return
        pendingIds = ids
        scope.launch {
            var undone = false
            try {
                val result = snackbar.showSnackbar(tr("Удалено треков: ${ids.size}"), tr("Отменить"), withDismissAction = true)
                undone = result == SnackbarResult.ActionPerformed
                if (!undone) {
                    withContext(Dispatchers.IO) { AudioCache.removeOffline(context, ids) }
                    refresh()
                }
            } catch (failure: kotlinx.coroutines.CancellationException) {
                if (!undone) withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { runCatching { AudioCache.removeOffline(context, ids) } }
                throw failure
            } catch (failure: Exception) {
                error = failure.message ?: "Не удалось удалить треки"
            } finally { pendingIds = emptySet() }
        }
    }
    AriaDialog(
        onDismissRequest = onDismiss,
        title = { Text("Память приложения") },
        scrollable = false,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Готово") } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (loading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                val entries = snapshot.offline.filter { it.id !in pendingIds }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        Text("Сохранённая музыка не удаляется автоматически. Экспортированные файлы в Music/Aria остаются в памяти телефона.", style = MaterialTheme.typography.bodySmall)
                    }
                    val categories = listOf(
                        Triple("listening", "Кэш прослушивания", snapshot.listeningBytes),
                        Triple("artwork", "Обложки", snapshot.artworkBytes),
                        Triple("temporary", "Временные файлы", snapshot.temporaryBytes),
                        Triple("lyrics", "Сохранённые тексты", snapshot.lyricsBytes),
                    )
                    items(categories, key = { it.first }) { (key, title, bytes) ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(title, style = MaterialTheme.typography.bodyLarge)
                                Text(storageSize(bytes), style = MaterialTheme.typography.bodySmall, color = SoftText)
                            }
                            IconButton(onClick = { confirmation = key to title }, enabled = bytes > 0 && !busy && !loading) {
                                Icon(Icons.Rounded.DeleteOutline, tr("Очистить: $title"))
                            }
                        }
                    }
                    item {
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text("Сохранено для офлайна", style = MaterialTheme.typography.titleMedium)
                        Text("${entries.size} · ${storageSize(entries.sumOf { it.bytes })}", color = SoftText, style = MaterialTheme.typography.bodySmall)
                    }
                    if (entries.isEmpty()) item { Text("Сохранённых треков пока нет", modifier = Modifier.padding(vertical = 12.dp)) }
                    items(entries, key = { it.id }) { track ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(track.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("${track.artist} · ${storageSize(track.bytes)}", color = SoftText, style = MaterialTheme.typography.bodySmall)
                            }
                            IconButton(onClick = { remove(setOf(track.id)) }, enabled = !busy && pendingIds.isEmpty()) {
                                Icon(Icons.Rounded.DeleteOutline, tr("Удалить: ${track.title}"))
                            }
                        }
                    }
                    if (entries.isNotEmpty()) item {
                        TextButton(onClick = { confirmation = "offline" to "Вся сохранённая музыка" }, enabled = !busy && pendingIds.isEmpty()) {
                            Text("Удалить все сохранённые треки", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                SnackbarHost(snackbar)
            }
        },
    )
    confirmation?.let { (key, title) ->
        AriaDialog(
            onDismissRequest = { confirmation = null },
            title = { Text("Очистить «$title»?") },
            text = { Text(when (key) {
                "offline" -> "Музыку понадобится сохранить заново для прослушивания без сети. После удаления можно нажать «Отменить»."
                "lyrics" -> "Без сети эти тексты будут недоступны, пока вы не откроете их снова с интернетом."
                else -> "Эти файлы будут загружены заново, когда понадобятся. Сохранённая музыка останется."
            }) },
            confirmButton = { TextButton(onClick = {
                confirmation = null
                if (key == "offline") remove(snapshot.offline.mapTo(hashSetOf()) { it.id })
                else scope.launch {
                    clearing = true
                    try { withContext(Dispatchers.IO) { AppStorage.clear(context, key) }; refresh() }
                    catch (failure: Exception) { error = failure.message }
                    finally { clearing = false }
                }
            }) { Text("Очистить", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirmation = null }) { Text("Отмена") } },
        )
    }
}

internal fun storageSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> String.format(java.util.Locale.getDefault(), "%.1f %s", bytes / (1024.0 * 1024 * 1024), tr("ГБ"))
    bytes >= 1024L * 1024L -> String.format(java.util.Locale.getDefault(), "%.1f %s", bytes / (1024.0 * 1024), tr("МБ"))
    else -> "${(bytes / 1024).coerceAtLeast(0)} ${tr("КБ")}"
}
