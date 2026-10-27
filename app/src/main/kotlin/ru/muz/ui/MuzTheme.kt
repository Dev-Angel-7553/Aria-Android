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
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import kotlin.math.roundToInt
import ru.muz.settings.AccentStyle
import ru.muz.settings.AppSettings
import ru.muz.settings.ThemeMode
import ru.muz.settings.AppFont
import ru.muz.settings.PlayShape
import androidx.compose.material3.Typography
import androidx.compose.ui.platform.LocalDensity
import ru.muz.theme.AppPalette

val LocalArtworkCornerScale = staticCompositionLocalOf { 1f }
val LocalCornerRadiusScale = staticCompositionLocalOf { 1f }
val LocalAnimateArtwork = staticCompositionLocalOf { true }
val LocalPlayerAccent = staticCompositionLocalOf<Color?> { null }
val LocalOnPlayerAccent = staticCompositionLocalOf<Color?> { null }
val LocalAnimationSpeed = staticCompositionLocalOf { 1f }
val LocalSeekStyle = staticCompositionLocalOf { ru.muz.settings.SeekStyle.LINE }
val LocalAnimateLoading = staticCompositionLocalOf { true }
val LocalPlayerTransitions = staticCompositionLocalOf { true }
val LocalReduceMotion = staticCompositionLocalOf { false }
internal val LocalPlayShape = staticCompositionLocalOf { PlayShape.CIRCLE }
internal val LocalMiniPrevious = staticCompositionLocalOf { false }

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
    val track = if (settings.trackColors) TrackColors.cover else null
    val dynamic = track == null && settings.useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val fixedSeed = Color(accentColor(settings.accentStyle, settings.customAccent))
    val seed by androidx.compose.animation.animateColorAsState(track ?: fixedSeed, androidx.compose.animation.core.tween(900), label = "track-seed")
    var scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic -> dynamicLightColorScheme(context)
        else -> staticScheme(seed.toArgb(), dark)
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

    if (settings.appStyle == ru.muz.settings.AppStyle.MINIMAL) {
        val ink = scheme.onSurface
        scheme = scheme.copy(
            primary = ink,
            onPrimary = scheme.surface,
            primaryContainer = ink.copy(alpha = 0.12f).compositeOver(scheme.surface),
            onPrimaryContainer = ink,
            secondary = ink.copy(alpha = 0.75f),
            secondaryContainer = ink.copy(alpha = 0.10f).compositeOver(scheme.surface),
            onSecondaryContainer = ink,
            tertiary = ink.copy(alpha = 0.6f),
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
        LocalAppStyle provides settings.appStyle,
        LocalGlassLight provides rememberGlassLight(settings.appStyle == ru.muz.settings.AppStyle.GLASS),
        LocalGlassStrength provides settings.glassStrength / 100f,
        LocalSeekStyle provides settings.seekStyle,
        LocalPlayShape provides settings.playShape,
        LocalMiniPrevious provides settings.miniShowPrevious,
        LocalDensity provides androidx.compose.ui.unit.Density(
            LocalDensity.current.density,
            LocalDensity.current.fontScale * settings.textScalePercent.coerceIn(80, 130) / 100f,
        ),
    ) {
        val radius = settings.cornerRadiusPercent / 100f
        MaterialTheme(
            colorScheme = scheme,
            typography = typography(settings.appFont),
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

internal fun accentColor(accent: AccentStyle, custom: Int = 0xFFB388FF.toInt()): Int = when (accent) {
    AccentStyle.CUSTOM -> custom or 0xFF000000.toInt()
    AccentStyle.YELLOW -> 0xFFFFD33D.toInt()
    AccentStyle.ORANGE -> 0xFFFF8A3D.toInt()
    AccentStyle.PINK -> 0xFFFF6B9E.toInt()
    AccentStyle.BLUE -> 0xFF77B8FF.toInt()
    AccentStyle.GREEN -> 0xFF73DC8C.toInt()
}

internal fun playShape(shape: PlayShape): androidx.compose.ui.graphics.Shape = when (shape) {
    PlayShape.CIRCLE -> androidx.compose.foundation.shape.CircleShape
    PlayShape.SQUIRCLE -> RoundedCornerShape(30)
    PlayShape.SQUARE -> RoundedCornerShape(14)
    PlayShape.COOKIE -> CookieShape
}

internal fun fontFamily(font: AppFont): androidx.compose.ui.text.font.FontFamily? = when (font) {
    AppFont.SYSTEM -> null
    AppFont.SANS -> androidx.compose.ui.text.font.FontFamily.SansSerif
    AppFont.SERIF -> androidx.compose.ui.text.font.FontFamily.Serif
    AppFont.MONO -> androidx.compose.ui.text.font.FontFamily.Monospace
    AppFont.CONDENSED -> androidx.compose.ui.text.font.FontFamily(
        androidx.compose.ui.text.font.Typeface(android.graphics.Typeface.create("sans-serif-condensed", android.graphics.Typeface.NORMAL)),
    )
}

private fun typography(font: AppFont): Typography {
    val base = Typography()
    val family = fontFamily(font) ?: return base
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family),
    )
}

private fun staticScheme(accent: Int, dark: Boolean): ColorScheme {
    val p = AppPalette.generate(accent, dark)
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
