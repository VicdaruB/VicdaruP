package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class ClimberSoundEngine {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sampleRate = 22050

    @Volatile
    var isMuted: Boolean = false

    private val startPcm: ShortArray by lazy {
        synthSequence(
            listOf(
                ToneNote(440.0, 0.065, 0.18),
                ToneNote(554.37, 0.065, 0.20),
                ToneNote(659.25, 0.11, 0.22)
            )
        )
    }

    private val movePcm: ShortArray by lazy {
        synthSweep(380.0, 470.0, 0.042, 0.10)
    }

    private val bounceNormalPcm: ShortArray by lazy {
        synthSweep(250.0, 430.0, 0.09, 0.22)
    }

    private val bounceBoostPcm: ShortArray by lazy {
        synthSequence(
            listOf(
                ToneNote(360.0, 0.055, 0.20),
                ToneNote(587.33, 0.095, 0.24)
            )
        )
    }

    private val speedBoostPcm: ShortArray by lazy {
        synthSequence(
            listOf(
                ToneNote(523.25, 0.055, 0.20),
                ToneNote(659.25, 0.055, 0.22),
                ToneNote(880.0, 0.12, 0.24)
            )
        )
    }

    private val trapFreezePcm: ShortArray by lazy {
        synthSweep(340.0, 145.0, 0.15, 0.24)
    }

    private val trapReleasePcm: ShortArray by lazy {
        synthSweep(392.0, 587.33, 0.10, 0.18)
    }

    private val stunPcm: ShortArray by lazy {
        synthVibrato(370.0, 22.0, 45.0, 0.22, 0.24)
    }

    private val gameOverPcm: ShortArray by lazy {
        synthSequence(
            listOf(
                ToneNote(392.0, 0.10, 0.20),
                ToneNote(329.63, 0.10, 0.18),
                ToneNote(246.94, 0.18, 0.16)
            )
        )
    }

    private val challengeCompletePcm: ShortArray by lazy {
        synthSequence(
            listOf(
                ToneNote(523.25, 0.06, 0.20),
                ToneNote(659.25, 0.06, 0.22),
                ToneNote(783.99, 0.06, 0.22),
                ToneNote(1046.50, 0.14, 0.25)
            )
        )
    }

    fun playStart() = playPcm(startPcm)
    fun playMove() = playPcm(movePcm)
    fun playBounce(isBoostPlatform: Boolean) = playPcm(if (isBoostPlatform) bounceBoostPcm else bounceNormalPcm)
    fun playSpeedBoost() = playPcm(speedBoostPcm)
    fun playTrapFreeze() = playPcm(trapFreezePcm)
    fun playTrapRelease() = playPcm(trapReleasePcm)
    fun playStun() = playPcm(stunPcm)
    fun playGameOver() = playPcm(gameOverPcm)
    fun playChallengeComplete() = playPcm(challengeCompletePcm)

    fun release() {
        scope.cancel()
    }

    private fun playPcm(pcm: ShortArray) {
        if (isMuted || pcm.isEmpty()) return
        scope.launch {
            var track: AudioTrack? = null
            try {
                val byteLen = pcm.size * 2
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
                    .setBufferSizeInBytes(byteLen)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()
                track.write(pcm, 0, pcm.size)
                track.play()
                val durationMs = (pcm.size * 1000L / sampleRate) + 40L
                kotlinx.coroutines.delay(durationMs)
            } catch (_: Exception) {
            } finally {
                try {
                    track?.stop()
                    track?.release()
                } catch (_: Exception) {
                }
            }
        }
    }

    private data class ToneNote(val hz: Double, val durationSec: Double, val gain: Double)

    private fun synthSweep(startHz: Double, endHz: Double, durationSec: Double, gain: Double): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val t = i.toDouble() / count
            val freq = startHz + (endHz - startHz) * t
            val attack = (i.toDouble() / (sampleRate * 0.008)).coerceIn(0.0, 1.0)
            val decay = exp(-3.5 * t)
            val sample = (sin(phase) * 0.8 + sin(phase * 2.0) * 0.2) * attack * decay * gain
            phase += 2.0 * PI * freq / sampleRate
            out[i] = (sample.coerceIn(-0.9, 0.9) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthVibrato(
        baseHz: Double,
        lfoHz: Double,
        depthHz: Double,
        durationSec: Double,
        gain: Double,
    ): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val tSec = i.toDouble() / sampleRate
            val tNorm = i.toDouble() / count
            val freq = baseHz + sin(2.0 * PI * lfoHz * tSec) * depthHz
            val attack = (i.toDouble() / (sampleRate * 0.01)).coerceIn(0.0, 1.0)
            val decay = (1.0 - tNorm).coerceIn(0.0, 1.0)
            val sample = sin(phase) * attack * decay * gain
            phase += 2.0 * PI * freq / sampleRate
            out[i] = (sample.coerceIn(-0.9, 0.9) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthSequence(notes: List<ToneNote>): ShortArray {
        val chunks = notes.map { n ->
            val count = (sampleRate * n.durationSec).toInt().coerceAtLeast(1)
            val arr = ShortArray(count)
            var phase = 0.0
            for (i in 0 until count) {
                val t = i.toDouble() / count
                val attack = (i.toDouble() / (sampleRate * 0.007)).coerceIn(0.0, 1.0)
                val decay = exp(-3.2 * t)
                val sample = (sin(phase) * 0.82 + sin(phase * 2.0) * 0.18) * attack * decay * n.gain
                phase += 2.0 * PI * n.hz / sampleRate
                arr[i] = (sample.coerceIn(-0.9, 0.9) * Short.MAX_VALUE).toInt().toShort()
            }
            arr
        }
        val total = chunks.sumOf { it.size }
        val result = ShortArray(total)
        var offset = 0
        chunks.forEach { chunk ->
            chunk.copyInto(result, offset)
            offset += chunk.size
        }
        return result
    }
}
