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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
    LISTS("Главный экран и списки"),
    PLAYER("Плеер"),
    LYRICS("Текст песни"),
    PLAYBACK("Воспроизведение"),
    ACCOUNT("Аккаунт и данные"),
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
) {
    var page by remember { mutableStateOf(SettingsPage.ROOT) }
    var confirmReset by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }
    BackHandler(enabled = page != SettingsPage.ROOT) { page = SettingsPage.ROOT }
    Scaffold(
        containerColor = Color.Transparent,
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
            Modifier.fillMaxSize().statusBarsPadding().padding(scaffoldPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            stickyHeader {
                Column(Modifier.fillMaxWidth().background(AppBackground)) {
                    SettingsHeader(page.title) { if (page == SettingsPage.ROOT) onBack() else page = SettingsPage.ROOT }
                    when (page) {
                        SettingsPage.APPEARANCE -> AppearancePreview()
                        SettingsPage.LISTS -> ListPreview(settings)
                        SettingsPage.PLAYER -> MiniPlayerPreview(settings)
                        SettingsPage.LYRICS -> LyricsPreview(settings)
                        else -> Unit
                    }
                }
            }
            when (page) {
                SettingsPage.ROOT -> {
                    item { SettingsTitle("Интерфейс") }
                    item { SettingsNavRow(Icons.Filled.Palette, "Оформление", themeLabel(settings)) { page = SettingsPage.APPEARANCE } }
                    item {
                        SettingsNavRow(
                            Icons.Filled.AutoAwesome,
                            "Анимации",
                            if (settings.reduceMotion) "Минимум движения" else "Скорость ${settings.animationSpeedPercent}%",
                        ) { page = SettingsPage.ANIMATION }
                    }
                    item {
                        SettingsNavRow(
                            Icons.Filled.Dashboard,
                            "Главный экран и списки",
                            "${if (settings.compactLists) "Компактно" else "Обычно"} · масштаб ${settings.listScalePercent}%",
                        ) { page = SettingsPage.LISTS }
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
                        ) { page = SettingsPage.PLAYER }
                    }
                    item {
                        SettingsNavRow(
                            Icons.Filled.Lyrics,
                            "Текст песни",
                            LYRICS_PRESETS.firstOrNull { it.matches(settings) }?.title
                                ?: "Своя настройка · ${settings.lyricsFontSize} sp",
                        ) { page = SettingsPage.LYRICS }
                    }
                    item {
                        SettingsNavRow(
                            Icons.Filled.Headphones,
                            "Воспроизведение",
                            if (settings.dataSaver) "Экономия трафика" else "Высокое качество",
                        ) { page = SettingsPage.PLAYBACK }
                    }
                    item { SettingsTitle("Профиль") }
                    item {
                        SettingsNavRow(
                            Icons.Filled.AccountCircle,
                            "Аккаунт и данные",
                            listOfNotNull(
                                account?.displayName ?: "Яндекс Музыка",
                                if (settings.reportListening) "прослушивания уходят в Яндекс" else null,
                            ).joinToString(" · "),
                        ) { page = SettingsPage.ACCOUNT }
                    }
                    item {
                        TextButton(
                            onClick = { confirmReset = true },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                        ) { Text("Сбросить все настройки") }
                    }
                }
                SettingsPage.APPEARANCE -> appearanceSettings(settings, onChange)
                SettingsPage.ANIMATION -> animationSettings(settings, onChange)
                SettingsPage.LISTS -> listSettings(settings, onChange)
                SettingsPage.PLAYER -> playerSettings(settings, onChange)
                SettingsPage.LYRICS -> lyricsSettings(settings, onChange)
                SettingsPage.PLAYBACK -> playbackSettings(settings, onChange)
                SettingsPage.ACCOUNT -> accountSettings(
                    account, settings, onChange, { confirmLogout = true }, onSyncNow, syncBusy,
                    exportBusy, onExportCollection,
                )
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
    item {
        SectionReset("Сбросить анимации") { defaults ->
            onChange(
                settings.copy(
                    animationSpeedPercent = defaults.animationSpeedPercent,
                    reduceMotion = defaults.reduceMotion,
                    animatePlayerTransitions = defaults.animatePlayerTransitions,
                    animateLoading = defaults.animateLoading,
                ),
            )
        }
    }
}

private fun LazyListScope.appearanceSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
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
                        selected = !settings.useDynamicColors && settings.accentStyle == accent,
                    ) { onChange(settings.copy(accentStyle = accent, useDynamicColors = false)) }
                }
            }
        }
    }
    item {
        SectionReset("Сбросить оформление") { defaults ->
            onChange(settings.copy(themeMode = defaults.themeMode, useDynamicColors = defaults.useDynamicColors, accentStyle = defaults.accentStyle))
        }
    }
}

@Composable
private fun AccentSwatch(accent: AccentStyle, selected: Boolean, onClick: () -> Unit) {
    val colour = Color(accentColor(accent))
    Box(
        Modifier.size(46.dp).clip(CircleShape)
            .border(if (selected) 3.dp else 0.dp, if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent, CircleShape)
            .padding(if (selected) 5.dp else 0.dp)
            .clip(CircleShape)
            .background(colour)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Filled.Check, accentName(accent), tint = Color(0xFF16130A), modifier = Modifier.size(20.dp))
    }
}

private fun LazyListScope.listSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Отображение") }
    item { SettingSwitch("Компактные списки", "Больше треков помещается на экране", settings.compactLists) { onChange(settings.copy(compactLists = it)) } }
    item { SettingSwitch("Подписи навигации", "Текст под значками в нижней панели", settings.showNavigationLabels) { onChange(settings.copy(showNavigationLabels = it)) } }
    item { SettingsTitle("Размеры и форма") }
    item { SettingSlider("Скругление обложек", settings.artworkCornerPercent, 0..160, 10, { "$it%" }) { onChange(settings.copy(artworkCornerPercent = it)) } }
    item { SettingSlider("Скругление интерфейса", settings.cornerRadiusPercent, 0..160, 10, { "$it%" }) { onChange(settings.copy(cornerRadiusPercent = it)) } }
    item { SettingSlider("Масштаб строк", settings.listScalePercent, 80..125, 5, { "$it%" }) { onChange(settings.copy(listScalePercent = it)) } }
    item { SettingSlider("Треков за страницу", settings.listPageSize, 30..180, 10, { "$it" }) { onChange(settings.copy(listPageSize = it)) } }
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
                    showNavigationLabels = defaults.showNavigationLabels,
                    artworkCornerPercent = defaults.artworkCornerPercent,
                    cornerRadiusPercent = defaults.cornerRadiusPercent,
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
    item { SettingsTitle("Цвет и обложка") }
    item { SettingSwitch("Цвет обложки в фоне", "Мягкий градиент цвета обложки сверху экрана", settings.coverColorBackground) { onChange(settings.copy(coverColorBackground = it)) } }
    item {
        AnimatedVisibility(!settings.coverColorBackground) {
            SettingSwitch("Лёгкий цветной фон", "Тот же градиент, но вдвое слабее", settings.playerBlurBackground) { onChange(settings.copy(playerBlurBackground = it)) }
        }
    }
    item { SettingSwitch("Акцент от обложки", "Кнопки и ползунок берут цвет обложки", settings.coverAccent) { onChange(settings.copy(coverAccent = it)) } }
    item { SettingSwitch("Плавная загрузка обложек", "Плавно показывает загруженное изображение", settings.animateImageLoading) { onChange(settings.copy(animateImageLoading = it)) } }
    item { SettingSlider("Размер обложки", settings.playerArtworkPercent, 70..110, 5, { "$it%" }) { onChange(settings.copy(playerArtworkPercent = it)) } }
    item { SettingsTitle("Очередь") }
    item { SettingSwitch("Предзагрузка следующего трека", "Готовит аудиоссылку заранее", settings.preloadNextTracks) { onChange(settings.copy(preloadNextTracks = it)) } }
    item { SettingsTitle("Компоновка") }
    PlayerLayout.entries.forEach { layout ->
        item { SettingRadio(if (layout == PlayerLayout.COVER_FIRST) "Сначала обложка" else "Сначала управление", settings.playerLayout == layout) { onChange(settings.copy(playerLayout = layout)) } }
    }
    item {
        SectionReset("Сбросить плеер") { defaults ->
            onChange(settings.copy(openPlayerOnTrack = defaults.openPlayerOnTrack, swipeMiniPlayer = defaults.swipeMiniPlayer, showMiniPlayerProgress = defaults.showMiniPlayerProgress, playerBlurBackground = defaults.playerBlurBackground, coverColorBackground = defaults.coverColorBackground, coverAccent = defaults.coverAccent, animateImageLoading = defaults.animateImageLoading, preloadNextTracks = defaults.preloadNextTracks, playerLayout = defaults.playerLayout, playerArtworkPercent = defaults.playerArtworkPercent, fullPlayerSwipe = defaults.fullPlayerSwipe))
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
            onChange(settings.copy(showLyricsByDefault = defaults.showLyricsByDefault, karaokeFill = defaults.karaokeFill, karaokeFillStyle = defaults.karaokeFillStyle, fadeLyrics = defaults.fadeLyrics, boldActiveLyrics = defaults.boldActiveLyrics, lyricsFontSize = defaults.lyricsFontSize, lyricsLineHeight = defaults.lyricsLineHeight, lyricsHorizontalPadding = defaults.lyricsHorizontalPadding, lyricsActiveScale = defaults.lyricsActiveScale, lyricsTapToSeek = defaults.lyricsTapToSeek, lyricsLineSpacing = defaults.lyricsLineSpacing, lyricsAnchorPercent = defaults.lyricsAnchorPercent, lyricsInactivePercent = defaults.lyricsInactivePercent, lyricsUnsungPercent = defaults.lyricsUnsungPercent, lyricsSungPercent = defaults.lyricsSungPercent, lyricsGlowBlur = defaults.lyricsGlowBlur, lyricsGlowAlpha = defaults.lyricsGlowAlpha, lyricsScrollSpeed = defaults.lyricsScrollSpeed, lyricsAlignment = defaults.lyricsAlignment, lyricsAutoScroll = defaults.lyricsAutoScroll, lyricsOffsetMs = defaults.lyricsOffsetMs))
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
        SettingSlider("Активная строка на высоте", settings.lyricsAnchorPercent, 25..55, 1, { "$it%" }) { onChange(settings.copy(lyricsAnchorPercent = it)) }
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

private fun LazyListScope.playbackSettings(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    item { SettingsTitle("Качество и устройство") }
    item { SettingSwitch("Экономия трафика", "Запрашивает поток с меньшим битрейтом", settings.dataSaver) { onChange(settings.copy(dataSaver = it)) } }
    item { SettingSwitch("Не выключать экран", "Пока приложение открыто", settings.keepScreenOn) { onChange(settings.copy(keepScreenOn = it)) } }
    item { SectionReset("Сбросить воспроизведение") { defaults -> onChange(settings.copy(dataSaver = defaults.dataSaver, keepScreenOn = defaults.keepScreenOn)) } }
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
    val reduceMotion = LocalReduceMotion.current
    val fill = if (reduceMotion || !settings.karaokeFill) 0.55f else {
        val transition = rememberInfiniteTransition(label = "lyrics-preview")
        val value by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
            label = "lyrics-preview-fill",
        )
        value
    }
    val fontSize = (settings.lyricsFontSize * 0.72f).sp
    val align = if (settings.lyricsAlignment == LyricsAlignment.CENTER) TextAlign.Center else TextAlign.Start
    val ink = MaterialTheme.colorScheme.onSurface
    val activeWeight = if (settings.boldActiveLyrics) FontWeight.Bold else FontWeight.SemiBold
    val glow = settings.karaokeFill && settings.karaokeFillStyle == KaraokeFillStyle.SOFT_GLOW && settings.lyricsGlowAlpha > 0
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(20.dp * LocalCornerRadiusScale.current))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = (settings.lyricsHorizontalPadding * 0.6f).dp, vertical = 14.dp),
    ) {
        Text(
            "Я верю в музыку",
            color = ink.copy(alpha = settings.lyricsInactivePercent / 100f),
            fontSize = fontSize,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
        )
        Box(Modifier.fillMaxWidth().padding(vertical = (settings.lyricsLineSpacing * 0.4f).dp)) {
            val style = TextStyle(fontSize = fontSize * (1f + settings.lyricsActiveScale / 100f), fontWeight = activeWeight, textAlign = align)
            Text(
                "Она всегда рядом",
                style = style,
                color = ink.copy(alpha = if (settings.karaokeFill) settings.lyricsUnsungPercent / 100f else 1f),
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
            )
            if (settings.karaokeFill) {
                Text(
                    "Она всегда рядом",
                    style = style.copy(
                        shadow = if (glow) Shadow(ink.copy(alpha = settings.lyricsGlowAlpha / 100f), blurRadius = settings.lyricsGlowBlur * 2f) else null,
                    ),
                    color = ink.copy(alpha = (settings.lyricsSungPercent / 100f).coerceAtMost(1f)),
                    modifier = Modifier.fillMaxWidth().clipToBounds().drawWithContent {
                        clipRect(right = size.width * fill) { this@drawWithContent.drawContent() }
                    },
                    maxLines = 1,
                )
            }
        }
        Text(
            "И звучит для меня",
            color = ink.copy(alpha = settings.lyricsInactivePercent / 100f),
            fontSize = fontSize,
            textAlign = align,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
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
    exportBusy: Boolean,
    onExportCollection: (share: Boolean) -> Unit,
) {
    item {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                Text(account?.displayName?.firstOrNull()?.uppercase() ?: "Я", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Black)
            }
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(account?.displayName ?: "Яндекс Музыка", fontWeight = FontWeight.Bold)
                Text(account?.login.orEmpty(), color = SoftText, fontSize = 13.sp)
            }
        }
    }
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
    item { SettingsTitle("Экспорт") }
    item {
        SettingsActionRow(
            Icons.Filled.FileDownload,
            if (exportBusy) "Готовим CSV…" else "Вся коллекция в CSV",
            "«Мне нравится» и все плейлисты в одном файле: исполнитель, название, альбом. Подходит для TuneMyMusic, Soundiiz и Excel",
            !exportBusy,
        ) { onExportCollection(false) }
    }
    item { SettingsActionRow(Icons.Filled.Share, "Отправить CSV коллекции", null, !exportBusy) { onExportCollection(true) } }
    item { SettingsTitle("Выход") }
    item { TextButton(onClick = onLogout, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Выйти и удалить токен", color = MaterialTheme.colorScheme.error) } }
}

@Composable
private fun SettingsHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Назад") }
        Text(title, fontSize = 23.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SettingsNavRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(22.dp)) }
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
        Switch(checked = checked, onCheckedChange = onChange)
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
        Slider(
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
}
