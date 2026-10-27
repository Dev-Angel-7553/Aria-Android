package ru.muz.ui

import android.os.Build
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import ru.muz.settings.AppStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AriaSheet(onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    if (LocalAppStyle.current != AppStyle.GLASS) {
        ModalBottomSheet(onDismissRequest = onDismissRequest, content = content)
        return
    }
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.4f
    val context = LocalContext.current
    val blur = remember {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (context.getSystemService(android.content.Context.WINDOW_SERVICE) as? WindowManager)?.isCrossWindowBlurEnabled == true
    }
    val base = if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val strength = LocalGlassStrength.current
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = base.copy(alpha = if (blur) 0.62f + 0.18f * (1f - strength) else 0.95f),
        scrimColor = Color.Black.copy(alpha = if (dark) 0.22f else 0.10f),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp),
        dragHandle = {
            Box(
                Modifier.padding(top = 10.dp, bottom = 8.dp).size(38.dp, 5.dp).clip(RoundedCornerShape(50))
                    .background(scheme.onSurface.copy(alpha = 0.22f)),
            )
        },
        modifier = Modifier.drawWithContent {
            drawContent()
            drawLine(
                Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = if (dark) 0.35f else 0.8f), Color.White.copy(alpha = 0f))),
                Offset(34.dp.toPx(), 0.5.dp.toPx()),
                Offset(size.width - 34.dp.toPx(), 0.5.dp.toPx()),
                1.dp.toPx(),
            )
        },
    ) {
        if (blur) WindowBlurBehind((18f + 26f * strength).dp.value)
        content()
    }
}

@Composable
private fun WindowBlurBehind(radiusDp: Float) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val view = LocalView.current
    val density = LocalDensity.current
    DisposableEffect(view, radiusDp) {
        var parent: View? = view
        var window: android.view.Window? = null
        while (parent != null && window == null) {
            window = (parent as? DialogWindowProvider)?.window
            parent = parent.parent as? View
        }
        window?.let {
            it.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            it.attributes = it.attributes.apply { blurBehindRadius = with(density) { radiusDp.dp.roundToPx() } }
        }
        onDispose { }
    }
}
