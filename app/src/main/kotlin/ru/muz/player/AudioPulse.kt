package ru.muz.player

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import kotlin.math.sqrt

object AudioPulse {
    @Volatile var level: Float = 0f
        private set
    @Volatile var beats: Long = 0L
        private set
    @Volatile var updatedAt: Long = 0L
        private set
    private var average = 0f
    private var lastBeat = 0L

    internal fun push(rms: Float) {
        val now = SystemClock.elapsedRealtime()
        average += (rms - average) * 0.03f
        level = rms
        updatedAt = now
        if (rms > 0.03f && rms > average * 1.32f && now - lastBeat > 240) {
            lastBeat = now
            beats++
        }
    }

    internal fun reset() {
        level = 0f
        average = 0f
    }
}

@UnstableApi
internal class PulseProcessor : BaseAudioProcessor() {
    private var encoding = C.ENCODING_INVALID

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT && inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT) {
            return AudioProcessor.AudioFormat.NOT_SET
        }
        encoding = inputAudioFormat.encoding
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        AudioPulse.push(measure(inputBuffer))
        val output = replaceOutputBuffer(size)
        output.put(inputBuffer)
        output.flip()
    }

    override fun onFlush() {
        AudioPulse.reset()
    }

    private fun measure(buffer: ByteBuffer): Float {
        val start = buffer.position()
        val end = buffer.limit()
        var sum = 0.0
        var count = 0
        if (encoding == C.ENCODING_PCM_FLOAT) {
            var i = start
            while (i + 4 <= end) {
                val v = buffer.getFloat(i)
                sum += v * v
                count++
                i += 16
            }
        } else {
            var i = start
            while (i + 2 <= end) {
                val v = buffer.getShort(i) / 32768f
                sum += v * v
                count++
                i += 8
            }
        }
        return if (count == 0) 0f else sqrt(sum / count).toFloat()
    }
}
