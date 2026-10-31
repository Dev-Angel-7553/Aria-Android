package ru.muz.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.rounded.InsertLink
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Visibility
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
    AriaSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(playlist.title, ru.muz.text.tracksLabel(playlist.trackCount), playlist.coverUrl)
            SheetAction(Icons.Rounded.Visibility, "Открыть", onOpen)
            SheetAction(Icons.Rounded.PlayArrow, "Слушать с начала", onPlay, !busy)
            SheetDivider()
            SheetAction(Icons.Rounded.DownloadForOffline, if (offlineBusy) "Сохраняем для офлайна…" else "Сохранить для офлайна", onSaveOffline, !offlineBusy)
            SheetAction(Icons.Rounded.Download, if (busy) "Загрузка…" else "Скачать файлы в «Музыку»", onDownload, !busy)
            SheetDivider()
            SheetAction(Icons.Rounded.InsertLink, "Поделиться ссылкой", onShare)
            SheetAction(Icons.Rounded.FileDownload, if (exportBusy) "Готовим CSV…" else "Сохранить CSV в Загрузки", { onExportCsv(false) }, !exportBusy)
            SheetAction(Icons.Rounded.Share, "Отправить CSV", { onExportCsv(true) }, !exportBusy)
            Spacer(Modifier.height(24.dp))
        }
    }
}
