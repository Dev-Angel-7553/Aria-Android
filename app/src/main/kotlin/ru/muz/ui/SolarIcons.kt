package ru.muz.ui

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import ru.muz.settings.IconPack

internal val IconPack.solar: Boolean
    get() = this == IconPack.SOLAR_LINEAR || this == IconPack.SOLAR_BOLD || this == IconPack.SOLAR_BROKEN || this == IconPack.SOLAR_DUOTONE

internal object SolarIcons {
    private val cache = HashMap<String, ImageVector>()
    private val black = SolidColor(androidx.compose.ui.graphics.Color.Black)

    fun forMaterial(name: String, pack: IconPack, outlinedHint: Boolean = false): ImageVector? {
        val style = when {
            name in SolarAlwaysFilled -> filled(pack)
            outlinedHint && pack == IconPack.SOLAR_BOLD -> SolarStyle.LINEAR
            else -> regular(pack)
        }
        return vector(name, style)
    }

    fun forTab(tab: MainTab, active: Boolean, pack: IconPack): ImageVector? {
        val name = when (tab) {
            MainTab.HOME -> "Home"
            MainTab.LIBRARY -> "LibraryMusic"
            MainTab.SEARCH -> "Search"
            MainTab.STATS -> "BarChart"
        }
        val style = if (active) filled(pack) else if (pack == IconPack.SOLAR_BOLD) SolarStyle.LINEAR else regular(pack)
        return vector(name, style)
    }

    private fun regular(pack: IconPack): SolarStyle = when (pack) {
        IconPack.SOLAR_BOLD -> SolarStyle.BOLD
        IconPack.SOLAR_BROKEN -> SolarStyle.BROKEN
        IconPack.SOLAR_DUOTONE -> SolarStyle.LINE_DUOTONE
        else -> SolarStyle.LINEAR
    }

    private fun filled(pack: IconPack): SolarStyle =
        if (pack == IconPack.SOLAR_DUOTONE) SolarStyle.BOLD_DUOTONE else SolarStyle.BOLD

    private fun vector(name: String, style: SolarStyle): ImageVector? {
        val key = "${style.name}:$name"
        synchronized(cache) { cache[key]?.let { return it } }
        val paths = solarPaths(style, name) ?: return null
        val builder = ImageVector.Builder(
            name = "Solar.$key",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        val rotated = name in SolarRotated
        if (rotated) builder.addGroup(rotate = 90f, pivotX = 12f, pivotY = 12f)
        paths.forEach { path ->
            builder.addPath(
                pathData = addPathNodes(path.d),
                pathFillType = if (path.evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
                fill = if (path.fill) black else null,
                fillAlpha = path.alpha,
                stroke = if (path.stroke) black else null,
                strokeAlpha = path.alpha,
                strokeLineWidth = path.width,
                strokeLineCap = if (path.roundCap) StrokeCap.Round else StrokeCap.Butt,
                strokeLineJoin = if (path.roundJoin) StrokeJoin.Round else StrokeJoin.Miter,
            )
        }
        if (rotated) builder.clearGroup()
        val vector = builder.build()
        synchronized(cache) { cache[key] = vector }
        return vector
    }
}
