package ru.muz.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import kotlinx.coroutines.launch
import ru.muz.download.TrackTransfer
import ru.muz.stats.ListeningStats
import ru.muz.stats.ListeningStatsSnapshot
import ru.muz.stats.PeriodStats
import ru.muz.stats.StatsPeriod
import ru.muz.stats.TrackStat
import ru.muz.text.plural

internal fun StatsPeriod.title(): String = when (this) {
    StatsPeriod.WEEK -> "Неделя"
    StatsPeriod.MONTH -> "Месяц"
    StatsPeriod.YEAR -> "Год"
    StatsPeriod.ALL -> "Всё время"
}

private fun StatsPeriod.previousTitle(): String = when (this) {
    StatsPeriod.WEEK -> "к прошлой неделе"
    StatsPeriod.MONTH -> "к прошлому месяцу"
    StatsPeriod.YEAR -> "к прошлому году"
    StatsPeriod.ALL -> ""
}

internal fun StatsPeriod.headline(): String = when (this) {
    StatsPeriod.WEEK -> "за 7 дней"
    StatsPeriod.MONTH -> "за 30 дней"
    StatsPeriod.YEAR -> "за год"
    StatsPeriod.ALL -> "за всё время"
}

internal fun formatListening(ms: Long): String {
    if (ms in 1_000L until 60_000L) return "${ms / 1_000L} с"
    val minutes = ms.coerceAtLeast(0L) / 60_000L
    val hours = minutes / 60L
    val rest = minutes % 60L
    return when {
        hours >= 100 -> "$hours ч"
        hours > 0 && rest > 0 -> "$hours ч $rest мин"
        hours > 0 -> "$hours ч"
        else -> "$minutes мин"
    }
}

@Composable
internal fun StatisticsScreen(
    stats: ListeningStatsSnapshot,
    padding: PaddingValues,
    source: ListeningStats? = null,
    onPlayTracks: (List<TrackStat>, Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var period by rememberSaveable { mutableStateOf(StatsPeriod.WEEK) }
    var refresh by remember { mutableIntStateOf(0) }
    val data = remember(stats, period, refresh, source) { source?.period(period) ?: PeriodStats(period) }
    var confirmReset by remember { mutableStateOf(false) }
    var share by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(top = 20.dp, bottom = 32.dp),
    ) {
        item {
            Text("Статистика", fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                if (data.hasData || stats.hasData) "Считается только то, что реально играло" else "История появится после первого трека",
                color = SoftText,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatsPeriod.entries.forEach { value ->
                    item { FilterChip(selected = period == value, onClick = { period = value }, label = { Text(value.title()) }) }
                }
            }
        }
        item { HeroCard(data) { share = true } }
        if (data.bars.any { it.listenedMs > 0 }) item { BarsCard(data) }
        item { StreakRow(data) }
        if (data.topArtists.isNotEmpty()) {
            item { Section("Исполнители") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    itemsIndexed(data.topArtists) { index, artist ->
                        Column(Modifier.width(92.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box {
                                Artwork(artist.coverUrl, Modifier.size(84.dp).clip(CircleShape), 42.dp)
                                Box(
                                    Modifier.align(Alignment.BottomStart).size(26.dp).clip(CircleShape).background(Accent),
                                    contentAlignment = Alignment.Center,
                                ) { Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Black) }
                            }
                            Text(artist.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            Text(formatListening(artist.listenedMs), color = SoftText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        if (data.topTracks.isNotEmpty()) {
            item {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 22.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Треки", fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.weight(1f))
                    TextButton(onClick = { onPlayTracks(data.topTracks, 0) }) {
                        Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                        Text("Слушать все", modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            itemsIndexed(data.topTracks) { index, track ->
                val max = data.topTracks.first().listenedMs.coerceAtLeast(1)
                Row(
                    Modifier.fillMaxWidth().clickable { onPlayTracks(data.topTracks, index) }.padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${index + 1}", color = if (index < 3) Accent else SoftText, fontWeight = FontWeight.Black, modifier = Modifier.width(28.dp))
                    Artwork(track.coverUrl, Modifier.size(48.dp), 9.dp)
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.artist, color = SoftText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Box(Modifier.padding(top = 5.dp).fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(SoftText.copy(alpha = 0.15f))) {
                            Box(Modifier.fillMaxWidth(track.listenedMs.toFloat() / max).height(3.dp).clip(RoundedCornerShape(50)).background(Accent.copy(alpha = 0.8f)))
                        }
                    }
                    Text(formatListening(track.listenedMs), color = SoftText, fontSize = 12.sp)
                }
            }
        }
        if (data.hours.any { it > 0 }) item { HoursCard(data.hours) }
        item {
            Section("Данные")
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = {
                    val json = source?.exportJson().orEmpty()
                    if (json.isNotBlank()) scope.launch {
                        TrackTransfer.shareExport(context, "Aria статистика ${LocalDate.now()}.json", "application/json", json.toByteArray(), "Сохранить статистику")
                    }
                }) {
                    Icon(Icons.Filled.FileDownload, null, modifier = Modifier.size(18.dp))
                    Text("Выгрузить", modifier = Modifier.padding(start = 6.dp))
                }
                TextButton(onClick = { confirmReset = true }) {
                    Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    Text("Сбросить", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить статистику?") },
            text = { Text("Удалится вся локальная история прослушиваний. Яндекс Музыку это не затронет.") },
            confirmButton = {
                TextButton(onClick = {
                    source?.reset()
                    refresh++
                    confirmReset = false
                }) { Text("Сбросить") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
    if (share) StatsShareDialog(data) { share = false }
}

@Composable
private fun Section(title: String) {
    Text(title, fontWeight = FontWeight.Black, fontSize = 19.sp, modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp))
}

@Composable
private fun HeroCard(data: PeriodStats, onShare: () -> Unit) {
    val accent = Accent
    Column(
        Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(RoundedCornerShape(26.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0.08f))))
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Вы слушали ${data.period.headline()}", color = SoftText, fontSize = 13.sp, modifier = Modifier.weight(1f))
            if (data.hasData) {
                Box(Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)).clickable(onClick = onShare), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Share, "Поделиться итогами", modifier = Modifier.size(18.dp))
                }
            }
        }
        Text(formatListening(data.listenedMs), fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 2.dp))
        val change = data.change
        if (change != null && data.period != StatsPeriod.ALL) {
            val up = change >= 0
            Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (up) Icons.Filled.TrendingUp else Icons.Filled.TrendingDown,
                    null,
                    tint = if (up) Color(0xFF5FD38D) else Color(0xFFFF7A7A),
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    "${if (up) "+" else ""}$change% ${data.period.previousTitle()}",
                    color = if (up) Color(0xFF5FD38D) else Color(0xFFFF7A7A),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroValue("${data.uniqueTracks}", plural(data.uniqueTracks, "трек", "трека", "треков"), Modifier.weight(1f))
            HeroValue("${data.uniqueArtists}", plural(data.uniqueArtists, "исполнитель", "исполнителя", "исполнителей"), Modifier.weight(1f))
            HeroValue("${data.activeDays}", plural(data.activeDays, "день", "дня", "дней") + " с музыкой", Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroValue(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surface.copy(alpha = 0.45f)).padding(12.dp)) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = SoftText, fontSize = 11.sp, maxLines = 2)
    }
}

@Composable
private fun BarsCard(data: PeriodStats) {
    val accent = Accent
    val soft = SoftText
    val max = data.bars.maxOf { it.listenedMs }.coerceAtLeast(1)
    val grow by animateFloatAsState(1f, tween(animationDuration(600)), label = "bars")
    var selected by remember(data.period) { mutableIntStateOf(data.bars.indexOfLast { it.current }) }
    val chosen = data.bars.getOrNull(selected)
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                when (data.period) {
                    StatsPeriod.WEEK, StatsPeriod.MONTH -> "По дням"
                    else -> "По месяцам"
                },
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            if (chosen != null) Text("${chosen.label}: ${formatListening(chosen.listenedMs)}", color = accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(
            Modifier.fillMaxWidth().height(130.dp).padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(if (data.bars.size > 14) 2.dp else 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            data.bars.forEachIndexed { index, bar ->
                val k = (bar.listenedMs.toFloat() / max).coerceIn(0f, 1f) * grow
                Column(
                    Modifier.weight(1f).clickable { selected = index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                ) {
                    Canvas(Modifier.fillMaxWidth().height(if (data.bars.size > 14) 112.dp else 100.dp)) {
                        val h = (size.height * k).coerceAtLeast(if (bar.listenedMs > 0) 4f else 2f)
                        drawRoundRect(
                            color = if (index == selected) accent else if (bar.current) accent.copy(alpha = 0.7f) else accent.copy(alpha = 0.35f),
                            topLeft = Offset(0f, size.height - h),
                            size = Size(size.width, h),
                            cornerRadius = CornerRadius(minOf(size.width / 2f, 10f)),
                        )
                    }
                    if (data.bars.size <= 14) {
                        Text(bar.label, color = if (index == selected) accent else soft, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp), maxLines = 1)
                    }
                }
            }
        }
        if (data.bars.size > 14) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(data.bars.first().label, color = soft, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text(data.bars.last().label, color = soft, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun StreakRow(data: PeriodStats) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Tile(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = Color(0xFFFF9A3D), modifier = Modifier.size(20.dp))
                Text("${data.currentStreak}", fontWeight = FontWeight.Black, fontSize = 20.sp, modifier = Modifier.padding(start = 4.dp))
            }
            Text("${plural(data.currentStreak, "день", "дня", "дней")} подряд", color = SoftText, fontSize = 11.sp)
        }
        Tile(Modifier.weight(1f)) {
            Text("${data.bestStreak}", fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("лучшая серия", color = SoftText, fontSize = 11.sp)
        }
        Tile(Modifier.weight(1f)) {
            Text(formatListening(data.averageDayMs), fontWeight = FontWeight.Black, fontSize = 16.sp, maxLines = 1)
            Text("в среднем за день", color = SoftText, fontSize = 11.sp)
        }
    }
    data.bestDay?.takeIf { it.listenedMs >= 60_000L }?.let { day ->
        val date = LocalDate.ofEpochDay(day.epochDay)
        Text(
            "Самый музыкальный день: ${date.dayOfMonth} ${MONTHS_GEN[date.monthValue - 1]}, ${formatListening(day.listenedMs)}",
            color = SoftText,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

private val MONTHS_GEN = listOf("января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря")

@Composable
private fun Tile(modifier: Modifier, content: @Composable () -> Unit) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainer).padding(14.dp)) { content() }
}

@Composable
private fun HoursCard(hours: List<Long>) {
    val parts = listOf(
        "Ночь" to (0..5),
        "Утро" to (6..11),
        "День" to (12..17),
        "Вечер" to (18..23),
    ).map { (name, range) -> name to range.sumOf { hours.getOrElse(it) { 0L } } }
    val total = parts.sumOf { it.second }.coerceAtLeast(1)
    val top = parts.maxBy { it.second }
    val accent = Accent
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer).padding(18.dp),
    ) {
        Text("Когда слушаете", fontWeight = FontWeight.Bold)
        Text("Чаще всего: ${top.first.lowercase()}", color = SoftText, fontSize = 13.sp)
        Canvas(Modifier.fillMaxWidth().height(56.dp).padding(top = 12.dp)) {
            val max = hours.maxOrNull()?.coerceAtLeast(1) ?: 1
            val bar = size.width / 24f
            hours.forEachIndexed { i, ms ->
                val h = (size.height * ms / max).coerceAtLeast(2f)
                drawRoundRect(accent.copy(alpha = 0.3f + 0.7f * ms / max), Offset(i * bar + bar * 0.15f, size.height - h), Size(bar * 0.7f, h), CornerRadius(bar * 0.35f))
            }
        }
        Row(Modifier.fillMaxWidth()) {
            listOf("0", "6", "12", "18", "24").forEachIndexed { i, label ->
                Text(label, color = SoftText, fontSize = 10.sp, modifier = Modifier.weight(1f), textAlign = if (i == 4) TextAlign.End else TextAlign.Start)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            parts.forEach { (name, ms) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if (name == top.first) accent.copy(alpha = 0.18f) else Color.Transparent).padding(8.dp)) {
                    Text("${(ms * 100 / total)}%", fontWeight = FontWeight.Black)
                    Text(name, color = SoftText, fontSize = 11.sp)
                }
            }
        }
    }
}
