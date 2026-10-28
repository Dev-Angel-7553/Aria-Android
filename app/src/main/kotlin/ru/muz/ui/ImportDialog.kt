package ru.muz.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.muz.text.playlistsLabel
import ru.muz.text.tracksLabel

internal data class ImportState(
    val playlist: String,
    val done: Int,
    val total: Int,
    val found: Int,
    val finished: Boolean = false,
    val created: List<String> = emptyList(),
    val missing: List<String> = emptyList(),
    val error: String? = null,
)

@Composable
internal fun ImportDialog(
    state: ImportState,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onCopyMissing: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (state.finished) onClose() },
        title = { Text(if (state.finished) "Импорт завершён" else "Импорт плейлиста") },
        text = {
            Column(Modifier.fillMaxWidth()) {
                if (!state.finished) {
                    Text("«${state.playlist}»", fontWeight = FontWeight.SemiBold)
                    Text("Ищем в Яндекс Музыке: ${state.done} из ${state.total}", color = SoftText, fontSize = 13.sp)
                    LinearProgressIndicator(
                        progress = { if (state.total == 0) 0f else state.done / state.total.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                    Text("Найдено ${tracksLabel(state.found)}", color = SoftText, fontSize = 13.sp)
                } else {
                    if (state.created.isNotEmpty()) {
                        Text("Создано ${playlistsLabel(state.created.size)}: ${state.created.joinToString(", ")}")
                    }
                    Text(
                        "Найдено ${state.found} из ${state.total}",
                        color = SoftText,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    state.error?.let {
                        Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                    }
                    if (state.missing.isNotEmpty()) {
                        Text("Не нашлись:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp))
                        Column(Modifier.heightIn(max = 220.dp).verticalScroll(rememberScrollState())) {
                            state.missing.forEach { Text(it, color = SoftText, fontSize = 13.sp) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (state.finished) TextButton(onClick = onClose) { Text("Готово") }
            else TextButton(onClick = onCancel) { Text("Отменить") }
        },
        dismissButton = {
            if (state.finished && state.missing.isNotEmpty()) {
                TextButton(onClick = onCopyMissing) { Text("Скопировать список") }
            }
        },
    )
}
