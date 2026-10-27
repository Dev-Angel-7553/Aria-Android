package ru.muz.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.muz.settings.AppSettings

private val MOODS = listOf("all" to "Любое", "active" to "Бодрое", "fun" to "Весёлое", "calm" to "Спокойное", "sad" to "Грустное")
private val DIVERSITY = listOf("default" to "Как обычно", "favorite" to "Любимое", "popular" to "Популярное", "discover" to "Незнакомое")
private val LANGUAGES = listOf("any" to "Любой", "russian" to "Русский", "not-russian" to "Иностранный")

internal fun waveSettingsLabel(settings: AppSettings): String {
    val parts = listOfNotNull(
        MOODS.firstOrNull { it.first == settings.waveMood && it.first != "all" }?.second,
        DIVERSITY.firstOrNull { it.first == settings.waveDiversity && it.first != "default" }?.second,
        LANGUAGES.firstOrNull { it.first == settings.waveLanguage && it.first != "any" }?.second,
    )
    return if (parts.isEmpty()) "" else parts.joinToString(" · ")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WaveSettingsSheet(
    settings: AppSettings,
    onApply: (String, String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var mood by remember { mutableStateOf(settings.waveMood) }
    var diversity by remember { mutableStateOf(settings.waveDiversity) }
    var language by remember { mutableStateOf(settings.waveLanguage) }
    AriaSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth()) {
            Text("Настроить «Мою волну»", fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 20.dp))
            Text("Настройки сохраняются в аккаунте и работают и в официальном приложении", color = SoftText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
            Group("Настроение", MOODS, mood) { mood = it }
            Group("Характер", DIVERSITY, diversity) { diversity = it }
            Group("Язык", LANGUAGES, language) { language = it }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    mood = "all"
                    diversity = "default"
                    language = "any"
                }) { Text("Сбросить") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onApply(mood, diversity, language) }) { Text("Готово") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Group(title: String, options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 14.dp))
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(options) { (key, label) ->
            FilterChip(selected = key == selected, onClick = { onSelect(key) }, label = { Text(label) })
        }
    }
}
