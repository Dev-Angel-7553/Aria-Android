package ru.muz.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Share
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.InsertLink
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
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
internal fun PlaylistActionsSheet(
    playlist: YandexClient.Playlist,
    busy: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onPlay: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    exportBusy: Boolean = false,
    onExportCsv: (share: Boolean) -> Unit = {},
    offlineBusy: Boolean = false,
    onSaveOffline: () -> Unit = {},
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
            Artwork(playlist.coverUrl, Modifier.size(64.dp), 14.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(playlist.title, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(ru.muz.text.tracksLabel(playlist.trackCount), color = SoftText)
            }
        }
        PlaylistAction(Icons.Filled.Visibility, "Открыть", onOpen)
        PlaylistAction(Icons.Filled.PlayArrow, "Слушать с начала", onPlay, !busy)
        PlaylistAction(Icons.Filled.DownloadForOffline, if (offlineBusy) "Сохраняем для офлайна…" else "Сохранить для офлайна", onSaveOffline, !offlineBusy)
        PlaylistAction(Icons.Filled.Download, if (busy) "Загрузка…" else "Скачать файлы в «Музыку»", onDownload, !busy)
        PlaylistAction(Icons.Filled.InsertLink, "Поделиться ссылкой", onShare)
        PlaylistAction(Icons.Filled.FileDownload, if (exportBusy) "Готовим CSV…" else "Сохранить CSV в Загрузки", { onExportCsv(false) }, !exportBusy)
        PlaylistAction(Icons.Filled.Share, "Отправить CSV", { onExportCsv(true) }, !exportBusy)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun PlaylistAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, null) },
        modifier = Modifier.fillMaxWidth()
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier.alpha(0.4f)),
    )
}
