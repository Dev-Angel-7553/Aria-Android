package ru.muz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.FilterChip
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ru.muz.lyrics.KaraokeDirection
import ru.muz.lyrics.KaraokeFillStyle
import ru.muz.player.PlayerState
import ru.muz.settings.AccentStyle
import ru.muz.settings.AppFont
import ru.muz.settings.AppSettings
import ru.muz.settings.AppStyle
import ru.muz.settings.AppTab
import ru.muz.settings.DoubleTapAction
import ru.muz.settings.HomeSection
import ru.muz.settings.LyricsAlignment
import ru.muz.settings.PlayShape
import ru.muz.settings.PlayerAction
import ru.muz.settings.PlayerLayout
import ru.muz.settings.SeekStyle
import ru.muz.settings.ThemeMode
import ru.muz.update.AppRelease
import ru.muz.yandex.YandexClient

private enum class SettingsPage(val title: String, val depth: Int) {
    ROOT("Настройки", 0),
    APPEARANCE("Оформление", 1),
    LIBRARY("Главная и списки", 1),
    PLAYER("Плеер", 1),
    LYRICS("Текст песни", 1),
    PLAYBACK("Звук и офлайн", 1),
    ACCOUNT("Аккаунт", 1),
    SCROBBLING("Подключения", 1),
    EXPORT("Импорт и экспорт", 1),
    ABOUT("О приложении", 1),
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
    var page by rememberSaveable { mutableStateOf(SettingsPage.ROOT) }
    var query by rememberSaveable { mutableStateOf("") }
    var showStorage by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    BackHandler(enabled = page != SettingsPage.ROOT || query.isNotBlank()) {
        if (page != SettingsPage.ROOT) page = SettingsPage.ROOT else query = ""
    }
    val glassStyle = LocalAppStyle.current == AppStyle.GLASS
    val rootList = rememberLazyListState()
    val forwardEnter = pageEnter(true)
    val forwardExit = pageExit(true)
    val backEnter = pageEnter(false)
    val backExit = pageExit(false)
    GlassScaffold(
        topBar = {
            val header: @Composable () -> Unit = {
                SettingsHeader(page.title) { if (page == SettingsPage.ROOT) onBack() else page = SettingsPage.ROOT }
            }
            if (glassStyle) {
                Column(
                    Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp)
                        .glass(LocalGlassBackdrop.current, RoundedCornerShape(28.dp), corner = 28.dp),
                ) { header() }
            } else {
                Column(Modifier.fillMaxWidth().background(AppBackground).statusBarsPadding()) { header() }
            }
        },
        bottomBar = {
            Column {
                PlaybackErrorBanner(player.error)
                AnimatedVisibility(player.title.isNotBlank() && MiniPlayerState.hiddenId != player.mediaId) {
                    CompactPlayer(
                        player, onMiniClick, onPlayPause, onNext, onPrevious,
                        swipeMiniPlayer, showMiniPlayerProgress,
                    )
                }
            }
        },
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = page,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                val forward = targetState.depth >= initialState.depth
                val transform = (if (forward) forwardEnter else backEnter) togetherWith (if (forward) forwardExit else backExit)
                transform.targetContentZIndex = if (forward) 1f else 0f
                transform
            },
            label = "settings-page",
        ) { current ->
            val list = if (current == SettingsPage.ROOT) rootList else rememberLazyListState()
            LazyColumn(
                Modifier.fillMaxSize().then(if (glassStyle) Modifier else Modifier.background(AppBackground)),
                state = list,
                contentPadding = PaddingValues(
                    top = scaffoldPadding.calculateTopPadding() + 4.dp,
                    bottom = scaffoldPadding.calculateBottomPadding() + 28.dp,
                ),
            ) {
                when (current) {
                    SettingsPage.ROOT -> rootSettings(
                        settings, account, availableUpdate, appVersion, cacheUsedBytes, query,
                        onQuery = { query = it },
                        onOpen = { target -> query = ""; page = target },
                        onReset = { confirmReset = true },
                    )
                    SettingsPage.APPEARANCE -> appearanceSettings(settings, onChange)
                    SettingsPage.LIBRARY -> librarySettings(settings, onChange)
                    SettingsPage.PLAYER -> playerSettings(settings, onChange)
                    SettingsPage.LYRICS -> lyricsSettings(settings, onChange)
                    SettingsPage.PLAYBACK -> playbackSettings(settings, onChange, cacheUsedBytes, { showStorage = true }, onClearCache, onRefreshCache)
                    SettingsPage.ABOUT -> aboutSettings(
                        settings, onChange, appVersion, availableUpdate, updateChecking, updateProgress,
                        onCheckUpdates, onInstallUpdate,
                    )
                    SettingsPage.SCROBBLING -> item { ScrobbleConnections(settings.privateSession) }
                    SettingsPage.ACCOUNT -> accountSettings(account, settings, onChange, { confirmLogout = true }, onSyncNow, syncBusy)
                    SettingsPage.EXPORT -> exportSettings(exportBusy, onExportCollection, onImportCsv, onExportSettings, onImportSettings)
                }
            }
        }
    }
    if (showStorage) StorageDialog({ showStorage = false }, onRefreshCache)
    if (confirmReset) {
        AriaDialog(
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
        AriaDialog(
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

private fun LazyListScope.rootSettings(
    settings: AppSettings,
    account: YandexClient.Account?,
    availableUpdate: AppRelease?,
    appVersion: String,
    cacheUsedBytes: Long,
    query: String,
    onQuery: (String) -> Unit,
    onOpen: (SettingsPage) -> Unit,
    onReset: () -> Unit,
) {
    item(key = "account") { AccountCard(account, settings.reportListening) { onOpen(SettingsPage.ACCOUNT) } }
    if (availableUpdate != null) {
        item(key = "update") {
            SettingsCard {
                NavRow(Icons.Rounded.SystemUpdate, "Доступна Aria ${availableUpdate.version}", "Нажмите, чтобы обновить") { onOpen(SettingsPage.ABOUT) }
            }
        }
    }
    item(key = "search") { SettingsSearchField(query, onQuery) }
    if (query.isNotBlank()) {
        val results = searchSettings(query)
        item(key = "results") {
            if (results.isEmpty()) {
                Text("Ничего не нашлось", color = SoftText, modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp))
            } else {
                SettingsCard {
                    results.forEach { entry ->
                        NavRow(entry.page.icon(), entry.title, entry.page.title) { onOpen(entry.page) }
                    }
                }
            }
        }
        return
    }
    item(key = "interface") {
        SettingsCard("Интерфейс") {
            NavRow(Icons.Rounded.Palette, "Оформление", themeLabel(settings)) { onOpen(SettingsPage.APPEARANCE) }
            NavRow(
                Icons.Rounded.Dashboard,
                "Главная и списки",
                listOf(
                    "${settings.tabs.size} ${ru.muz.text.plural(settings.tabs.size, "вкладка", "вкладки", "вкладок")}",
                    if (settings.compactLists) "компактно" else "обычные списки",
                ).joinToString(" · "),
            ) { onOpen(SettingsPage.LIBRARY) }
        }
    }
    item(key = "music") {
        SettingsCard("Музыка") {
            NavRow(
                Icons.Rounded.PlayCircle,
                "Плеер",
                if (settings.playerLayout == PlayerLayout.COVER_FIRST) "Сначала обложка" else "Сначала управление",
            ) { onOpen(SettingsPage.PLAYER) }
            NavRow(
                Icons.Rounded.Lyrics,
                "Текст песни",
                LYRICS_PRESETS.firstOrNull { it.matches(settings) }?.title ?: "Своя настройка · ${settings.lyricsFontSize} sp",
            ) { onOpen(SettingsPage.LYRICS) }
            NavRow(
                Icons.Rounded.Headphones,
                "Звук и офлайн",
                listOf(
                    if (settings.dataSaver) "Экономия трафика" else "Высокое качество",
                    if (settings.cacheSizeMb == 0) "кэш выключен" else "кэш ${formatSize(cacheUsedBytes)}",
                ).joinToString(" · "),
            ) { onOpen(SettingsPage.PLAYBACK) }
            NavRow(Icons.Rounded.Sync, "Подключения", "Last.fm и ListenBrainz") { onOpen(SettingsPage.SCROBBLING) }
        }
    }
    item(key = "app") {
        SettingsCard("Приложение") {
            NavRow(Icons.Rounded.ImportExport, "Импорт и экспорт", "CSV и резервная копия") { onOpen(SettingsPage.EXPORT) }
            NavRow(
                Icons.Rounded.Info,
                "О приложении",
                availableUpdate?.let { "Доступна версия ${it.version}" } ?: "Aria $appVersion",
            ) { onOpen(SettingsPage.ABOUT) }
        }
    }
    item(key = "reset") {
        Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
            TextButton(onClick = onReset) { Text("Сбросить все настройки", color = SoftText) }
        }
    }
}

private fun LazyListScope.appearanceSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item(key = "style") {
        SettingsCard("Стиль") {
            StylePicker(settings, onChange)
            AnimatedVisibility(settings.appStyle == AppStyle.GLASS, enter = revealIn(), exit = revealOut()) {
                SettingSlider("Сила стекла", settings.glassStrength, 10..100, 10, { "$it%" }) { onChange(settings.copy(glassStrength = it)) }
            }
        }
    }
    item(key = "theme") {
        SettingsCard("Тема и цвета") {
            ChoiceRow("Тема", ThemeMode.entries, settings.themeMode, ::themeName) { onChange(settings.copy(themeMode = it)) }
            SettingSwitch("Monet", "Цвета обоев, Android 12 и новее", settings.useDynamicColors) { onChange(settings.copy(useDynamicColors = it)) }
            SettingSwitch("Цвета под песню", "Оттенки меняются вместе с обложкой", settings.trackColors) { onChange(settings.copy(trackColors = it)) }
            Text(
                if (settings.useDynamicColors) "Акцент · выбор цвета выключит Monet" else "Акцент · ${accentName(settings.accentStyle).lowercase()}",
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp),
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(AccentStyle.entries) { accent ->
                    AccentSwatch(
                        accent = accent,
                        custom = settings.customAccent,
                        selected = !settings.useDynamicColors && settings.accentStyle == accent,
                    ) { onChange(settings.copy(accentStyle = accent, useDynamicColors = false)) }
                }
            }
            AnimatedVisibility(!settings.useDynamicColors && settings.accentStyle == AccentStyle.CUSTOM, enter = revealIn(), exit = revealOut()) {
                ColorPicker(settings.customAccent) { onChange(settings.copy(customAccent = it, accentStyle = AccentStyle.CUSTOM, useDynamicColors = false)) }
            }
            SettingSwitch("Повышенный контраст", "Непрозрачные панели и чёткий текст", settings.highContrast) { onChange(settings.copy(highContrast = it)) }
        }
    }
    item(key = "type") {
        SettingsCard("Текст и форма") {
            ChoiceRow("Шрифт", AppFont.entries, settings.appFont, ::fontName) { onChange(settings.copy(appFont = it)) }
            SettingSlider("Размер текста", settings.textScalePercent, 80..130, 5, { "$it%" }) { onChange(settings.copy(textScalePercent = it)) }
            SettingSlider("Скругление интерфейса", settings.cornerRadiusPercent, 0..160, 10, { "$it%" }) { onChange(settings.copy(cornerRadiusPercent = it)) }
            SettingSlider("Скругление обложек", settings.artworkCornerPercent, 0..160, 10, { "$it%" }) { onChange(settings.copy(artworkCornerPercent = it)) }
        }
    }
    item(key = "icons") {
        SettingsCard("Значки и навигация") {
            IconPackPicker(settings, onChange)
            SettingSwitch("Подписи под значками", "Текст в нижней панели", settings.showNavigationLabels) { onChange(settings.copy(showNavigationLabels = it)) }
        }
    }
    item(key = "motion") {
        SettingsCard("Анимации") {
            SettingSwitch("Уменьшить движение", "Только важные переходы", settings.reduceMotion) { onChange(settings.copy(reduceMotion = it)) }
            AnimatedVisibility(!settings.reduceMotion, enter = revealIn(), exit = revealOut()) {
                Column {
                    SettingSlider("Скорость анимаций", settings.animationSpeedPercent, 50..200, 25, { "$it%" }) { onChange(settings.copy(animationSpeedPercent = it)) }
                    SettingSwitch("Переходы в плеере", "Смена обложки, текста и кнопок", settings.animatePlayerTransitions) { onChange(settings.copy(animatePlayerTransitions = it)) }
                    SettingSwitch("Анимация загрузки", "Индикаторы и «играет сейчас»", settings.animateLoading) { onChange(settings.copy(animateLoading = it)) }
                    SettingSwitch("Плавное появление обложек", null, settings.animateImageLoading) { onChange(settings.copy(animateImageLoading = it)) }
                }
            }
        }
    }
    item(key = "language") {
        val options = listOf(ru.muz.i18n.I18n.SYSTEM) + ru.muz.i18n.I18n.languages.map { it.code }
        val names = mapOf(ru.muz.i18n.I18n.SYSTEM to "Как в системе") + ru.muz.i18n.I18n.languages.associate { it.code to it.name }
        SettingsCard("Язык", footer = "Новые языки подгружаются из GitHub сами, обновлять приложение не нужно") {
            ChoiceRow(null, options, settings.language, { names[it] ?: it }) { onChange(settings.copy(language = it)) }
        }
    }
    item(key = "reset") {
        SectionReset("Сбросить оформление") { defaults ->
            onChange(
                settings.copy(
                    themeMode = defaults.themeMode,
                    iconPack = defaults.iconPack,
                    highContrast = defaults.highContrast,
                    appFont = defaults.appFont,
                    textScalePercent = defaults.textScalePercent,
                    useDynamicColors = defaults.useDynamicColors,
                    accentStyle = defaults.accentStyle,
                    cornerRadiusPercent = defaults.cornerRadiusPercent,
                    artworkCornerPercent = defaults.artworkCornerPercent,
                    showNavigationLabels = defaults.showNavigationLabels,
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

private fun LazyListScope.librarySettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item(key = "home") {
        SettingsCard("Блоки на главной") {
            ReorderList(
                enabled = settings.homeSections,
                all = HomeSection.entries,
                label = { homeSectionName(it) },
                icon = { homeSectionIcon(it) },
                maxEnabled = null,
                minEnabled = 0,
            ) { onChange(settings.copy(homeSections = it)) }
        }
    }
    item(key = "tabs") {
        SettingsCard("Вкладки внизу") {
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
            ChoiceRow("Открывать при запуске", settings.tabs, settings.startTab, ::tabName) { onChange(settings.copy(startTab = it)) }
        }
    }
    item(key = "lists") {
        SettingsCard("Списки треков") {
            SettingSwitch("Компактные списки", "Больше треков на экране", settings.compactLists) { onChange(settings.copy(compactLists = it)) }
            SettingSlider("Масштаб строк", settings.listScalePercent, 80..125, 5, { "$it%" }) { onChange(settings.copy(listScalePercent = it)) }
            SettingSlider("Треков за раз", settings.listPageSize, 30..180, 10, { "$it" }) { onChange(settings.copy(listPageSize = it)) }
            SettingSwitch("Заранее загружать обложки", "Для следующих треков очереди", settings.preloadArtwork) { onChange(settings.copy(preloadArtwork = it)) }
        }
    }
    item(key = "search") {
        SettingsCard("Поиск") {
            SettingSlider("История поиска", settings.keepSearchHistory, 0..30, 5, { if (it == 0) "Не сохранять" else "$it запросов" }) {
                onChange(settings.copy(keepSearchHistory = it))
            }
        }
    }
    item(key = "reset") {
        SectionReset("Сбросить главную и списки") { defaults ->
            onChange(
                settings.copy(
                    homeSections = defaults.homeSections,
                    tabs = defaults.tabs,
                    startTab = defaults.startTab,
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
    item(key = "look") {
        SettingsCard("Вид") {
            ChoiceRow(
                "Расположение",
                PlayerLayout.entries,
                settings.playerLayout,
                { if (it == PlayerLayout.COVER_FIRST) "Сначала обложка" else "Сначала управление" },
            ) { onChange(settings.copy(playerLayout = it)) }
            SettingSlider("Размер обложки", settings.playerArtworkPercent, 70..110, 5, { "$it%" }) { onChange(settings.copy(playerArtworkPercent = it)) }
            ChoiceRow("Полоска прогресса", SeekStyle.entries, settings.seekStyle, ::seekStyleName) { onChange(settings.copy(seekStyle = it)) }
            PlayShapePicker(settings, onChange)
        }
    }
    item(key = "colors") {
        SettingsCard("Цвет") {
            SettingSwitch("Цвет обложки в фоне", "Мягкий градиент сверху экрана", settings.coverColorBackground) { onChange(settings.copy(coverColorBackground = it)) }
            AnimatedVisibility(!settings.coverColorBackground, enter = revealIn(), exit = revealOut()) {
                SettingSwitch("Лёгкий цветной фон", "Тот же градиент, вдвое слабее", settings.playerBlurBackground) { onChange(settings.copy(playerBlurBackground = it)) }
            }
            SettingSwitch("Акцент от обложки", "Кнопки и ползунок в цвет обложки", settings.coverAccent) { onChange(settings.copy(coverAccent = it)) }
            SettingSwitch("Цветные кнопки на экране текста", null, settings.accentControls) { onChange(settings.copy(accentControls = it)) }
            SettingSwitch("Фон в такт музыке", "Пятна цвета пульсируют с битом", settings.pulseBackground) { onChange(settings.copy(pulseBackground = it)) }
            AnimatedVisibility(settings.pulseBackground, enter = revealIn(), exit = revealOut()) {
                SettingSlider("Сила пульсации", settings.pulseStrength, 10..100, 10, { "$it%" }) { onChange(settings.copy(pulseStrength = it)) }
            }
        }
    }
    item(key = "buttons") {
        SettingsCard("Кнопки под ползунком", footer = "Помещается до 5 кнопок, остальные будут в меню «Ещё»") {
            ReorderList(
                enabled = settings.playerActions,
                all = PlayerAction.entries,
                label = { it.title() },
                icon = { it.icon() },
                maxEnabled = 5,
                minEnabled = 0,
            ) { onChange(settings.copy(playerActions = it)) }
        }
    }
    item(key = "mini") {
        SettingsCard("Мини-плеер") {
            SettingSwitch("Свайпы", "Вбок — трек, вверх — раскрыть, вниз — скрыть", settings.swipeMiniPlayer) { onChange(settings.copy(swipeMiniPlayer = it)) }
            SettingSwitch("Полоска прогресса", null, settings.showMiniPlayerProgress) { onChange(settings.copy(showMiniPlayerProgress = it)) }
            SettingSwitch("Цвет обложки", "Мини-плеер слегка окрашивается", settings.miniCoverTint) { onChange(settings.copy(miniCoverTint = it)) }
            SettingSwitch("Кнопка «Предыдущий»", null, settings.miniShowPrevious) { onChange(settings.copy(miniShowPrevious = it)) }
        }
    }
    item(key = "gestures") {
        SettingsCard("Жесты и поведение") {
            SettingSwitch("Открывать плеер при выборе трека", null, settings.openPlayerOnTrack) { onChange(settings.copy(openPlayerOnTrack = it)) }
            SettingSwitch("Жесты полного плеера", "Вбок — трек, вверх — очередь, вниз — свернуть", settings.fullPlayerSwipe) { onChange(settings.copy(fullPlayerSwipe = it)) }
            SettingSwitch("Двойной тап по обложке", null, settings.doubleTapLike) { onChange(settings.copy(doubleTapLike = it)) }
            AnimatedVisibility(settings.doubleTapLike, enter = revealIn(), exit = revealOut()) {
                ChoiceRow(null, DoubleTapAction.entries, settings.doubleTapAction, ::doubleTapName) { onChange(settings.copy(doubleTapAction = it)) }
            }
            SettingSwitch("Вибрация", "Лёгкий отклик на «Мне нравится»", settings.haptics) { onChange(settings.copy(haptics = it)) }
            SettingSwitch("Не выключать экран", "Пока приложение открыто", settings.keepScreenOn) { onChange(settings.copy(keepScreenOn = it)) }
        }
    }
    item(key = "reset") {
        SectionReset("Сбросить плеер") { defaults ->
            onChange(
                settings.copy(
                    openPlayerOnTrack = defaults.openPlayerOnTrack,
                    swipeMiniPlayer = defaults.swipeMiniPlayer,
                    showMiniPlayerProgress = defaults.showMiniPlayerProgress,
                    miniCoverTint = defaults.miniCoverTint,
                    playerBlurBackground = defaults.playerBlurBackground,
                    coverColorBackground = defaults.coverColorBackground,
                    coverAccent = defaults.coverAccent,
                    accentControls = defaults.accentControls,
                    pulseBackground = defaults.pulseBackground,
                    pulseStrength = defaults.pulseStrength,
                    seekStyle = defaults.seekStyle,
                    keepScreenOn = defaults.keepScreenOn,
                    playerActions = defaults.playerActions,
                    playShape = defaults.playShape,
                    miniShowPrevious = defaults.miniShowPrevious,
                    playerLayout = defaults.playerLayout,
                    playerArtworkPercent = defaults.playerArtworkPercent,
                    fullPlayerSwipe = defaults.fullPlayerSwipe,
                    doubleTapLike = defaults.doubleTapLike,
                    doubleTapAction = defaults.doubleTapAction,
                    haptics = defaults.haptics,
                ),
            )
        }
    }
}

@Composable
private fun PlayShapePicker(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Кнопка воспроизведения", fontWeight = FontWeight.SemiBold)
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PlayShape.entries.forEach { shape ->
                val chosen = settings.playShape == shape
                val tint by androidx.compose.animation.animateColorAsState(
                    if (chosen) Accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                    androidx.compose.animation.core.tween(animationDuration(200)),
                    label = "play-shape",
                )
                Box(
                    Modifier.size(48.dp).pressScale(0.9f).clip(playShape(shape))
                        .background(tint)
                        .clickable(role = Role.RadioButton) { onChange(settings.copy(playShape = shape)) },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        null,
                        tint = if (chosen) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

private fun LazyListScope.lyricsSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item(key = "preview") { LyricsPreview(settings) }
    item(key = "style") {
        SettingsCard("Стиль") {
            ChoiceRow("Экран текста", listOf(true, false), settings.lyricsModern, { if (it) "Новый" else "Классический" }) {
                onChange(settings.copy(lyricsModern = it))
            }
            Hint(
                if (settings.lyricsModern) "Строка сверху, дальние размыты, кнопки прячутся сами"
                else "Строка посередине, всё чётко, кнопки всегда на месте",
            )
            AnimatedVisibility(settings.lyricsModern, enter = revealIn(), exit = revealOut()) {
                Column {
                    SettingSwitch("Размывать дальние строки", "Android 12 и новее", settings.lyricsBlur) { onChange(settings.copy(lyricsBlur = it)) }
                    AnimatedVisibility(settings.lyricsBlur, enter = revealIn(), exit = revealOut()) {
                        SettingSwitch("Резкий текст при листании", null, settings.lyricsSharpOnScroll) { onChange(settings.copy(lyricsSharpOnScroll = it)) }
                    }
                    SettingSwitch("Прятать кнопки", "Через 3 секунды без касаний", settings.lyricsAutoHide) { onChange(settings.copy(lyricsAutoHide = it)) }
                }
            }
        }
    }
    item(key = "look") {
        val preset = LYRICS_PRESETS.firstOrNull { it.matches(settings) }
        SettingsCard("Вид") {
            ChoiceRow(null, LYRICS_PRESETS, preset, { it?.title ?: "" }) { chosen -> chosen?.let { onChange(it.apply(settings)) } }
            Hint(preset?.subtitle ?: "Своя настройка")
            ExpandableSection("Тонкая настройка", "Размеры, контраст, свечение и прокрутка") { LyricsFineTuning(settings, onChange) }
        }
    }
    item(key = "karaoke") {
        SettingsCard("Караоке") {
            SettingSwitch("Заливка по словам", "Активная строка заполняется, пока её поют", settings.karaokeFill) { onChange(settings.copy(karaokeFill = it)) }
            AnimatedVisibility(settings.karaokeFill, enter = revealIn(), exit = revealOut()) {
                Column {
                    ChoiceRow("Эффект", KaraokeFillStyle.entries, settings.karaokeFillStyle, ::karaokeStyleName) { onChange(settings.copy(karaokeFillStyle = it)) }
                    ChoiceRow(
                        "Направление",
                        KaraokeDirection.entries,
                        settings.karaokeDirection,
                        { if (it == KaraokeDirection.HORIZONTAL) "Слева направо" else "Сверху вниз" },
                    ) { onChange(settings.copy(karaokeDirection = it)) }
                }
            }
        }
    }
    item(key = "behavior") {
        SettingsCard("Поведение") {
            SettingSwitch("Сразу открывать текст", "При запуске нового трека", settings.showLyricsByDefault) { onChange(settings.copy(showLyricsByDefault = it)) }
            SettingSwitch("Нажатие перематывает к строке", null, settings.lyricsTapToSeek) { onChange(settings.copy(lyricsTapToSeek = it)) }
            SettingSwitch("Следовать за песней", "Возвращается через 3 секунды после прокрутки", settings.lyricsAutoScroll) { onChange(settings.copy(lyricsAutoScroll = it)) }
        }
    }
    item(key = "sync") {
        SettingsCard("Синхронизация", footer = "Плюс — текст раньше, минус — позже. Помогает, если строки опаздывают.") {
            SettingSlider("Сдвиг текста", settings.lyricsOffsetMs, -5000..5000, 100, { formatOffset(it) }) { onChange(settings.copy(lyricsOffsetMs = it)) }
        }
    }
    item(key = "reset") {
        SectionReset("Сбросить экран текста") { defaults ->
            onChange(settings.copy(showLyricsByDefault = defaults.showLyricsByDefault, karaokeFill = defaults.karaokeFill, karaokeFillStyle = defaults.karaokeFillStyle, karaokeDirection = defaults.karaokeDirection, fadeLyrics = defaults.fadeLyrics, boldActiveLyrics = defaults.boldActiveLyrics, lyricsFontSize = defaults.lyricsFontSize, lyricsLineHeight = defaults.lyricsLineHeight, lyricsHorizontalPadding = defaults.lyricsHorizontalPadding, lyricsActiveScale = defaults.lyricsActiveScale, lyricsTapToSeek = defaults.lyricsTapToSeek, lyricsLineSpacing = defaults.lyricsLineSpacing, lyricsAnchorPercent = defaults.lyricsAnchorPercent, lyricsInactivePercent = defaults.lyricsInactivePercent, lyricsUnsungPercent = defaults.lyricsUnsungPercent, lyricsSungPercent = defaults.lyricsSungPercent, lyricsGlowBlur = defaults.lyricsGlowBlur, lyricsGlowAlpha = defaults.lyricsGlowAlpha, lyricsScrollSpeed = defaults.lyricsScrollSpeed, lyricsAlignment = defaults.lyricsAlignment, lyricsAutoScroll = defaults.lyricsAutoScroll, lyricsOffsetMs = defaults.lyricsOffsetMs))
        }
    }
}

private fun LazyListScope.playbackSettings(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    cacheUsedBytes: Long,
    onStorage: () -> Unit,
    onClearCache: () -> Unit,
    onRefreshCache: () -> Unit,
) {
    item(key = "quality") {
        SettingsCard("Качество") {
            SettingSwitch("Экономия трафика", "Поток с меньшим битрейтом", settings.dataSaver) { onChange(settings.copy(dataSaver = it)) }
            SettingSwitch("Предзагрузка следующего трека", "Следующий трек начнётся без паузы", settings.preloadNextTracks) { onChange(settings.copy(preloadNextTracks = it)) }
            SettingSwitch("Выравнивать громкость", "По данным Яндекса", settings.normalizeVolume) { onChange(settings.copy(normalizeVolume = it)) }
        }
    }
    item(key = "sound") {
        SettingsCard("Звук") {
            SoundControls(settings, onChange, Modifier.padding(top = 12.dp, bottom = 4.dp))
        }
    }
    item(key = "recommendations") {
        SettingsCard("Рекомендации") {
            SettingSwitch("Приватный режим", "Прослушивания не уходят в Яндекс и скробблинг", settings.privateSession) { onChange(settings.copy(privateSession = it)) }
            SettingSwitch("Без откровенных треков", "Пропускать треки с пометкой E", settings.hideExplicit) { onChange(settings.copy(hideExplicit = it)) }
        }
    }
    item(key = "cache") {
        LaunchedEffect(Unit) { onRefreshCache() }
        SettingsCard(
            "Кэш и офлайн",
            footer = "Кэш очищается сам при заполнении. Музыка для офлайна хранится отдельно. Новый размер применится после перезапуска.",
        ) {
            SettingSlider(
                "Размер кэша",
                settings.cacheSizeMb,
                0..8192,
                512,
                { if (it == 0) "Выключен" else formatSize(it * 1024L * 1024L) },
            ) { onChange(settings.copy(cacheSizeMb = it)) }
            SettingsActionRow(Icons.Rounded.DeleteSweep, "Очистить кэш", "Сейчас занято ${formatSize(cacheUsedBytes)}", cacheUsedBytes > 0, onClick = onClearCache)
            SettingsActionRow(Icons.Rounded.Storage, "Память приложения", "Музыка, обложки и временные файлы", true, onClick = onStorage)
        }
    }
    item(key = "reset") {
        SectionReset("Сбросить звук и офлайн") { defaults ->
            onChange(
                settings.copy(
                    dataSaver = defaults.dataSaver,
                    preloadNextTracks = defaults.preloadNextTracks,
                    normalizeVolume = defaults.normalizeVolume,
                    cacheSizeMb = defaults.cacheSizeMb,
                ),
            )
        }
    }
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
    item(key = "app") {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
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
    item(key = "updates") {
        SettingsCard("Обновления") {
            SettingSwitch("Проверять обновления", "Смотрит релизы на GitHub", settings.autoCheckUpdates) { onChange(settings.copy(autoCheckUpdates = it)) }
            if (availableUpdate != null) {
                SettingsActionRow(
                    Icons.Rounded.SystemUpdate,
                    if (updateProgress != null) "Скачиваем… ${(updateProgress * 100).toInt()}%" else "Установить Aria ${availableUpdate.version}",
                    null,
                    updateProgress == null,
                ) { onInstallUpdate(availableUpdate) }
                ReleaseNotes(availableUpdate.notes, MaterialTheme.colorScheme.onSurface, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 10.dp), collapsed = 6)
            }
            SettingsActionRow(Icons.Rounded.Sync, if (updateChecking) "Проверяем…" else "Проверить сейчас", null, !updateChecking, onClick = onCheckUpdates)
        }
    }
    item(key = "source") {
        val context = androidx.compose.ui.platform.LocalContext.current
        SettingsCard {
            SettingsActionRow(Icons.Rounded.Code, "Исходный код на GitHub", "github.com/Dev-Angel-7553/Aria-Android", true) {
                ru.muz.update.UpdateInstaller.openPage(context, "https://github.com/Dev-Angel-7553/Aria-Android")
            }
        }
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
    item(key = "card") { AccountCard(account, settings.reportListening, onClick = null) }
    item(key = "yandex") {
        SettingsCard("Яндекс Музыка") {
            SettingSwitch(
                "Отправлять прослушивания",
                "История аккаунта и «Моя волна» учатся на том, что вы слушаете здесь",
                settings.reportListening,
            ) { onChange(settings.copy(reportListening = it)) }
        }
    }
    item(key = "sync") {
        SettingsCard("Синхронизация библиотеки", footer = "Кэш открывается сразу, свежие данные подмешиваются в фоне.") {
            SettingSwitch("Автосинхронизация", "Обновлять библиотеку по расписанию", settings.syncIntervalMinutes > 0) {
                onChange(settings.copy(syncIntervalMinutes = if (it) 60 else 0))
            }
            AnimatedVisibility(settings.syncIntervalMinutes > 0, enter = revealIn(), exit = revealOut()) {
                SettingSlider(
                    "Проверять библиотеку",
                    settings.syncIntervalMinutes.coerceIn(30, 360),
                    30..360,
                    30,
                    { "раз в ${formatInterval(it)}" },
                ) { onChange(settings.copy(syncIntervalMinutes = it)) }
            }
            SettingsActionRow(Icons.Rounded.Sync, if (syncBusy) "Синхронизация…" else "Синхронизировать сейчас", null, !syncBusy, onClick = onSyncNow)
        }
    }
    item(key = "logout") {
        SettingsCard {
            SettingsActionRow(Icons.Rounded.Logout, "Выйти и удалить токен", null, true, destructive = true, onClick = onLogout)
        }
    }
}

private fun LazyListScope.exportSettings(
    exportBusy: Boolean,
    onExportCollection: (share: Boolean) -> Unit,
    onImportCsv: () -> Unit,
    onExportSettings: () -> Unit,
    onImportSettings: () -> Unit,
) {
    item(key = "import") {
        SettingsCard("Плейлисты", footer = "CSV из Spotify, TuneMyMusic, Exportify или список «Исполнитель — Название». Экспорт понимают TuneMyMusic, Soundiiz и Excel.") {
            SettingsActionRow(Icons.Rounded.FileUpload, "Импорт из CSV", "Треки найдутся в Яндексе, плейлисты появятся в аккаунте", true, onClick = onImportCsv)
            SettingsActionRow(
                Icons.Rounded.FileDownload,
                if (exportBusy) "Готовим CSV…" else "Сохранить коллекцию в Загрузки",
                "«Мне нравится» и все плейлисты в одном CSV",
                !exportBusy,
            ) { onExportCollection(false) }
            SettingsActionRow(Icons.Rounded.Share, "Отправить файлом", "Через Telegram, почту или другое приложение", !exportBusy) { onExportCollection(true) }
        }
    }
    item(key = "backup") {
        SettingsCard("Настройки") {
            SettingsActionRow(Icons.Rounded.Save, "Сохранить настройки в файл", "Всё, кроме аккаунта", true, onClick = onExportSettings)
            SettingsActionRow(Icons.Rounded.Restore, "Загрузить настройки из файла", "Например, с другого телефона", true, onClick = onImportSettings)
        }
    }
}

@Composable
private fun SettingsCard(
    title: String? = null,
    footer: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val glass = LocalAppStyle.current == AppStyle.GLASS
    val minimal = LocalAppStyle.current == AppStyle.MINIMAL
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape((if (minimal) 10.dp else 22.dp) * LocalCornerRadiusScale.current.coerceIn(0.2f, 1.4f))
    Column(Modifier.fillMaxWidth().padding(top = if (title != null) 18.dp else 10.dp)) {
        if (title != null) {
            Text(
                title,
                color = SoftText,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, bottom = 8.dp),
            )
        }
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .clip(shape)
                .background(
                    when {
                        glass -> scheme.surface.copy(alpha = 0.46f)
                        minimal -> scheme.surfaceContainerLow
                        else -> scheme.surfaceContainer
                    },
                )
                .then(if (glass) Modifier.border(1.dp, Color.White.copy(alpha = 0.12f), shape) else Modifier)
                .padding(vertical = 6.dp),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                color = SoftText,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun Hint(value: String) {
    Text(value, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp))
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceRow(
    title: String?,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        if (title != null) Text(title, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp, bottom = 2.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                FilterChip(
                    selected = option == selected,
                    onClick = { onSelect(option) },
                    label = { Text(label(option), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    leadingIcon = if (option == selected) {
                        { Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
            }
        }
    }
}

@Composable
private fun SectionReset(label: String, onReset: (AppSettings) -> Unit) {
    Box(Modifier.fillMaxWidth().padding(top = 12.dp), contentAlignment = Alignment.Center) {
        TextButton(onClick = { onReset(AppSettings()) }) { Text(label, color = SoftText) }
    }
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
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon(value), null, tint = Accent, modifier = Modifier.size(22.dp))
                Text(
                    label(value),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 14.dp),
                )
                IconButton(
                    onClick = { onChange(enabled.toMutableList().apply { add(index - 1, removeAt(index)) }) },
                    enabled = index > 0,
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Rounded.KeyboardArrowUp, "Выше") }
                IconButton(
                    onClick = { onChange(enabled.toMutableList().apply { add(index + 1, removeAt(index)) }) },
                    enabled = index < enabled.lastIndex,
                    modifier = Modifier.size(40.dp),
                ) { Icon(Icons.Rounded.KeyboardArrowDown, "Ниже") }
                AriaSwitch(checked = true, enabled = enabled.size > minEnabled, onCheckedChange = { onChange(enabled - value) })
            }
        }
        disabled.forEach { value ->
            val canAdd = maxEnabled == null || enabled.size < maxEnabled
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(icon(value), null, tint = SoftText, modifier = Modifier.size(22.dp))
                Text(
                    label(value),
                    color = SoftText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(start = 14.dp),
                )
                AriaSwitch(checked = false, enabled = canAdd, onCheckedChange = { onChange(enabled + value) })
            }
        }
    }
}

@Composable
private fun SettingsSearchField(query: String, onQuery: (String) -> Unit) {
    androidx.compose.material3.OutlinedTextField(
        value = query,
        onValueChange = onQuery,
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        singleLine = true,
        placeholder = { Text("Поиск по настройкам", maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = { Icon(Icons.Rounded.Search, null) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQuery("") }) { Icon(Icons.Rounded.Close, "Очистить") } }
        } else null,
        shape = RoundedCornerShape(28.dp * LocalCornerRadiusScale.current),
    )
}

private class SettingsEntry(val title: String, val page: SettingsPage, val keywords: String = "")

private val SETTINGS_INDEX = listOf(
    SettingsEntry("Скробблинг", SettingsPage.SCROBBLING, "last.fm lastfm listenbrainz подключения scrobble аккаунты"),
    SettingsEntry("Стиль", SettingsPage.APPEARANCE, "классика material you стекло минимал glass"),
    SettingsEntry("Тема", SettingsPage.APPEARANCE, "тёмная светлая amoled чёрная системная"),
    SettingsEntry("Monet", SettingsPage.APPEARANCE, "обои цвет динамические"),
    SettingsEntry("Цвета под песню", SettingsPage.APPEARANCE, "обложка трек оттенок акцент"),
    SettingsEntry("Язык", SettingsPage.APPEARANCE, "language english русский"),
    SettingsEntry("Цвет акцента", SettingsPage.APPEARANCE, "жёлтый синий розовый зелёный оранжевый свой hex палитра"),
    SettingsEntry("Скругление", SettingsPage.APPEARANCE, "углы форма обложки интерфейс"),
    SettingsEntry("Подписи под значками", SettingsPage.APPEARANCE, "навигация нижняя панель"),
    SettingsEntry("Набор иконок", SettingsPage.APPEARANCE, "контурные угловатые двухцветные значки"),
    SettingsEntry("Повышенный контраст", SettingsPage.APPEARANCE, "доступность читаемость"),
    SettingsEntry("Шрифт", SettingsPage.APPEARANCE, "гарнитура засечки моноширинный размер текста"),
    SettingsEntry("Анимации", SettingsPage.APPEARANCE, "скорость уменьшить движение переходы загрузка"),
    SettingsEntry("Эквалайзер", SettingsPage.PLAYBACK, "звук бас пресет частоты"),
    SettingsEntry("Плавный переход", SettingsPage.PLAYBACK, "кроссфейд crossfade затухание"),
    SettingsEntry("Экономия трафика", SettingsPage.PLAYBACK, "качество битрейт мобильный интернет"),
    SettingsEntry("Выравнивать громкость", SettingsPage.PLAYBACK, "нормализация"),
    SettingsEntry("Приватный режим", SettingsPage.PLAYBACK, "рекомендации откровенные"),
    SettingsEntry("Кэш и офлайн", SettingsPage.PLAYBACK, "память очистить размер кэша"),
    SettingsEntry("Расположение плеера", SettingsPage.PLAYER, "сначала обложка управление компоновка размер обложки"),
    SettingsEntry("Полоска прогресса", SettingsPage.PLAYER, "волна линия капсула ползунок"),
    SettingsEntry("Кнопка воспроизведения", SettingsPage.PLAYER, "форма круг квадрат"),
    SettingsEntry("Цвет обложки в фоне", SettingsPage.PLAYER, "градиент фон акцент пульсация в такт"),
    SettingsEntry("Кнопки плеера", SettingsPage.PLAYER, "под ползунком таймер скорость цитата действия"),
    SettingsEntry("Мини-плеер", SettingsPage.PLAYER, "свайпы прогресс предыдущий"),
    SettingsEntry("Жесты", SettingsPage.PLAYER, "свайпы двойной тап вибрация экран"),
    SettingsEntry("Вид текста песни", SettingsPage.LYRICS, "пресет крупно компактно по центру шрифт"),
    SettingsEntry("Караоке", SettingsPage.LYRICS, "заливка свечение glow"),
    SettingsEntry("Сдвиг текста", SettingsPage.LYRICS, "синхронизация опаздывает раньше позже"),
    SettingsEntry("Сразу открывать текст", SettingsPage.LYRICS),
    SettingsEntry("Блоки на главной", SettingsPage.LIBRARY, "порядок волна чарт плейлисты"),
    SettingsEntry("Вкладки внизу", SettingsPage.LIBRARY, "навигация статистика поиск запуск"),
    SettingsEntry("Компактные списки", SettingsPage.LIBRARY, "масштаб строк треков за раз"),
    SettingsEntry("История поиска", SettingsPage.LIBRARY),
    SettingsEntry("Экспорт в CSV", SettingsPage.EXPORT, "tunemymusic soundiiz excel перенос плейлисты"),
    SettingsEntry("Импорт из CSV", SettingsPage.EXPORT, "spotify перенос плейлист загрузить"),
    SettingsEntry("Резервная копия настроек", SettingsPage.EXPORT, "сохранить загрузить перенос телефон"),
    SettingsEntry("Прослушивания в Яндексе", SettingsPage.ACCOUNT, "история моя волна"),
    SettingsEntry("Синхронизация библиотеки", SettingsPage.ACCOUNT, "обновить"),
    SettingsEntry("Выйти из аккаунта", SettingsPage.ACCOUNT, "токен"),
    SettingsEntry("Обновления", SettingsPage.ABOUT, "версия github"),
)

private fun searchSettings(query: String): List<SettingsEntry> {
    val words = query.trim().lowercase().split(' ').filter { it.isNotBlank() }
    return SETTINGS_INDEX.filter { entry ->
        val haystack = "${entry.title} ${entry.page.title} ${entry.keywords} ${ru.muz.i18n.I18n.t(entry.title)}".lowercase()
        words.all { haystack.contains(it) }
    }
}

private fun SettingsPage.icon(): ImageVector = when (this) {
    SettingsPage.APPEARANCE -> Icons.Rounded.Palette
    SettingsPage.LIBRARY -> Icons.Rounded.Dashboard
    SettingsPage.PLAYER -> Icons.Rounded.PlayCircle
    SettingsPage.LYRICS -> Icons.Rounded.Lyrics
    SettingsPage.PLAYBACK -> Icons.Rounded.Headphones
    SettingsPage.SCROBBLING -> Icons.Rounded.Sync
    SettingsPage.ACCOUNT -> Icons.Rounded.AccountCircle
    SettingsPage.EXPORT -> Icons.Rounded.ImportExport
    SettingsPage.ABOUT -> Icons.Rounded.Info
    SettingsPage.ROOT -> Icons.Rounded.Search
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBack, "Назад") }
        androidx.compose.animation.Crossfade(title, Modifier.weight(1f), androidx.compose.animation.core.tween(animationDuration(200)), label = "settings-title") { value ->
            FitText(value, fontSize = 22.sp, minFontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, end = 12.dp))
        }
    }
}

@Composable
private fun NavRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(20.dp))
        }
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, color = SoftText, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = SoftText, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    enabled: Boolean,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = when {
        !enabled -> SoftText
        destructive -> MaterialTheme.colorScheme.error
        else -> Accent
    }
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 16.dp)) {
            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                color = if (destructive && enabled) MaterialTheme.colorScheme.error else if (enabled) MaterialTheme.colorScheme.onSurface else SoftText,
            )
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun ExpandableSection(title: String, subtitle: String, content: @Composable () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val turn by androidx.compose.animation.core.animateFloatAsState(
        if (expanded) 180f else 0f,
        androidx.compose.animation.core.tween(animationDuration(260), easing = EmphasizedEasing),
        label = "expand-arrow",
    )
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = tr(if (expanded) "Свернуть" else "Развернуть")) { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Accent)
                Text(subtitle, color = SoftText, fontSize = 12.sp)
            }
            Icon(Icons.Rounded.ExpandMore, null, tint = SoftText, modifier = Modifier.graphicsLayer { rotationZ = turn })
        }
        AnimatedVisibility(expanded, enter = revealIn(), exit = revealOut()) { content() }
    }
}

@Composable
private fun SettingsSubtitle(value: String) {
    Text(value, color = Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 2.dp))
}

@Composable
private fun SettingSwitch(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) Text(subtitle, color = SoftText, fontSize = 12.sp, lineHeight = 16.sp)
        }
        AriaSwitch(checked = checked, onCheckedChange = null, modifier = Modifier.clearAndSetSemantics { })
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
    Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
            Text(label(value), color = SoftText, fontSize = 13.sp, maxLines = 1)
        }
        StyledSlider(
            value = value.toFloat(),
            onValueChange = { raw -> snap(raw).let { if (it != value) onChange(it) } },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = stops,
        )
    }
}

private fun seekStyleName(style: SeekStyle): String = when (style) {
    SeekStyle.LINE -> "Линия"
    SeekStyle.WAVY -> "Волна"
    SeekStyle.CAPSULE -> "Капсула"
    SeekStyle.HAIRLINE -> "Тонкая с точкой"
}

private fun doubleTapName(action: DoubleTapAction): String = when (action) {
    DoubleTapAction.LIKE -> "«Мне нравится»"
    DoubleTapAction.PLAY_PAUSE -> "Пауза"
    DoubleTapAction.NEXT -> "Следующий трек"
}

private fun karaokeStyleName(style: KaraokeFillStyle): String = when (style) {
    KaraokeFillStyle.SOLID -> "Чистый"
    KaraokeFillStyle.SOFT_GLOW -> "Свечение"
    KaraokeFillStyle.WORD_PULSE -> "Пульсация слова"
}

@Composable
private fun StylePicker(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 8.dp),
    ) {
        items(AppStyle.entries.toList()) { style ->
            val chosen = settings.appStyle == style
            val outline by androidx.compose.animation.animateColorAsState(
                if (chosen) Accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                androidx.compose.animation.core.tween(animationDuration(220)),
                label = "style-outline",
            )
            Column(
                Modifier.width(118.dp).pressScale().clip(RoundedCornerShape(20.dp))
                    .border(if (chosen) 2.dp else 1.dp, outline, RoundedCornerShape(20.dp))
                    .clickable(role = Role.RadioButton) { onChange(stylePreset(settings, style)) }
                    .padding(7.dp),
            ) {
                StyleThumb(style, Modifier.fillMaxWidth().height(124.dp))
                Row(Modifier.padding(top = 7.dp, start = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(styleName(style), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    if (chosen) Icon(Icons.Rounded.Check, null, tint = Accent, modifier = Modifier.size(16.dp))
                }
                Text(styleHint(style), color = SoftText, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 3.dp))
            }
        }
    }
}

@Composable
internal fun LanguagePicker(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(ru.muz.i18n.I18n.SYSTEM) + ru.muz.i18n.I18n.languages.map { it.code }
    val names = mapOf(ru.muz.i18n.I18n.SYSTEM to "Как в системе") + ru.muz.i18n.I18n.languages.associate { it.code to it.name }
    ChoiceRow(null, options, selected, { names[it] ?: it }, onSelect)
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

@Composable
private fun LyricsFineTuning(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    Column {
        SettingsSubtitle("Текст и положение")
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
        ChoiceRow(
            "Выравнивание",
            LyricsAlignment.entries,
            settings.lyricsAlignment,
            { if (it == LyricsAlignment.LEFT) "По левому краю" else "По центру" },
        ) { onChange(settings.copy(lyricsAlignment = it)) }
        SettingsSubtitle("Контраст и свечение")
        SettingSwitch("Затухание к краям", "Оставляет активную строку в фокусе", settings.fadeLyrics) { onChange(settings.copy(fadeLyrics = it)) }
        SettingSlider("Яркость соседних строк", settings.lyricsInactivePercent, 20..70, 5, { "$it%" }) { onChange(settings.copy(lyricsInactivePercent = it)) }
        SettingSlider("Ещё не спетая часть", settings.lyricsUnsungPercent, 40..85, 5, { "$it%" }) { onChange(settings.copy(lyricsUnsungPercent = it)) }
        SettingSlider("Спетая часть", settings.lyricsSungPercent, 70..125, 5, { "$it%" }) { onChange(settings.copy(lyricsSungPercent = it)) }
        SettingSlider("Сила свечения", settings.lyricsGlowAlpha, 0..45, 5, { "$it%" }) { onChange(settings.copy(lyricsGlowAlpha = it)) }
        SettingSlider("Размытие свечения", settings.lyricsGlowBlur, 0..18, 1, { "$it dp" }) { onChange(settings.copy(lyricsGlowBlur = it)) }
        SettingsSubtitle("Прокрутка")
        SettingSlider("Длительность прокрутки", settings.lyricsScrollSpeed, 120..900, 60, { "$it мс" }) { onChange(settings.copy(lyricsScrollSpeed = it)) }
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
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(280.dp)
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

private fun homeSectionName(section: HomeSection): String = when (section) {
    HomeSection.WAVE -> "Моя волна"
    HomeSection.RECENT -> "Недавно добавленное"
    HomeSection.PLAYLISTS -> "Ваши плейлисты"
    HomeSection.CHART -> "Чарт"
}

private fun homeSectionIcon(section: HomeSection): ImageVector = when (section) {
    HomeSection.WAVE -> Icons.Rounded.GraphicEq
    HomeSection.RECENT -> Icons.Rounded.History
    HomeSection.PLAYLISTS -> Icons.Rounded.QueueMusic
    HomeSection.CHART -> Icons.Rounded.TrendingUp
}

private fun tabName(tab: AppTab): String = when (tab) {
    AppTab.HOME -> "Главная"
    AppTab.LIBRARY -> "Коллекция"
    AppTab.SEARCH -> "Поиск"
    AppTab.STATS -> "Статистика"
}

private fun tabIcon(tab: AppTab): ImageVector = when (tab) {
    AppTab.HOME -> Icons.Rounded.Home
    AppTab.LIBRARY -> Icons.Rounded.LibraryMusic
    AppTab.SEARCH -> Icons.Rounded.Search
    AppTab.STATS -> Icons.Rounded.Equalizer
}

private fun fontName(font: AppFont): String = when (font) {
    AppFont.SYSTEM -> "Системный"
    AppFont.SANS -> "Гротеск"
    AppFont.SERIF -> "С засечками"
    AppFont.MONO -> "Моноширинный"
    AppFont.CONDENSED -> "Узкий"
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
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
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
        if (onClick != null) Icon(Icons.Rounded.ChevronRight, null, tint = SoftText)
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
            selected -> Icon(Icons.Rounded.Check, accentName(accent), tint = mark, modifier = Modifier.size(20.dp))
            accent == AccentStyle.CUSTOM -> Icon(Icons.Rounded.Colorize, accentName(accent), tint = mark, modifier = Modifier.size(20.dp))
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
                        Icon(Icons.Rounded.PlayArrow, null, tint = if (style == ru.muz.settings.AppStyle.MATERIAL_YOU) scheme.onPrimary else Color.Black)
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
