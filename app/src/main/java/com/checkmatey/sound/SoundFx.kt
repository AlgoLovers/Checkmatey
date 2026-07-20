package com.checkmatey.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class Sfx {
    MOVE, CAPTURE, CHECK, WIN, LOSE,
    // Capture tiers — a heftier "thunk" the bigger the piece you took.
    CAPTURE_PAWN, CAPTURE_MINOR, CAPTURE_ROOK, CAPTURE_QUEEN,
    // Puzzle trainer feedback (the puzzles tab used to be silent).
    CORRECT, WRONG, LEVEL_UP;

    companion object {
        /** The capture sound matching a captured piece's FEN letter — pure, so callers without a
         *  [SoundFx] instance (e.g. the shared move-feedback helper) can pick the tier too. */
        fun forCapture(pieceLetter: Char): Sfx = when (pieceLetter.lowercaseChar()) {
            'p' -> CAPTURE_PAWN
            'n', 'b' -> CAPTURE_MINOR
            'r' -> CAPTURE_ROOK
            'q' -> CAPTURE_QUEEN
            else -> CAPTURE
        }
    }
}

/**
 * Chess sound effects synthesized in code (short decaying tones) — no audio assets needed.
 * Each play spins a tiny AudioTrack on a background thread and is fully guarded, so audio can
 * never crash or block the UI. (Not unit-tested; it's pure I/O — verified by ear on device.)
 */
class SoundFx {

    private val sampleRate = 44100

    // Fuller than pure sines: a couple of harmonics + a click transient give each sound some body,
    // so it reads as a crafted "thock" rather than a thin beep.
    private val samples: Map<Sfx, ShortArray> = mapOf(
        Sfx.MOVE to tone(listOf(220.0 to 1.0, 440.0 to 0.35, 660.0 to 0.12), durationMs = 95, decay = 34.0, volume = 0.55, click = 0.25),
        // Generic capture kept for callers that don't know the piece; the tiered ones below are richer.
        Sfx.CAPTURE to tone(listOf(150.0 to 1.0, 300.0 to 0.5, 90.0 to 0.6), durationMs = 150, decay = 26.0, volume = 0.7, click = 0.5),
        Sfx.CAPTURE_PAWN to tone(listOf(240.0 to 1.0, 480.0 to 0.3), durationMs = 110, decay = 36.0, volume = 0.6, click = 0.4),
        Sfx.CAPTURE_MINOR to tone(listOf(170.0 to 1.0, 340.0 to 0.45, 110.0 to 0.4), durationMs = 150, decay = 26.0, volume = 0.7, click = 0.5),
        Sfx.CAPTURE_ROOK to tone(listOf(120.0 to 1.0, 180.0 to 0.5, 60.0 to 0.7), durationMs = 190, decay = 20.0, volume = 0.78, click = 0.6),
        Sfx.CAPTURE_QUEEN to tone(listOf(98.0 to 1.0, 147.0 to 0.6, 49.0 to 0.9, 294.0 to 0.3), durationMs = 260, decay = 15.0, volume = 0.85, click = 0.7),
        Sfx.CHECK to sequence(listOf(660.0 to 70, 990.0 to 100), volume = 0.55),
        Sfx.WIN to sequence(listOf(523.0 to 90, 659.0 to 90, 784.0 to 90, 1047.0 to 200), volume = 0.55),
        Sfx.LOSE to sequence(listOf(392.0 to 120, 330.0 to 130, 262.0 to 200), volume = 0.5),
        Sfx.CORRECT to sequence(listOf(659.0 to 80, 988.0 to 140), volume = 0.55),
        Sfx.WRONG to tone(listOf(180.0 to 1.0, 120.0 to 0.6), durationMs = 220, decay = 14.0, volume = 0.5, click = 0.3),
        Sfx.LEVEL_UP to sequence(listOf(523.0 to 90, 659.0 to 90, 784.0 to 90, 1047.0 to 90, 1319.0 to 220), volume = 0.55),
    )

    /** The capture sound matching a captured piece — pairs with the board's tiered visual. */
    fun captureFor(pieceLetter: Char): Sfx = Sfx.forCapture(pieceLetter)

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

    /**
     * A short decaying tone made of [partials] (freq to relative amplitude). [click] adds a brief
     * noise transient at the very start (a percussive attack), which is what makes a synthesized
     * "thock" feel physical instead of like a pure beep.
     */
    private fun tone(
        partials: List<Pair<Double, Double>>,
        durationMs: Int,
        decay: Double,
        volume: Double,
        click: Double = 0.0,
    ): ShortArray {
        val n = durationMs * sampleRate / 1000
        val ampSum = partials.sumOf { it.second }
        val out = ShortArray(n)
        var noise = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * decay)
            var s = 0.0
            for ((freq, amp) in partials) s += amp * sin(2 * PI * freq * t)
            var v = (s / ampSum) * env
            if (click > 0.0) {
                // Deterministic pseudo-noise (no RNG) decaying fast — the attack transient.
                noise = (noise * 1103515245.0 + 12345.0) % 65536.0
                val clickEnv = exp(-t * 220.0)
                v += click * clickEnv * ((noise / 32768.0) - 1.0)
            }
            out[i] = (v * volume * Short.MAX_VALUE).coerceIn(-32767.0, 32767.0).toInt().toShort()
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
