package ru.muz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.material.icons.filled.MotionPhotosOff
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.toArgb
import ru.muz.settings.AppFont
import ru.muz.settings.AppTab
import ru.muz.settings.HomeSection
import ru.muz.settings.PlayShape
import ru.muz.settings.PlayerAction
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Code
import ru.muz.update.AppRelease
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ru.muz.lyrics.KaraokeDirection
import ru.muz.lyrics.KaraokeFillStyle
import ru.muz.player.PlayerState
import ru.muz.settings.AccentStyle
import ru.muz.settings.AppSettings
import ru.muz.settings.LyricsAlignment
import ru.muz.settings.PlayerLayout
import ru.muz.settings.ThemeMode
import ru.muz.yandex.YandexClient

private enum class SettingsPage(val title: String) {
    ROOT("Настройки"),
    APPEARANCE("Оформление"),
    ANIMATION("Анимации"),
    LISTS("Списки и поиск"),
    HOME("Главная и вкладки"),
    PLAYER("Плеер"),
    LYRICS("Текст песни"),
    PLAYBACK("Звук и офлайн"),
    ACCOUNT("Аккаунт"),
    EXPORT("Импорт и экспорт"),
    ABOUT("О приложении"),
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
internal fun SettingsScreen(
    settings: AppSettings,
    account: YandexClient.Account?,
    player: PlayerState,
    swipeMiniPlayer: Boolean,
    showMiniPlayerProgress: Boolean,
    onChange: (AppSettings) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit,
    onMiniClick: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSyncNow: () -> Unit = {},
    syncBusy: Boolean = false,
    exportBusy: Boolean = false,
    onExportCollection: (share: Boolean) -> Unit = {},
    appVersion: String = "",
    availableUpdate: AppRelease? = null,
    updateChecking: Boolean = false,
    updateProgress: Float? = null,
    onCheckUpdates: () -> Unit = {},
    onInstallUpdate: (AppRelease) -> Unit = {},
    cacheUsedBytes: Long = 0L,
    onClearCache: () -> Unit = {},
    onRefreshCache: () -> Unit = {},
    onImportCsv: () -> Unit = {},
    onExportSettings: () -> Unit = {},
    onImportSettings: () -> Unit = {},
) {
    var page by remember { mutableStateOf(SettingsPage.ROOT) }
    var query by remember { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    BackHandler(enabled = page != SettingsPage.ROOT || query.isNotBlank()) {
        if (page != SettingsPage.ROOT) page = SettingsPage.ROOT else query = ""
    }
    val glassStyle = LocalAppStyle.current == ru.muz.settings.AppStyle.GLASS
    val header: @Composable () -> Unit = {
        SettingsHeader(page.title) { if (page == SettingsPage.ROOT) onBack() else page = SettingsPage.ROOT }
        when (page) {
            SettingsPage.APPEARANCE -> AppearancePreview()
            SettingsPage.LISTS -> ListPreview(settings)
            SettingsPage.PLAYER -> MiniPlayerPreview(settings)
            SettingsPage.LYRICS -> LyricsPreview(settings)
            else -> Unit
        }
    }
    GlassScaffold(
        topBar = {
            if (glassStyle) {
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp)
                        .glass(LocalGlassBackdrop.current, RoundedCornerShape(28.dp), corner = 28.dp),
                ) { header() }
            }
        },
        bottomBar = {
            Column {
                PlaybackErrorBanner(player.error)
                AnimatedVisibility(player.title.isNotBlank()) {
                    CompactPlayer(
                        player, onMiniClick, onPlayPause, onNext, onPrevious,
                        swipeMiniPlayer, showMiniPlayerProgress,
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        LazyColumn(
            if (glassStyle) Modifier.fillMaxSize() else Modifier.fillMaxSize().statusBarsPadding().padding(scaffoldPadding),
            contentPadding = if (glassStyle) {
                PaddingValues(top = scaffoldPadding.calculateTopPadding() + 8.dp, bottom = scaffoldPadding.calculateBottomPadding() + 32.dp)
            } else PaddingValues(bottom = 32.dp),
        ) {
            if (!glassStyle) {
                stickyHeader {
                    Column(Modifier.fillMaxWidth().background(AppBackground)) { header() }
                }
            }
            when (page) {
                SettingsPage.ROOT -> {
                    item { AccountCard(account, settings.reportListening) { page = SettingsPage.ACCOUNT } }
                    if (query.isBlank()) {
                        item { SettingsTitle("Стиль") }
                        item { StylePicker(settings, onChange) }
                        item { QuickToggles(settings, onChange) }
                    }
                    if (availableUpdate != null) {
                        item {
                            SettingsNavRow(Icons.Filled.SystemUpdate, "Доступна Aria ${availableUpdate.version}", "Нажмите, чтобы обновить") {
                                page = SettingsPage.ABOUT
                            }
                        }
                    }
                    item { SettingsSearchField(query) { query = it } }
                    if (query.isNotBlank()) {
                        val results = searchSettings(query)
                        if (results.isEmpty()) {
                            item { Text("Ничего не нашлось", color = SoftText, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) }
                        }
                        results.forEach { entry ->
                            item {
                                SettingsNavRow(entry.page.icon(), entry.title, entry.page.title) {
                                    query = ""
                                    page = entry.page
                                }
                            }
                        }
                    } else {
                        item { SettingsTitle("Внешний вид") }
                        item { SettingsNavRow(Icons.Filled.Palette, "Оформление", themeLabel(settings), Color(0xFFE5487A)) { page = SettingsPage.APPEARANCE } }
                        item {
                            SettingsNavRow(
                                Icons.Filled.AutoAwesome,
                                "Анимации",
                                if (settings.reduceMotion) "Минимум движения" else "Скорость ${settings.animationSpeedPercent}%",
                                tint = Color(0xFF8E5CF7),
                            ) { page = SettingsPage.ANIMATION }
                        }
                        item { SettingsTitle("Музыка") }
                        item {
                            SettingsNavRow(
                                Icons.Filled.PlayCircle,
                                "Плеер",
                                buildString {
                                    append(if (settings.playerLayout == PlayerLayout.COVER_FIRST) "Сначала обложка" else "Сначала управление")
                                    if (settings.coverAccent) append(" · цвет обложки")
                                },
                                tint = Color(0xFFFF8A3D),
                            ) { page = SettingsPage.PLAYER }
                        }
                        item {
                            SettingsNavRow(
                                Icons.Filled.Lyrics,
                                "Текст песни",
                                LYRICS_PRESETS.firstOrNull { it.matches(settings) }?.title
                                    ?: "Своя настройка · ${settings.lyricsFontSize} sp",
                                tint = Color(0xFF3D8BFF),
                            ) { page = SettingsPage.LYRICS }
                        }
                        item {
                            SettingsNavRow(
                                Icons.Filled.Headphones,
                                "Звук и офлайн",
                                listOf(
                                    if (settings.dataSaver) "Экономия трафика" else "Высокое качество",
                                    if (settings.cacheSizeMb == 0) "кэш выключен" else "кэш ${formatSize(cacheUsedBytes)} из ${formatSize(settings.cacheSizeMb * 1024L * 1024L)}",
                                ).joinToString(" · "),
                                tint = Color(0xFF2FBF71),
                            ) { page = SettingsPage.PLAYBACK }
                        }
                        item { SettingsTitle("Библиотека") }
                        item {
                            SettingsNavRow(
                                Icons.Filled.Home,
                                "Главная и вкладки",
                                listOf(
                                    "${settings.homeSections.size} ${ru.muz.text.plural(settings.homeSections.size, "блок", "блока", "блоков")}",
                                    "${settings.tabs.size} ${ru.muz.text.plural(settings.tabs.size, "вкладка", "вкладки", "вкладок")}",
                                ).joinToString(" · "),
                                tint = Color(0xFF17ADA8),
                            ) { page = SettingsPage.HOME }
                        }
                        item {
                            SettingsNavRow(
                                Icons.Filled.Dashboard,
                                "Списки и поиск",
                                "${if (settings.compactLists) "Компактно" else "Обычно"} · масштаб ${settings.listScalePercent}%",
                                tint = Color(0xFF5B6CF0),
                            ) { page = SettingsPage.LISTS }
                        }
                        item { SettingsNavRow(Icons.Filled.ImportExport, "Импорт и экспорт", "Плейлисты в CSV и обратно, резервная копия настроек", Color(0xFFF2A516)) { page = SettingsPage.EXPORT } }
                        item { SettingsTitle("Приложение") }
                        item {
                            SettingsNavRow(
                                Icons.Filled.Info,
                                "О приложении",
                                availableUpdate?.let { "Доступна версия ${it.version}" } ?: "Aria $appVersion",
                                tint = Color(0xFF8A8F98),
                            ) { page = SettingsPage.ABOUT }
                        }
                        item {
                            TextButton(
                                onClick = { confirmReset = true },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            ) { Text("Сбросить все настройки") }
                        }
                    }
                }
                SettingsPage.APPEARANCE -> appearanceSettings(settings, onChange)
                SettingsPage.ANIMATION -> animationSettings(settings, onChange)
                SettingsPage.LISTS -> listSettings(settings, onChange)
                SettingsPage.HOME -> homeSettings(settings, onChange)
                SettingsPage.PLAYER -> playerSettings(settings, onChange)
                SettingsPage.LYRICS -> lyricsSettings(settings, onChange)
                SettingsPage.PLAYBACK -> playbackSettings(settings, onChange, cacheUsedBytes, onClearCache, onRefreshCache)
                SettingsPage.ABOUT -> aboutSettings(
                    settings, onChange, appVersion, availableUpdate, updateChecking, updateProgress,
                    onCheckUpdates, onInstallUpdate,
                )
                SettingsPage.ACCOUNT -> accountSettings(
                    account, settings, onChange, { confirmLogout = true }, onSyncNow, syncBusy,
                )
                SettingsPage.EXPORT -> exportSettings(exportBusy, onExportCollection, onImportCsv, onExportSettings, onImportSettings)
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить настройки?") },
            text = { Text("Тема, плеер, списки и экран текста вернутся к исходным значениям. Аккаунт и статистика останутся.") },
            confirmButton = {
                TextButton(onClick = { onChange(AppSettings()); confirmReset = false }) { Text("Сбросить") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Выйти из аккаунта?") },
            text = { Text("Токен будет удалён с устройства. Чтобы вернуться, понадобится вставить его снова.") },
            confirmButton = {
                TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Выйти") }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Отмена") } },
        )
    }
}

private fun LazyListScope.animationSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Скорость эффектов") }
    item {
        SettingSlider("Скорость анимаций", settings.animationSpeedPercent, 50..200, 25, { "$it%" }) {
            onChange(settings.copy(animationSpeedPercent = it))
        }
    }
    item {
        SettingSwitch(
            "Уменьшить движение",
            "Отключает плавные переходы и оставляет только важные состояния",
            settings.reduceMotion,
        ) { onChange(settings.copy(reduceMotion = it)) }
    }
    item { SettingSwitch("Переходы в плеере", "Плавная смена обложки, текста и кнопок", settings.animatePlayerTransitions) { onChange(settings.copy(animatePlayerTransitions = it)) } }
    item { SettingSwitch("Анимация загрузки", "Пульсация обложек, индикаторы ожидания и «играет сейчас» в списках", settings.animateLoading) { onChange(settings.copy(animateLoading = it)) } }
    item { SettingSwitch("Плавное появление обложек", "Обложка проявляется, а не возникает резко", settings.animateImageLoading) { onChange(settings.copy(animateImageLoading = it)) } }
    item {
        SectionReset("Сбросить анимации") { defaults ->
            onChange(
                settings.copy(
                    animationSpeedPercent = defaults.animationSpeedPercent,
                    reduceMotion = defaults.reduceMotion,
                    animatePlayerTransitions = defaults.animatePlayerTransitions,
                    animateLoading = defaults.animateLoading,
                    animateImageLoading = defaults.animateImageLoading,
                ),
            )
        }
    }
}

private fun LazyListScope.appearanceSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Стиль") }
    item { StylePicker(settings, onChange) }
    item {
        AnimatedVisibility(settings.appStyle == ru.muz.settings.AppStyle.GLASS) {
            SettingSlider("Сила стекла", settings.glassStrength, 10..100, 10, { "$it%" }) { onChange(settings.copy(glassStrength = it)) }
        }
    }
    item { SettingsTitle("Тема") }
    ThemeMode.entries.forEach { mode ->
        item { SettingRadio(themeName(mode), settings.themeMode == mode) { onChange(settings.copy(themeMode = mode)) } }
    }
    item { SettingsTitle("Цвета") }
    item {
        SettingSwitch("Monet / динамические цвета", "Цвета обоев на Android 12 и новее", settings.useDynamicColors) {
            onChange(settings.copy(useDynamicColors = it))
        }
    }
    item {
        SettingSwitch("Цвета под песню", "Акцент и оттенки всего приложения берутся из обложки того, что играет, и плавно меняются вместе с треком", settings.trackColors) {
            onChange(settings.copy(trackColors = it))
        }
    }
    item {
        Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Text("Свой акцент", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp))
            Text(
                if (settings.useDynamicColors) "Выбор цвета выключит Monet" else accentName(settings.accentStyle),
                color = SoftText,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(AccentStyle.entries) { accent ->
                    AccentSwatch(
                        accent = accent,
                        custom = settings.customAccent,
                        selected = !settings.useDynamicColors && settings.accentStyle == accent,
                    ) { onChange(settings.copy(accentStyle = accent, useDynamicColors = false)) }
                }
            }
        }
    }
    item {
        AnimatedVisibility(!settings.useDynamicColors && settings.accentStyle == AccentStyle.CUSTOM) {
            ColorPicker(settings.customAccent) { onChange(settings.copy(customAccent = it, accentStyle = AccentStyle.CUSTOM, useDynamicColors = false)) }
        }
    }
    item { SettingsTitle("Шрифт") }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(AppFont.entries) { font ->
                FilterChip(
                    selected = settings.appFont == font,
                    onClick = { onChange(settings.copy(appFont = font)) },
                    label = { Text(fontName(font), fontFamily = fontFamily(font)) },
                )
            }
        }
    }
    item { SettingSlider("Размер текста", settings.textScalePercent, 80..130, 5, { "$it%" }) { onChange(settings.copy(textScalePercent = it)) } }
    item { SettingsTitle("Форма") }
    item { SettingSlider("Скругление интерфейса", settings.cornerRadiusPercent, 0..160, 10, { "$it%" }) { onChange(settings.copy(cornerRadiusPercent = it)) } }
    item { SettingSlider("Скругление обложек", settings.artworkCornerPercent, 0..160, 10, { "$it%" }) { onChange(settings.copy(artworkCornerPercent = it)) } }
    item { SettingsTitle("Навигация") }
    item { SettingSwitch("Подписи под значками", "Текст в нижней панели", settings.showNavigationLabels) { onChange(settings.copy(showNavigationLabels = it)) } }
    item {
        SectionReset("Сбросить оформление") { defaults ->
            onChange(
                settings.copy(
                    themeMode = defaults.themeMode,
                    appFont = defaults.appFont,
                    textScalePercent = defaults.textScalePercent,
                    useDynamicColors = defaults.useDynamicColors,
                    accentStyle = defaults.accentStyle,
                    cornerRadiusPercent = defaults.cornerRadiusPercent,
                    artworkCornerPercent = defaults.artworkCornerPercent,
                    showNavigationLabels = defaults.showNavigationLabels,
                ),
            )
        }
    }
}

@Composable
private fun AccentSwatch(accent: AccentStyle, custom: Int, selected: Boolean, onClick: () -> Unit) {
    val colour = Color(accentColor(accent, custom))
    Box(
        Modifier.size(46.dp).clip(CircleShape)
            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(colour)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val mark = Color(ru.muz.theme.ColorMath.contentColorFor(colour.toArgb()))
        when {
            selected -> Icon(Icons.Filled.Check, accentName(accent), tint = mark, modifier = Modifier.size(20.dp))
            accent == AccentStyle.CUSTOM -> Icon(Icons.Filled.Colorize, accentName(accent), tint = mark, modifier = Modifier.size(20.dp))
        }
    }
}

private fun LazyListScope.listSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Списки треков") }
    item { SettingSwitch("Компактные списки", "Больше треков помещается на экране", settings.compactLists) { onChange(settings.copy(compactLists = it)) } }
    item { SettingSlider("Масштаб строк", settings.listScalePercent, 80..125, 5, { "$it%" }) { onChange(settings.copy(listScalePercent = it)) } }
    item { SettingSlider("Треков за раз", settings.listPageSize, 30..180, 10, { "$it" }) { onChange(settings.copy(listPageSize = it)) } }
    item { SettingSwitch("Заранее загружать обложки", "Готовит обложки следующих треков очереди", settings.preloadArtwork) { onChange(settings.copy(preloadArtwork = it)) } }
    item { SettingsTitle("Поиск") }
    item {
        SettingSlider(
            "История поиска",
            settings.keepSearchHistory,
            0..30,
            5,
            { if (it == 0) "Не сохранять" else "$it запросов" },
        ) { onChange(settings.copy(keepSearchHistory = it)) }
    }
    item {
        SectionReset("Сбросить списки") { defaults ->
            onChange(
                settings.copy(
                    compactLists = defaults.compactLists,
                    listScalePercent = defaults.listScalePercent,
                    listPageSize = defaults.listPageSize,
                    preloadArtwork = defaults.preloadArtwork,
                    keepSearchHistory = defaults.keepSearchHistory,
                ),
            )
        }
    }
}

private fun LazyListScope.playerSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Поведение") }
    item { SettingSwitch("Открывать плеер при выборе", "Показывает полный экран нового трека", settings.openPlayerOnTrack) { onChange(settings.copy(openPlayerOnTrack = it)) } }
    item { SettingSwitch("Свайпы мини-плеера", "Влево — следующий, вправо — предыдущий", settings.swipeMiniPlayer) { onChange(settings.copy(swipeMiniPlayer = it)) } }
    item { SettingSwitch("Прогресс мини-плеера", "Тонкая полоса под треком", settings.showMiniPlayerProgress) { onChange(settings.copy(showMiniPlayerProgress = it)) } }
    item { SettingSwitch("Жесты полного плеера", "Влево/вправо — трек, вверх — очередь, вниз — свернуть", settings.fullPlayerSwipe) { onChange(settings.copy(fullPlayerSwipe = it)) } }
    item { SettingSwitch("Не выключать экран", "Пока приложение открыто", settings.keepScreenOn) { onChange(settings.copy(keepScreenOn = it)) } }
    item { SettingSwitch("Двойной тап по обложке", "Действие можно выбрать ниже", settings.doubleTapLike) { onChange(settings.copy(doubleTapLike = it)) } }
    item {
        AnimatedVisibility(settings.doubleTapLike) {
            Column {
                ru.muz.settings.DoubleTapAction.entries.forEach { action ->
                    SettingRadio(
                        when (action) {
                            ru.muz.settings.DoubleTapAction.LIKE -> "«Мне нравится»"
                            ru.muz.settings.DoubleTapAction.PLAY_PAUSE -> "Пауза и продолжить"
                            ru.muz.settings.DoubleTapAction.NEXT -> "Следующий трек"
                        },
                        settings.doubleTapAction == action,
                    ) { onChange(settings.copy(doubleTapAction = action)) }
                }
            }
        }
    }
    item { SettingSwitch("Вибрация", "Лёгкий отклик на «Мне нравится»", settings.haptics) { onChange(settings.copy(haptics = it)) } }
    item { SettingsTitle("Полоска прогресса") }
    ru.muz.settings.SeekStyle.entries.forEach { style ->
        item {
            SettingRadio(
                when (style) {
                    ru.muz.settings.SeekStyle.LINE -> "Линия с бегунком"
                    ru.muz.settings.SeekStyle.WAVY -> "Волна, пока играет"
                    ru.muz.settings.SeekStyle.CAPSULE -> "Капсула, как в iOS"
                    ru.muz.settings.SeekStyle.HAIRLINE -> "Тонкая с точкой"
                },
                settings.seekStyle == style,
            ) { onChange(settings.copy(seekStyle = style)) }
        }
    }
    item { SettingSwitch("Фон в такт музыке", "Пятна цвета обложки пульсируют вместе с битом", settings.pulseBackground) { onChange(settings.copy(pulseBackground = it)) } }
    item {
        AnimatedVisibility(settings.pulseBackground) {
            SettingSlider("Сила пульсации", settings.pulseStrength, 10..100, 10, { "$it%" }) { onChange(settings.copy(pulseStrength = it)) }
        }
    }
    item { SettingSwitch("Цвет кнопок от обложки", "Кнопка паузы в плеере с текстом окрашивается под трек", settings.accentControls) { onChange(settings.copy(accentControls = it)) } }
    item { SettingsTitle("Цвет и обложка") }
    item { SettingSwitch("Цвет обложки в фоне", "Мягкий градиент цвета обложки сверху экрана", settings.coverColorBackground) { onChange(settings.copy(coverColorBackground = it)) } }
    item {
        AnimatedVisibility(!settings.coverColorBackground) {
            SettingSwitch("Лёгкий цветной фон", "Тот же градиент, но вдвое слабее", settings.playerBlurBackground) { onChange(settings.copy(playerBlurBackground = it)) }
        }
    }
    item { SettingSwitch("Акцент от обложки", "Кнопки и ползунок берут цвет обложки", settings.coverAccent) { onChange(settings.copy(coverAccent = it)) } }
    item { SettingSlider("Размер обложки", settings.playerArtworkPercent, 70..110, 5, { "$it%" }) { onChange(settings.copy(playerArtworkPercent = it)) } }
    item { SettingsTitle("Кнопки") }
    item {
        Text(
            "Под ползунком помещается до 5 кнопок, остальные будут в меню «Ещё».",
            color = SoftText,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    item {
        ReorderList(
            enabled = settings.playerActions,
            all = PlayerAction.entries,
            label = { it.title() },
            icon = { it.icon() },
            maxEnabled = 5,
            minEnabled = 0,
        ) { onChange(settings.copy(playerActions = it)) }
    }
    item {
        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text("Форма кнопки воспроизведения", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 20.dp))
            Row(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PlayShape.entries.forEach { shape ->
                    Box(
                        Modifier.size(52.dp).clip(playShape(shape))
                            .background(if (settings.playShape == shape) Accent else MaterialTheme.colorScheme.surfaceContainerHighest)
                            .clickable { onChange(settings.copy(playShape = shape)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            null,
                            tint = if (settings.playShape == shape) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
    item { SettingSwitch("«Предыдущий» в мини-плеере", "Кнопка назад рядом с паузой", settings.miniShowPrevious) { onChange(settings.copy(miniShowPrevious = it)) } }
    item { SettingsTitle("Компоновка") }
    PlayerLayout.entries.forEach { layout ->
        item { SettingRadio(if (layout == PlayerLayout.COVER_FIRST) "Сначала обложка" else "Сначала управление", settings.playerLayout == layout) { onChange(settings.copy(playerLayout = layout)) } }
    }
    item {
        SectionReset("Сбросить плеер") { defaults ->
            onChange(settings.copy(openPlayerOnTrack = defaults.openPlayerOnTrack, swipeMiniPlayer = defaults.swipeMiniPlayer, showMiniPlayerProgress = defaults.showMiniPlayerProgress, playerBlurBackground = defaults.playerBlurBackground, coverColorBackground = defaults.coverColorBackground, coverAccent = defaults.coverAccent, keepScreenOn = defaults.keepScreenOn, playerActions = defaults.playerActions, playShape = defaults.playShape, miniShowPrevious = defaults.miniShowPrevious, playerLayout = defaults.playerLayout, playerArtworkPercent = defaults.playerArtworkPercent, fullPlayerSwipe = defaults.fullPlayerSwipe))
        }
    }
}

private class LyricsPreset(
    val title: String,
    val subtitle: String,
    val apply: (AppSettings) -> AppSettings,
) {
    fun matches(settings: AppSettings): Boolean = apply(settings) == settings
}

private val LYRICS_PRESETS = listOf(
    LyricsPreset("Как в обмерах", "29 sp, слева, строка на 38% высоты") { s ->
        val d = AppSettings()
        s.copy(
            lyricsFontSize = d.lyricsFontSize, lyricsLineHeight = d.lyricsLineHeight,
            lyricsLineSpacing = d.lyricsLineSpacing, lyricsHorizontalPadding = d.lyricsHorizontalPadding,
            lyricsAlignment = d.lyricsAlignment, lyricsAnchorPercent = d.lyricsAnchorPercent,
            lyricsActiveScale = d.lyricsActiveScale, boldActiveLyrics = d.boldActiveLyrics,
        )
    },
    LyricsPreset("Крупно", "Для чтения с расстояния") { s ->
        s.copy(
            lyricsFontSize = 35, lyricsLineHeight = 46, lyricsLineSpacing = 20, lyricsHorizontalPadding = 22,
            lyricsAlignment = LyricsAlignment.LEFT, lyricsAnchorPercent = 36, lyricsActiveScale = 2, boldActiveLyrics = true,
        )
    },
    LyricsPreset("Компактно", "Больше строк на экране") { s ->
        s.copy(
            lyricsFontSize = 23, lyricsLineHeight = 32, lyricsLineSpacing = 10, lyricsHorizontalPadding = 20,
            lyricsAlignment = LyricsAlignment.LEFT, lyricsAnchorPercent = 34, lyricsActiveScale = 1, boldActiveLyrics = true,
        )
    },
    LyricsPreset("По центру", "Как на концертном экране") { s ->
        s.copy(
            lyricsFontSize = 30, lyricsLineHeight = 42, lyricsLineSpacing = 18, lyricsHorizontalPadding = 28,
            lyricsAlignment = LyricsAlignment.CENTER, lyricsAnchorPercent = 42, lyricsActiveScale = 3, boldActiveLyrics = true,
        )
    },
)

private fun LazyListScope.lyricsSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Стиль") }
    item {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = settings.lyricsModern, onClick = { onChange(settings.copy(lyricsModern = true)) }, label = { Text("Новый") })
            FilterChip(selected = !settings.lyricsModern, onClick = { onChange(settings.copy(lyricsModern = false)) }, label = { Text("Классический") })
        }
    }
    item {
        Text(
            if (settings.lyricsModern) "Строка сверху, дальние строки размыты, текст плывёт волной, кнопки прячутся сами"
            else "Прежний вид: строка посередине, всё чётко, кнопки всегда на месте",
            color = SoftText,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    item {
        AnimatedVisibility(settings.lyricsModern) {
            Column {
                SettingSwitch("Размывать дальние строки", "На Android 12 и новее", settings.lyricsBlur) { onChange(settings.copy(lyricsBlur = it)) }
                AnimatedVisibility(settings.lyricsBlur) {
                    SettingSwitch("Резкий текст при листании", "Пока вы листаете, строки не размыты, потом размытие возвращается", settings.lyricsSharpOnScroll) { onChange(settings.copy(lyricsSharpOnScroll = it)) }
                }
                SettingSwitch("Прятать кнопки", "Через 3 секунды без касаний, нажатие возвращает", settings.lyricsAutoHide) { onChange(settings.copy(lyricsAutoHide = it)) }
            }
        }
    }
    item { SettingsTitle("Вид") }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(LYRICS_PRESETS) { preset ->
                FilterChip(
                    selected = preset.matches(settings),
                    onClick = { onChange(preset.apply(settings)) },
                    label = { Text(preset.title) },
                )
            }
        }
    }
    item {
        Text(
            LYRICS_PRESETS.firstOrNull { it.matches(settings) }?.subtitle ?: "Своя настройка — подробности ниже",
            color = SoftText,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    item { SettingsTitle("Поведение") }
    item { SettingSwitch("Сразу открывать текст", "При запуске нового трека", settings.showLyricsByDefault) { onChange(settings.copy(showLyricsByDefault = it)) } }
    item { SettingSwitch("Караоке-заливка", "Плавно заполняет активную строку по словам", settings.karaokeFill) { onChange(settings.copy(karaokeFill = it)) } }
    item {
        AnimatedVisibility(settings.karaokeFill) {
            Column {
                KaraokeFillStyle.entries.forEach { style ->
                    SettingRadio(
                        when (style) {
                            KaraokeFillStyle.SOLID -> "Чистый текст"
                            KaraokeFillStyle.SOFT_GLOW -> "Мягкое свечение"
                            KaraokeFillStyle.WORD_PULSE -> "Лёгкая пульсация слова"
                        },
                        settings.karaokeFillStyle == style,
                    ) { onChange(settings.copy(karaokeFillStyle = style)) }
                }
                SettingRadio("Заливка слева направо", settings.karaokeDirection == KaraokeDirection.HORIZONTAL) {
                    onChange(settings.copy(karaokeDirection = KaraokeDirection.HORIZONTAL))
                }
                SettingRadio("Заливка сверху вниз", settings.karaokeDirection == KaraokeDirection.VERTICAL) {
                    onChange(settings.copy(karaokeDirection = KaraokeDirection.VERTICAL))
                }
            }
        }
    }
    item { SettingSwitch("Нажатие перематывает к строке", "Выключите, если часто случайно нажимаете на текст", settings.lyricsTapToSeek) { onChange(settings.copy(lyricsTapToSeek = it)) } }
    item { SettingSwitch("Следовать за песней", "После ручной прокрутки возвращается через 3 секунды", settings.lyricsAutoScroll) { onChange(settings.copy(lyricsAutoScroll = it)) } }
    item { SettingsTitle("Синхронизация") }
    item {
        SettingSlider("Сдвиг текста", settings.lyricsOffsetMs, -5000..5000, 100, { formatOffset(it) }) {
            onChange(settings.copy(lyricsOffsetMs = it))
        }
    }
    item { Text("Плюс — текст появляется раньше, минус — позже. Помогает, если строки опаздывают.", color = SoftText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp)) }
    item { ExpandableSection("Тонкая настройка", "Размеры, контраст, свечение и прокрутка") { LyricsFineTuning(settings, onChange) } }
    item {
        SectionReset("Сбросить экран текста") { defaults ->
            onChange(settings.copy(showLyricsByDefault = defaults.showLyricsByDefault, karaokeFill = defaults.karaokeFill, karaokeFillStyle = defaults.karaokeFillStyle, karaokeDirection = defaults.karaokeDirection, fadeLyrics = defaults.fadeLyrics, boldActiveLyrics = defaults.boldActiveLyrics, lyricsFontSize = defaults.lyricsFontSize, lyricsLineHeight = defaults.lyricsLineHeight, lyricsHorizontalPadding = defaults.lyricsHorizontalPadding, lyricsActiveScale = defaults.lyricsActiveScale, lyricsTapToSeek = defaults.lyricsTapToSeek, lyricsLineSpacing = defaults.lyricsLineSpacing, lyricsAnchorPercent = defaults.lyricsAnchorPercent, lyricsInactivePercent = defaults.lyricsInactivePercent, lyricsUnsungPercent = defaults.lyricsUnsungPercent, lyricsSungPercent = defaults.lyricsSungPercent, lyricsGlowBlur = defaults.lyricsGlowBlur, lyricsGlowAlpha = defaults.lyricsGlowAlpha, lyricsScrollSpeed = defaults.lyricsScrollSpeed, lyricsAlignment = defaults.lyricsAlignment, lyricsAutoScroll = defaults.lyricsAutoScroll, lyricsOffsetMs = defaults.lyricsOffsetMs))
        }
    }
}

@Composable
private fun LyricsFineTuning(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Column {
        SettingsTitle("Текст и положение")
        SettingSlider("Размер шрифта", settings.lyricsFontSize, 20..38, 1, { "$it sp" }) { onChange(settings.copy(lyricsFontSize = it)) }
        SettingSlider("Высота строки (перенос)", settings.lyricsLineHeight, 28..64, 2, { "$it sp" }) { onChange(settings.copy(lyricsLineHeight = it)) }
        SettingSlider("Отступ между строками", settings.lyricsLineSpacing, 4..34, 1, { "$it dp" }) { onChange(settings.copy(lyricsLineSpacing = it)) }
        SettingSlider("Поля по бокам", settings.lyricsHorizontalPadding, 8..48, 2, { "$it dp" }) { onChange(settings.copy(lyricsHorizontalPadding = it)) }
        if (settings.lyricsModern) {
            SettingSlider("Активная строка на высоте", settings.lyricsModernAnchor, 10..45, 1, { "$it%" }) { onChange(settings.copy(lyricsModernAnchor = it)) }
        } else {
            SettingSlider("Активная строка на высоте", settings.lyricsAnchorPercent, 25..55, 1, { "$it%" }) { onChange(settings.copy(lyricsAnchorPercent = it)) }
        }
        SettingSlider("Увеличение активной строки", settings.lyricsActiveScale, 0..5, 1, { "+$it%" }) { onChange(settings.copy(lyricsActiveScale = it)) }
        SettingSwitch("Жирная активная строка", null, settings.boldActiveLyrics) { onChange(settings.copy(boldActiveLyrics = it)) }
        Text("Выравнивание", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 20.dp, top = 12.dp))
        LyricsAlignment.entries.forEach { alignment ->
            SettingRadio(if (alignment == LyricsAlignment.LEFT) "По левому краю" else "По центру", settings.lyricsAlignment == alignment) { onChange(settings.copy(lyricsAlignment = alignment)) }
        }
        SettingsTitle("Контраст и свечение")
        SettingSwitch("Затухание к краям", "Оставляет активную строку в фокусе", settings.fadeLyrics) { onChange(settings.copy(fadeLyrics = it)) }
        SettingSlider("Яркость соседних строк", settings.lyricsInactivePercent, 20..70, 5, { "$it%" }) { onChange(settings.copy(lyricsInactivePercent = it)) }
        SettingSlider("Ещё не спетая часть", settings.lyricsUnsungPercent, 40..85, 5, { "$it%" }) { onChange(settings.copy(lyricsUnsungPercent = it)) }
        SettingSlider("Спетая часть", settings.lyricsSungPercent, 70..125, 5, { "$it%" }) { onChange(settings.copy(lyricsSungPercent = it)) }
        SettingSlider("Сила свечения", settings.lyricsGlowAlpha, 0..45, 5, { "$it%" }) { onChange(settings.copy(lyricsGlowAlpha = it)) }
        SettingSlider("Размытие свечения", settings.lyricsGlowBlur, 0..18, 1, { "$it dp" }) { onChange(settings.copy(lyricsGlowBlur = it)) }
        SettingsTitle("Прокрутка")
        SettingSlider("Длительность прокрутки", settings.lyricsScrollSpeed, 120..900, 60, { "$it мс" }) { onChange(settings.copy(lyricsScrollSpeed = it)) }
    }
}

private fun LazyListScope.playbackSettings(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    cacheUsedBytes: Long,
    onClearCache: () -> Unit,
    onRefreshCache: () -> Unit,
) {
    item { SettingsTitle("Качество") }
    item { SettingSwitch("Экономия трафика", "Запрашивает поток с меньшим битрейтом", settings.dataSaver) { onChange(settings.copy(dataSaver = it)) } }
    item { SettingSwitch("Предзагрузка следующего трека", "Следующий трек начинается без паузы", settings.preloadNextTracks) { onChange(settings.copy(preloadNextTracks = it)) } }
    item { SettingSwitch("Выравнивать громкость", "Тихие и громкие треки звучат одинаково, по данным Яндекса", settings.normalizeVolume) { onChange(settings.copy(normalizeVolume = it)) } }
    item { SettingsTitle("Рекомендации") }
    item { SettingSwitch("Приватный режим", "Прослушивания не уходят в Яндекс и не влияют на «Мою волну». Локальная статистика считается", settings.privateSession) { onChange(settings.copy(privateSession = it)) } }
    item { SettingSwitch("Без откровенных треков", "Пропускать треки с пометкой E в волне и при запуске списков", settings.hideExplicit) { onChange(settings.copy(hideExplicit = it)) } }
    item { SettingsTitle("Кэш и офлайн") }
    item {
        androidx.compose.runtime.LaunchedEffect(Unit) { onRefreshCache() }
        SettingSlider(
            "Размер кэша",
            settings.cacheSizeMb,
            0..8192,
            512,
            { if (it == 0) "Выключен" else formatSize(it * 1024L * 1024L) },
        ) { onChange(settings.copy(cacheSizeMb = it)) }
    }
    item {
        Text(
            "Прослушанные треки и плейлисты, сохранённые для офлайна, играют без сети. " +
                "Когда место заканчивается, первыми удаляются давно не игравшие. Новый размер применится после перезапуска приложения.",
            color = SoftText,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
    }
    item {
        SettingsActionRow(
            Icons.Filled.DeleteSweep,
            "Очистить кэш",
            "Сейчас занято ${formatSize(cacheUsedBytes)}",
            cacheUsedBytes > 0,
            onClearCache,
        )
    }
    item { SectionReset("Сбросить воспроизведение") { defaults -> onChange(settings.copy(dataSaver = defaults.dataSaver, preloadNextTracks = defaults.preloadNextTracks, cacheSizeMb = defaults.cacheSizeMb)) } }
}

private fun LazyListScope.aboutSettings(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    appVersion: String,
    availableUpdate: AppRelease?,
    updateChecking: Boolean,
    updateProgress: Float?,
    onCheckUpdates: () -> Unit,
    onInstallUpdate: (AppRelease) -> Unit,
) {
    item {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { context -> android.widget.ImageView(context).apply { setImageResource(ru.muz.R.mipmap.ic_launcher) } },
                modifier = Modifier.size(56.dp),
            )
            Column(Modifier.padding(start = 14.dp)) {
                Text("Aria", fontWeight = FontWeight.Black, fontSize = 22.sp)
                Text("Версия $appVersion", color = SoftText, fontSize = 13.sp)
            }
        }
    }
    item { SettingsTitle("Обновления") }
    item {
        SettingSwitch("Проверять обновления", "Раз в несколько часов смотрит релизы на GitHub", settings.autoCheckUpdates) {
            onChange(settings.copy(autoCheckUpdates = it))
        }
    }
    if (availableUpdate != null) {
        item {
            SettingsActionRow(
                Icons.Filled.SystemUpdate,
                if (updateProgress != null) "Скачиваем… ${(updateProgress * 100).toInt()}%" else "Установить Aria ${availableUpdate.version}",
                "Что нового в этой версии",
                updateProgress == null,
            ) { onInstallUpdate(availableUpdate) }
        }
        item {
            ReleaseNotes(availableUpdate.notes, MaterialTheme.colorScheme.onSurface, Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp), collapsed = 6)
        }
    }
    item {
        SettingsActionRow(
            Icons.Filled.Sync,
            if (updateChecking) "Проверяем…" else "Проверить сейчас",
            null,
            !updateChecking,
            onCheckUpdates,
        )
    }
    item {
        val context = androidx.compose.ui.platform.LocalContext.current
        SettingsActionRow(Icons.Filled.Code, "Исходный код на GitHub", "github.com/Dev-Angel-7553/Aria-Android", true) {
            ru.muz.update.UpdateInstaller.openPage(context, "https://github.com/Dev-Angel-7553/Aria-Android")
        }
    }
}

private fun formatSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return when {
        bytes <= 0 -> "0 МБ"
        mb >= 1024 -> String.format(java.util.Locale.ROOT, "%.1f ГБ", mb / 1024).replace('.', ',')
        else -> "${mb.roundToInt()} МБ"
    }
}

@Composable
private fun AppearancePreview() {
    val corners = LocalCornerRadiusScale.current
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(22.dp * corners))
            .background(scheme.surfaceContainerHigh).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.PlayCircle, null, tint = scheme.onPrimary)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text("Предпросмотр темы", fontWeight = FontWeight.Bold)
            Text("Цвета применяются сразу", color = SoftText, fontSize = 13.sp)
        }
        Box(
            Modifier.clip(RoundedCornerShape(10.dp * corners)).background(scheme.secondaryContainer)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) { Text("Выбрано", color = scheme.onSecondaryContainer, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ListPreview(settings: AppSettings) {
    val scale = settings.listScalePercent / 100f
    val corners = LocalCornerRadiusScale.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp * corners))
            .background(Elevated).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size((if (settings.compactLists) 46.dp else 52.dp) * scale)
                .clip(RoundedCornerShape(11.dp * settings.artworkCornerPercent / 100f * corners))
                .background(Accent),
        )
        Column(Modifier.padding(start = 12.dp)) {
            Text("Название трека", fontWeight = FontWeight.SemiBold)
            Text("Исполнитель · Альбом", color = SoftText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MiniPlayerPreview(settings: AppSettings) {
    val preview = PlayerState(title = "Название трека", artist = "Исполнитель", positionMs = 42_000, durationMs = 100_000)
    Box(Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
        CompactPlayer(preview, {}, {}, {}, {}, false, settings.showMiniPlayerProgress)
    }
}

@Composable
private fun LyricsPreview(settings: AppSettings) {
    var position by remember { mutableStateOf(0L) }
    var loops by remember { mutableStateOf(0L) }
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            position = 8_500L
            return@LaunchedEffect
        }
        val start = androidx.compose.runtime.withFrameMillis { it }
        while (true) {
            val now = androidx.compose.runtime.withFrameMillis { it }
            val t = (now - start) % SampleLyrics.lengthMs
            if (t < position) loops++
            position = t
        }
    }
    val tint = Accent
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp).height(300.dp)
            .clip(RoundedCornerShape(24.dp * LocalCornerRadiusScale.current)),
    ) {
        if (settings.coverColorBackground) {
            MovingCoverBackground(tint, strength = 1f)
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow))
        }
        ru.muz.lyrics.LyricsScreen(
            lyrics = SampleLyrics.lyrics,
            positionMs = position,
            onSeek = {},
            settings = lyricsSettingsFor(settings, scale = 0.82f),
            background = androidx.compose.ui.graphics.Color.Transparent,
            discontinuity = loops,
            modifier = Modifier.fillMaxSize(),
        )
        Text(
            "Так будет выглядеть текст",
            color = SoftText,
            fontSize = 11.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp),
        )
    }
}

@Composable
private fun SectionReset(label: String, onReset: (AppSettings) -> Unit) {
    TextButton(onClick = { onReset(AppSettings()) }, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Text(label)
    }
}

private fun LazyListScope.accountSettings(
    account: YandexClient.Account?,
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onLogout: () -> Unit,
    onSyncNow: () -> Unit,
    syncBusy: Boolean,
) {
    item { AccountCard(account, settings.reportListening, onClick = null) }
    item { SettingsTitle("Яндекс Музыка") }
    item {
        SettingSwitch(
            "Отправлять прослушивания в Яндекс",
            "Треки попадают в историю аккаунта, а «Моя волна» учится на том, что вы слушаете и пропускаете здесь",
            settings.reportListening,
        ) { onChange(settings.copy(reportListening = it)) }
    }
    item { SettingsTitle("Синхронизация библиотеки") }
    item {
        SettingSwitch("Автосинхронизация", "Обновлять библиотеку по расписанию, используя локальный кэш", settings.syncIntervalMinutes > 0) {
            onChange(settings.copy(syncIntervalMinutes = if (it) 60 else 0))
        }
    }
    item {
        AnimatedVisibility(settings.syncIntervalMinutes > 0) {
            SettingSlider(
                "Проверять библиотеку",
                settings.syncIntervalMinutes.coerceIn(30, 360),
                30..360,
                30,
                { "раз в ${formatInterval(it)}" },
            ) { onChange(settings.copy(syncIntervalMinutes = it)) }
        }
    }
    item { Text("Кэш открывается сразу после входа, свежие данные подмешиваются в фоне.", color = SoftText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) }
    item {
        SettingsActionRow(Icons.Filled.Sync, if (syncBusy) "Синхронизация…" else "Синхронизировать сейчас", null, !syncBusy, onSyncNow)
    }
    item { SettingsTitle("Выход") }
    item { TextButton(onClick = onLogout, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Выйти и удалить токен", color = MaterialTheme.colorScheme.error) } }
}

private fun LazyListScope.exportSettings(
    exportBusy: Boolean,
    onExportCollection: (share: Boolean) -> Unit,
    onImportCsv: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    item { SettingsTitle("Импорт плейлистов") }
    item {
        SettingsActionRow(
            Icons.Filled.FileUpload,
            "Импорт из CSV",
            "Файл из Spotify, TuneMyMusic, Exportify или список «Исполнитель — Название». Треки найдутся в Яндексе, плейлисты создадутся в вашем аккаунте",
            true,
            onImportCsv,
        )
    }
    item { SettingsTitle("Экспорт коллекции") }
    item {
        SettingsActionRow(
            Icons.Filled.FileDownload,
            if (exportBusy) "Готовим CSV…" else "Сохранить в Загрузки",
            "«Мне нравится» и все плейлисты в одном CSV. Понимают TuneMyMusic, Soundiiz и Excel",
            !exportBusy,
        ) { onExportCollection(false) }
    }
    item { SettingsActionRow(Icons.Filled.Share, "Отправить файлом", "Через Telegram, почту или любое приложение", !exportBusy) { onExportCollection(true) } }
    item { SettingsTitle("Настройки") }
    item { SettingsActionRow(Icons.Filled.Save, "Сохранить настройки в файл", "Тема, плеер, текст, главная — всё, кроме аккаунта", true, onExportSettings) }
    item { SettingsActionRow(Icons.Filled.Restore, "Загрузить настройки из файла", "Например, с другого телефона", true, onImportSettings) }
}

private fun LazyListScope.homeSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Блоки на главной") }
    item {
        ReorderList(
            enabled = settings.homeSections,
            all = HomeSection.entries,
            label = { homeSectionName(it) },
            icon = { homeSectionIcon(it) },
            maxEnabled = null,
            minEnabled = 0,
        ) { onChange(settings.copy(homeSections = it)) }
    }
    item { SettingsTitle("Вкладки внизу") }
    item {
        ReorderList(
            enabled = settings.tabs,
            all = AppTab.entries,
            label = { tabName(it) },
            icon = { tabIcon(it) },
            maxEnabled = null,
            minEnabled = 1,
        ) { tabs ->
            onChange(settings.copy(tabs = tabs, startTab = if (settings.startTab in tabs) settings.startTab else tabs.first()))
        }
    }
    item { SettingsTitle("Открывать при запуске") }
    settings.tabs.forEach { tab ->
        item { SettingRadio(tabName(tab), settings.startTab == tab) { onChange(settings.copy(startTab = tab)) } }
    }
    item {
        SectionReset("Сбросить главную и вкладки") { defaults ->
            onChange(settings.copy(homeSections = defaults.homeSections, tabs = defaults.tabs, startTab = defaults.startTab))
        }
    }
}

private fun homeSectionName(section: HomeSection): String = when (section) {
    HomeSection.WAVE -> "Моя волна"
    HomeSection.RECENT -> "Недавно добавленное"
    HomeSection.PLAYLISTS -> "Ваши плейлисты"
    HomeSection.CHART -> "Чарт"
}

private fun homeSectionIcon(section: HomeSection): ImageVector = when (section) {
    HomeSection.WAVE -> Icons.Filled.GraphicEq
    HomeSection.RECENT -> Icons.Filled.History
    HomeSection.PLAYLISTS -> Icons.Filled.QueueMusic
    HomeSection.CHART -> Icons.Filled.TrendingUp
}

private fun tabName(tab: AppTab): String = when (tab) {
    AppTab.HOME -> "Главная"
    AppTab.LIBRARY -> "Коллекция"
    AppTab.SEARCH -> "Поиск"
    AppTab.STATS -> "Статистика"
}

private fun tabIcon(tab: AppTab): ImageVector = when (tab) {
    AppTab.HOME -> Icons.Filled.Home
    AppTab.LIBRARY -> Icons.Filled.LibraryMusic
    AppTab.SEARCH -> Icons.Filled.Search
    AppTab.STATS -> Icons.Filled.Equalizer
}

private fun fontName(font: AppFont): String = when (font) {
    AppFont.SYSTEM -> "Системный"
    AppFont.SANS -> "Гротеск"
    AppFont.SERIF -> "С засечками"
    AppFont.MONO -> "Моноширинный"
    AppFont.CONDENSED -> "Узкий"
}

@Composable
private fun <T> ReorderList(
    enabled: List<T>,
    all: List<T>,
    label: (T) -> String,
    icon: (T) -> ImageVector,
    maxEnabled: Int?,
    minEnabled: Int,
    onChange: (List<T>) -> Unit,
) {
    val disabled = all.filter { it !in enabled }
    Column(Modifier.fillMaxWidth()) {
        enabled.forEachIndexed { index, value ->
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon(value), null, tint = Accent)
                Text(label(value), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 14.dp))
                IconButton(
                    onClick = { onChange(enabled.toMutableList().apply { add(index - 1, removeAt(index)) }) },
                    enabled = index > 0,
                ) { Icon(Icons.Filled.KeyboardArrowUp, "Выше") }
                IconButton(
                    onClick = { onChange(enabled.toMutableList().apply { add(index + 1, removeAt(index)) }) },
                    enabled = index < enabled.lastIndex,
                ) { Icon(Icons.Filled.KeyboardArrowDown, "Ниже") }
                AriaSwitch(
                    checked = true,
                    enabled = enabled.size > minEnabled,
                    onCheckedChange = { onChange(enabled - value) },
                )
            }
        }
        disabled.forEach { value ->
            val canAdd = maxEnabled == null || enabled.size < maxEnabled
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon(value), null, tint = SoftText)
                Text(label(value), color = SoftText, modifier = Modifier.weight(1f).padding(start = 14.dp))
                AriaSwitch(checked = false, enabled = canAdd, onCheckedChange = { onChange(enabled + value) })
            }
        }
    }
}

@Composable
private fun ColorPicker(color: Int, onChange: (Int) -> Unit) {
    val hsl = remember(color) { ru.muz.theme.ColorMath.hsl(color) }
    var hue by remember(color) { mutableStateOf(hsl[0]) }
    var saturation by remember(color) { mutableStateOf(hsl[1]) }
    var lightness by remember(color) { mutableStateOf(hsl[2]) }
    var hex by remember(color) { mutableStateOf(String.format(java.util.Locale.ROOT, "%06X", color and 0xFFFFFF)) }
    fun commit() = onChange(ru.muz.theme.ColorMath.fromHsl(hue, saturation, lightness))
    val preview = Color(ru.muz.theme.ColorMath.fromHsl(hue, saturation, lightness))
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(preview))
            androidx.compose.material3.OutlinedTextField(
                value = hex,
                onValueChange = { raw ->
                    val clean = raw.removePrefix("#").uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                    hex = clean
                    if (clean.length == 6) onChange(0xFF000000.toInt() or clean.toInt(16))
                },
                prefix = { Text("#") },
                singleLine = true,
                label = { Text("HEX") },
                modifier = Modifier.padding(start = 16.dp).width(160.dp),
            )
        }
        Box(
            Modifier.fillMaxWidth().padding(top = 14.dp).height(10.dp).clip(RoundedCornerShape(5.dp))
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        (0..12).map { Color(ru.muz.theme.ColorMath.fromHsl(it * 30f, 0.85f, 0.6f)) },
                    ),
                ),
        )
        StyledSlider(value = hue, onValueChange = { hue = it }, onValueChangeFinished = ::commit, valueRange = 0f..359f)
        Text("Насыщенность", color = SoftText, fontSize = 13.sp)
        StyledSlider(value = saturation, onValueChange = { saturation = it }, onValueChangeFinished = ::commit, valueRange = 0f..1f)
        Text("Яркость", color = SoftText, fontSize = 13.sp)
        StyledSlider(value = lightness, onValueChange = { lightness = it }, onValueChangeFinished = ::commit, valueRange = 0.2f..0.85f)
    }
}

@Composable
private fun AccountCard(account: YandexClient.Account?, reportListening: Boolean, onClick: (() -> Unit)?) {
    val scheme = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp * LocalCornerRadiusScale.current))
            .background(scheme.surfaceContainerHigh)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
            Text(account?.displayName?.firstOrNull()?.uppercase() ?: "Я", color = scheme.onPrimary, fontWeight = FontWeight.Black, fontSize = 20.sp)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(account?.displayName ?: "Яндекс Музыка", fontWeight = FontWeight.Bold, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(account?.login?.takeIf { it.isNotBlank() }, if (reportListening) "прослушивания в Яндексе" else null).joinToString(" · "),
                color = SoftText,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (onClick != null) Icon(Icons.Filled.ChevronRight, null, tint = SoftText)
    }
}

@Composable
private fun SettingsSearchField(query: String, onQuery: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = true,
        placeholder = { Text("Поиск по настройкам") },
        leadingIcon = { Icon(Icons.Filled.Search, null) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQuery("") }) { Icon(Icons.Filled.Close, "Очистить") } }
        } else null,
        shape = RoundedCornerShape(28.dp * LocalCornerRadiusScale.current),
    )
}

private class SettingsEntry(val title: String, val page: SettingsPage, val keywords: String = "")

private val SETTINGS_INDEX = listOf(
    SettingsEntry("Тема", SettingsPage.APPEARANCE, "тёмная светлая amoled чёрная системная"),
    SettingsEntry("Monet / динамические цвета", SettingsPage.APPEARANCE, "обои цвет"),
    SettingsEntry("Цвета под песню", SettingsPage.APPEARANCE, "обложка трек оттенок акцент"),
    SettingsEntry("Цвет акцента", SettingsPage.APPEARANCE, "жёлтый синий розовый зелёный оранжевый"),
    SettingsEntry("Скругление интерфейса", SettingsPage.APPEARANCE, "углы форма"),
    SettingsEntry("Скругление обложек", SettingsPage.APPEARANCE, "углы форма"),
    SettingsEntry("Подписи под значками", SettingsPage.APPEARANCE, "навигация нижняя панель"),
    SettingsEntry("Скорость анимаций", SettingsPage.ANIMATION),
    SettingsEntry("Уменьшить движение", SettingsPage.ANIMATION, "анимации выключить"),
    SettingsEntry("Переходы в плеере", SettingsPage.ANIMATION),
    SettingsEntry("Анимация загрузки", SettingsPage.ANIMATION),
    SettingsEntry("Плавное появление обложек", SettingsPage.ANIMATION),
    SettingsEntry("Открывать плеер при выборе", SettingsPage.PLAYER),
    SettingsEntry("Свайпы мини-плеера", SettingsPage.PLAYER, "жесты"),
    SettingsEntry("Прогресс мини-плеера", SettingsPage.PLAYER),
    SettingsEntry("Жесты полного плеера", SettingsPage.PLAYER, "свайпы"),
    SettingsEntry("Не выключать экран", SettingsPage.PLAYER, "экран"),
    SettingsEntry("Цвет обложки в фоне", SettingsPage.PLAYER, "градиент фон"),
    SettingsEntry("Акцент от обложки", SettingsPage.PLAYER, "цвет кнопок"),
    SettingsEntry("Размер обложки", SettingsPage.PLAYER),
    SettingsEntry("Компоновка плеера", SettingsPage.PLAYER, "сначала обложка управление"),
    SettingsEntry("Вид текста песни", SettingsPage.LYRICS, "пресет крупно компактно по центру"),
    SettingsEntry("Караоке-заливка", SettingsPage.LYRICS, "текст свечение glow"),
    SettingsEntry("Сдвиг текста", SettingsPage.LYRICS, "синхронизация опаздывает раньше позже"),
    SettingsEntry("Размер шрифта текста", SettingsPage.LYRICS, "кегль"),
    SettingsEntry("Сразу открывать текст", SettingsPage.LYRICS),
    SettingsEntry("Экономия трафика", SettingsPage.PLAYBACK, "качество битрейт мобильный интернет"),
    SettingsEntry("Предзагрузка следующего трека", SettingsPage.PLAYBACK),
    SettingsEntry("Размер кэша", SettingsPage.PLAYBACK, "офлайн память"),
    SettingsEntry("Очистить кэш", SettingsPage.PLAYBACK, "память место"),
    SettingsEntry("Компактные списки", SettingsPage.LISTS),
    SettingsEntry("Масштаб строк", SettingsPage.LISTS),
    SettingsEntry("Треков за раз", SettingsPage.LISTS, "страница"),
    SettingsEntry("История поиска", SettingsPage.LISTS),
    SettingsEntry("Экспорт в CSV", SettingsPage.EXPORT, "tunemymusic soundiiz excel перенос плейлисты"),
    SettingsEntry("Прослушивания в Яндексе", SettingsPage.ACCOUNT, "история моя волна"),
    SettingsEntry("Синхронизация библиотеки", SettingsPage.ACCOUNT, "обновить"),
    SettingsEntry("Выйти из аккаунта", SettingsPage.ACCOUNT, "токен"),
    SettingsEntry("Обновления", SettingsPage.ABOUT, "версия github"),
    SettingsEntry("Свой цвет", SettingsPage.APPEARANCE, "палитра hex акцент"),
    SettingsEntry("Шрифт", SettingsPage.APPEARANCE, "гарнитура засечки моноширинный"),
    SettingsEntry("Размер текста интерфейса", SettingsPage.APPEARANCE, "крупнее мельче"),
    SettingsEntry("Кнопки плеера", SettingsPage.PLAYER, "под ползунком таймер скорость цитата"),
    SettingsEntry("Форма кнопки воспроизведения", SettingsPage.PLAYER, "круг квадрат"),
    SettingsEntry("Блоки на главной", SettingsPage.HOME, "порядок волна чарт плейлисты"),
    SettingsEntry("Вкладки внизу", SettingsPage.HOME, "навигация статистика поиск"),
    SettingsEntry("Импорт из CSV", SettingsPage.EXPORT, "spotify перенос плейлист загрузить"),
    SettingsEntry("Резервная копия настроек", SettingsPage.EXPORT, "сохранить загрузить перенос телефон"),
)

private fun searchSettings(query: String): List<SettingsEntry> {
    val words = query.trim().lowercase().split(' ').filter { it.isNotBlank() }
    return SETTINGS_INDEX.filter { entry ->
        val haystack = "${entry.title} ${entry.page.title} ${entry.keywords}".lowercase()
        words.all { haystack.contains(it) }
    }
}

private fun SettingsPage.icon(): ImageVector = when (this) {
    SettingsPage.APPEARANCE -> Icons.Filled.Palette
    SettingsPage.ANIMATION -> Icons.Filled.AutoAwesome
    SettingsPage.LISTS -> Icons.Filled.Dashboard
    SettingsPage.HOME -> Icons.Filled.Home
    SettingsPage.PLAYER -> Icons.Filled.PlayCircle
    SettingsPage.LYRICS -> Icons.Filled.Lyrics
    SettingsPage.PLAYBACK -> Icons.Filled.Headphones
    SettingsPage.ACCOUNT -> Icons.Filled.AccountCircle
    SettingsPage.EXPORT -> Icons.Filled.ImportExport
    SettingsPage.ABOUT -> Icons.Filled.Info
    SettingsPage.ROOT -> Icons.Filled.Search
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Назад") }
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingsNavRow(icon: ImageVector, title: String, value: String, tint: Color? = null, onClick: () -> Unit) {
    val colored = tint != null && LocalAppStyle.current != ru.muz.settings.AppStyle.MINIMAL
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp)
                .clip(if (colored) RoundedCornerShape(12.dp) else CircleShape)
                .background(if (colored) tint!! else MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = if (colored) Color.White else MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp)) }
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, color = SoftText, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = SoftText)
    }
}

@Composable
private fun SettingsActionRow(icon: ImageVector, title: String, subtitle: String?, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (enabled) Accent else SoftText)
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = if (enabled) MaterialTheme.colorScheme.onSurface else SoftText)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = SoftText, fontSize = 13.sp)
        }
    }
}

@Composable
private fun ExpandableSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = Accent)
                Text(subtitle, color = SoftText, fontSize = 13.sp)
            }
            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, if (expanded) "Свернуть" else "Развернуть", tint = SoftText)
        }
        AnimatedVisibility(expanded) { content() }
    }
}

@Composable
private fun SettingsTitle(value: String) {
    Text(value, color = Accent, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp))
}

@Composable
private fun SettingSwitch(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = SoftText, fontSize = 13.sp)
        }
        AriaSwitch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingRadio(title: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(title, modifier = Modifier.padding(start = 8.dp), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SettingSlider(
    title: String,
    value: Int,
    range: IntRange,
    step: Int,
    label: (Int) -> String,
    onChange: (Int) -> Unit,
) {
    fun snap(raw: Float): Int =
        (range.first + ((raw - range.first) / step).roundToInt() * step).coerceIn(range.first, range.last)
    val stops = ((range.last - range.first) / step - 1).coerceAtLeast(0)
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(label(value), color = SoftText)
        }
        StyledSlider(
            value = value.toFloat(),
            onValueChange = { raw -> snap(raw).let { if (it != value) onChange(it) } },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = stops,
        )
    }
}

private fun formatOffset(ms: Int): String = when {
    ms == 0 -> "0 мс"
    kotlin.math.abs(ms) >= 1000 -> String.format(java.util.Locale.ROOT, "%+.1f с", ms / 1000f).replace('.', ',')
    else -> String.format(java.util.Locale.ROOT, "%+d мс", ms)
}

private fun formatInterval(minutes: Int): String = when {
    minutes < 60 -> "$minutes мин"
    minutes % 60 == 0 -> "${minutes / 60} ч"
    else -> "${minutes / 60} ч ${minutes % 60} мин"
}

private fun themeName(mode: ThemeMode): String = when (mode) {
    ThemeMode.SYSTEM -> "Как в системе"
    ThemeMode.LIGHT -> "Светлая"
    ThemeMode.DARK -> "Тёмная"
    ThemeMode.AMOLED -> "AMOLED — чистый чёрный"
}

private fun themeLabel(settings: AppSettings): String = buildString {
    append(themeName(settings.themeMode))
    if (settings.useDynamicColors) append(" · Monet") else append(" · ${accentName(settings.accentStyle).lowercase()}")
}

private fun accentName(accent: AccentStyle): String = when (accent) {
    AccentStyle.YELLOW -> "Жёлтый"
    AccentStyle.ORANGE -> "Оранжевый"
    AccentStyle.PINK -> "Розовый"
    AccentStyle.BLUE -> "Синий"
    AccentStyle.GREEN -> "Зелёный"
    AccentStyle.CUSTOM -> "Свой цвет"
}

@Composable
private fun StylePicker(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(vertical = 6.dp),
    ) {
        items(ru.muz.settings.AppStyle.entries.toList()) { style ->
            val chosen = settings.appStyle == style
            Column(
                Modifier.width(140.dp).pressScale().clip(RoundedCornerShape(22.dp))
                    .border(if (chosen) 2.dp else 1.dp, if (chosen) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
                    .clickable { onChange(stylePreset(settings, style)) }
                    .padding(8.dp),
            ) {
                StyleThumb(style, Modifier.fillMaxWidth().height(150.dp))
                Row(Modifier.padding(top = 8.dp, start = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(styleName(style), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    if (chosen) Icon(Icons.Filled.Check, null, tint = Accent, modifier = Modifier.size(18.dp))
                }
                Text(styleHint(style), color = SoftText, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun StyleThumb(style: ru.muz.settings.AppStyle, modifier: Modifier) {
    val base = MaterialTheme.colorScheme
    val scheme = if (style == ru.muz.settings.AppStyle.MINIMAL) {
        val ink = base.onSurface
        base.copy(
            primary = ink,
            onPrimary = base.surface,
            primaryContainer = ink.copy(alpha = 0.12f).compositeOver(base.surface),
            onPrimaryContainer = ink,
            secondaryContainer = ink.copy(alpha = 0.10f).compositeOver(base.surface),
            onSecondaryContainer = ink,
        )
    } else if (style == ru.muz.settings.AppStyle.GLASS) {
        base.copy(primary = Color(0xFF2F7BFF), primaryContainer = Color(0xFF2F7BFF).copy(alpha = 0.25f).compositeOver(base.surface))
    } else base
    androidx.compose.foundation.layout.BoxWithConstraints(modifier.clip(RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
        val full = 300.dp
        val scale = maxWidth / full
        val height = full * (maxHeight / maxWidth)
        Box(
            Modifier.requiredSize(full, height).graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        ) {
            MaterialTheme(colorScheme = scheme) {
                androidx.compose.runtime.CompositionLocalProvider(
                    LocalAppStyle provides style,
                    LocalReduceMotion provides true,
                ) { MiniApp(style) }
            }
        }
        Box(Modifier.matchParentSize())
    }
}

@Composable
private fun MiniApp(style: ru.muz.settings.AppStyle) {
    val scheme = MaterialTheme.colorScheme
    val glass = style == ru.muz.settings.AppStyle.GLASS
    val backdrop = if (glass) rememberGlassBackdrop() else null
    val covers = listOf(
        listOf(Color(0xFFFF6A88), Color(0xFFFFB36B)),
        listOf(Color(0xFF6A8BFF), Color(0xFF9B6BFF)),
        listOf(Color(0xFF2FD3A1), Color(0xFF1C8CFF)),
        listOf(Color(0xFFFFC94A), Color(0xFFFF6A3D)),
    )
    Box(Modifier.fillMaxSize().background(if (style == ru.muz.settings.AppStyle.MINIMAL) Color(0xFF0B0B0B) else scheme.background)) {
        Box(Modifier.fillMaxSize().glassSource(backdrop)) {
            if (glass) MovingCoverBackground(scheme.primary, strength = 0.8f)
            Column(Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 20.dp)) {
                Text("Главная", fontSize = if (glass) 32.sp else 28.sp, fontWeight = FontWeight.Black, color = scheme.onSurface)
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.fillMaxWidth().height(110.dp)
                        .clip(if (style == ru.muz.settings.AppStyle.MATERIAL_YOU) RoundedCornerShape(34.dp) else RoundedCornerShape(if (style == ru.muz.settings.AppStyle.MINIMAL) 6.dp else 22.dp))
                        .background(
                            when (style) {
                                ru.muz.settings.AppStyle.MINIMAL -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF1C1C1C), Color(0xFF1C1C1C)))
                                ru.muz.settings.AppStyle.MATERIAL_YOU -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(scheme.primaryContainer, scheme.tertiaryContainer))
                                else -> androidx.compose.ui.graphics.Brush.linearGradient(listOf(scheme.primary, scheme.primary.copy(alpha = 0.55f)))
                            },
                        )
                        .padding(16.dp),
                ) {
                    Text("Моя волна", fontSize = 20.sp, fontWeight = FontWeight.Black, color = if (style == ru.muz.settings.AppStyle.MATERIAL_YOU) scheme.onPrimaryContainer else Color.White)
                    Box(
                        Modifier.align(Alignment.BottomEnd).size(44.dp)
                            .clip(if (style == ru.muz.settings.AppStyle.MATERIAL_YOU) CookieShape else if (style == ru.muz.settings.AppStyle.MINIMAL) RoundedCornerShape(6.dp) else CircleShape)
                            .background(if (style == ru.muz.settings.AppStyle.MATERIAL_YOU) scheme.primary else Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, null, tint = if (style == ru.muz.settings.AppStyle.MATERIAL_YOU) scheme.onPrimary else Color.Black)
                    }
                }
                Spacer(Modifier.height(12.dp))
                covers.forEachIndexed { i, colors ->
                    Row(Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(46.dp).clip(RoundedCornerShape(if (style == ru.muz.settings.AppStyle.MINIMAL) 4.dp else 10.dp))
                                .background(
                                    if (style == ru.muz.settings.AppStyle.MINIMAL) androidx.compose.ui.graphics.Brush.linearGradient(listOf(Color(0xFF3A3A3A), Color(0xFF222222)))
                                    else androidx.compose.ui.graphics.Brush.linearGradient(colors),
                                ),
                        )
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(listOf("Ночной город", "Огни", "До рассвета", "Домой")[i], fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface)
                            Text("Исполнитель", fontSize = 13.sp, color = scheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                }
            }
        }
        Box(Modifier.align(Alignment.BottomCenter)) {
            AriaTabBar(MainTab.entries.toList(), MainTab.HOME, {}, true, backdrop, false, insets = false)
        }
    }
}

@Composable
private fun QuickToggles(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        QuickTile(Icons.Filled.VisibilityOff, "Приватно", settings.privateSession, Modifier.weight(1f)) { onChange(settings.copy(privateSession = !settings.privateSession)) }
        QuickTile(Icons.Filled.DataSaverOn, "Экономия", settings.dataSaver, Modifier.weight(1f)) { onChange(settings.copy(dataSaver = !settings.dataSaver)) }
        QuickTile(Icons.Filled.MotionPhotosOff, "Без анимаций", settings.reduceMotion, Modifier.weight(1f)) { onChange(settings.copy(reduceMotion = !settings.reduceMotion)) }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, title: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val background by androidx.compose.animation.animateColorAsState(if (on) Accent else scheme.surfaceContainerHigh, label = "quick-bg")
    val content by androidx.compose.animation.animateColorAsState(if (on) scheme.onPrimary else scheme.onSurface, label = "quick-fg")
    Column(
        modifier.pressScale().clip(RoundedCornerShape(20.dp)).background(background).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, tint = content, modifier = Modifier.size(22.dp))
        Text(title, color = content, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
        Text(if (on) "Вкл" else "Выкл", color = content.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}

