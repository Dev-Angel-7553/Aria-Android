package ru.muz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.muz.settings.AppSettings
import ru.muz.settings.IconPack

@Composable
internal fun IconPackPicker(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(IconPack.entries) { pack ->
            val active = settings.iconPack == pack
            val label = when (pack) {
                IconPack.ROUNDED -> "Скруглённые"
                IconPack.OUTLINED -> "Контурные"
                IconPack.SHARP -> "Угловатые"
                IconPack.TWO_TONE -> "Двухцветные"
            }
            Surface(onClick = { onChange(settings.copy(iconPack = pack)) }, shape = RoundedCornerShape(20.dp),
                color = if (active) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.semantics { selected = active }) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CompositionLocalProvider(LocalIconPack provides pack) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.Home, null)
                            Icon(Icons.Rounded.PlayArrow, null)
                            Icon(Icons.Rounded.LibraryMusic, null)
                        }
                    }
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
