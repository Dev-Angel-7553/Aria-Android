package ru.muz.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

object ColorMath {
    const val BLACK = 0xFF000000.toInt()
    const val WHITE = 0xFFFFFFFF.toInt()

    fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int =
        (a.coerceIn(0, 255) shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)

    fun red(c: Int) = (c shr 16) and 0xFF
    fun green(c: Int) = (c shr 8) and 0xFF
    fun blue(c: Int) = c and 0xFF
    fun alpha(c: Int) = (c ushr 24) and 0xFF

    fun blend(from: Int, to: Int, t: Float): Int {
        val k = t.coerceIn(0f, 1f)
        fun mix(a: Int, b: Int) = (a + (b - a) * k + 0.5f).toInt()
        return argb(mix(red(from), red(to)), mix(green(from), green(to)), mix(blue(from), blue(to)))
    }

    fun luminance(c: Int): Double {
        fun channel(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(red(c)) + 0.7152 * channel(green(c)) + 0.0722 * channel(blue(c))
    }

    fun contrast(a: Int, b: Int): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
    }

    fun contentColorFor(background: Int, dark: Int = 0xFF16130A.toInt(), light: Int = WHITE): Int =
        if (contrast(dark, background) >= contrast(light, background)) dark else light

    fun ensureContrast(color: Int, background: Int, minimum: Double): Int {
        if (contrast(color, background) >= minimum) return color
        val target = if (luminance(background) > 0.18) BLACK else WHITE
        var t = 0f
        var result = color
        while (t < 1f && contrast(result, background) < minimum) {
            t += 0.02f
            result = blend(color, target, t)
        }
        return result
    }

    fun hsl(c: Int): FloatArray {
        val r = red(c) / 255f
        val g = green(c) / 255f
        val b = blue(c) / 255f
        val maxC = max(r, max(g, b))
        val minC = min(r, min(g, b))
        val l = (maxC + minC) / 2f
        val d = maxC - minC
        if (d == 0f) return floatArrayOf(0f, 0f, l)
        val s = d / (1f - abs(2f * l - 1f))
        val h = when (maxC) {
            r -> 60f * (((g - b) / d) % 6f)
            g -> 60f * ((b - r) / d + 2f)
            else -> 60f * ((r - g) / d + 4f)
        }.let { if (it < 0) it + 360f else it }
        return floatArrayOf(h, s.coerceIn(0f, 1f), l)
    }

    fun fromHsl(h: Float, s: Float, l: Float): Int {
        val c = (1f - abs(2f * l - 1f)) * s
        val hh = ((h % 360f) + 360f) % 360f / 60f
        val x = c * (1f - abs(hh % 2f - 1f))
        val (r, g, b) = when (hh.toInt()) {
            0 -> Triple(c, x, 0f)
            1 -> Triple(x, c, 0f)
            2 -> Triple(0f, c, x)
            3 -> Triple(0f, x, c)
            4 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        val m = l - c / 2f
        return argb(((r + m) * 255f + 0.5f).toInt(), ((g + m) * 255f + 0.5f).toInt(), ((b + m) * 255f + 0.5f).toInt())
    }

    fun dominantVibrant(pixels: IntArray): Int? {
        if (pixels.isEmpty()) return null
        val buckets = 24
        val weight = DoubleArray(buckets)
        val sumR = DoubleArray(buckets)
        val sumG = DoubleArray(buckets)
        val sumB = DoubleArray(buckets)
        var greyWeight = 0.0
        var greyR = 0.0
        var greyG = 0.0
        var greyB = 0.0
        for (pixel in pixels) {
            if (alpha(pixel) < 128) continue
            val (h, s, l) = hsl(pixel).let { Triple(it[0], it[1], it[2]) }
            if (l < 0.08f || l > 0.95f) continue
            if (s < 0.18f) {
                greyWeight += 1.0
                greyR += red(pixel); greyG += green(pixel); greyB += blue(pixel)
                continue
            }
            val w = s * (1.0 - abs(l - 0.5) * 1.4).coerceAtLeast(0.1)
            val index = (h / 360f * buckets).toInt().coerceIn(0, buckets - 1)
            weight[index] += w
            sumR[index] += red(pixel) * w
            sumG[index] += green(pixel) * w
            sumB[index] += blue(pixel) * w
        }
        val best = weight.indices.maxByOrNull { weight[it] + 0.6 * (weight[(it + 1) % buckets] + weight[(it + buckets - 1) % buckets]) }
        val colourful = best?.let { weight[it] } ?: 0.0
        if (best == null || colourful < 0.04 * pixels.size) {
            if (greyWeight == 0.0) return null
            return argb((greyR / greyWeight).toInt(), (greyG / greyWeight).toInt(), (greyB / greyWeight).toInt())
        }
        return argb((sumR[best] / colourful).toInt(), (sumG[best] / colourful).toInt(), (sumB[best] / colourful).toInt())
    }

    fun playerAccent(cover: Int, background: Int): Int {
        val (h, s, l) = hsl(cover).let { Triple(it[0], it[1], it[2]) }
        val dark = luminance(background) < 0.18
        val lively = if (s < 0.18f) {
            fromHsl(h, s, if (dark) l.coerceIn(0.62f, 0.82f) else l.coerceIn(0.25f, 0.42f))
        } else {
            fromHsl(h, s.coerceIn(0.45f, 0.9f), if (dark) l.coerceIn(0.55f, 0.72f) else l.coerceIn(0.32f, 0.45f))
        }
        return ensureContrast(lively, background, 3.0)
    }
}
