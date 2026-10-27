package ru.muz.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import ru.muz.settings.AppStyle

internal val LocalGlassBackdrop = compositionLocalOf<GlassBackdrop?> { null }

@Composable
internal fun GlassScaffold(
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    val glass = LocalAppStyle.current == AppStyle.GLASS
    val backdrop = rememberGlassBackdrop()
    val active = if (glass) backdrop else null
    Scaffold(
        containerColor = Color.Transparent,
        topBar = { CompositionLocalProvider(LocalGlassBackdrop provides active) { topBar() } },
        bottomBar = { CompositionLocalProvider(LocalGlassBackdrop provides active) { bottomBar() } },
    ) { padding ->
        CompositionLocalProvider(LocalGlassBackdrop provides null) {
            Box(Modifier.fillMaxSize().glassSource(active)) {
                if (glass) MovingCoverBackground(Accent, strength = 0.55f)
                content(padding)
            }
        }
    }
}
