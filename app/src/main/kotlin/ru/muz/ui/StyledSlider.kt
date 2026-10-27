package ru.muz.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import ru.muz.settings.AppStyle

@Composable
internal fun StyledSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val style = LocalAppStyle.current
    if (style != AppStyle.GLASS && style != AppStyle.MINIMAL) {
        Slider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            valueRange = valueRange,
            steps = steps,
            onValueChangeFinished = onValueChangeFinished,
        )
        return
    }
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    var pressed by remember { mutableStateOf(false) }
    val grow by animateFloatAsState(if (pressed) 1f else 0f, tween(animationDuration(160)), label = "slider-grow")
    val span = valueRange.endInclusive - valueRange.start
    fun snap(fraction: Float): Float {
        val raw = valueRange.start + span * fraction.coerceIn(0f, 1f)
        if (steps <= 0 || span <= 0f) return raw
        val stepSize = span / (steps + 1)
        return (valueRange.start + ((raw - valueRange.start) / stepSize).roundToInt() * stepSize).coerceIn(valueRange.start, valueRange.endInclusive)
    }
    val fraction = if (span > 0f) ((value - valueRange.start) / span).coerceIn(0f, 1f) else 0f
    val scheme = MaterialTheme.colorScheme
    val accent = Accent
    val glass = style == AppStyle.GLASS
    Canvas(
        modifier.fillMaxWidth().height(if (glass) 44.dp else 36.dp).pointerInput(valueRange, steps) {
            val pad = (if (glass) 15.dp else 8.dp).toPx()
            fun at(x: Float) = (x - pad) / (size.width - pad * 2f).coerceAtLeast(1f)
            awaitEachGesture {
                val down = awaitFirstDown()
                pressed = true
                down.consume()
                change(snap(at(down.position.x)))
                while (true) {
                    val event = awaitPointerEvent()
                    val pointer = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!pointer.pressed) break
                    change(snap(at(pointer.position.x)))
                    pointer.consume()
                }
                pressed = false
                finished?.invoke()
            }
        },
    ) {
        val y = size.height / 2f
        val pad = (if (glass) 15.dp else 8.dp).toPx()
        val start = pad
        val end = size.width - pad
        val x = start + (end - start) * fraction
        if (glass) {
            val h = 4.dp.toPx()
            drawRoundRect(scheme.onSurface.copy(alpha = 0.16f), Offset(start, y - h / 2f), Size(end - start, h), CornerRadius(h / 2f))
            drawRoundRect(accent, Offset(start, y - h / 2f), Size(x - start, h), CornerRadius(h / 2f))
            val r = (13.dp + 3.dp * grow).toPx()
            drawCircle(Color.Black.copy(alpha = 0.18f), r + 1.5f, Offset(x, y + 2.dp.toPx()))
            if (grow > 0.01f) {
                drawCircle(Color.White.copy(alpha = 1f - 0.65f * grow), r, Offset(x, y))
                drawCircle(Color.White.copy(alpha = 0.9f), r, Offset(x, y), style = Stroke(1.2.dp.toPx()))
                drawCircle(accent.copy(alpha = 0.25f * grow), r * 0.55f, Offset(x, y))
            } else {
                drawCircle(Color.White, r, Offset(x, y))
            }
        } else {
            val stroke = 2.dp.toPx()
            drawLine(scheme.onSurface.copy(alpha = 0.18f), Offset(start, y), Offset(end, y), stroke, StrokeCap.Round)
            drawLine(scheme.onSurface, Offset(start, y), Offset(x, y), stroke, StrokeCap.Round)
            if (steps in 1..24) {
                for (i in 0..steps + 1) {
                    val tx = start + (end - start) * i / (steps + 1)
                    drawCircle(scheme.onSurface.copy(alpha = if (tx <= x) 0.9f else 0.25f), 1.5.dp.toPx(), Offset(tx, y))
                }
            }
            drawCircle(scheme.onSurface, (5.dp + 3.dp * grow).toPx(), Offset(x, y))
        }
    }
}
