package com.example

import android.app.Application
import com.example.data.local.SettingsDataStore
import com.example.data.local.SmsChatDatabase
import com.example.data.repository.SmsChatRepository
import com.example.media.CallAudioController
import com.example.media.VoiceAudioManager

class SmsChatApplication : Application() {
    val database: SmsChatDatabase by lazy { SmsChatDatabase.getInstance(this) }
    val settingsDataStore: SettingsDataStore by lazy { SettingsDataStore(this) }
    val repository: SmsChatRepository by lazy {
        SmsChatRepository(
            context = this,
            dao = database.smsChatDao(),
            settingsDataStore = settingsDataStore
        )
    }
    val voiceAudioManager: VoiceAudioManager by lazy { VoiceAudioManager(this) }
    val callAudioController: CallAudioController by lazy { CallAudioController(this) }
}
