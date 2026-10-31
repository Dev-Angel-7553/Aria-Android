package ru.muz.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ru.muz.yandex.YandexClient

@Composable
internal fun ArtistPicker(artists: List<YandexClient.Artist>, onSelect: (YandexClient.Artist) -> Unit, onDismiss: () -> Unit) {
    AriaSheet(onDismissRequest = onDismiss) {
        Text("Выберите исполнителя", style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp).semantics { heading() })
        LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            items(artists, key = { it.id.ifBlank { it.name } }) { artist ->
                SheetAction(Icons.Rounded.Person, artist.name, { onSelect(artist) })
            }
        }
    }
}
