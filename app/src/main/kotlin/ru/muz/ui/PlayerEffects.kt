package ru.muz.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import ru.muz.player.AudioPulse

@Composable
internal fun rememberAudioEnergy(active: Boolean): androidx.compose.runtime.State<Float> {
    val energy = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(active) {
        var smooth = energy.floatValue
        var average = 0.08f
        while (active || smooth > 0.01f) {
            withFrameNanos {
                val fresh = android.os.SystemClock.elapsedRealtime() - AudioPulse.updatedAt < 400
                val raw = if (active && fresh) AudioPulse.level else 0f
                if (raw > 0f) average += (raw - average) * 0.012f
                val target = if (raw <= 0f) 0f else ((raw / average.coerceAtLeast(0.02f) - 0.75f) / 0.85f).coerceIn(0f, 1f)
                smooth += (target - smooth) * if (target > smooth) 0.32f else 0.07f
                energy.floatValue = smooth
            }
        }
        energy.floatValue = 0f
    }
    return energy
}

@Composable
internal fun MorphLoading(modifier: Modifier = Modifier, color: Color = Accent) {
    val animated = LocalAnimateLoading.current
    val transition = rememberInfiniteTransition(label = "morph")
    val spin by transition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(animationDuration(2600), easing = LinearEasing)), label = "morph-spin",
    )
    val morph by transition.animateFloat(
        0f, 1f, infiniteRepeatable(tween(animationDuration(850)), RepeatMode.Reverse), label = "morph-shape",
    )
    Canvas(modifier) {
        val k = if (animated) morph else 0.5f
        val radius = min(size.width, size.height) / 2f * 0.92f
        val depth = 0.05f + 0.11f * k
        val lobes = 8
        val path = Path()
        val steps = 96
        for (i in 0..steps) {
            val t = i / steps.toFloat() * 2f * PI.toFloat()
            val r = radius * (1f - depth + depth * cos(lobes * t))
            val x = center.x + cos(t) * r
            val y = center.y + sin(t) * r
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        rotate(if (animated) spin else 0f) { drawPath(path, color) }
    }
}
