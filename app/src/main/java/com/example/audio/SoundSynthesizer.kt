package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Procedural synthesizer generating original sound effects in memory without external audio files:
 * - Coin collection chime
 * - Jump upward sweep
 * - Slide snow swoosh
 * - Power-up harmonic arpeggio
 * - Shield absorb ping
 * - Collision crash thud
 * - UI button click
 */
class SoundSynthesizer(initialEnabled: Boolean = true) {

    @Volatile
    var soundEnabled: Boolean = initialEnabled

    private val sampleRate = 22050
    private val executor = Executors.newFixedThreadPool(3)

    private val coinPcm: ShortArray by lazy { buildCoinChime() }
    private val jumpPcm: ShortArray by lazy { buildJumpSweep() }
    private val slidePcm: ShortArray by lazy { buildSlideSwoosh() }
    private val powerUpPcm: ShortArray by lazy { buildPowerUpArpeggio() }
    private val shieldBreakPcm: ShortArray by lazy { buildShieldShatter() }
    private val crashPcm: ShortArray by lazy { buildCrashThud() }
    private val buttonPcm: ShortArray by lazy { buildButtonClick() }

    fun playCoin() = playBuffer(coinPcm, 0.55f)
    fun playJump() = playBuffer(jumpPcm, 0.50f)
    fun playSlide() = playBuffer(slidePcm, 0.40f)
    fun playPowerUp() = playBuffer(powerUpPcm, 0.65f)
    fun playShieldBreak() = playBuffer(shieldBreakPcm, 0.65f)
    fun playCollision() = playBuffer(crashPcm, 0.75f)
    fun playButton() = playBuffer(buttonPcm, 0.45f)

    private fun playBuffer(pcm: ShortArray, volume: Float) {
        if (!soundEnabled) return
        try {
            executor.execute {
                var track: AudioTrack? = null
                try {
                    val byteSize = pcm.size * 2
                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(sampleRate)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(byteSize.coerceAtLeast(1024))
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()

                    track.write(pcm, 0, pcm.size)
                    track.setVolume(volume.coerceIn(0f, 1f))
                    track.play()
                    val durationMs = ((pcm.size * 1000L) / sampleRate) + 45L
                    Thread.sleep(durationMs)
                } catch (_: Throwable) {
                    // Safely ignore in headless JVM / muted environments
                } finally {
                    try {
                        track?.stop()
                    } catch (_: Throwable) {
                    }
                    try {
                        track?.release()
                    } catch (_: Throwable) {
                    }
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun shutdown() {
        try {
            executor.shutdownNow()
        } catch (_: Throwable) {
        }
    }

    private fun buildCoinChime(): ShortArray {
        val duration = 0.16
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val freq = if (t < 0.065) 987.77 else 1318.51 // B5 -> E6
            val env = exp(-t * 16.0)
            val sample = (sin(2.0 * PI * freq * t) * 0.75 + sin(4.0 * PI * freq * t) * 0.25) * env
            out[i] = (sample * Short.MAX_VALUE * 0.7).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildJumpSweep(): ShortArray {
        val duration = 0.18
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration
            val freq = 240.0 + progress * 460.0
            phase += 2.0 * PI * freq / sampleRate
            val env = (1.0 - progress) * (1.0 - exp(-t * 80.0))
            val sample = sin(phase) * env
            out[i] = (sample * Short.MAX_VALUE * 0.65).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildSlideSwoosh(): ShortArray {
        val duration = 0.17
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        val rng = Random(42)
        var filtered = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val progress = t / duration
            val white = rng.nextDouble(-1.0, 1.0)
            filtered = filtered * 0.82 + white * 0.18
            val env = sin(PI * progress)
            out[i] = (filtered * env * Short.MAX_VALUE * 0.7).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildPowerUpArpeggio(): ShortArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C5 E5 G5 C6
        val noteLen = 0.065
        val duration = notes.size * noteLen + 0.08
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val idx = (t / noteLen).toInt().coerceAtMost(notes.lastIndex)
            val localT = t - idx * noteLen
            val freq = notes[idx]
            val env = exp(-localT * 11.0) * (1.0 - (t / duration) * 0.35)
            val wave = sin(2.0 * PI * freq * t) * 0.7 + sin(3.0 * PI * freq * t) * 0.3
            out[i] = (wave * env * Short.MAX_VALUE * 0.65).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildShieldShatter(): ShortArray {
        val duration = 0.25
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        val rng = Random(108)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 14.0)
            val ring = sin(2.0 * PI * 880.0 * t) * 0.5 + sin(2.0 * PI * 1420.0 * t) * 0.3 + rng.nextDouble(-0.2, 0.2)
            out[i] = (ring * env * Short.MAX_VALUE * 0.7).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildCrashThud(): ShortArray {
        val duration = 0.32
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        val rng = Random(77)
        var phase = 0.0
        var lowNoise = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val freq = 140.0 * exp(-t * 8.0) + 38.0
            phase += 2.0 * PI * freq / sampleRate
            lowNoise = lowNoise * 0.75 + rng.nextDouble(-1.0, 1.0) * 0.25
            val env = exp(-t * 9.5)
            val sample = (sin(phase) * 0.65 + lowNoise * 0.55) * env
            out[i] = (sample * Short.MAX_VALUE * 0.8).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }

    private fun buildButtonClick(): ShortArray {
        val duration = 0.055
        val count = (sampleRate * duration).toInt()
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate
            val env = exp(-t * 55.0)
            val sample = sin(2.0 * PI * 740.0 * t) * env
            out[i] = (sample * Short.MAX_VALUE * 0.55).toInt().coerceIn(-32767, 32767).toShort()
        }
        return out
    }
}
