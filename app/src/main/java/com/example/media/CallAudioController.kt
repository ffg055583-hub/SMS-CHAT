package com.example.media

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator

class CallAudioController(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var toneGenerator: ToneGenerator? = null

    fun setSpeakerphone(enabled: Boolean) {
        runCatching {
            audioManager?.mode = AudioManager.MODE_IN_COMMUNICATION
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = enabled
        }
    }

    fun setMicMuted(muted: Boolean) {
        runCatching {
            audioManager?.isMicrophoneMute = muted
        }
    }

    fun playRingTonePulse() {
        runCatching {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 50)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_SUP_RINGTONE, 450)
        }
    }

    fun playEndCallTone() {
        runCatching {
            if (toneGenerator == null) {
                toneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 50)
            }
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 220)
        }
    }

    fun resetAudioMode() {
        runCatching {
            audioManager?.isMicrophoneMute = false
            @Suppress("DEPRECATION")
            audioManager?.isSpeakerphoneOn = false
            audioManager?.mode = AudioManager.MODE_NORMAL
            toneGenerator?.release()
            toneGenerator = null
        }
    }
}
