package ru.muz.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.muz.player.AudioEffects
import ru.muz.settings.AppSettings
import ru.muz.settings.VocalMode

internal data class EqPreset(val name: String, val bands: List<Int>)

internal val EQ_PRESETS = listOf(
    EqPreset("Ровно", listOf(0, 0, 0, 0, 0)),
    EqPreset("Бас", listOf(6, 3, 0, 0, 1)),
    EqPreset("Голос", listOf(-2, 0, 3, 4, 1)),
    EqPreset("Акустика", listOf(3, 1, 1, 2, 3)),
    EqPreset("Электроника", listOf(5, 2, -1, 2, 4)),
    EqPreset("Рок", listOf(4, 1, -2, 2, 4)),
    EqPreset("Наушники", listOf(2, 0, -1, 1, 3)),
    EqPreset("Тихо ночью", listOf(-3, 0, 2, 1, -3)),
)

internal fun vocalModeName(mode: VocalMode): String = when (mode) {
    VocalMode.NORMAL -> "Как есть"
    VocalMode.NO_VOCALS -> "Без голоса"
    VocalMode.VOCALS -> "Голос"
}

private fun vocalModeHint(mode: VocalMode): String = when (mode) {
    VocalMode.NORMAL -> "Трек звучит без изменений"
    VocalMode.NO_VOCALS -> "Убирает то, что звучит по центру, обычно это голос. Бас остаётся. Лучше всего работает на студийных записях, на живых и моно-треках голос может остаться"
    VocalMode.VOCALS -> "Выводит вперёд центр и полосу голоса, остальное приглушается. Это не полное отделение, инструменты по центру тоже слышны"
}

private fun bandLabel(hz: Float): String = if (hz >= 1000f) "${(hz / 1000f).let { if (it % 1f == 0f) it.toInt().toString() else String.format(java.util.Locale.ROOT, "%.1f", it) }} кГц" else "${hz.toInt()} Гц"

@Composable
internal fun SoundControls(settings: AppSettings, onChange: (AppSettings) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Text("Голос", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                .padding(3.dp),
        ) {
            VocalMode.entries.forEach { mode ->
                val active = settings.vocalMode == mode
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(11.dp))
                        .background(if (active) MaterialTheme.colorScheme.surface else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onChange(settings.copy(vocalMode = mode)) }
                        .padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(vocalModeName(mode), fontWeight = if (active) FontWeight.Bold else FontWeight.Medium, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
            }
        }
        Text(vocalModeHint(settings.vocalMode), color = SoftText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))

        Spacer(Modifier.height(18.dp))
        Text("Эквалайзер", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp))
        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(EQ_PRESETS) { preset ->
                FilterChip(
                    selected = settings.eqBands == preset.bands,
                    onClick = { onChange(settings.copy(eqBands = preset.bands)) },
                    label = { Text(preset.name) },
                )
            }
        }
        AudioEffects.FREQUENCIES.forEachIndexed { index, hz ->
            val gain = settings.eqBands.getOrElse(index) { 0 }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(bandLabel(hz), color = SoftText, fontSize = 13.sp, modifier = Modifier.width(64.dp))
                StyledSlider(
                    value = gain.toFloat(),
                    onValueChange = { raw ->
                        val next = kotlin.math.round(raw).toInt().coerceIn(-AudioEffects.MAX_GAIN_DB, AudioEffects.MAX_GAIN_DB)
                        if (next != gain) onChange(settings.copy(eqBands = settings.eqBands.toMutableList().also { list -> while (list.size < 5) list += 0; list[index] = next }))
                    },
                    valueRange = -AudioEffects.MAX_GAIN_DB.toFloat()..AudioEffects.MAX_GAIN_DB.toFloat(),
                    steps = AudioEffects.MAX_GAIN_DB * 2 - 1,
                    modifier = Modifier.weight(1f),
                )
                Text(if (gain > 0) "+$gain" else "$gain", fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.width(36.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("Плавный переход", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(if (settings.crossfadeSeconds == 0) "Выключен" else "${settings.crossfadeSeconds} с", color = SoftText)
        }
        StyledSlider(
            value = settings.crossfadeSeconds.toFloat(),
            onValueChange = { raw -> kotlin.math.round(raw).toInt().coerceIn(0, 12).let { if (it != settings.crossfadeSeconds) onChange(settings.copy(crossfadeSeconds = it)) } },
            valueRange = 0f..12f,
            steps = 11,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Text(
            "Конец трека затихает, следующий плавно набирает громкость",
            color = SoftText,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp),
        )
    }
}

@Composable
internal fun SoundSheet(settings: AppSettings, onChange: (AppSettings) -> Unit, onDismiss: () -> Unit) {
    AriaSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Text("Звук", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(12.dp))
            SoundControls(settings, onChange)
            Spacer(Modifier.height(28.dp))
        }
    }
}
