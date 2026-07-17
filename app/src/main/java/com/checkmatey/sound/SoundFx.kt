package com.checkmatey.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Sfx { MOVE, CAPTURE, CHECK, WIN, LOSE }

/**
 * Chess sound effects synthesized in code (short decaying tones) — no audio assets needed.
 * Each play spins a tiny AudioTrack on a background thread and is fully guarded, so audio can
 * never crash or block the UI. (Not unit-tested; it's pure I/O — verified by ear on device.)
 */
class SoundFx {

    private val sampleRate = 44100

    private val samples: Map<Sfx, ShortArray> = mapOf(
        Sfx.MOVE to tone(listOf(196.0 to 1.0), durationMs = 85, decay = 42.0, volume = 0.45),
        Sfx.CAPTURE to tone(listOf(130.0 to 1.0, 260.0 to 0.4), durationMs = 120, decay = 30.0, volume = 0.6),
        Sfx.CHECK to sequence(listOf(660.0 to 70, 990.0 to 90), volume = 0.5),
        Sfx.WIN to sequence(listOf(523.0 to 80, 659.0 to 80, 784.0 to 150), volume = 0.5),
        Sfx.LOSE to sequence(listOf(392.0 to 110, 311.0 to 170), volume = 0.5),
    )

    fun play(sfx: Sfx) {
        val pcm = samples[sfx] ?: return
        try {
            Thread {
                try {
                    val track = build(pcm.size)
                    track.play()
                    track.write(pcm, 0, pcm.size)
                    Thread.sleep(pcm.size * 1000L / sampleRate + 60)
                    track.stop()
                    track.release()
                } catch (_: Throwable) {
                    // Audio is non-essential; never let it surface.
                }
            }.start()
        } catch (_: Throwable) {
        }
    }

    private fun build(numSamples: Int): AudioTrack {
        val minBuf = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val bufSize = maxOf(numSamples * 2, minBuf)
        return AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            bufSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE,
        )
    }

    /** A short decaying tone made of [partials] (freq to relative amplitude). */
    private fun tone(partials: List<Pair<Double, Double>>, durationMs: Int, decay: Double, volume: Double): ShortArray {
        val n = durationMs * sampleRate / 1000
        val ampSum = partials.sumOf { it.second }
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * decay)
            var s = 0.0
            for ((freq, amp) in partials) s += amp * sin(2 * PI * freq * t)
            val v = (s / ampSum) * env * volume * Short.MAX_VALUE
            out[i] = v.coerceIn(-32767.0, 32767.0).toInt().toShort()
        }
        return out
    }

    /** Concatenate single tones into a short melody (freq to durationMs). */
    private fun sequence(notes: List<Pair<Double, Int>>, volume: Double): ShortArray {
        val parts = notes.map { (freq, ms) -> tone(listOf(freq to 1.0), ms, decay = 12.0, volume = volume) }
        val out = ShortArray(parts.sumOf { it.size })
        var offset = 0
        for (part in parts) {
            part.copyInto(out, offset)
            offset += part.size
        }
        return out
    }
}
