package ru.muz.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun NoLyricsArt(modifier: Modifier = Modifier) {
    val ink = MaterialTheme.colorScheme.onSurface
    val accent = PlayerAccent
    Canvas(modifier.size(132.dp)) {
        val w = size.width
        val h = size.height
        val page = Offset(w * 0.12f, h * 0.08f)
        val pageSize = Size(w * 0.62f, h * 0.76f)
        drawRoundRect(ink.copy(alpha = 0.10f), page, pageSize, CornerRadius(w * 0.09f))
        for (i in 0 until 5) {
            val y = page.y + pageSize.height * (0.18f + i * 0.15f)
            val end = if (i % 2 == 0) 0.78f else 0.58f
            drawLine(
                ink.copy(alpha = 0.28f),
                Offset(page.x + pageSize.width * 0.14f, y),
                Offset(page.x + pageSize.width * end, y),
                strokeWidth = w * 0.04f,
                cap = StrokeCap.Round,
            )
        }
        val lens = Offset(w * 0.64f, h * 0.62f)
        val radius = w * 0.17f
        drawCircle(accent.copy(alpha = 0.18f), radius, lens)
        drawCircle(accent, radius, lens, style = Stroke(w * 0.055f))
        drawLine(
            accent,
            Offset(lens.x + radius * 0.72f, lens.y + radius * 0.72f),
            Offset(lens.x + radius * 1.55f, lens.y + radius * 1.55f),
            strokeWidth = w * 0.075f,
            cap = StrokeCap.Round,
        )
    }
}
