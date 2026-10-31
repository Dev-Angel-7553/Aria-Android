package ru.muz.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

internal object AriaIcons {
    fun forTab(tab: MainTab, active: Boolean): ImageVector = when (tab) {
        MainTab.HOME -> if (active) Icons.Rounded.Home else Icons.Outlined.Home
        MainTab.LIBRARY -> if (active) Icons.Rounded.LibraryMusic else Icons.Outlined.LibraryMusic
        MainTab.SEARCH -> if (active) Icons.Rounded.Search else Icons.Outlined.Search
        MainTab.STATS -> if (active) Icons.Rounded.BarChart else Icons.Outlined.BarChart
    }
}
