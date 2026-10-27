package ru.muz.settings

import android.content.Context
import ru.muz.lyrics.KaraokeDirection
import ru.muz.lyrics.KaraokeFillStyle

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }
enum class AccentStyle { YELLOW, ORANGE, PINK, BLUE, GREEN, CUSTOM }
enum class PlayerAction { QUEUE, DOWNLOAD, LINK, SHARE_FILE, SAVE_COVER, OPEN_COVER, SLEEP, SPEED, QUOTE, COPY_ARTIST, TRACK_WAVE, DISLIKE, PRIVATE }
enum class PlayShape { CIRCLE, SQUIRCLE, SQUARE, COOKIE }
enum class AppStyle { CLASSIC, MATERIAL_YOU, GLASS, MINIMAL }
enum class DoubleTapAction { LIKE, PLAY_PAUSE, NEXT }
enum class HomeSection { WAVE, RECENT, PLAYLISTS, CHART }
enum class AppTab { HOME, LIBRARY, SEARCH, STATS }
enum class AppFont { SYSTEM, SANS, SERIF, MONO, CONDENSED }
enum class PlayerLayout { COVER_FIRST, CONTROLS_FIRST }
enum class LyricsAlignment { LEFT, CENTER }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColors: Boolean = true,
    val accentStyle: AccentStyle = AccentStyle.YELLOW,
    val compactLists: Boolean = false,
    val showNavigationLabels: Boolean = true,
    val artworkCornerPercent: Int = 100,
    val cornerRadiusPercent: Int = 100,
    val listScalePercent: Int = 100,
    val listPageSize: Int = 60,
    val openPlayerOnTrack: Boolean = true,
    val swipeMiniPlayer: Boolean = true,
    val showMiniPlayerProgress: Boolean = true,
    val playerBlurBackground: Boolean = false,
    val coverColorBackground: Boolean = true,
    val coverAccent: Boolean = true,
    val preloadNextTracks: Boolean = true,
    val animateImageLoading: Boolean = true,
    val playerLayout: PlayerLayout = PlayerLayout.COVER_FIRST,
    val playerArtworkPercent: Int = 100,
    val fullPlayerSwipe: Boolean = true,
    val showLyricsByDefault: Boolean = false,
    val karaokeFill: Boolean = true,
    val karaokeFillStyle: KaraokeFillStyle = KaraokeFillStyle.SOFT_GLOW,
    val lyricsGlowBlur: Int = 7,
    val lyricsGlowAlpha: Int = 22,
    val lyricsScrollSpeed: Int = 360,
    val fadeLyrics: Boolean = true,
    val boldActiveLyrics: Boolean = true,
    val lyricsFontSize: Int = 29,
    val lyricsLineSpacing: Int = 18,
    val lyricsAnchorPercent: Int = 38,
    val lyricsInactivePercent: Int = 50,
    val lyricsUnsungPercent: Int = 67,
    val lyricsSungPercent: Int = 100,
    val lyricsAlignment: LyricsAlignment = LyricsAlignment.LEFT,
    val lyricsLineHeight: Int = 40,
    val lyricsHorizontalPadding: Int = 24,
    val lyricsActiveScale: Int = 2,
    val lyricsTapToSeek: Boolean = true,
    val dataSaver: Boolean = false,
    val keepScreenOn: Boolean = false,
    val animationSpeedPercent: Int = 100,
    val reduceMotion: Boolean = false,
    val animatePlayerTransitions: Boolean = true,
    val animateLoading: Boolean = true,
    val lyricsAutoScroll: Boolean = true,
    val lyricsOffsetMs: Int = 0,
    val syncIntervalMinutes: Int = 60,
    val preloadArtwork: Boolean = true,
    val keepSearchHistory: Int = 12,
    val reportListening: Boolean = true,
    val cacheSizeMb: Int = 1024,
    val autoCheckUpdates: Boolean = true,
    val playbackSpeedPercent: Int = 100,
    val skipSilence: Boolean = false,
    val customAccent: Int = 0xFFB388FF.toInt(),
    val appFont: AppFont = AppFont.SYSTEM,
    val textScalePercent: Int = 100,
    val playerActions: List<PlayerAction> = listOf(PlayerAction.QUEUE, PlayerAction.DOWNLOAD, PlayerAction.SLEEP, PlayerAction.QUOTE),
    val playShape: PlayShape = PlayShape.CIRCLE,
    val miniShowPrevious: Boolean = false,
    val homeSections: List<HomeSection> = listOf(HomeSection.WAVE, HomeSection.RECENT, HomeSection.PLAYLISTS, HomeSection.CHART),
    val tabs: List<AppTab> = AppTab.entries.toList(),
    val startTab: AppTab = AppTab.HOME,
    val lyricsModern: Boolean = true,
    val lyricsBlur: Boolean = true,
    val lyricsAutoHide: Boolean = true,
    val lyricsModernAnchor: Int = 20,
    val karaokeDirection: KaraokeDirection = KaraokeDirection.HORIZONTAL,
    val wavySeekBar: Boolean = true,
    val accentControls: Boolean = true,
    val speedChangesPitch: Boolean = false,
    val pulseBackground: Boolean = true,
    val pulseStrength: Int = 60,
    val coverSparks: Boolean = true,
    val doubleTapAction: DoubleTapAction = DoubleTapAction.LIKE,
    val appStyle: AppStyle = AppStyle.CLASSIC,
    val glassStrength: Int = 60,
    val privateSession: Boolean = false,
    val normalizeVolume: Boolean = true,
    val waveMood: String = "all",
    val waveDiversity: String = "default",
    val waveLanguage: String = "any",
    val hideExplicit: Boolean = false,
    val doubleTapLike: Boolean = true,
    val haptics: Boolean = true,
)

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings {
        val migratedTheme = if (prefs.getBoolean("pure_black", false)) {
            ThemeMode.AMOLED
        } else {
            enumValue(prefs.getString("theme_mode", null), ThemeMode.DARK)
        }
        return AppSettings(
            themeMode = migratedTheme,
            useDynamicColors = prefs.getBoolean("dynamic_colors", true),
            accentStyle = enumValue(prefs.getString("accent_style", null), AccentStyle.YELLOW),
            compactLists = prefs.getBoolean("compact_lists", false),
            showNavigationLabels = prefs.getBoolean("navigation_labels", true),
            artworkCornerPercent = prefs.getInt("artwork_corners", 100).coerceIn(0, 160),
            cornerRadiusPercent = prefs.getInt("corner_radius", 100).coerceIn(0, 160),
            listScalePercent = prefs.getInt("list_scale", 100).coerceIn(80, 125),
            listPageSize = prefs.getInt("list_page_size", 60).coerceIn(30, 180),
            openPlayerOnTrack = prefs.getBoolean("open_player", true),
            swipeMiniPlayer = prefs.getBoolean("swipe_mini_player", true),
            showMiniPlayerProgress = prefs.getBoolean("mini_player_progress", true),
            playerBlurBackground = prefs.getBoolean("player_blur", false),
            coverColorBackground = prefs.getBoolean("cover_color_background", true),
            coverAccent = prefs.getBoolean("cover_accent", true),
            preloadNextTracks = prefs.getBoolean("preload_next", true),
            animateImageLoading = prefs.getBoolean("animate_image_loading", true),
            playerLayout = enumValue(prefs.getString("player_layout", null), PlayerLayout.COVER_FIRST),
            playerArtworkPercent = prefs.getInt("player_artwork_size", 100).coerceIn(70, 110),
            fullPlayerSwipe = prefs.getBoolean("full_player_swipe", true),
            showLyricsByDefault = prefs.getBoolean("lyrics_default", false),
            karaokeFill = prefs.getBoolean("karaoke", true),
            karaokeFillStyle = enumValue(prefs.getString("karaoke_style", null), KaraokeFillStyle.SOFT_GLOW),
            lyricsGlowBlur = prefs.getInt("lyrics_glow_blur", 7).coerceIn(0, 18),
            lyricsGlowAlpha = prefs.getInt("lyrics_glow_alpha", 22).coerceIn(0, 45),
            lyricsScrollSpeed = prefs.getInt("lyrics_scroll_speed", 360).coerceIn(120, 900),
            fadeLyrics = prefs.getBoolean("fade_lyrics", true),
            boldActiveLyrics = prefs.getBoolean("bold_lyrics", true),
            lyricsFontSize = prefs.getInt("lyrics_font", 29).coerceIn(20, 38),
            lyricsLineSpacing = prefs.getInt("lyrics_spacing", 18).coerceIn(4, 34),
            lyricsAnchorPercent = prefs.getInt("lyrics_anchor", 38).coerceIn(25, 55),
            lyricsInactivePercent = prefs.getInt("lyrics_inactive", 50).coerceIn(20, 70),
            lyricsUnsungPercent = prefs.getInt("lyrics_unsung", 67).coerceIn(40, 85),
            lyricsSungPercent = prefs.getInt("lyrics_sung", 100).coerceIn(70, 125),
            lyricsAlignment = enumValue(prefs.getString("lyrics_alignment", null), LyricsAlignment.LEFT),
            lyricsLineHeight = prefs.getInt("lyrics_line_height", 40).coerceIn(28, 64),
            lyricsHorizontalPadding = prefs.getInt("lyrics_horizontal_padding", 24).coerceIn(8, 48),
            lyricsActiveScale = prefs.getInt("lyrics_active_scale", 2).coerceIn(0, 5),
            lyricsTapToSeek = prefs.getBoolean("lyrics_tap_to_seek", true),
            dataSaver = prefs.getBoolean("data_saver", false),
            keepScreenOn = prefs.getBoolean("keep_screen_on", false),
            animationSpeedPercent = prefs.getInt("animation_speed", 100).coerceIn(50, 200),
            reduceMotion = prefs.getBoolean("reduce_motion", false),
            animatePlayerTransitions = prefs.getBoolean("animate_player_transitions", true),
            animateLoading = prefs.getBoolean("animate_loading", true),
            lyricsAutoScroll = prefs.getBoolean("lyrics_auto_scroll", true),
            lyricsOffsetMs = prefs.getInt("lyrics_offset_ms", 0).coerceIn(-5000, 5000),
            syncIntervalMinutes = prefs.getInt("sync_interval_minutes", 60).coerceIn(0, 1440),
            preloadArtwork = prefs.getBoolean("preload_artwork", true),
            keepSearchHistory = prefs.getInt("keep_search_history", 12).coerceIn(0, 30),
            reportListening = prefs.getBoolean("report_listening", true),
            cacheSizeMb = prefs.getInt("cache_size_mb", 1024).coerceIn(0, 8192),
            autoCheckUpdates = prefs.getBoolean("auto_check_updates", true),
            playbackSpeedPercent = prefs.getInt("playback_speed", 100).coerceIn(50, 200),
            skipSilence = prefs.getBoolean("skip_silence", false),
            customAccent = prefs.getInt("custom_accent", 0xFFB388FF.toInt()),
            appFont = enumValue(prefs.getString("app_font", null), AppFont.SYSTEM),
            textScalePercent = prefs.getInt("text_scale", 100).coerceIn(80, 130),
            playerActions = enumList(prefs.getString("player_actions", null), AppSettings().playerActions),
            playShape = enumValue(prefs.getString("play_shape", null), PlayShape.CIRCLE),
            miniShowPrevious = prefs.getBoolean("mini_previous", false),
            homeSections = enumList(prefs.getString("home_sections", null), AppSettings().homeSections),
            tabs = enumList(prefs.getString("tabs", null), AppSettings().tabs).ifEmpty { AppSettings().tabs },
            startTab = enumValue(prefs.getString("start_tab", null), AppTab.HOME),
            lyricsModern = prefs.getBoolean("lyrics_modern", true),
            lyricsBlur = prefs.getBoolean("lyrics_blur", true),
            lyricsAutoHide = prefs.getBoolean("lyrics_auto_hide", true),
            lyricsModernAnchor = prefs.getInt("lyrics_modern_anchor", 20).coerceIn(10, 45),
            karaokeDirection = enumValue(prefs.getString("karaoke_direction", null), KaraokeDirection.HORIZONTAL),
            wavySeekBar = prefs.getBoolean("wavy_seek_bar", true),
            accentControls = prefs.getBoolean("accent_controls", true),
            speedChangesPitch = prefs.getBoolean("speed_changes_pitch", false),
            pulseBackground = prefs.getBoolean("pulse_background", true),
            pulseStrength = prefs.getInt("pulse_strength", 60).coerceIn(10, 100),
            coverSparks = prefs.getBoolean("cover_sparks", true),
            doubleTapAction = enumValue(prefs.getString("double_tap_action", null), DoubleTapAction.LIKE),
            appStyle = enumValue(prefs.getString("app_style", null), AppStyle.CLASSIC),
            glassStrength = prefs.getInt("glass_strength", 60).coerceIn(10, 100),
            privateSession = prefs.getBoolean("private_session", false),
            normalizeVolume = prefs.getBoolean("normalize_volume", true),
            waveMood = prefs.getString("wave_mood", null) ?: "all",
            waveDiversity = prefs.getString("wave_diversity", null) ?: "default",
            waveLanguage = prefs.getString("wave_language", null) ?: "any",
            hideExplicit = prefs.getBoolean("hide_explicit", false),
            doubleTapLike = prefs.getBoolean("double_tap_like", true),
            haptics = prefs.getBoolean("haptics", true),
        )
    }

    fun save(value: AppSettings) {
        prefs.edit()
            .putString("theme_mode", value.themeMode.name)
            .putBoolean("dynamic_colors", value.useDynamicColors)
            .putString("accent_style", value.accentStyle.name)
            .putBoolean("compact_lists", value.compactLists)
            .putBoolean("navigation_labels", value.showNavigationLabels)
            .putInt("artwork_corners", value.artworkCornerPercent)
            .putInt("corner_radius", value.cornerRadiusPercent)
            .putInt("list_scale", value.listScalePercent)
            .putInt("list_page_size", value.listPageSize)
            .putBoolean("open_player", value.openPlayerOnTrack)
            .putBoolean("swipe_mini_player", value.swipeMiniPlayer)
            .putBoolean("mini_player_progress", value.showMiniPlayerProgress)
            .putBoolean("player_blur", value.playerBlurBackground)
            .putBoolean("cover_color_background", value.coverColorBackground)
            .putBoolean("cover_accent", value.coverAccent)
            .putBoolean("preload_next", value.preloadNextTracks)
            .putBoolean("animate_image_loading", value.animateImageLoading)
            .putString("player_layout", value.playerLayout.name)
            .putInt("player_artwork_size", value.playerArtworkPercent)
            .putBoolean("full_player_swipe", value.fullPlayerSwipe)
            .putBoolean("lyrics_default", value.showLyricsByDefault)
            .putBoolean("karaoke", value.karaokeFill)
            .putString("karaoke_style", value.karaokeFillStyle.name)
            .putInt("lyrics_glow_blur", value.lyricsGlowBlur)
            .putInt("lyrics_glow_alpha", value.lyricsGlowAlpha)
            .putInt("lyrics_scroll_speed", value.lyricsScrollSpeed)
            .putBoolean("fade_lyrics", value.fadeLyrics)
            .putBoolean("bold_lyrics", value.boldActiveLyrics)
            .putInt("lyrics_font", value.lyricsFontSize)
            .putInt("lyrics_spacing", value.lyricsLineSpacing)
            .putInt("lyrics_anchor", value.lyricsAnchorPercent)
            .putInt("lyrics_inactive", value.lyricsInactivePercent)
            .putInt("lyrics_unsung", value.lyricsUnsungPercent)
            .putInt("lyrics_sung", value.lyricsSungPercent)
            .putString("lyrics_alignment", value.lyricsAlignment.name)
            .putInt("lyrics_line_height", value.lyricsLineHeight)
            .putInt("lyrics_horizontal_padding", value.lyricsHorizontalPadding)
            .putInt("lyrics_active_scale", value.lyricsActiveScale)
            .putBoolean("lyrics_tap_to_seek", value.lyricsTapToSeek)
            .putBoolean("data_saver", value.dataSaver)
            .putBoolean("keep_screen_on", value.keepScreenOn)
            .putInt("animation_speed", value.animationSpeedPercent)
            .putBoolean("reduce_motion", value.reduceMotion)
            .putBoolean("animate_player_transitions", value.animatePlayerTransitions)
            .putBoolean("animate_loading", value.animateLoading)
            .putBoolean("lyrics_auto_scroll", value.lyricsAutoScroll)
            .putInt("lyrics_offset_ms", value.lyricsOffsetMs)
            .putInt("sync_interval_minutes", value.syncIntervalMinutes)
            .putBoolean("preload_artwork", value.preloadArtwork)
            .putInt("keep_search_history", value.keepSearchHistory)
            .putBoolean("report_listening", value.reportListening)
            .putInt("cache_size_mb", value.cacheSizeMb)
            .putBoolean("auto_check_updates", value.autoCheckUpdates)
            .putInt("playback_speed", value.playbackSpeedPercent)
            .putBoolean("skip_silence", value.skipSilence)
            .putInt("custom_accent", value.customAccent)
            .putString("app_font", value.appFont.name)
            .putInt("text_scale", value.textScalePercent)
            .putString("player_actions", value.playerActions.joinToString(",") { it.name })
            .putString("play_shape", value.playShape.name)
            .putBoolean("mini_previous", value.miniShowPrevious)
            .putString("home_sections", value.homeSections.joinToString(",") { it.name })
            .putString("tabs", value.tabs.joinToString(",") { it.name })
            .putString("start_tab", value.startTab.name)
            .putBoolean("lyrics_modern", value.lyricsModern)
            .putBoolean("lyrics_blur", value.lyricsBlur)
            .putBoolean("lyrics_auto_hide", value.lyricsAutoHide)
            .putInt("lyrics_modern_anchor", value.lyricsModernAnchor)
            .putString("karaoke_direction", value.karaokeDirection.name)
            .putBoolean("wavy_seek_bar", value.wavySeekBar)
            .putBoolean("accent_controls", value.accentControls)
            .putBoolean("speed_changes_pitch", value.speedChangesPitch)
            .putBoolean("pulse_background", value.pulseBackground)
            .putInt("pulse_strength", value.pulseStrength)
            .putBoolean("cover_sparks", value.coverSparks)
            .putString("double_tap_action", value.doubleTapAction.name)
            .putString("app_style", value.appStyle.name)
            .putInt("glass_strength", value.glassStrength)
            .putBoolean("private_session", value.privateSession)
            .putBoolean("normalize_volume", value.normalizeVolume)
            .putString("wave_mood", value.waveMood)
            .putString("wave_diversity", value.waveDiversity)
            .putString("wave_language", value.waveLanguage)
            .putBoolean("hide_explicit", value.hideExplicit)
            .putBoolean("double_tap_like", value.doubleTapLike)
            .putBoolean("haptics", value.haptics)
            .remove("pure_black")
            .remove("prefetch_pages")
            .apply()
    }

    fun exportJson(): String {
        val root = org.json.JSONObject()
        root.put("app", "Aria")
        root.put("version", 1)
        val values = org.json.JSONObject()
        prefs.all.toSortedMap().forEach { (key, value) ->
            val typed = org.json.JSONObject()
            when (value) {
                is Boolean -> typed.put("b", value)
                is Int -> typed.put("i", value)
                is Long -> typed.put("l", value)
                is Float -> typed.put("f", value.toDouble())
                is String -> typed.put("s", value)
                else -> return@forEach
            }
            values.put(key, typed)
        }
        root.put("settings", values)
        return root.toString(2)
    }

    fun importJson(raw: String): Boolean {
        val root = runCatching { org.json.JSONObject(raw) }.getOrNull() ?: return false
        if (root.optString("app") != "Aria") return false
        val values = root.optJSONObject("settings") ?: return false
        val editor = prefs.edit().clear()
        values.keys().forEach { key ->
            val typed = values.optJSONObject(key) ?: return@forEach
            when {
                typed.has("b") -> editor.putBoolean(key, typed.getBoolean("b"))
                typed.has("i") -> editor.putInt(key, typed.getInt("i"))
                typed.has("l") -> editor.putLong(key, typed.getLong("l"))
                typed.has("f") -> editor.putFloat(key, typed.getDouble("f").toFloat())
                typed.has("s") -> editor.putString(key, typed.getString("s"))
            }
        }
        editor.commit()
        return true
    }

    private inline fun <reified T : Enum<T>> enumList(raw: String?, fallback: List<T>): List<T> {
        if (raw == null) return fallback
        if (raw.isBlank()) return emptyList()
        return raw.split(',').mapNotNull { name -> enumValues<T>().firstOrNull { it.name == name.trim() } }.distinct()
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
