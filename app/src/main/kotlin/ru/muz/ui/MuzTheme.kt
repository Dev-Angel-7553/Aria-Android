package ru.muz.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt
import ru.muz.settings.AccentStyle
import ru.muz.settings.AppSettings
import ru.muz.settings.ThemeMode
import ru.muz.theme.AppPalette

val LocalArtworkCornerScale = staticCompositionLocalOf { 1f }
val LocalCornerRadiusScale = staticCompositionLocalOf { 1f }
val LocalAnimateArtwork = staticCompositionLocalOf { true }
val LocalPlayerAccent = staticCompositionLocalOf<Color?> { null }
val LocalOnPlayerAccent = staticCompositionLocalOf<Color?> { null }
val LocalAnimationSpeed = staticCompositionLocalOf { 1f }
val LocalAnimateLoading = staticCompositionLocalOf { true }
val LocalPlayerTransitions = staticCompositionLocalOf { true }
val LocalReduceMotion = staticCompositionLocalOf { false }

@Composable
internal fun animationDuration(baseMs: Int): Int =
    if (LocalReduceMotion.current) 1
    else (baseMs / LocalAnimationSpeed.current).roundToInt().coerceIn(1, 10_000)

val Accent: Color @Composable get() = MaterialTheme.colorScheme.primary
val AppBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val Elevated: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val SoftText: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val PlayerAccent: Color @Composable get() = LocalPlayerAccent.current ?: Accent
val OnPlayerAccent: Color @Composable get() = LocalOnPlayerAccent.current ?: MaterialTheme.colorScheme.onPrimary

@Composable
fun MuzTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val dynamic = settings.useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> staticScheme(settings.accentStyle, dark)
    }
    if (settings.themeMode == ThemeMode.AMOLED) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceDim = Color.Black,
            surfaceBright = Color(0xFF111111),
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF050505),
            surfaceContainer = Color(0xFF080808),
            surfaceContainerHigh = Color(0xFF121212),
            surfaceContainerHighest = Color(0xFF181818),
            surfaceVariant = Color(0xFF161616),
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            (context as? Activity)?.window?.let { window ->
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
        }
    }

    CompositionLocalProvider(
        LocalArtworkCornerScale provides settings.artworkCornerPercent / 100f,
        LocalCornerRadiusScale provides settings.cornerRadiusPercent / 100f,
        LocalAnimateArtwork provides (settings.animateImageLoading && !settings.reduceMotion),
        LocalAnimateLoading provides (settings.animateLoading && !settings.reduceMotion),
        LocalPlayerTransitions provides (settings.animatePlayerTransitions && !settings.reduceMotion),
        LocalAnimationSpeed provides settings.animationSpeedPercent.coerceIn(50, 200) / 100f,
        LocalReduceMotion provides settings.reduceMotion,
    ) {
        val radius = settings.cornerRadiusPercent / 100f
        MaterialTheme(
            colorScheme = scheme,
            shapes = Shapes(
                extraSmall = RoundedCornerShape(4.dp * radius),
                small = RoundedCornerShape(8.dp * radius),
                medium = RoundedCornerShape(12.dp * radius),
                large = RoundedCornerShape(16.dp * radius),
                extraLarge = RoundedCornerShape(28.dp * radius),
            ),
            content = content,
        )
    }
}

internal fun accentColor(accent: AccentStyle): Int = when (accent) {
    AccentStyle.YELLOW -> 0xFFFFD33D.toInt()
    AccentStyle.ORANGE -> 0xFFFF8A3D.toInt()
    AccentStyle.PINK -> 0xFFFF6B9E.toInt()
    AccentStyle.BLUE -> 0xFF77B8FF.toInt()
    AccentStyle.GREEN -> 0xFF73DC8C.toInt()
}

private fun staticScheme(accent: AccentStyle, dark: Boolean): ColorScheme {
    val p = AppPalette.generate(accentColor(accent), dark)
    fun c(value: Int) = Color(value)
    return if (dark) {
        darkColorScheme(
            primary = c(p.primary), onPrimary = c(p.onPrimary),
            primaryContainer = c(p.primaryContainer), onPrimaryContainer = c(p.onPrimaryContainer),
            inversePrimary = c(p.inversePrimary),
            secondary = c(p.secondary), onSecondary = c(p.onSecondary),
            secondaryContainer = c(p.secondaryContainer), onSecondaryContainer = c(p.onSecondaryContainer),
            tertiary = c(p.tertiary), onTertiary = c(p.onTertiary),
            tertiaryContainer = c(p.tertiaryContainer), onTertiaryContainer = c(p.onTertiaryContainer),
            background = c(p.background), onBackground = c(p.onBackground),
            surface = c(p.surface), onSurface = c(p.onSurface),
            surfaceVariant = c(p.surfaceVariant), onSurfaceVariant = c(p.onSurfaceVariant),
            surfaceTint = c(p.surfaceTint),
            inverseSurface = c(p.inverseSurface), inverseOnSurface = c(p.inverseOnSurface),
            error = c(p.error), onError = c(p.onError),
            errorContainer = c(p.errorContainer), onErrorContainer = c(p.onErrorContainer),
            outline = c(p.outline), outlineVariant = c(p.outlineVariant),
            surfaceBright = c(p.surfaceBright), surfaceDim = c(p.surfaceDim),
            surfaceContainer = c(p.surfaceContainer), surfaceContainerHigh = c(p.surfaceContainerHigh),
            surfaceContainerHighest = c(p.surfaceContainerHighest), surfaceContainerLow = c(p.surfaceContainerLow),
            surfaceContainerLowest = c(p.surfaceContainerLowest),
        )
    } else {
        lightColorScheme(
            primary = c(p.primary), onPrimary = c(p.onPrimary),
            primaryContainer = c(p.primaryContainer), onPrimaryContainer = c(p.onPrimaryContainer),
            inversePrimary = c(p.inversePrimary),
            secondary = c(p.secondary), onSecondary = c(p.onSecondary),
            secondaryContainer = c(p.secondaryContainer), onSecondaryContainer = c(p.onSecondaryContainer),
            tertiary = c(p.tertiary), onTertiary = c(p.onTertiary),
            tertiaryContainer = c(p.tertiaryContainer), onTertiaryContainer = c(p.onTertiaryContainer),
            background = c(p.background), onBackground = c(p.onBackground),
            surface = c(p.surface), onSurface = c(p.onSurface),
            surfaceVariant = c(p.surfaceVariant), onSurfaceVariant = c(p.onSurfaceVariant),
            surfaceTint = c(p.surfaceTint),
            inverseSurface = c(p.inverseSurface), inverseOnSurface = c(p.inverseOnSurface),
            error = c(p.error), onError = c(p.onError),
            errorContainer = c(p.errorContainer), onErrorContainer = c(p.onErrorContainer),
            outline = c(p.outline), outlineVariant = c(p.outlineVariant),
            surfaceBright = c(p.surfaceBright), surfaceDim = c(p.surfaceDim),
            surfaceContainer = c(p.surfaceContainer), surfaceContainerHigh = c(p.surfaceContainerHigh),
            surfaceContainerHighest = c(p.surfaceContainerHighest), surfaceContainerLow = c(p.surfaceContainerLow),
            surfaceContainerLowest = c(p.surfaceContainerLowest),
        )
    }
}
