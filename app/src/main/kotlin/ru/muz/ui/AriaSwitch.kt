package ru.muz.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import ru.muz.settings.AppStyle

private val IosGreen = Color(0xFF34C759)

@Composable
internal fun AriaSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    if (LocalAppStyle.current != AppStyle.GLASS) {
        Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = modifier, enabled = enabled)
        return
    }
    val haptic = LocalHapticFeedback.current
    val change by rememberUpdatedState(onCheckedChange)
    val current by rememberUpdatedState(checked)
    var pressed by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.72f, stiffness = 520f), label = "switch-progress")
    val stretch by animateFloatAsState(if (pressed) 1f else 0f, tween(animationDuration(140)), label = "switch-stretch")
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val off = if (dark) Color(0xFF39393D) else Color(0xFFE5E5EA)
    Canvas(
        modifier
            .size(51.dp, 31.dp)
            .semantics {
                role = Role.Switch
                stateDescription = if (checked) "Вкл" else "Выкл"
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        change?.invoke(!current)
                    },
                )
            },
    ) {
        val width = size.width
        val height = size.height
        val alpha = if (enabled) 1f else 0.45f
        drawRoundRect(lerp(off, IosGreen, progress).copy(alpha = alpha), Offset.Zero, Size(width, height), CornerRadius(height / 2f))
        val inset = 2.dp.toPx()
        val thumbHeight = height - inset * 2f
        val thumbWidth = thumbHeight + 6.dp.toPx() * stretch
        val x = inset + (width - inset * 2f - thumbWidth) * progress
        val radius = CornerRadius(thumbHeight / 2f)
        drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(x, inset + 2.5.dp.toPx()), Size(thumbWidth, thumbHeight), radius)
        drawRoundRect(Color.Black.copy(alpha = 0.10f), Offset(x, inset + 1.dp.toPx()), Size(thumbWidth, thumbHeight), radius)
        drawRoundRect(Color.White.copy(alpha = alpha), Offset(x, inset), Size(thumbWidth, thumbHeight), radius)
    }
}
