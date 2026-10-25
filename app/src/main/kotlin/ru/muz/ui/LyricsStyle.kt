package ru.muz.ui

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import ru.muz.lyrics.LyricLine
import ru.muz.lyrics.LyricWord
import ru.muz.lyrics.Lyrics
import ru.muz.lyrics.LyricsSettings
import ru.muz.settings.AppSettings
import ru.muz.settings.LyricsAlignment

internal fun lyricsSettingsFor(settings: AppSettings, scale: Float = 1f): LyricsSettings = LyricsSettings.MEASURED.copy(
    modern = settings.lyricsModern,
    blurDistant = settings.lyricsBlur,
    fillDirection = settings.karaokeDirection,
    fontSize = (settings.lyricsFontSize * scale).sp,
    lineHeight = (settings.lyricsLineHeight * scale).sp,
    lineSpacingDp = (settings.lyricsLineSpacing * scale).toInt(),
    horizontalPaddingDp = (settings.lyricsHorizontalPadding * scale).toInt(),
    anchorFraction = (if (settings.lyricsModern) settings.lyricsModernAnchor else settings.lyricsAnchorPercent) / 100f,
    activeUnsungAlpha = settings.lyricsUnsungPercent / 100f,
    sungAlpha = (settings.lyricsSungPercent / 100f).coerceIn(0.7f, 1.25f),
    inactiveAlpha = settings.lyricsInactivePercent / 100f,
    textAlign = if (settings.lyricsAlignment == LyricsAlignment.CENTER) TextAlign.Center else TextAlign.Start,
    fadeEdges = settings.fadeLyrics,
    karaokeFill = settings.karaokeFill,
    karaokeFillStyle = settings.karaokeFillStyle,
    glowBlurDp = settings.lyricsGlowBlur.toFloat(),
    glowAlpha = settings.lyricsGlowAlpha / 100f,
    scrollDurationMs = settings.lyricsScrollSpeed,
    boldActiveLine = settings.boldActiveLyrics,
    activeScale = settings.lyricsActiveScale / 100f,
    tapToSeek = false,
    autoScroll = true,
).let { if (scale == 1f) it.copy(tapToSeek = settings.lyricsTapToSeek, autoScroll = settings.lyricsAutoScroll) else it }

internal object SampleLyrics {
    private val lines = listOf(
        "Ночной город дышит тихо",
        "Фонари ведут меня домой",
        "",
        "Я включаю нашу песню",
        "И она звучит со мной",
        "Пусть играет до рассвета",
        "Пока горят огни",
    )
    private const val LINE_MS = 3_200L
    private const val PAUSE_MS = 6_400L

    val lyrics: Lyrics by lazy {
        var start = 800L
        val built = lines.map { text ->
            val length = if (text.isBlank()) PAUSE_MS else LINE_MS
            val words = if (text.isBlank()) emptyList() else {
                val parts = text.split(" ")
                val sung = length - 500
                val step = sung / parts.size
                parts.mapIndexed { i, word ->
                    LyricWord(start + i * step, start + (i + 1) * step, if (i < parts.lastIndex) "$word " else word)
                }
            }
            val line = LyricLine(start, start + length, text, words)
            start += length
            line
        }
        Lyrics(built, synced = true)
    }

    val lengthMs: Long get() = lyrics.lines.last().endMs + 1_500
}
