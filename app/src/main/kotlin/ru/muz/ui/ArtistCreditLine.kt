package ru.muz.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import ru.muz.text.ArtistCredit

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ArtistCreditLine(
    artist: String,
    onArtist: () -> Unit,
    onCopy: () -> Unit,
) {
    val credit = remember(artist) { ArtistCredit.copyText(artist) }
    var copied by remember(artist) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    Row(
        modifier = Modifier.heightIn(min = 48.dp).combinedClickable(
            enabled = credit.isNotEmpty(),
            onClickLabel = tr("Открыть исполнителя"),
            onLongClickLabel = tr("Копировать исполнителей"),
            onClick = onArtist,
            onLongClick = { onCopy(); copied = true },
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            if (copied) "Исполнители скопированы" else credit.ifBlank { "Исполнитель не указан" },
            modifier = Modifier.weight(1f, fill = false),
            color = if (copied) PlayerAccent else SoftText,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (credit.isNotEmpty()) {
            Icon(
                if (copied) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = if (copied) PlayerAccent else SoftText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
