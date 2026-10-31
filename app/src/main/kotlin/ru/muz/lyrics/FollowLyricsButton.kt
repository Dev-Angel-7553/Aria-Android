package ru.muz.lyrics

import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun FollowLyricsButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier) { ru.muz.ui.Text("К текущей строке") }
}
