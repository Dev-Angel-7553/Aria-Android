package ru.muz.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random
import ru.muz.BuildConfig
import ru.muz.yandex.YandexClient

internal enum class AriaWaveMode(val title: String, val description: String) {
    MIX("Мой микс", "Треки из вашей коллекции с упором на то, что давно не звучало."),
    REDISCOVER("Давно не слушал", "Треки, которых не было в истории прослушивания за последние 7 дней."),
    OFFLINE("Скачанное", "Только полностью сохранённые треки, которые есть в коллекции."),
}

internal fun makeAriaWave(
    tracks: List<YandexClient.Track>,
    recent: Set<String>,
    previous: Set<String>,
    saved: Set<String>,
    available: Set<String>?,
    hideExplicit: Boolean,
    mode: AriaWaveMode,
    random: Random = Random.Default,
): List<YandexClient.Track> {
    fun id(track: YandexClient.Track) = track.id.substringBefore(':')
    fun artist(track: YandexClient.Track) = track.artists.firstOrNull()?.name.orEmpty()
        .ifBlank { track.artist }.trim().lowercase()
    val recentIds = recent.mapTo(hashSetOf()) { it.substringBefore(':') }
    val candidates = tracks.asSequence().filter { track ->
        val id = id(track)
        id.isNotBlank() && (!hideExplicit || !track.explicit) && (available == null || id in available) &&
            (mode != AriaWaveMode.OFFLINE || id in saved) && (mode != AriaWaveMode.REDISCOVER || id !in recentIds)
    }.distinctBy(::id).toList().shuffled(random)
        .sortedBy { (if (id(it) in recentIds) 2 else 0) + if (id(it) in previous) 1 else 0 }
        .toMutableList()
    val result = mutableListOf<YandexClient.Track>()
    while (candidates.isNotEmpty() && result.size < 30) {
        val nearby = result.takeLast(2).map(::artist).filter(String::isNotBlank)
        val next = candidates.indexOfFirst { artist(it) !in nearby }.coerceAtLeast(0)
        result += candidates.removeAt(next)
    }
    return result
}

@Composable
internal fun NewFeatureBadge(introducedIn: Int) {
    if (BuildConfig.VERSION_CODE == introducedIn) FeatureBadge("Новое")
}

@Composable
private fun FeatureBadge(label: String) {
    Surface(
        color = Accent.copy(alpha = 0.16f),
        shape = MaterialTheme.shapes.small,
        contentColor = Color.Unspecified,
    ) {
        Text(label, color = Accent, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun AriaWaveScreen(
    padding: PaddingValues,
    mode: AriaWaveMode,
    tracks: List<YandexClient.Track>,
    loading: Boolean,
    generated: Boolean,
    compact: Boolean,
    listScale: Int,
    onSettings: () -> Unit,
    onGenerate: (AriaWaveMode) -> Unit,
    onPlay: (Int) -> Unit,
    onTrackMore: (YandexClient.Track) -> Unit,
) {
    LazyColumn(contentPadding = padding) {
        item {
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Волна Aria", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    FeatureBadge("Бета")
                    NewFeatureBadge(50)
                    IconButton(onClick = onSettings) { Icon(Icons.Rounded.Settings, "Настройки") }
                }
                Text("Соберите очередь из своей библиотеки. Она запускается только по вашему нажатию.", color = SoftText)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AriaWaveMode.entries.forEach { option ->
                        FilterChip(
                            selected = mode == option,
                            onClick = { onGenerate(option) },
                            enabled = !loading,
                            label = { Text(option.title) },
                        )
                    }
                }
                Text(mode.description, color = SoftText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = !loading, onClick = { onGenerate(mode) }) {
                        Text(if (loading) "Собираем…" else if (generated) "Пересобрать" else "Собрать")
                    }
                    if (tracks.isNotEmpty() && !loading) {
                        Button(onClick = { onPlay(0) }) { Text("Включить") }
                    }
                }
                if (generated && tracks.isEmpty() && !loading) Text("Для этой подборки пока нет треков.", color = SoftText)
            }
        }
        if (!loading) itemsIndexed(tracks, key = { _, track -> track.id }) { index, track ->
            TrackRow(track, compact, listScale, onMore = onTrackMore) { onPlay(index) }
        }
    }
}
