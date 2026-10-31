package ru.muz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal val LocalPlayableIds = compositionLocalOf<Set<String>?> { null }

@Composable
internal fun StatusBanner(offline: Boolean, noPlus: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    AnimatedVisibility(
        visible = offline || noPlus,
        modifier = modifier.statusBarsPadding().padding(top = 6.dp),
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val scheme = MaterialTheme.colorScheme
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(scheme.inverseSurface.copy(alpha = 0.92f))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (offline) Icons.Rounded.CloudOff else Icons.Rounded.WorkspacePremium,
                null,
                tint = scheme.inverseOnSurface,
                modifier = Modifier.size(16.dp),
            )
            Text(
                if (offline) "Нет сети · играют сохранённые треки" else "Без Плюса · доступны сохранённые треки",
                color = scheme.inverseOnSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
internal fun PlusNoticeDialog(onOpen: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.WorkspacePremium, null) },
        title = { Text("Подписка Плюс не активна") },
        text = {
            Text(
                "Вы по-прежнему можете слушать треки, которые уже сохранены на этом телефоне, и смотреть их тексты. Новые треки, «Моя волна», поиск по каталогу и скачивание вернутся, когда подписка снова будет активна.",
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Понятно") } },
        dismissButton = { TextButton(onClick = onOpen) { Text("Открыть Плюс") } },
    )
}
