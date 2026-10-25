package ru.muz.lyrics

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

enum class KaraokeFillStyle {
    SOLID,
    SOFT_GLOW,
    WORD_PULSE,
}

enum class KaraokeDirection {
    HORIZONTAL,
    VERTICAL,
}

data class LyricsSettings(
    val fontSize: TextUnit = 29.sp,
    val lineHeight: TextUnit = 40.sp,
    val lineSpacingDp: Int = 18,
    val horizontalPaddingDp: Int = 24,
    val anchorFraction: Float = 0.38f,
    val textAlign: TextAlign = TextAlign.Start,
    val sungAlpha: Float = 1.0f,
    val activeUnsungAlpha: Float = 0.67f,
    val inactiveAlpha: Float = 0.63f,
    val activeScale: Float = 0.018f,
    val fadeEdges: Boolean = true,
    val karaokeFill: Boolean = true,
    val karaokeFillStyle: KaraokeFillStyle = KaraokeFillStyle.SOFT_GLOW,
    val glowBlurDp: Float = 7f,
    val glowAlpha: Float = 0.22f,
    val scrollDurationMs: Int = 360,
    val boldActiveLine: Boolean = true,
    val tapToSeek: Boolean = true,
    val autoScroll: Boolean = true,
    val showTranslation: Boolean = false,
    val blurredCoverBackground: Boolean = false,
    val modern: Boolean = true,
    val blurDistant: Boolean = true,
    val fillDirection: KaraokeDirection = KaraokeDirection.HORIZONTAL,
) {
    companion object {
        val MEASURED = LyricsSettings()
    }
}
