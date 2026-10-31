package ru.muz.ui

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.InsertLink
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueuePlayNext
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.OpenInNew
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
    onCopyArtist: () -> Unit = {},
    onDownloadCover: () -> Unit = {},
    onOpenCover: () -> Unit = {},
    onTrackWave: (() -> Unit)? = null,
    onArtistWave: (() -> Unit)? = null,
    onDislike: (() -> Unit)? = null,
) {
    AriaSheet(onDismissRequest = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SheetHeader(track.title, track.artist, track.coverUrl)
            onTrackWave?.let { SheetAction(Icons.Rounded.GraphicEq, "Волна по треку", it) }
            onArtistWave?.let { SheetAction(Icons.Rounded.Person, "Волна по исполнителю", it) }
            SheetAction(Icons.Rounded.QueuePlayNext, "Играть следующим", onPlayNext)
            SheetAction(Icons.Rounded.PlaylistAdd, "Добавить в очередь", onAddToQueue)
            SheetAction(if (liked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, if (liked) "Убрать лайк" else "Мне нравится", onToggleLike)
            SheetDivider()
            SheetAction(Icons.Rounded.ContentCopy, "Копировать исполнителей", onCopyArtist, track.artist.isNotBlank())
            SheetAction(Icons.Rounded.InsertLink, "Поделиться ссылкой", onShareLink)
            SheetAction(Icons.Rounded.Share, if (busy) "Подготовка файла…" else "Отправить аудиофайл", onShareFile, !busy)
            SheetDivider()
            SheetAction(Icons.Rounded.Download, if (busy) "Скачивание…" else "Скачать трек", onDownload, !busy)
            SheetAction(Icons.Rounded.Image, "Скачать обложку", onDownloadCover, !busy)
            SheetAction(Icons.Rounded.OpenInNew, "Открыть обложку", onOpenCover)
            onDislike?.let {
                SheetDivider()
                SheetAction(Icons.Rounded.Block, "Не рекомендовать", it)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
