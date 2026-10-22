package ru.muz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import ru.muz.stats.ListeningStatsSnapshot

@Composable
internal fun ListeningStatsCard(stats: ListeningStatsSnapshot) {
    var expanded by remember { mutableStateOf(false) }
    val corners = LocalCornerRadiusScale.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(22.dp * corners))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .animateContentSize()
            .clickable { expanded = !expanded }
            .padding(vertical = 18.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(48.dp).clip(RoundedCornerShape(15.dp * corners))
                    .background(Accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.LibraryMusic, null, tint = Accent)
            }
            Column(Modifier.weight(1f).padding(horizontal = 13.dp)) {
                Text("Ваша музыка", fontSize = 19.sp, fontWeight = FontWeight.Black)
                Text(
                    if (stats.hasData) "${formatStatsDuration(stats.listenedMs)} · ${stats.trackStarts} запусков"
                    else "Статистика появится после прослушивания",
                    color = SoftText,
                    fontSize = 12.sp,
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                if (expanded) "Свернуть статистику" else "Показать статистику",
                tint = SoftText,
            )
        }

        if (!stats.hasData) {
            Text(
                "Включите трек: время, серии и любимые исполнители будут считаться на этом устройстве.",
                color = SoftText,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
            )
        } else {
            SummaryMetrics(stats)
        }

        AnimatedVisibility(visible = expanded && stats.hasData) {
            Column {
                HorizontalDivider(
                    Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f),
                )
                PeriodMetrics(stats)
                RecentChart(stats)
                StreakSection(stats)
                ListeningTimeOfDay(stats.hourHistogram)
                RankingSection(stats)
            }
        }
    }
}

@Composable
private fun SummaryMetrics(stats: ListeningStatsSnapshot) {
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatValue(Icons.Filled.Timer, formatStatsDuration(stats.listenedMs), "в эфире", Modifier.weight(1f))
        StatValue(Icons.Filled.GraphicEq, stats.trackStarts.toString(), "запусков", Modifier.weight(1f))
        StatValue(Icons.Filled.Equalizer, stats.uniqueArtists.toString(), "артистов", Modifier.weight(1f))
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Person, null, tint = Accent, modifier = Modifier.size(22.dp))
        Column(Modifier.padding(start = 10.dp)) {
            Text("Чаще всего · ${ru.muz.text.daysLabel(stats.activeDays)} с музыкой", color = SoftText, fontSize = 12.sp)
            Text(stats.topArtist, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun PeriodMetrics(stats: ListeningStatsSnapshot) {
    Text(
        "По периодам",
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 18.dp),
    )
    Row(
        Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PeriodValue("Сегодня", formatStatsDuration(stats.todayMs), Modifier.weight(1f))
        PeriodValue("7 дней", formatStatsDuration(stats.weekMs), Modifier.weight(1f))
        PeriodValue("30 дней", formatStatsDuration(stats.monthMs), Modifier.weight(1f))
    }
}

@Composable
private fun PeriodValue(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = SoftText, fontSize = 11.sp)
    }
}

@Composable
private fun RecentChart(stats: ListeningStatsSnapshot) {
    val values = stats.recentDays.map { it.listenedMs }
    val max = values.maxOrNull().orZero().coerceAtLeast(1L)
    val accent = Accent
    val emptyBar = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    Text(
        "Последние 14 дней",
        color = SoftText,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 20.dp),
    )
    Canvas(
        Modifier.fillMaxWidth().height(92.dp).padding(horizontal = 18.dp, vertical = 10.dp),
    ) {
        if (values.isEmpty()) return@Canvas
        val gap = 5.dp.toPx()
        val barWidth = ((size.width - gap * (values.size - 1)) / values.size).coerceAtLeast(2.dp.toPx())
        values.forEachIndexed { index, value ->
            val left = index * (barWidth + gap)
            drawRoundRect(
                color = emptyBar,
                topLeft = Offset(left, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
            val fraction = value.toFloat() / max.toFloat()
            if (fraction > 0f) {
                val barHeight = (size.height * fraction).coerceAtLeast(4.dp.toPx())
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }
    }
    if (stats.recentDays.isNotEmpty()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            listOf(0, stats.recentDays.lastIndex / 2, stats.recentDays.lastIndex).distinct().forEach { index ->
                val date = LocalDate.ofEpochDay(stats.recentDays[index].epochDay)
                Text("${date.dayOfMonth}.${date.monthValue}", color = SoftText, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun StreakSection(stats: ListeningStatsSnapshot) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Filled.LocalFireDepartment, null, tint = Accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(ru.muz.text.daysLabel(stats.currentStreak), fontWeight = FontWeight.Black)
            Text("текущая серия", color = SoftText, fontSize = 11.sp)
        }
        Icon(Icons.Filled.CalendarMonth, null, tint = Accent, modifier = Modifier.size(24.dp))
        Column(Modifier.weight(1f)) {
            Text(ru.muz.text.daysLabel(stats.bestStreak), fontWeight = FontWeight.Black)
            Text("лучшая серия", color = SoftText, fontSize = 11.sp)
        }
        Column(Modifier.weight(1.2f)) {
            Text(formatStatsDuration(stats.averageActiveDayMs), fontWeight = FontWeight.Black)
            Text("в активный день", color = SoftText, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ListeningTimeOfDay(histogram: List<Long>) {
    val periods = listOf(
        "Ночь" to histogram.sliceSum(0..5),
        "Утро" to histogram.sliceSum(6..11),
        "День" to histogram.sliceSum(12..17),
        "Вечер" to histogram.sliceSum(18..23),
    )
    val max = periods.maxOfOrNull { it.second }.orZero().coerceAtLeast(1L)
    Text("Когда слушаете", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp))
    periods.forEach { (label, value) ->
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = SoftText, fontSize = 11.sp, modifier = Modifier.width(52.dp))
            Box(
                Modifier.weight(1f).height(7.dp).clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            ) {
                Box(
                    Modifier.fillMaxWidth(value.toFloat() / max.toFloat()).height(7.dp)
                        .background(Accent.copy(alpha = 0.88f)),
                )
            }
            Text(formatStatsDuration(value), color = SoftText, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun RankingSection(stats: ListeningStatsSnapshot) {
    if (stats.topArtists.isNotEmpty()) {
        Text(
            "Любимые исполнители",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
        )
        stats.topArtists.take(5).forEachIndexed { index, artist ->
            RankingRow(index + 1, artist.name, formatStatsDuration(artist.listenedMs))
        }
    }
    if (stats.topTracks.isNotEmpty()) {
        Text(
            "Чаще всего звучат",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 6.dp),
        )
        stats.topTracks.take(5).forEachIndexed { index, track ->
            RankingRow(index + 1, track.title, track.artist.ifBlank { formatStatsDuration(track.listenedMs) })
        }
    }
}

@Composable
private fun RankingRow(position: Int, title: String, subtitle: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(position.toString(), color = Accent, fontWeight = FontWeight.Black, modifier = Modifier.width(28.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = SoftText, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatValue(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(25.dp))
        Column(Modifier.padding(start = 8.dp)) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
            Text(label, color = SoftText, fontSize = 11.sp)
        }
    }
}

private fun formatStatsDuration(milliseconds: Long): String {
    val minutes = milliseconds.coerceAtLeast(0L) / 60_000L
    val hours = minutes / 60L
    val rest = minutes % 60L
    return when {
        hours >= 100 -> "${hours} ч"
        hours > 0 && rest > 0 -> "${hours} ч ${rest} мин"
        hours > 0 -> "${hours} ч"
        else -> "${minutes} мин"
    }
}

private fun Long?.orZero(): Long = this ?: 0L

private fun List<Long>.sliceSum(range: IntRange): Long = range.sumOf { getOrElse(it) { 0L } }
