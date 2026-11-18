package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

enum class FocusState(val label: String, val subtitle: String, val ambientGain: Float, val harmonicShift: Float) {
    WARMUP("Calentamiento", "Ritmo suave de inicio", 0.72f, 1.0f),
    DEEP_FLOW("Flujo Profundo", "Audio envolvente estable", 1.0f, 0.85f),
    BREAK_RECHARGE("Recarga Serena", "Campanas suaves y pausa", 0.58f, 1.2f)
}

enum class AmbientSoundType(val key: String, val displayName: String, val baseHz: Double, val secondHz: Double) {
    NIGHT_RAIN("NIGHT_RAIN", "Lluvia Nocturna", 174.61, 261.63),
    MAGIC_FOREST("MAGIC_FOREST", "Bosque Mágico", 196.00, 293.66),
    ASTRAL_CHIMES("ASTRAL_CHIMES", "Campanas Astrales", 220.00, 329.63),
    DOJO_BREEZE("DOJO_BREEZE", "Brisa del Dojo", 164.81, 246.94),
    SERENE_LAB("SERENE_LAB", "Laboratorio Serena", 146.83, 220.00),
    STAR_WAVE("STAR_WAVE", "Onda Estelar", 207.65, 311.13);

    companion object {
        fun fromKey(key: String?): AmbientSoundType =
            entries.firstOrNull { it.key == key } ?: NIGHT_RAIN
    }
}

class AmbientFocusEngine(private val context: Context) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var synthJob: Job? = null
    private var trackVizJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var mediaPlayer: MediaPlayer? = null

    private var trackEnergyEnvelope: FloatArray = FloatArray(128) { 0.35f }
    private var trackDurationMs: Int = 1

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _selectedTrackName = MutableStateFlow("Paisaje Ambiental Integrado")
    val selectedTrackName: StateFlow<String> = _selectedTrackName.asStateFlow()

    private val _hasLocalTrack = MutableStateFlow(false)
    val hasLocalTrack: StateFlow<Boolean> = _hasLocalTrack.asStateFlow()

    private val _spectrumBands = MutableStateFlow(List(24) { 0.15f })
    val spectrumBands: StateFlow<List<Float>> = _spectrumBands.asStateFlow()

    private val _beatEnergy = MutableStateFlow(0.2f)
    val beatEnergy: StateFlow<Float> = _beatEnergy.asStateFlow()

    @Volatile
    private var currentSound: AmbientSoundType = AmbientSoundType.NIGHT_RAIN

    @Volatile
    private var currentFocusState: FocusState = FocusState.WARMUP

    @Volatile
    private var currentSensitivity: Float = 1.15f

    fun updateParameters(
        soundKey: String,
        focusState: FocusState,
        sensitivity: Float,
    ) {
        currentSound = AmbientSoundType.fromKey(soundKey)
        currentFocusState = focusState
        currentSensitivity = sensitivity.coerceIn(0.2f, 2.5f)
    }

    fun selectLocalTrack(uri: Uri, displayName: String) {
        stopPlayback()
        _selectedTrackName.value = displayName
        _hasLocalTrack.value = true
        scope.launch(Dispatchers.IO) {
            trackEnergyEnvelope = extractLightweightEnvelope(uri)
        }
        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setDataSource(context, uri)
                isLooping = true
                prepare()
            }
            mediaPlayer = player
            trackDurationMs = player.duration.coerceAtLeast(1)
            startPlayback()
        } catch (_: Exception) {
            _hasLocalTrack.value = false
            _selectedTrackName.value = "Paisaje Ambiental Integrado"
            startPlayback()
        }
    }

    fun useBuiltInAmbientOnly() {
        stopPlayback()
        _hasLocalTrack.value = false
        _selectedTrackName.value = "Paisaje Ambiental Integrado"
        startPlayback()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pausePlayback()
        } else {
            startPlayback()
        }
    }

    fun startPlayback() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        startAmbientSynthesizer()
        if (_hasLocalTrack.value) {
            try {
                mediaPlayer?.start()
            } catch (_: Exception) {
                // Ignore invalid player state
            }
            startTrackVisualizerLoop()
        }
    }

    fun pausePlayback() {
        _isPlaying.value = false
        synthJob?.cancel()
        synthJob = null
        trackVizJob?.cancel()
        trackVizJob = null
        try {
            audioTrack?.pause()
            audioTrack?.flush()
            audioTrack?.release()
        } catch (_: Exception) {
        }
        audioTrack = null
        try {
            if (mediaPlayer?.isPlaying == true) mediaPlayer?.pause()
        } catch (_: Exception) {
        }
        _spectrumBands.value = List(24) { 0.1f }
        _beatEnergy.value = 0.1f
    }

    fun stopPlayback() {
        pausePlayback()
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    private fun startAmbientSynthesizer() {
        synthJob?.cancel()
        synthJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 22050
            val minBuf = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(2048)

            val track = try {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            } catch (_: Exception) {
                return@launch
            }

            audioTrack = track
            try {
                track.play()
            } catch (_: Exception) {
                return@launch
            }

            val chunkSize = 1024
            val pcm = ShortArray(chunkSize)
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            var lfoPhase = 0.0
            var filteredNoise = 0.0

            while (isActive && _isPlaying.value) {
                val sound = currentSound
                val focus = currentFocusState
                val sens = currentSensitivity
                val ambientMaster = if (_hasLocalTrack.value) 0.25 else (0.55 * focus.ambientGain)

                val f1 = sound.baseHz * focus.harmonicShift
                val f2 = sound.secondHz * focus.harmonicShift
                val f3 = f1 * 1.5

                var sumSq = 0.0
                for (i in 0 until chunkSize) {
                    val lfo = 0.65 + 0.35 * sin(lfoPhase)
                    lfoPhase += 2.0 * PI * 0.14 / sampleRate

                    val pad = (sin(phase1) * 0.45 + sin(phase2) * 0.32 + sin(phase3) * 0.23) * lfo
                    phase1 += 2.0 * PI * f1 / sampleRate
                    phase2 += 2.0 * PI * f2 / sampleRate
                    phase3 += 2.0 * PI * f3 / sampleRate

                    // Soft filtered pink-like rain/breeze texture
                    val white = Random.nextDouble(-1.0, 1.0)
                    filteredNoise = filteredNoise * 0.965 + white * 0.035

                    val mixed = (pad * 0.72 + filteredNoise * 0.55) * ambientMaster
                    val clamped = mixed.coerceIn(-0.95, 0.95)
                    pcm[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
                    sumSq += clamped * clamped
                }

                try {
                    track.write(pcm, 0, chunkSize)
                } catch (_: Exception) {
                    break
                }

                if (!_hasLocalTrack.value) {
                    val rms = kotlin.math.sqrt(sumSq / chunkSize).toFloat()
                    val t = System.currentTimeMillis() / 320.0
                    val bands = List(24) { idx ->
                        val harmonicWave = abs(sin(t + idx * 0.42)).toFloat()
                        ((rms * 2.2f + harmonicWave * 0.48f) * sens * focus.ambientGain).coerceIn(0.08f, 1.0f)
                    }
                    _spectrumBands.value = bands
                    _beatEnergy.value = (rms * 2.5f * sens).coerceIn(0.12f, 1.0f)
                }
            }
        }
    }

    private fun startTrackVisualizerLoop() {
        trackVizJob?.cancel()
        trackVizJob = scope.launch {
            while (isActive && _isPlaying.value && _hasLocalTrack.value) {
                val player = mediaPlayer
                val pos = try {
                    player?.currentPosition ?: 0
                } catch (_: Exception) {
                    0
                }
                val ratio = (pos.toFloat() / trackDurationMs.toFloat()).coerceIn(0f, 0.999f)
                val idx = (ratio * trackEnergyEnvelope.size).toInt().coerceIn(0, trackEnergyEnvelope.lastIndex)
                val baseEnergy = trackEnergyEnvelope[idx]
                val sens = currentSensitivity
                val t = pos / 180.0

                val bands = List(24) { b ->
                    val mod = (0.55 + 0.45 * sin(t + b * 0.55)).toFloat()
                    (baseEnergy * mod * sens).coerceIn(0.1f, 1.0f)
                }
                _spectrumBands.value = bands
                _beatEnergy.value = (baseEnergy * sens).coerceIn(0.15f, 1.0f)
                delay(33L)
            }
        }
    }

    private fun extractLightweightEnvelope(uri: Uri): FloatArray {
        val bins = 128
        val out = FloatArray(bins) { 0.45f }
        val extractor = MediaExtractor()
        return try {
            extractor.setDataSource(context, uri, null)
            var durationUs = 1L
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    durationUs = if (fmt.containsKey(MediaFormat.KEY_DURATION)) {
                        fmt.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
                    } else 60_000_000L
                    break
                }
            }
            val buf = java.nio.ByteBuffer.allocate(8192)
            val sums = FloatArray(bins)
            val counts = IntArray(bins)
            var samples = 0
            while (samples < 1200) {
                val read = extractor.readSampleData(buf, 0)
                if (read < 0) break
                val timeUs = extractor.sampleTime.coerceAtLeast(0L)
                val bin = ((timeUs.toDouble() / durationUs.toDouble()) * bins).toInt().coerceIn(0, bins - 1)
                var energy = 0f
                val step = (read / 16).coerceAtLeast(1)
                for (p in 0 until read step step) {
                    energy += abs(buf.get(p).toInt()) / 128f
                }
                sums[bin] += (energy / 16f).coerceIn(0.1f, 1f)
                counts[bin]++
                extractor.advance()
                samples++
            }
            for (i in 0 until bins) {
                if (counts[i] > 0) {
                    out[i] = (sums[i] / counts[i]).coerceIn(0.2f, 1.0f)
                }
            }
            out
        } catch (_: Exception) {
            out
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
    }
}
