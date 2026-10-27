package ru.muz.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh
import ru.muz.settings.VocalMode

object AudioEffects {
    val FREQUENCIES = floatArrayOf(60f, 230f, 910f, 3600f, 14000f)
    const val MAX_GAIN_DB = 12

    @Volatile var mode: VocalMode = VocalMode.NORMAL
        private set
    @Volatile var bands: IntArray = IntArray(FREQUENCIES.size)
        private set
    @Volatile var crossfadeMs: Long = 0L
        private set
    @Volatile var version: Int = 0
        private set

    fun apply(mode: VocalMode, bands: List<Int>, crossfadeSeconds: Int) {
        val next = IntArray(FREQUENCIES.size) { bands.getOrElse(it) { 0 }.coerceIn(-MAX_GAIN_DB, MAX_GAIN_DB) }
        crossfadeMs = crossfadeSeconds.coerceIn(0, 12) * 1000L
        if (mode == this.mode && next.contentEquals(this.bands)) return
        this.mode = mode
        this.bands = next
        version++
    }
}

private class Biquad {
    var b0 = 1.0; var b1 = 0.0; var b2 = 0.0; var a1 = 0.0; var a2 = 0.0
    private var z1 = 0.0
    private var z2 = 0.0

    fun process(x: Double): Double {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y
    }

    fun reset() {
        z1 = 0.0
        z2 = 0.0
    }

    private fun set(nb0: Double, nb1: Double, nb2: Double, a0: Double, na1: Double, na2: Double) {
        b0 = nb0 / a0; b1 = nb1 / a0; b2 = nb2 / a0; a1 = na1 / a0; a2 = na2 / a0
    }

    fun peaking(rate: Double, freq: Double, gainDb: Double, q: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * freq / rate
        val alpha = sin(w) / (2 * q)
        val c = cos(w)
        set(1 + alpha * a, -2 * c, 1 - alpha * a, 1 + alpha / a, -2 * c, 1 - alpha / a)
    }

    fun lowShelf(rate: Double, freq: Double, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * freq / rate
        val c = cos(w)
        val alpha = sin(w) / 2 * sqrt(2.0)
        val s = 2 * sqrt(a) * alpha
        set(a * ((a + 1) - (a - 1) * c + s), 2 * a * ((a - 1) - (a + 1) * c), a * ((a + 1) - (a - 1) * c - s),
            (a + 1) + (a - 1) * c + s, -2 * ((a - 1) + (a + 1) * c), (a + 1) + (a - 1) * c - s)
    }

    fun highShelf(rate: Double, freq: Double, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * freq / rate
        val c = cos(w)
        val alpha = sin(w) / 2 * sqrt(2.0)
        val s = 2 * sqrt(a) * alpha
        set(a * ((a + 1) + (a - 1) * c + s), -2 * a * ((a - 1) + (a + 1) * c), a * ((a + 1) + (a - 1) * c - s),
            (a + 1) - (a - 1) * c + s, 2 * ((a - 1) - (a + 1) * c), (a + 1) - (a - 1) * c - s)
    }

    fun lowPass(rate: Double, freq: Double) {
        val w = 2 * PI * freq / rate
        val alpha = sin(w) / (2 * 0.7071)
        val c = cos(w)
        set((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }

    fun highPass(rate: Double, freq: Double) {
        val w = 2 * PI * freq / rate
        val alpha = sin(w) / (2 * 0.7071)
        val c = cos(w)
        set((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }
}

@UnstableApi
internal class EffectsProcessor : BaseAudioProcessor() {
    private var encoding = C.ENCODING_INVALID
    private var channels = 0
    private var rate = 44100.0
    private var seen = -1
    private var eq: Array<Array<Biquad>> = emptyArray()
    private var eqOn = false
    private var mode = VocalMode.NORMAL
    private val bassKeep = Biquad()
    private val voiceLow = Biquad()
    private val voiceHigh = Biquad()
    private val frame = DoubleArray(8)

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        if (inputAudioFormat.channelCount !in 1..8) return AudioProcessor.AudioFormat.NOT_SET
        encoding = inputAudioFormat.encoding
        channels = inputAudioFormat.channelCount
        rate = inputAudioFormat.sampleRate.toDouble()
        seen = -1
        return inputAudioFormat
    }

    private fun refresh() {
        val version = AudioEffects.version
        if (version == seen && eq.size == channels) return
        seen = version
        val gains = AudioEffects.bands
        mode = if (channels == 2) AudioEffects.mode else VocalMode.NORMAL
        eqOn = gains.any { it != 0 }
        val last = AudioEffects.FREQUENCIES.lastIndex
        eq = Array(channels) {
            Array(AudioEffects.FREQUENCIES.size) { band ->
                Biquad().apply {
                    val f = AudioEffects.FREQUENCIES[band].toDouble().coerceAtMost(rate * 0.45)
                    when (band) {
                        0 -> lowShelf(rate, f, gains[band].toDouble())
                        last -> highShelf(rate, f, gains[band].toDouble())
                        else -> peaking(rate, f, gains[band].toDouble(), 1.0)
                    }
                }
            }
        }
        bassKeep.lowPass(rate, 140.0)
        voiceLow.highPass(rate, 170.0)
        voiceHigh.lowPass(rate, 7000.0)
        bassKeep.reset()
        voiceLow.reset()
        voiceHigh.reset()
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        refresh()
        val output = replaceOutputBuffer(size)
        if (!eqOn && mode == VocalMode.NORMAL) {
            output.put(inputBuffer)
            output.flip()
            return
        }
        inputBuffer.order(ByteOrder.nativeOrder())
        output.order(ByteOrder.nativeOrder())
        val float = encoding == C.ENCODING_PCM_FLOAT
        val bytes = if (float) 4 else 2
        val frameBytes = bytes * channels
        val frames = size / frameBytes
        repeat(frames) {
            for (ch in 0 until channels) {
                frame[ch] = if (float) inputBuffer.float.toDouble() else inputBuffer.short / 32768.0
            }
            when (mode) {
                VocalMode.NO_VOCALS -> {
                    val mid = (frame[0] + frame[1]) * 0.5
                    val side = (frame[0] - frame[1]) * 0.5 * 1.6
                    val bass = bassKeep.process(mid)
                    frame[0] = bass + side
                    frame[1] = bass - side
                }
                VocalMode.VOCALS -> {
                    val mid = (frame[0] + frame[1]) * 0.5
                    val side = (frame[0] - frame[1]) * 0.5
                    val voice = voiceHigh.process(voiceLow.process(mid - side * 0.35)) * 1.3
                    frame[0] = voice
                    frame[1] = voice
                }
                VocalMode.NORMAL -> Unit
            }
            for (ch in 0 until channels) {
                var v = frame[ch]
                if (eqOn) for (filter in eq[ch]) v = filter.process(v)
                val a = abs(v)
                if (a > 0.92) v = (if (v < 0) -1.0 else 1.0) * (0.92 + 0.08 * tanh((a - 0.92) / 0.08))
                if (float) output.putFloat(v.toFloat()) else output.putShort((v * 32767.0).toInt().coerceIn(-32768, 32767).toShort())
            }
        }
        inputBuffer.position(inputBuffer.limit())
        output.flip()
    }

    override fun onFlush() {
        seen = -1
    }
}
