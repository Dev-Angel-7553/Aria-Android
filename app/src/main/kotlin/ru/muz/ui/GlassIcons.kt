package ru.muz.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal object GlassIcons {
    private fun icon(name: String, filled: Boolean, width: Float = 1.7f, block: PathBuilder.() -> Unit, solid: (PathBuilder.() -> Unit)? = null): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            if (filled && solid != null) {
                path(fill = SolidColor(Color.Black), pathBuilder = solid)
            }
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = if (filled) width + 0.5f else width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = block,
            )
        }.build()

    private val house: PathBuilder.() -> Unit = {
        moveTo(3.8f, 10.6f)
        lineTo(12f, 3.9f)
        lineTo(20.2f, 10.6f)
        moveTo(5.6f, 9.2f)
        lineTo(5.6f, 18.6f)
        arcToRelative(1.6f, 1.6f, 0f, false, false, 1.6f, 1.6f)
        lineTo(9.8f, 20.2f)
        lineTo(9.8f, 15.2f)
        arcToRelative(1f, 1f, 0f, false, true, 1f, -1f)
        lineTo(13.2f, 14.2f)
        arcToRelative(1f, 1f, 0f, false, true, 1f, 1f)
        lineTo(14.2f, 20.2f)
        lineTo(16.8f, 20.2f)
        arcToRelative(1.6f, 1.6f, 0f, false, false, 1.6f, -1.6f)
        lineTo(18.4f, 9.2f)
    }
    private val houseSolid: PathBuilder.() -> Unit = {
        moveTo(5.6f, 9.2f)
        lineTo(12f, 4f)
        lineTo(18.4f, 9.2f)
        lineTo(18.4f, 18.6f)
        arcToRelative(1.6f, 1.6f, 0f, false, true, -1.6f, 1.6f)
        lineTo(14.2f, 20.2f)
        lineTo(14.2f, 15.2f)
        arcToRelative(1f, 1f, 0f, false, false, -1f, -1f)
        lineTo(10.8f, 14.2f)
        arcToRelative(1f, 1f, 0f, false, false, -1f, 1f)
        lineTo(9.8f, 20.2f)
        lineTo(7.2f, 20.2f)
        arcToRelative(1.6f, 1.6f, 0f, false, true, -1.6f, -1.6f)
        close()
    }

    private val library: PathBuilder.() -> Unit = {
        moveTo(4f, 6f)
        lineTo(13.5f, 6f)
        moveTo(4f, 10.5f)
        lineTo(13.5f, 10.5f)
        moveTo(4f, 15f)
        lineTo(9.5f, 15f)
        moveTo(18f, 16.6f)
        lineTo(18f, 4.6f)
        lineTo(21f, 5.6f)
        moveTo(18f, 16.6f)
        arcToRelative(2.2f, 2.2f, 0f, true, true, -4.4f, 0f)
        arcToRelative(2.2f, 2.2f, 0f, true, true, 4.4f, 0f)
    }
    private val librarySolid: PathBuilder.() -> Unit = {
        moveTo(18f, 16.6f)
        arcToRelative(2.2f, 2.2f, 0f, true, true, -4.4f, 0f)
        arcToRelative(2.2f, 2.2f, 0f, true, true, 4.4f, 0f)
        close()
    }

    private val search: PathBuilder.() -> Unit = {
        moveTo(17f, 10.6f)
        arcToRelative(6.4f, 6.4f, 0f, true, true, -12.8f, 0f)
        arcToRelative(6.4f, 6.4f, 0f, true, true, 12.8f, 0f)
        moveTo(15.4f, 15.4f)
        lineTo(20.2f, 20.2f)
    }

    private val stats: PathBuilder.() -> Unit = {
        moveTo(5f, 20f)
        lineTo(5f, 13.5f)
        moveTo(10f, 20f)
        lineTo(10f, 8f)
        moveTo(15f, 20f)
        lineTo(15f, 11f)
        moveTo(20f, 20f)
        lineTo(20f, 4.5f)
    }

    val Home = icon("glass.home", false, block = house, solid = houseSolid)
    val HomeActive = icon("glass.home.fill", true, block = house, solid = houseSolid)
    val Library = icon("glass.library", false, block = library, solid = librarySolid)
    val LibraryActive = icon("glass.library.fill", true, block = library, solid = librarySolid)
    val Search = icon("glass.search", false, block = search)
    val SearchActive = icon("glass.search.bold", true, width = 2.1f, block = search)
    val Stats = icon("glass.stats", false, width = 2f, block = stats)
    val StatsActive = icon("glass.stats.bold", true, width = 2.6f, block = stats)

    fun forTab(tab: MainTab, active: Boolean): ImageVector = when (tab) {
        MainTab.HOME -> if (active) HomeActive else Home
        MainTab.LIBRARY -> if (active) LibraryActive else Library
        MainTab.SEARCH -> if (active) SearchActive else Search
        MainTab.STATS -> if (active) StatsActive else Stats
    }
}
