package ru.muz.ui

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import ru.muz.player.MediaExtras
import ru.muz.settings.PlayerAction

internal data class PlayerTools(
    val actions: List<PlayerAction> = emptyList(),
    val sleepEndsAt: Long = 0L,
    val onSleep: (Int) -> Unit = {},
    val onSpeed: (Float) -> Unit = {},
    val speedChangesPitch: Boolean = false,
    val onSpeedChangesPitch: (Boolean) -> Unit = {},
    val skipSilence: Boolean = false,
    val onSkipSilence: (Boolean) -> Unit = {},
    val trackLink: String? = null,
    val resolveAudio: (suspend () -> Result<String>)? = null,
    val onTrackWave: () -> Unit = {},
    val onDislike: () -> Unit = {},
    val privateSession: Boolean = false,
    val onPrivate: (Boolean) -> Unit = {},
)

internal fun PlayerAction.title(): String = when (this) {
    PlayerAction.QUEUE -> "Очередь"
    PlayerAction.DOWNLOAD -> "Скачать"
    PlayerAction.LINK -> "Поделиться ссылкой"
    PlayerAction.SHARE_FILE -> "Отправить аудиофайл"
    PlayerAction.SAVE_COVER -> "Сохранить обложку"
    PlayerAction.OPEN_COVER -> "Открыть обложку"
    PlayerAction.SLEEP -> "Таймер сна"
    PlayerAction.SPEED -> "Скорость"
    PlayerAction.QUOTE -> "Карточка с цитатой"
    PlayerAction.COPY_ARTIST -> "Скопировать имя артиста"
    PlayerAction.TRACK_WAVE -> "Волна по треку"
    PlayerAction.DISLIKE -> "Не рекомендовать"
    PlayerAction.PRIVATE -> "Приватный режим"
}

internal fun PlayerAction.icon(): ImageVector = when (this) {
    PlayerAction.QUEUE -> Icons.Filled.QueueMusic
    PlayerAction.DOWNLOAD -> Icons.Filled.Download
    PlayerAction.LINK -> Icons.Filled.InsertLink
    PlayerAction.SHARE_FILE -> Icons.Filled.Share
    PlayerAction.SAVE_COVER -> Icons.Filled.Image
    PlayerAction.OPEN_COVER -> Icons.Filled.OpenInNew
    PlayerAction.SLEEP -> Icons.Filled.Bedtime
    PlayerAction.SPEED -> Icons.Filled.Speed
    PlayerAction.QUOTE -> Icons.Filled.FormatQuote
    PlayerAction.COPY_ARTIST -> Icons.Filled.ContentCopy
    PlayerAction.TRACK_WAVE -> Icons.Filled.GraphicEq
    PlayerAction.DISLIKE -> Icons.Filled.Block
    PlayerAction.PRIVATE -> Icons.Filled.VisibilityOff
}

@Composable
internal fun rememberSleepLabel(endsAt: Long): String? {
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(endsAt) {
        while (endsAt > 0) {
            now = SystemClock.elapsedRealtime()
            delay(15_000)
        }
    }
    return when {
        endsAt == MediaExtras.SLEEP_END_OF_TRACK.toLong() -> "Сон после трека"
        endsAt > 0 -> {
            val minutes = ((endsAt - now) / 60_000.0).roundToInt().coerceAtLeast(1)
            "Сон через $minutes мин"
        }
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepTimerSheet(endsAt: Long, onSelect: (Int) -> Unit, onDismiss: () -> Unit) {
    val label = rememberSleepLabel(endsAt)
    var custom by remember { mutableStateOf(45f) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Таймер сна", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Text(label ?: "Музыка плавно затихнет и встанет на паузу", color = SoftText, fontSize = 13.sp)
            LazyRow(
                Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(listOf(5, 15, 30, 60, 90)) { minutes ->
                    FilterChip(selected = false, onClick = { onSelect(minutes); onDismiss() }, label = { Text("$minutes мин") })
                }
                item {
                    FilterChip(
                        selected = endsAt == MediaExtras.SLEEP_END_OF_TRACK.toLong(),
                        onClick = { onSelect(MediaExtras.SLEEP_END_OF_TRACK); onDismiss() },
                        label = { Text("После трека") },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Своё время", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${custom.roundToInt()} мин", color = SoftText)
            }
            StyledSlider(value = custom, onValueChange = { custom = it }, valueRange = 5f..180f, steps = 34)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (endsAt != 0L) TextButton(onClick = { onSelect(0); onDismiss() }) { Text("Выключить") }
                TextButton(onClick = { onSelect(custom.roundToInt()); onDismiss() }) { Text("Запустить") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SpeedSheet(
    speed: Float,
    skipSilence: Boolean,
    onSpeed: (Float) -> Unit,
    onSkipSilence: (Boolean) -> Unit,
    changesPitch: Boolean,
    onChangesPitch: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by remember { mutableStateOf(speed) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Скорость", fontSize = 22.sp, fontWeight = FontWeight.Black)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Скорость воспроизведения", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(String.format(java.util.Locale.ROOT, "%.2f×", value).replace('.', ','), color = SoftText)
            }
            StyledSlider(
                value = value,
                onValueChange = { value = (it * 20).roundToInt() / 20f },
                onValueChangeFinished = { onSpeed(value) },
                valueRange = 0.5f..2f,
                steps = 29,
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf(0.75f, 1f, 1.25f, 1.5f, 2f)) { preset ->
                    FilterChip(
                        selected = kotlin.math.abs(value - preset) < 0.01f,
                        onClick = { value = preset; onSpeed(preset) },
                        label = { Text(String.format(java.util.Locale.ROOT, "%.2f×", preset).replace('.', ',').replace(",00×", "×")) },
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Менять тон", fontWeight = FontWeight.SemiBold)
                    Text("Выше скорость — выше голос, как у пластинки", color = SoftText, fontSize = 13.sp)
                }
                AriaSwitch(checked = changesPitch, onCheckedChange = onChangesPitch)
            }
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Пропускать тишину", fontWeight = FontWeight.SemiBold)
                    Text("Убирает паузы в начале, конце и внутри треков", color = SoftText, fontSize = 13.sp)
                }
                AriaSwitch(checked = skipSilence, onCheckedChange = onSkipSilence)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
