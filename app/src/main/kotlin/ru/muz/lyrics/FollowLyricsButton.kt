package ru.muz.lyrics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.muz.ui.Icon
import ru.muz.ui.Text
import ru.muz.ui.animationDuration

@Composable
internal fun FollowLyricsButton(visible: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val duration = animationDuration(220)
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(duration)) + scaleIn(tween(duration), initialScale = 0.85f),
        exit = fadeOut(tween(duration)) + scaleOut(tween(duration), targetScale = 0.85f),
    ) {
        FilledTonalButton(
            onClick = onClick,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.94f),
            ),
            elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 3.dp),
        ) {
            Icon(Icons.Rounded.MyLocation, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("К текущей строке", maxLines = 1)
        }
    }
}
