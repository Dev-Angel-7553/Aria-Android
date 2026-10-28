package ru.muz.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.muz.yandex.YandexClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TrackActionsSheet(
    track: YandexClient.Track,
    liked: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onShareLink: () -> Unit,
    onShareFile: () -> Unit,
    onDownloadCover: () -> Unit = {},
    onOpenCover: () -> Unit = {},
    onTrackWave: (() -> Unit)? = null,
    onArtistWave: (() -> Unit)? = null,
    onDislike: (() -> Unit)? = null,
) {
    AriaSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Artwork(track.coverUrl, Modifier.size(58.dp), 12.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(track.title, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = SoftText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        onTrackWave?.let { Action(Icons.Filled.GraphicEq, "Волна по треку", it) }
        onArtistWave?.let { Action(Icons.Filled.Person, "Волна по исполнителю", it) }
        Action(Icons.Filled.QueuePlayNext, "Играть следующим", onPlayNext)
        Action(Icons.Filled.PlaylistAdd, "Добавить в очередь", onAddToQueue)
        Action(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, if (liked) "Убрать лайк" else "Мне нравится", onToggleLike)
        Action(Icons.Filled.Download, if (busy) "Скачивание…" else "Скачать трек", onDownload, !busy)
        Action(Icons.Filled.Image, "Скачать обложку", onDownloadCover, !busy)
        Action(Icons.Filled.OpenInNew, "Открыть обложку", onOpenCover)
        Action(Icons.Filled.InsertLink, "Поделиться ссылкой", onShareLink)
        Action(Icons.Filled.Share, if (busy) "Подготовка файла…" else "Отправить аудиофайл", onShareFile, !busy)
        onDislike?.let { Action(Icons.Filled.Block, "Не рекомендовать", it) }
    }
}

@Composable
private fun Action(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null) },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.4f)),
    )
}
