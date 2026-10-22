package ru.muz.theme

import ru.muz.theme.ColorMath.blend
import ru.muz.theme.ColorMath.contentColorFor
import ru.muz.theme.ColorMath.ensureContrast

data class AppPalette(
    val primary: Int,
    val onPrimary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val onSecondary: Int,
    val secondaryContainer: Int,
    val onSecondaryContainer: Int,
    val tertiary: Int,
    val onTertiary: Int,
    val tertiaryContainer: Int,
    val onTertiaryContainer: Int,
    val background: Int,
    val onBackground: Int,
    val surface: Int,
    val onSurface: Int,
    val surfaceVariant: Int,
    val onSurfaceVariant: Int,
    val surfaceTint: Int,
    val inverseSurface: Int,
    val inverseOnSurface: Int,
    val inversePrimary: Int,
    val outline: Int,
    val outlineVariant: Int,
    val surfaceDim: Int,
    val surfaceBright: Int,
    val surfaceContainerLowest: Int,
    val surfaceContainerLow: Int,
    val surfaceContainer: Int,
    val surfaceContainerHigh: Int,
    val surfaceContainerHighest: Int,
    val error: Int,
    val onError: Int,
    val errorContainer: Int,
    val onErrorContainer: Int,
) {
    companion object {
        const val DARK_BACKGROUND = 0xFF1A1A1A.toInt()
        const val LIGHT_BACKGROUND = 0xFFFAFAF8.toInt()

        fun generate(accent: Int, dark: Boolean): AppPalette = if (dark) dark(accent) else light(accent)

        private fun dark(accent: Int): AppPalette {
            fun neutral(grey: Int) = blend(grey, accent, 0.025f)
            val background = DARK_BACKGROUND
            val primary = ensureContrast(accent, background, 4.5)
            val secondary = blend(accent, 0xFFB4B4BC.toInt(), 0.55f)
            val tertiary = ColorMath.hsl(accent).let { ColorMath.fromHsl(it[0] + 40f, (it[1] * 0.7f).coerceAtMost(0.7f), 0.72f) }
            val secondaryContainer = blend(neutral(0xFF2E2E31.toInt()), accent, 0.16f)
            val primaryContainer = blend(accent, 0xFF121212.toInt(), 0.62f)
            val tertiaryContainer = blend(tertiary, 0xFF121212.toInt(), 0.62f)
            return AppPalette(
                primary = primary,
                onPrimary = contentColorFor(primary),
                primaryContainer = primaryContainer,
                onPrimaryContainer = ensureContrast(blend(accent, ColorMath.WHITE, 0.55f), primaryContainer, 4.5),
                secondary = secondary,
                onSecondary = contentColorFor(secondary),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = ensureContrast(blend(accent, ColorMath.WHITE, 0.7f), secondaryContainer, 4.5),
                tertiary = tertiary,
                onTertiary = contentColorFor(tertiary),
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = ensureContrast(blend(tertiary, ColorMath.WHITE, 0.55f), tertiaryContainer, 4.5),
                background = background,
                onBackground = 0xFFEDEDED.toInt(),
                surface = background,
                onSurface = 0xFFEDEDED.toInt(),
                surfaceVariant = neutral(0xFF28282B.toInt()),
                onSurfaceVariant = 0xFFAAAAB3.toInt(),
                surfaceTint = primary,
                inverseSurface = 0xFFE6E6E6.toInt(),
                inverseOnSurface = 0xFF2A2A2A.toInt(),
                inversePrimary = ensureContrast(accent, 0xFFE6E6E6.toInt(), 3.0),
                outline = neutral(0xFF7A7A82.toInt()),
                outlineVariant = neutral(0xFF3C3C40.toInt()),
                surfaceDim = neutral(0xFF141414.toInt()),
                surfaceBright = neutral(0xFF38383B.toInt()),
                surfaceContainerLowest = neutral(0xFF121212.toInt()),
                surfaceContainerLow = neutral(0xFF1F1F21.toInt()),
                surfaceContainer = neutral(0xFF232325.toInt()),
                surfaceContainerHigh = neutral(0xFF2A2A2D.toInt()),
                surfaceContainerHighest = neutral(0xFF333336.toInt()),
                error = 0xFFFFB4AB.toInt(),
                onError = 0xFF690005.toInt(),
                errorContainer = 0xFF5C1A16.toInt(),
                onErrorContainer = 0xFFFFDAD6.toInt(),
            )
        }

        private fun light(accent: Int): AppPalette {
            fun neutral(grey: Int) = blend(grey, accent, 0.03f)
            val background = LIGHT_BACKGROUND
            val primary = ensureContrast(accent, background, 4.5)
            val secondary = ensureContrast(blend(accent, 0xFF6A6A72.toInt(), 0.55f), background, 4.5)
            val tertiary = ColorMath.hsl(accent).let { ColorMath.fromHsl(it[0] + 40f, (it[1] * 0.7f).coerceAtMost(0.7f), 0.36f) }
            val primaryContainer = blend(accent, ColorMath.WHITE, 0.62f)
            val secondaryContainer = blend(neutral(0xFFE9E9EC.toInt()), accent, 0.18f)
            val tertiaryContainer = blend(tertiary, ColorMath.WHITE, 0.78f)
            return AppPalette(
                primary = primary,
                onPrimary = contentColorFor(primary),
                primaryContainer = primaryContainer,
                onPrimaryContainer = ensureContrast(blend(accent, ColorMath.BLACK, 0.6f), primaryContainer, 4.5),
                secondary = secondary,
                onSecondary = contentColorFor(secondary),
                secondaryContainer = secondaryContainer,
                onSecondaryContainer = ensureContrast(blend(accent, ColorMath.BLACK, 0.7f), secondaryContainer, 4.5),
                tertiary = ensureContrast(tertiary, background, 4.5),
                onTertiary = contentColorFor(ensureContrast(tertiary, background, 4.5)),
                tertiaryContainer = tertiaryContainer,
                onTertiaryContainer = ensureContrast(blend(tertiary, ColorMath.BLACK, 0.5f), tertiaryContainer, 4.5),
                background = background,
                onBackground = 0xFF1B1B1D.toInt(),
                surface = background,
                onSurface = 0xFF1B1B1D.toInt(),
                surfaceVariant = neutral(0xFFEDEDEF.toInt()),
                onSurfaceVariant = 0xFF56565E.toInt(),
                surfaceTint = primary,
                inverseSurface = 0xFF2F2F31.toInt(),
                inverseOnSurface = 0xFFF2F2F2.toInt(),
                inversePrimary = ensureContrast(accent, 0xFF2F2F31.toInt(), 3.0),
                outline = neutral(0xFF7A7A82.toInt()),
                outlineVariant = neutral(0xFFCBCBD0.toInt()),
                surfaceDim = neutral(0xFFDCDCDC.toInt()),
                surfaceBright = background,
                surfaceContainerLowest = ColorMath.WHITE,
                surfaceContainerLow = neutral(0xFFF4F4F3.toInt()),
                surfaceContainer = neutral(0xFFEEEEEE.toInt()),
                surfaceContainerHigh = neutral(0xFFE8E8E9.toInt()),
                surfaceContainerHighest = neutral(0xFFE2E2E3.toInt()),
                error = 0xFFBA1A1A.toInt(),
                onError = ColorMath.WHITE,
                errorContainer = 0xFFFFDAD6.toInt(),
                onErrorContainer = 0xFF410002.toInt(),
            )
        }
    }
}
