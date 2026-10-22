package ru.muz.settings

import android.content.Context
import ru.muz.lyrics.KaraokeFillStyle

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }
enum class AccentStyle { YELLOW, ORANGE, PINK, BLUE, GREEN }
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
            .remove("pure_black")
            .remove("prefetch_pages")
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumValue(raw: String?, fallback: T): T =
        enumValues<T>().firstOrNull { it.name == raw } ?: fallback
}
