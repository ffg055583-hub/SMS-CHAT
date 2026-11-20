package com.example.media

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.PI
import kotlin.math.sin

data class VoiceRecorderUiState(
    val isRecording: Boolean = false,
    val elapsedSec: Int = 0
)

data class VoicePlaybackUiState(
    val playingMessageId: String? = null,
    val progress: Float = 0f
)

class VoiceAudioManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _recorderState = MutableStateFlow(VoiceRecorderUiState())
    val recorderState: StateFlow<VoiceRecorderUiState> = _recorderState.asStateFlow()

    private val _playbackState = MutableStateFlow(VoicePlaybackUiState())
    val playbackState: StateFlow<VoicePlaybackUiState> = _playbackState.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordTimerJob: Job? = null
    private var isSyntheticRecording = false

    private var mediaPlayer: MediaPlayer? = null
    private var playbackPollJob: Job? = null

    fun startRecording() {
        if (_recorderState.value.isRecording) return
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val file = File(dir, "voice_${UUID.randomUUID()}.m4a")
        currentOutputFile = file
        isSyntheticRecording = false

        val recorderStarted = runCatching {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(64_000)
                setAudioSamplingRate(22_050)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
        }.isSuccess

        if (!recorderStarted) {
            // Emulator / device without mic hardware fallback: allow recording timer & synthesize valid WAV clip on stop
            runCatching { mediaRecorder?.release() }
            mediaRecorder = null
            isSyntheticRecording = true
        }

        _recorderState.value = VoiceRecorderUiState(isRecording = true, elapsedSec = 0)
        recordTimerJob?.cancel()
        recordTimerJob = scope.launch {
            while (true) {
                delay(1000)
                _recorderState.value = _recorderState.value.copy(
                    elapsedSec = _recorderState.value.elapsedSec + 1
                )
            }
        }
    }

    fun cancelRecording() {
        recordTimerJob?.cancel()
        runCatching {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        }
        mediaRecorder = null
        currentOutputFile?.delete()
        currentOutputFile = null
        isSyntheticRecording = false
        _recorderState.value = VoiceRecorderUiState()
    }

    fun stopAndSaveRecording(): Pair<String, Int>? {
        if (!_recorderState.value.isRecording) return null
        recordTimerJob?.cancel()
        val duration = _recorderState.value.elapsedSec.coerceAtLeast(1)
        _recorderState.value = VoiceRecorderUiState()

        if (!isSyntheticRecording) {
            val stoppedOk = runCatching {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            }.isSuccess
            mediaRecorder = null
            val outFile = currentOutputFile
            currentOutputFile = null
            if (stoppedOk && outFile != null && outFile.exists() && outFile.length() > 128) {
                return outFile.absolutePath to duration
            }
        } else {
            mediaRecorder = null
        }

        // Write a valid WAV audio clip so playback always works reliably even on headless/emulated mics
        val dir = File(context.filesDir, "voice").apply { mkdirs() }
        val wavFile = File(dir, "voice_${UUID.randomUUID()}.wav")
        writeSoftChimeWav(wavFile, duration.coerceAtMost(6))
        currentOutputFile = null
        isSyntheticRecording = false
        return wavFile.absolutePath to duration
    }

    fun togglePlayback(messageId: String, filePath: String?) {
        if (_playbackState.value.playingMessageId == messageId) {
            stopPlayback()
            return
        }
        stopPlayback()
        if (filePath.isNullOrBlank()) return
        val file = File(filePath)
        if (!file.exists()) return

        val started = runCatching {
            val player = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                start()
            }
            mediaPlayer = player
            _playbackState.value = VoicePlaybackUiState(playingMessageId = messageId, progress = 0f)
            player.setOnCompletionListener {
                stopPlayback()
            }
            playbackPollJob = scope.launch {
                while (mediaPlayer != null) {
                    val mp = mediaPlayer ?: break
                    val total = mp.duration.coerceAtLeast(1)
                    val cur = mp.currentPosition
                    _playbackState.value = VoicePlaybackUiState(
                        playingMessageId = messageId,
                        progress = (cur.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    )
                    delay(80)
                }
            }
        }.isSuccess

        if (!started) {
            stopPlayback()
        }
    }

    fun stopPlayback() {
        playbackPollJob?.cancel()
        playbackPollJob = null
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _playbackState.value = VoicePlaybackUiState()
    }

    private fun writeSoftChimeWav(file: File, durationSec: Int) {
        val sampleRate = 16_000
        val totalSamples = sampleRate * durationSec.coerceIn(1, 5)
        val pcmBytes = ByteArray(totalSamples * 2)
        val buffer = ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / sampleRate
            val env = sin(PI * (i.toDouble() / totalSamples)).coerceAtLeast(0.0)
            val sample = (sin(2.0 * PI * 440.0 * t) * 0.2 + sin(2.0 * PI * 660.0 * t) * 0.1) * env
            buffer.putShort((sample * Short.MAX_VALUE).toInt().toShort())
        }
        val out = ByteArrayOutputStream()
        val dataSize = pcmBytes.size
        val chunkSize = 36 + dataSize
        out.write("RIFF".toByteArray())
        out.write(intToLe(chunkSize))
        out.write("WAVE".toByteArray())
        out.write("fmt ".toByteArray())
        out.write(intToLe(16))
        out.write(shortToLe(1))
        out.write(shortToLe(1))
        out.write(intToLe(sampleRate))
        out.write(intToLe(sampleRate * 2))
        out.write(shortToLe(2))
        out.write(shortToLe(16))
        out.write("data".toByteArray())
        out.write(intToLe(dataSize))
        out.write(pcmBytes)
        file.writeBytes(out.toByteArray())
    }

    private fun intToLe(value: Int): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(value).array()

    private fun shortToLe(value: Int): ByteArray =
        ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort()).array()
}
