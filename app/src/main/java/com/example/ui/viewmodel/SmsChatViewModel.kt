package com.example.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppPreferencesState
import com.example.data.local.NotificationPreferences
import com.example.data.local.PrivacyPreferences
import com.example.data.local.ThemeMode
import com.example.data.model.CallDirection
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StatusUpdateEntity
import com.example.data.model.UserProfileEntity
import com.example.data.repository.SmsChatRepository
import com.example.media.CallAudioController
import com.example.media.VoiceAudioManager
import com.example.media.VoicePlaybackUiState
import com.example.media.VoiceRecorderUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class AuthStep {
    PHONE_ENTRY,
    OTP_VERIFICATION,
    PROFILE_SETUP
}

data class AuthUiState(
    val step: AuthStep = AuthStep.PHONE_ENTRY,
    val countryCode: String = "+1",
    val phoneNumber: String = "",
    val generatedOtp: String = "",
    val otpInput: String = "",
    val otpSecondsRemaining: Int = 30,
    val displayName: String = "",
    val bio: String = "Hey there! I am using SMS CHAT.",
    val selectedAvatarUri: Uri? = null,
    val selectedColorHex: Long = 0xFF00C853,
    val errorMessage: String? = null,
    val isProcessing: Boolean = false
)

enum class CallPhase {
    INCOMING_RINGING,
    OUTGOING_DIALING,
    OUTGOING_RINGING,
    CONNECTED
}

data class ActiveCallState(
    val peerId: String,
    val peerName: String,
    val peerAvatarColorHex: Long,
    val isGroup: Boolean = false,
    val participantNames: List<String> = emptyList(),
    val callType: CallType = CallType.VOICE,
    val phase: CallPhase = CallPhase.OUTGOING_DIALING,
    val durationSec: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isCameraOn: Boolean = true,
    val useFrontCamera: Boolean = true
)

class SmsChatViewModel(
    private val repository: SmsChatRepository,
    private val voiceAudioManager: VoiceAudioManager,
    private val callAudioController: CallAudioController
) : ViewModel() {

    val currentUser: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val preferences: StateFlow<AppPreferencesState> = repository.preferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreferencesState())

    val contacts: StateFlow<List<ContactEntity>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversations: StateFlow<List<ConversationEntity>> = repository.conversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStatuses: StateFlow<List<StatusUpdateEntity>> = repository.observeActiveStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLogEntity>> = repository.callLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val voiceRecorderState: StateFlow<VoiceRecorderUiState> = voiceAudioManager.recorderState
    val voicePlaybackState: StateFlow<VoicePlaybackUiState> = voiceAudioManager.playbackState

    private val _authState = MutableStateFlow(AuthUiState())
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCallState?>(null)
    val activeCall: StateFlow<ActiveCallState?> = _activeCall.asStateFlow()

    private var otpTimerJob: Job? = null
    private var callLifecycleJob: Job? = null

    init {
        viewModelScope.launch {
            repository.purgeExpiredStatuses()
        }
    }

    fun observeConversation(conversationId: String): Flow<ConversationEntity?> =
        repository.observeConversation(conversationId)

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        repository.observeMessages(conversationId)

    // --- Authentication & Registration ---
    fun updateCountryCode(code: String) {
        _authState.value = _authState.value.copy(countryCode = code, errorMessage = null)
    }

    fun updatePhoneNumber(phone: String) {
        val digitsOnly = phone.filter { it.isDigit() || it == ' ' || it == '-' }
        _authState.value = _authState.value.copy(phoneNumber = digitsOnly, errorMessage = null)
    }

    fun requestOtp() {
        val digits = _authState.value.phoneNumber.filter { it.isDigit() }
        if (digits.length < 7) {
            _authState.value = _authState.value.copy(errorMessage = "Enter a valid phone number (at least 7 digits)")
            return
        }
        val otp = Random.nextInt(100000, 999999).toString()
        _authState.value = _authState.value.copy(
            step = AuthStep.OTP_VERIFICATION,
            generatedOtp = otp,
            otpInput = "",
            otpSecondsRemaining = 30,
            errorMessage = null
        )
        startOtpCountdown()
    }

    fun resendOtp() {
        val otp = Random.nextInt(100000, 999999).toString()
        _authState.value = _authState.value.copy(
            generatedOtp = otp,
            otpInput = "",
            otpSecondsRemaining = 30,
            errorMessage = null
        )
        startOtpCountdown()
    }

    fun updateOtpInput(value: String) {
        val clean = value.filter { it.isDigit() }.take(6)
        _authState.value = _authState.value.copy(otpInput = clean, errorMessage = null)
    }

    fun autofillOtp() {
        _authState.value = _authState.value.copy(
            otpInput = _authState.value.generatedOtp,
            errorMessage = null
        )
    }

    fun verifyOtp() {
        val state = _authState.value
        if (state.otpInput.length != 6 || state.otpInput != state.generatedOtp) {
            _authState.value = state.copy(errorMessage = "Invalid OTP code. Check the 6-digit verification code.")
            return
        }
        otpTimerJob?.cancel()
        _authState.value = state.copy(
            step = AuthStep.PROFILE_SETUP,
            errorMessage = null
        )
    }

    fun backToPhoneEntry() {
        otpTimerJob?.cancel()
        _authState.value = _authState.value.copy(step = AuthStep.PHONE_ENTRY, errorMessage = null)
    }

    fun updateRegistrationName(name: String) {
        _authState.value = _authState.value.copy(displayName = name, errorMessage = null)
    }

    fun updateRegistrationBio(bio: String) {
        _authState.value = _authState.value.copy(bio = bio, errorMessage = null)
    }

    fun updateRegistrationAvatar(uri: Uri?) {
        _authState.value = _authState.value.copy(selectedAvatarUri = uri)
    }

    fun updateRegistrationColor(colorHex: Long) {
        _authState.value = _authState.value.copy(selectedColorHex = colorHex)
    }

    fun completeRegistration() {
        val state = _authState.value
        if (state.displayName.trim().isEmpty()) {
            _authState.value = state.copy(errorMessage = "Please enter your display name")
            return
        }
        _authState.value = state.copy(isProcessing = true, errorMessage = null)
        viewModelScope.launch {
            val formattedPhone = "${state.countryCode} ${state.phoneNumber.trim()}"
            repository.registerUser(
                phoneNumber = formattedPhone,
                displayName = state.displayName,
                bio = state.bio,
                avatarUri = state.selectedAvatarUri,
                avatarColorHex = state.selectedColorHex
            )
            _authState.value = AuthUiState()
        }
    }

    private fun startOtpCountdown() {
        otpTimerJob?.cancel()
        otpTimerJob = viewModelScope.launch {
            while (_authState.value.otpSecondsRemaining > 0) {
                delay(1000)
                _authState.value = _authState.value.copy(
                    otpSecondsRemaining = (_authState.value.otpSecondsRemaining - 1).coerceAtLeast(0)
                )
            }
        }
    }

    // --- Profile & Settings ---
    fun updateProfile(name: String, bio: String, avatarUri: Uri?, colorHex: Long) {
        viewModelScope.launch {
            repository.updateProfile(name, bio, avatarUri, colorHex)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            voiceAudioManager.stopPlayback()
            voiceAudioManager.cancelRecording()
            repository.signOut()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }

    fun updatePrivacy(transform: (PrivacyPreferences) -> PrivacyPreferences) {
        viewModelScope.launch { repository.updatePrivacy(transform) }
    }

    fun updateNotifications(transform: (NotificationPreferences) -> NotificationPreferences) {
        viewModelScope.launch { repository.updateNotifications(transform) }
    }

    // --- Messaging & Contacts ---
    fun openOrCreateDirectChat(contact: ContactEntity, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val conv = repository.openOrCreateDirectChat(contact)
            onOpened(conv.id)
        }
    }

    fun addContactAndOpenChat(name: String, phone: String, onOpened: (String) -> Unit) {
        viewModelScope.launch {
            val conv = repository.addContactAndOpenChat(name, phone)
            onOpened(conv.id)
        }
    }

    fun createGroupChat(title: String, memberIds: List<String>, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val conv = repository.createGroupChat(title, memberIds)
            onCreated(conv.id)
        }
    }

    fun updateGroupMembers(conversationId: String, title: String, memberIds: List<String>) {
        viewModelScope.launch {
            repository.updateGroupMembers(conversationId, title, memberIds)
        }
    }

    fun markConversationRead(conversationId: String) {
        viewModelScope.launch { repository.clearUnread(conversationId) }
    }

    fun togglePinConversation(conversationId: String, pinned: Boolean) {
        viewModelScope.launch { repository.togglePinConversation(conversationId, pinned) }
    }

    fun sendTextMessage(conversationId: String, text: String, replyToText: String? = null) {
        viewModelScope.launch { repository.sendTextMessage(conversationId, text, replyToText) }
    }

    fun sendStickerMessage(conversationId: String, code: String, label: String) {
        viewModelScope.launch { repository.sendStickerMessage(conversationId, code, label) }
    }

    fun sendMediaMessage(conversationId: String, uri: Uri, isVideo: Boolean, caption: String = "") {
        viewModelScope.launch { repository.sendMediaMessage(conversationId, uri, isVideo, caption) }
    }

    fun deleteMessage(messageId: String, forEveryone: Boolean) {
        viewModelScope.launch { repository.deleteMessage(messageId, forEveryone) }
    }

    fun setContactBlocked(contactId: String, blocked: Boolean) {
        viewModelScope.launch { repository.setContactBlocked(contactId, blocked) }
    }

    fun reportAndBlockContact(contactId: String) {
        viewModelScope.launch { repository.reportAndBlockContact(contactId) }
    }

    // --- Voice Notes ---
    fun startVoiceRecording() {
        voiceAudioManager.startRecording()
    }

    fun cancelVoiceRecording() {
        voiceAudioManager.cancelRecording()
    }

    fun stopAndSendVoiceNote(conversationId: String) {
        val result = voiceAudioManager.stopAndSaveRecording() ?: return
        viewModelScope.launch {
            repository.sendVoiceMessage(conversationId, result.first, result.second)
        }
    }

    fun toggleVoicePlayback(messageId: String, filePath: String?) {
        voiceAudioManager.togglePlayback(messageId, filePath)
    }

    // --- Status & Stories ---
    fun postTextStatus(text: String, backgroundHex: Long) {
        viewModelScope.launch { repository.postTextStatus(text, backgroundHex) }
    }

    fun postMediaStatus(uri: Uri, isVideo: Boolean, caption: String) {
        viewModelScope.launch { repository.postMediaStatus(uri, isVideo, caption) }
    }

    fun markStatusViewed(statusId: String) {
        viewModelScope.launch { repository.markStatusViewed(statusId) }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch { repository.deleteStatus(statusId) }
    }

    fun purgeExpiredStatuses() {
        viewModelScope.launch { repository.purgeExpiredStatuses() }
    }

    // --- Voice & Video Calling ---
    fun startCall(
        peerId: String,
        peerName: String,
        peerAvatarColorHex: Long,
        callType: CallType,
        isGroup: Boolean = false,
        participantNames: List<String> = emptyList()
    ) {
        callLifecycleJob?.cancel()
        val speakerDefault = callType == CallType.VIDEO
        callAudioController.setSpeakerphone(speakerDefault)
        callAudioController.setMicMuted(false)

        _activeCall.value = ActiveCallState(
            peerId = peerId,
            peerName = peerName,
            peerAvatarColorHex = peerAvatarColorHex,
            isGroup = isGroup,
            participantNames = participantNames,
            callType = callType,
            phase = CallPhase.OUTGOING_DIALING,
            isSpeakerOn = speakerDefault,
            isCameraOn = callType == CallType.VIDEO
        )

        callLifecycleJob = viewModelScope.launch {
            delay(1100)
            if (_activeCall.value == null) return@launch
            _activeCall.value = _activeCall.value?.copy(phase = CallPhase.OUTGOING_RINGING)
            if (preferences.value.notifications.callRingtone) {
                callAudioController.playRingTonePulse()
            }
            delay(1800)
            if (_activeCall.value == null) return@launch
            _activeCall.value = _activeCall.value?.copy(phase = CallPhase.CONNECTED, durationSec = 0)
            startConnectedCallTicker()
        }
    }

    fun triggerIncomingCall(
        peerId: String,
        peerName: String,
        peerAvatarColorHex: Long,
        callType: CallType
    ) {
        callLifecycleJob?.cancel()
        callAudioController.setSpeakerphone(true)
        if (preferences.value.notifications.callRingtone) {
            callAudioController.playRingTonePulse()
        }
        _activeCall.value = ActiveCallState(
            peerId = peerId,
            peerName = peerName,
            peerAvatarColorHex = peerAvatarColorHex,
            callType = callType,
            phase = CallPhase.INCOMING_RINGING,
            isSpeakerOn = callType == CallType.VIDEO,
            isCameraOn = callType == CallType.VIDEO
        )
    }

    fun acceptIncomingCall() {
        val current = _activeCall.value ?: return
        if (current.phase != CallPhase.INCOMING_RINGING) return
        callLifecycleJob?.cancel()
        callAudioController.setSpeakerphone(current.callType == CallType.VIDEO)
        _activeCall.value = current.copy(phase = CallPhase.CONNECTED, durationSec = 0)
        callLifecycleJob = viewModelScope.launch {
            startConnectedCallTicker()
        }
    }

    fun rejectOrEndCall() {
        val current = _activeCall.value ?: return
        callLifecycleJob?.cancel()
        callLifecycleJob = null
        callAudioController.playEndCallTone()
        callAudioController.resetAudioMode()
        _activeCall.value = null

        val direction = when (current.phase) {
            CallPhase.INCOMING_RINGING -> CallDirection.MISSED
            CallPhase.CONNECTED -> if (current.participantNames.isEmpty()) CallDirection.OUTGOING else CallDirection.OUTGOING
            else -> CallDirection.OUTGOING
        }

        viewModelScope.launch {
            repository.logCall(
                peerId = current.peerId,
                peerName = current.peerName,
                peerAvatarColorHex = current.peerAvatarColorHex,
                isGroup = current.isGroup,
                participantNames = current.participantNames,
                callType = current.callType,
                direction = direction,
                durationSec = current.durationSec
            )
        }
    }

    fun toggleCallMute() {
        val current = _activeCall.value ?: return
        val next = !current.isMuted
        callAudioController.setMicMuted(next)
        _activeCall.value = current.copy(isMuted = next)
    }

    fun toggleCallSpeaker() {
        val current = _activeCall.value ?: return
        val next = !current.isSpeakerOn
        callAudioController.setSpeakerphone(next)
        _activeCall.value = current.copy(isSpeakerOn = next)
    }

    fun toggleCallCamera() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isCameraOn = !current.isCameraOn)
    }

    fun flipCallCamera() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(useFrontCamera = !current.useFrontCamera)
    }

    fun deleteCallLog(callLogId: String) {
        viewModelScope.launch { repository.deleteCallLog(callLogId) }
    }

    private suspend fun startConnectedCallTicker() {
        while (_activeCall.value?.phase == CallPhase.CONNECTED) {
            delay(1000)
            _activeCall.value = _activeCall.value?.let {
                if (it.phase == CallPhase.CONNECTED) it.copy(durationSec = it.durationSec + 1) else it
            }
        }
    }

    class Factory(
        private val repository: SmsChatRepository,
        private val voiceAudioManager: VoiceAudioManager,
        private val callAudioController: CallAudioController
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SmsChatViewModel(repository, voiceAudioManager, callAudioController) as T
        }
    }
}
