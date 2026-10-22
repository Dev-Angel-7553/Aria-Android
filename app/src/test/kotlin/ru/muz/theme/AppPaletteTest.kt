package ru.muz.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.muz.theme.ColorMath.contrast

class AppPaletteTest {
    private val accents = listOf(0xFFFFD33D, 0xFFFF8A3D, 0xFFFF6B9E, 0xFF77B8FF, 0xFF73DC8C).map { it.toInt() }

    private fun assertContrast(label: String, fg: Int, bg: Int, minimum: Double) {
        val ratio = contrast(fg, bg)
        assertTrue("$label: %.2f < $minimum".format(ratio), ratio >= minimum)
    }

    @Test
    fun everyAccentIsReadableInBothThemes() {
        for (accent in accents) for (dark in listOf(true, false)) {
            val p = AppPalette.generate(accent, dark)
            val tag = "%08X dark=$dark".format(accent)
            assertContrast("$tag primary/background", p.primary, p.background, 4.5)
            assertContrast("$tag onPrimary", p.onPrimary, p.primary, 4.5)
            assertContrast("$tag onPrimaryContainer", p.onPrimaryContainer, p.primaryContainer, 4.5)
            assertContrast("$tag onSecondaryContainer", p.onSecondaryContainer, p.secondaryContainer, 4.5)
            assertContrast("$tag onSurface", p.onSurface, p.surface, 7.0)
            assertContrast("$tag onSurfaceVariant/surface", p.onSurfaceVariant, p.surface, 4.5)
            assertContrast("$tag onSurfaceVariant/variant", p.onSurfaceVariant, p.surfaceVariant, 4.5)
            assertContrast("$tag onSurfaceVariant/containerHighest", p.onSurfaceVariant, p.surfaceContainerHighest, 4.5)
            assertContrast("$tag onErrorContainer", p.onErrorContainer, p.errorContainer, 4.5)
        }
    }

    @Test
    fun darkThemeKeepsMeasuredBackgroundAndRaisedContainers() {
        val p = AppPalette.generate(accents[0], dark = true)
        assertEquals(0xFF1A1A1A.toInt(), p.background)
        assertTrue(ColorMath.luminance(p.surfaceContainerHigh) > ColorMath.luminance(p.surface))
        assertNotEquals(p.surface, p.secondaryContainer)
    }

    @Test
    fun containersAreNotPurpleBaseline() {
        for (accent in accents) {
            assertNotEquals(0xFF4A4458.toInt(), AppPalette.generate(accent, true).secondaryContainer)
        }
    }

    @Test
    fun hslRoundTrip() {
        for (c in accents + listOf(0xFF336699.toInt(), 0xFF808080.toInt())) {
            val hsl = ColorMath.hsl(c)
            val back = ColorMath.fromHsl(hsl[0], hsl[1], hsl[2])
            assertTrue(kotlin.math.abs(ColorMath.red(c) - ColorMath.red(back)) <= 1)
            assertTrue(kotlin.math.abs(ColorMath.green(c) - ColorMath.green(back)) <= 1)
            assertTrue(kotlin.math.abs(ColorMath.blue(c) - ColorMath.blue(back)) <= 1)
        }
    }

    @Test
    fun vibrantPicksTheColourNotTheMud() {
        val grey = 0xFF707070.toInt()
        val red = 0xFFD02030.toInt()
        val pixels = IntArray(400) { if (it % 5 == 0) red else grey }
        val picked = ColorMath.dominantVibrant(pixels)!!
        assertTrue(ColorMath.red(picked) > 180 && ColorMath.green(picked) < 80)
    }

    @Test
    fun greyCoverStaysGrey() {
        val picked = ColorMath.dominantVibrant(IntArray(100) { 0xFF8A8A8A.toInt() })!!
        assertTrue(ColorMath.hsl(picked)[1] < 0.05f)
        assertNull(ColorMath.dominantVibrant(IntArray(0)))
    }

    @Test
    fun playerAccentIsVisibleOnBothBackgrounds() {
        val covers = listOf(0xFF0A0F2A, 0xFF000000, 0xFFFFFFFF, 0xFFFFE600, 0xFF8A8A8A, 0xFF3B1E10).map { it.toInt() }
        for (cover in covers) {
            assertContrast("dark %08X".format(cover), ColorMath.playerAccent(cover, AppPalette.DARK_BACKGROUND), AppPalette.DARK_BACKGROUND, 3.0)
            assertContrast("light %08X".format(cover), ColorMath.playerAccent(cover, AppPalette.LIGHT_BACKGROUND), AppPalette.LIGHT_BACKGROUND, 3.0)
            val accent = ColorMath.playerAccent(cover, AppPalette.DARK_BACKGROUND)
            assertContrast("icon on %08X".format(cover), ColorMath.contentColorFor(accent), accent, 4.5)
        }
    }
}
