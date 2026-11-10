package ru.muz.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.muz.settings.AppStyle
import ru.muz.settings.NavStyle
import ru.muz.yandex.YandexClient

internal val LocalNavMenu = compositionLocalOf<(() -> Unit)?> { null }

@Composable
internal fun NavMenuButton() {
    val open = LocalNavMenu.current ?: return
    IconButton(onClick = open) { Icon(Icons.Rounded.Menu, "Меню") }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AriaTopTabs(
    tabs: List<MainTab>,
    selected: MainTab,
    onTab: (MainTab) -> Unit,
    onHold: ((MainTab) -> Unit)?,
    haptics: Boolean,
    backdrop: GlassBackdrop?,
) {
    val style = LocalAppStyle.current
    val glass = style == AppStyle.GLASS
    val scheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val container = when {
        glass -> Modifier.padding(horizontal = 10.dp, vertical = 6.dp).glass(backdrop, RoundedCornerShape(50), corner = 28.dp)
        style == AppStyle.MINIMAL -> Modifier.background(scheme.background)
        else -> Modifier.background(scheme.surfaceContainer)
    }
    Box(Modifier.fillMaxWidth().then(if (glass) Modifier else Modifier.background(if (style == AppStyle.MINIMAL) scheme.background else scheme.surfaceContainer)).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().then(container).horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            tabs.forEach { tab ->
                val active = tab == selected
                val interaction = remember { MutableInteractionSource() }
                val pressed by interaction.collectIsPressedAsState()
                val squeeze by animateFloatAsState(
                    if (pressed && !LocalReduceMotion.current) 0.92f else 1f,
                    if (pressed) tween(TAB_HOLD_MS.toInt()) else spring(dampingRatio = 0.5f, stiffness = 600f),
                    label = "folder-press",
                )
                val background by animateColorAsState(
                    when {
                        !active -> scheme.onSurface.copy(alpha = 0f)
                        style == AppStyle.MATERIAL_YOU -> scheme.primaryContainer
                        glass -> scheme.onSurface.copy(alpha = 0.12f)
                        style == AppStyle.MINIMAL -> scheme.onSurface.copy(alpha = 0.08f)
                        else -> scheme.secondaryContainer
                    },
                    tween(animationDuration(220)),
                    label = "folder-background",
                )
                val content by animateColorAsState(
                    if (active) (if (glass) Accent else scheme.onSecondaryContainer) else scheme.onSurface.copy(alpha = 0.68f),
                    tween(animationDuration(220)),
                    label = "folder-content",
                )
                Row(
                    Modifier.graphicsLayer { scaleX = squeeze; scaleY = squeeze }
                        .clip(RoundedCornerShape(50))
                        .background(background)
                        .combinedClickable(
                            interactionSource = interaction,
                            indication = androidx.compose.material3.ripple(),
                            role = Role.Tab,
                            onLongClickLabel = tr("Быстрые действия"),
                            onLongClick = onHold?.let { hold ->
                                {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    hold(tab)
                                }
                            },
                            onClick = {
                                if (tab != selected && haptics) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onTab(tab)
                            },
                        )
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val pack = LocalIconPack.current
                    Icon(
                        (if (pack.solar) SolarIcons.forTab(tab, active, pack) else null) ?: AriaIcons.forTab(tab, active),
                        null,
                        tint = content,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(tab.title, color = content, fontSize = 14.sp, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
internal fun AriaDrawer(
    tabs: List<MainTab>,
    selected: MainTab,
    account: YandexClient.Account?,
    onTab: (MainTab) -> Unit,
    onSettings: () -> Unit,
) {
    ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.padding(horizontal = 12.dp).statusBarsPadding()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Text(
                        account?.displayName?.firstOrNull()?.uppercase() ?: "A",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                    )
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Aria", fontWeight = FontWeight.Black, fontSize = 20.sp)
                    if (!account?.displayName.isNullOrBlank()) {
                        androidx.compose.material3.Text(
                            account?.displayName.orEmpty(),
                            color = SoftText,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            tabs.forEach { tab ->
                val active = tab == selected
                val pack = LocalIconPack.current
                NavigationDrawerItem(
                    label = { Text(tab.title, fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium) },
                    icon = { Icon((if (pack.solar) SolarIcons.forTab(tab, active, pack) else null) ?: AriaIcons.forTab(tab, active), null) },
                    selected = active,
                    onClick = { onTab(tab) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            NavigationDrawerItem(
                label = { Text("Настройки") },
                icon = { Icon(Icons.Rounded.Settings, null) },
                selected = false,
                onClick = onSettings,
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )
        }
    }
}

internal class TabQuickAction(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val run: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TabHoldSheet(
    tab: MainTab,
    actions: List<TabQuickAction>,
    navStyle: NavStyle,
    onNavStyle: (NavStyle) -> Unit,
    onCustomize: () -> Unit,
    onDismiss: () -> Unit,
) {
    AriaSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
            Text(
                tab.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
            )
            actions.forEach { action ->
                SheetAction(action.icon, action.title, { onDismiss(); action.run() })
            }
            SheetDivider()
            Text(
                "Панель навигации",
                color = SoftText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 4.dp),
            )
            FlowRow(
                Modifier.padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NavStyle.entries.forEach { style ->
                    FilterChip(
                        selected = style == navStyle,
                        onClick = { onNavStyle(style) },
                        label = { Text(navStyleName(style), maxLines = 1) },
                        leadingIcon = if (style == navStyle) {
                            { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                        } else null,
                    )
                }
            }
            SheetAction(Icons.Rounded.Tune, "Настроить вкладки", { onDismiss(); onCustomize() })
            Spacer(Modifier.height(8.dp))
        }
    }
}

internal fun navStyleName(style: NavStyle): String = when (style) {
    NavStyle.BOTTOM -> "Снизу"
    NavStyle.TOP -> "Сверху, как папки"
    NavStyle.DRAWER -> "Боковое меню"
}

internal fun defaultTabActions(
    tab: MainTab,
    onOpen: (MainTab) -> Unit,
    onRefresh: () -> Unit,
    onWaveSettings: () -> Unit,
    onImportCsv: () -> Unit,
    onOpenLiked: () -> Unit,
): List<TabQuickAction> = when (tab) {
    MainTab.HOME -> listOf(
        TabQuickAction(Icons.Rounded.Refresh, "Обновить рекомендации") { onOpen(tab); onRefresh() },
        TabQuickAction(Icons.Rounded.Tune, "Настроить «Мою волну»", onWaveSettings),
    )
    MainTab.LIBRARY -> listOf(
        TabQuickAction(Icons.Rounded.Favorite, "«Мне нравится»", onOpenLiked),
        TabQuickAction(Icons.Rounded.FileUpload, "Импорт плейлиста из CSV", onImportCsv),
        TabQuickAction(Icons.Rounded.Refresh, "Обновить коллекцию") { onOpen(tab); onRefresh() },
    )
    MainTab.SEARCH, MainTab.STATS -> emptyList()
}
