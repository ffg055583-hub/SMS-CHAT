package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.local.AppPreferencesState
import com.example.data.local.NotificationPreferences
import com.example.data.local.PrivacyPreferences
import com.example.data.local.SettingsDataStore
import com.example.data.local.SmsChatDao
import com.example.data.local.ThemeMode
import com.example.data.model.CallDirection
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.MessageStatus
import com.example.data.model.MessageType
import com.example.data.model.StatusMediaType
import com.example.data.model.StatusUpdateEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.random.Random

class SmsChatRepository(
    private val context: Context,
    private val dao: SmsChatDao,
    private val settingsDataStore: SettingsDataStore
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val userProfile: Flow<UserProfileEntity?> = dao.observeUserProfile()
    val contacts: Flow<List<ContactEntity>> = dao.observeContacts()
    val conversations: Flow<List<ConversationEntity>> = dao.observeConversations()
    val callLogs: Flow<List<CallLogEntity>> = dao.observeCallLogs()
    val preferences: Flow<AppPreferencesState> = settingsDataStore.preferencesFlow

    fun observeConversation(conversationId: String): Flow<ConversationEntity?> =
        dao.observeConversation(conversationId)

    fun observeMessages(conversationId: String): Flow<List<MessageEntity>> =
        dao.observeMessages(conversationId)

    fun observeActiveStatuses(): Flow<List<StatusUpdateEntity>> =
        dao.observeActiveStatuses(System.currentTimeMillis())

    suspend fun purgeExpiredStatuses() {
        dao.purgeExpiredStatuses(System.currentTimeMillis())
    }

    suspend fun registerUser(
        phoneNumber: String,
        displayName: String,
        bio: String,
        avatarUri: Uri?,
        avatarColorHex: Long
    ): UserProfileEntity {
        val persistedAvatar = avatarUri?.let { persistMediaToAppStorage(it, "avatars", "jpg") }
        val uniqueId = generateUniqueUserId()
        val profile = UserProfileEntity(
            id = "me",
            uniqueUserId = uniqueId,
            phoneNumber = phoneNumber,
            displayName = displayName.trim().ifEmpty { "SMS User" },
            bio = bio.trim().ifEmpty { "Hey there! I am using SMS CHAT." },
            avatarUri = persistedAvatar,
            avatarColorHex = avatarColorHex,
            createdAtMillis = System.currentTimeMillis()
        )
        dao.upsertUserProfile(profile)
        seedStarterWorkspaceIfNeeded(profile)
        return profile
    }

    suspend fun updateProfile(
        displayName: String,
        bio: String,
        avatarUri: Uri?,
        avatarColorHex: Long
    ) {
        val existing = dao.getUserProfileOnce() ?: return
        val persistedAvatar = if (avatarUri != null) {
            persistMediaToAppStorage(avatarUri, "avatars", "jpg")
        } else {
            existing.avatarUri
        }
        dao.upsertUserProfile(
            existing.copy(
                displayName = displayName.trim().ifEmpty { existing.displayName },
                bio = bio.trim(),
                avatarUri = persistedAvatar,
                avatarColorHex = avatarColorHex
            )
        )
    }

    suspend fun signOut() {
        dao.clearUserProfile()
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        settingsDataStore.setThemeMode(mode)
    }

    suspend fun updatePrivacy(transform: (PrivacyPreferences) -> PrivacyPreferences) {
        settingsDataStore.updatePrivacy(transform)
    }

    suspend fun updateNotifications(transform: (NotificationPreferences) -> NotificationPreferences) {
        settingsDataStore.updateNotifications(transform)
    }

    suspend fun addContactAndOpenChat(name: String, phoneNumber: String): ConversationEntity {
        val contactId = "c_${UUID.randomUUID().toString().take(8)}"
        val palette = listOf(0xFF00C853, 0xFF00BFA5, 0xFF2979FF, 0xFFFF6D00, 0xFFAA00FF, 0xFF00E5FF)
        val color = palette.random()
        val contact = ContactEntity(
            id = contactId,
            uniqueUserId = generateUniqueUserId(),
            name = name.trim(),
            phoneNumber = phoneNumber.trim(),
            bio = "Using SMS CHAT",
            avatarColorHex = color,
            isOnline = true,
            lastSeenMillis = System.currentTimeMillis()
        )
        dao.upsertContact(contact)
        return openOrCreateDirectChat(contact)
    }

    suspend fun openOrCreateDirectChat(contact: ContactEntity): ConversationEntity {
        val existing = dao.getDirectConversationForContact(contact.id)
        if (existing != null) return existing
        val conv = ConversationEntity(
            id = "conv_${contact.id}",
            title = contact.name,
            isGroup = false,
            peerContactId = contact.id,
            memberIdsCsv = "me,${contact.id}",
            avatarColorHex = contact.avatarColorHex,
            avatarUri = contact.avatarUri,
            lastMessagePreview = "Start a conversation",
            lastMessageTimeMillis = System.currentTimeMillis(),
            unreadCount = 0
        )
        dao.upsertConversation(conv)
        return conv
    }

    suspend fun createGroupChat(title: String, memberIds: List<String>): ConversationEntity {
        val uniqueMembers = (listOf("me") + memberIds).distinct()
        val palette = listOf(0xFF00C853, 0xFF00BFA5, 0xFF1DE9B6, 0xFF00B0FF)
        val convId = "grp_${UUID.randomUUID().toString().take(8)}"
        val now = System.currentTimeMillis()
        val conv = ConversationEntity(
            id = convId,
            title = title.trim().ifEmpty { "New Group" },
            isGroup = true,
            peerContactId = null,
            memberIdsCsv = uniqueMembers.joinToString(","),
            avatarColorHex = palette.random(),
            lastMessagePreview = "Group created · ${uniqueMembers.size} members",
            lastMessageTimeMillis = now,
            unreadCount = 0
        )
        dao.upsertConversation(conv)
        dao.insertMessage(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = convId,
                senderId = "system",
                senderName = "System",
                type = MessageType.SYSTEM.name,
                text = "Group \"${conv.title}\" created with ${uniqueMembers.size} members",
                timestampMillis = now,
                status = MessageStatus.READ.name
            )
        )
        return conv
    }

    suspend fun updateGroupMembers(conversationId: String, newTitle: String, memberIds: List<String>) {
        val uniqueMembers = (listOf("me") + memberIds).distinct()
        dao.updateGroupDetails(conversationId, newTitle.trim(), uniqueMembers.joinToString(","))
    }

    suspend fun clearUnread(conversationId: String) {
        val profile = dao.getUserProfileOnce() ?: return
        dao.clearUnreadCount(conversationId)
        dao.markIncomingMessagesRead(conversationId, profile.id, MessageStatus.READ.name)
    }

    suspend fun togglePinConversation(conversationId: String, pinned: Boolean) {
        dao.setConversationPinned(conversationId, pinned)
    }

    suspend fun sendTextMessage(
        conversationId: String,
        text: String,
        replyToText: String? = null
    ) {
        val profile = dao.getUserProfileOnce() ?: return
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return

        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val message = MessageEntity(
            id = msgId,
            conversationId = conversationId,
            senderId = profile.id,
            senderName = profile.displayName,
            type = MessageType.TEXT.name,
            text = cleanText,
            replyToText = replyToText,
            timestampMillis = now,
            status = MessageStatus.SENT.name
        )
        dao.insertMessage(message)
        dao.updateConversationPreview(conversationId, cleanText, now, 0)
        scheduleReceiptAndInteractiveReply(conversationId, msgId, cleanText)
    }

    suspend fun sendStickerMessage(conversationId: String, stickerCode: String, label: String) {
        val profile = dao.getUserProfileOnce() ?: return
        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val message = MessageEntity(
            id = msgId,
            conversationId = conversationId,
            senderId = profile.id,
            senderName = profile.displayName,
            type = MessageType.STICKER.name,
            text = label,
            stickerCode = stickerCode,
            timestampMillis = now,
            status = MessageStatus.SENT.name
        )
        dao.insertMessage(message)
        dao.updateConversationPreview(conversationId, "Sticker $stickerCode", now, 0)
        scheduleReceiptAndInteractiveReply(conversationId, msgId, label)
    }

    suspend fun sendMediaMessage(
        conversationId: String,
        uri: Uri,
        isVideo: Boolean,
        caption: String = ""
    ) {
        val profile = dao.getUserProfileOnce() ?: return
        val ext = if (isVideo) "mp4" else "jpg"
        val storedPath = persistMediaToAppStorage(uri, "chat_media", ext) ?: uri.toString()
        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val preview = if (isVideo) "Video" else "Photo"
        val message = MessageEntity(
            id = msgId,
            conversationId = conversationId,
            senderId = profile.id,
            senderName = profile.displayName,
            type = if (isVideo) MessageType.VIDEO.name else MessageType.IMAGE.name,
            text = caption.trim(),
            mediaUri = storedPath,
            timestampMillis = now,
            status = MessageStatus.SENT.name
        )
        dao.insertMessage(message)
        dao.updateConversationPreview(
            conversationId,
            if (caption.isNotBlank()) "$preview: ${caption.trim()}" else preview,
            now,
            0
        )
        scheduleReceiptAndInteractiveReply(conversationId, msgId, preview)
    }

    suspend fun sendVoiceMessage(
        conversationId: String,
        audioFilePath: String,
        durationSec: Int
    ) {
        val profile = dao.getUserProfileOnce() ?: return
        val msgId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val message = MessageEntity(
            id = msgId,
            conversationId = conversationId,
            senderId = profile.id,
            senderName = profile.displayName,
            type = MessageType.VOICE.name,
            text = "Voice message (${durationSec}s)",
            mediaUri = audioFilePath,
            voiceDurationSec = durationSec.coerceAtLeast(1),
            timestampMillis = now,
            status = MessageStatus.SENT.name
        )
        dao.insertMessage(message)
        dao.updateConversationPreview(conversationId, "Voice message (${durationSec}s)", now, 0)
        scheduleReceiptAndInteractiveReply(conversationId, msgId, "voice message")
    }

    suspend fun deleteMessage(messageId: String, forEveryone: Boolean) {
        if (forEveryone) {
            dao.markMessageDeletedForAll(messageId)
        } else {
            dao.deleteMessageLocally(messageId)
        }
    }

    suspend fun postTextStatus(text: String, backgroundHex: Long) {
        val profile = dao.getUserProfileOnce() ?: return
        val now = System.currentTimeMillis()
        val status = StatusUpdateEntity(
            id = UUID.randomUUID().toString(),
            ownerId = profile.id,
            ownerName = profile.displayName,
            ownerAvatarColorHex = profile.avatarColorHex,
            ownerAvatarUri = profile.avatarUri,
            type = StatusMediaType.TEXT.name,
            text = text.trim(),
            backgroundHex = backgroundHex,
            createdAtMillis = now,
            expiresAtMillis = now + 24L * 60L * 60L * 1000L,
            isViewed = true
        )
        dao.insertStatus(status)
    }

    suspend fun postMediaStatus(uri: Uri, isVideo: Boolean, caption: String) {
        val profile = dao.getUserProfileOnce() ?: return
        val ext = if (isVideo) "mp4" else "jpg"
        val stored = persistMediaToAppStorage(uri, "statuses", ext) ?: uri.toString()
        val now = System.currentTimeMillis()
        val status = StatusUpdateEntity(
            id = UUID.randomUUID().toString(),
            ownerId = profile.id,
            ownerName = profile.displayName,
            ownerAvatarColorHex = profile.avatarColorHex,
            ownerAvatarUri = profile.avatarUri,
            type = if (isVideo) StatusMediaType.VIDEO.name else StatusMediaType.PHOTO.name,
            text = caption.trim(),
            mediaUri = stored,
            createdAtMillis = now,
            expiresAtMillis = now + 24L * 60L * 60L * 1000L,
            isViewed = true
        )
        dao.insertStatus(status)
    }

    suspend fun markStatusViewed(statusId: String) {
        dao.markStatusViewed(statusId)
    }

    suspend fun deleteStatus(statusId: String) {
        dao.deleteStatus(statusId)
    }

    suspend fun setContactBlocked(contactId: String, blocked: Boolean) {
        dao.setContactBlocked(contactId, blocked)
    }

    suspend fun reportAndBlockContact(contactId: String) {
        dao.reportAndBlockContact(contactId, reported = true, blocked = true)
    }

    suspend fun logCall(
        peerId: String,
        peerName: String,
        peerAvatarColorHex: Long,
        isGroup: Boolean,
        participantNames: List<String>,
        callType: CallType,
        direction: CallDirection,
        durationSec: Int
    ) {
        val log = CallLogEntity(
            id = UUID.randomUUID().toString(),
            peerId = peerId,
            peerName = peerName,
            peerAvatarColorHex = peerAvatarColorHex,
            isGroup = isGroup,
            participantNamesCsv = participantNames.joinToString(", "),
            callType = callType.name,
            direction = direction.name,
            timestampMillis = System.currentTimeMillis(),
            durationSec = durationSec
        )
        dao.insertCallLog(log)
    }

    suspend fun deleteCallLog(callLogId: String) {
        dao.deleteCallLog(callLogId)
    }

    private fun scheduleReceiptAndInteractiveReply(
        conversationId: String,
        msgId: String,
        outgoingSnippet: String
    ) {
        repoScope.launch {
            val conv = dao.getConversationById(conversationId) ?: return@launch
            val peer = conv.peerContactId?.let { dao.getContactById(it) }
            if (peer?.isBlocked == true) return@launch

            delay(450)
            dao.updateMessageStatus(msgId, MessageStatus.DELIVERED.name)

            val prefs = settingsDataStore.preferencesFlow.first()
            delay(750)
            if (prefs.privacy.readReceipts) {
                dao.updateMessageStatus(msgId, MessageStatus.READ.name)
            }

            // Provide contextual real-time peer response in local mode
            delay(1300)
            val responderName: String
            val responderId: String
            if (conv.isGroup) {
                val memberIds = conv.memberIdsCsv.split(",").filter { it != "me" && it.isNotBlank() }
                val firstMemberId = memberIds.firstOrNull() ?: return@launch
                val memberContact = dao.getContactById(firstMemberId)
                responderId = firstMemberId
                responderName = memberContact?.name ?: "Alex"
            } else if (peer != null) {
                responderId = peer.id
                responderName = peer.name
            } else {
                return@launch
            }

            val replyText = buildContextualReply(outgoingSnippet, responderName, conv.isGroup)
            val replyNow = System.currentTimeMillis()
            dao.insertMessage(
                MessageEntity(
                    id = UUID.randomUUID().toString(),
                    conversationId = conversationId,
                    senderId = responderId,
                    senderName = responderName,
                    type = MessageType.TEXT.name,
                    text = replyText,
                    timestampMillis = replyNow,
                    status = MessageStatus.READ.name
                )
            )
            val previewText = if (conv.isGroup) "$responderName: $replyText" else replyText
            dao.updateConversationPreview(conversationId, previewText, replyNow, 0)
        }
    }

    private fun buildContextualReply(input: String, senderName: String, isGroup: Boolean): String {
        val lower = input.lowercase()
        return when {
            lower.contains("voice message") -> "Got your voice note! Sounds crystal clear."
            lower.contains("photo") || lower.contains("video") -> "Received your media — looks awesome!"
            lower.contains("call") -> "Ready whenever you want to hop on a call."
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") ->
                if (isGroup) "Hey everyone! Good to see you active here." else "Hey! How is your day going?"
            lower.contains("?") -> "Yes, sounds great to me! Let's do it."
            else -> "Got it, thanks for the update!"
        }
    }

    private suspend fun persistMediaToAppStorage(
        uri: Uri,
        folderName: String,
        extension: String
    ): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, folderName).apply { mkdirs() }
            val dest = File(dir, "${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return@runCatching null
            dest.absolutePath
        }.getOrNull()
    }

    private fun generateUniqueUserId(): String {
        val p1 = Random.nextInt(1000, 9999)
        val p2 = Random.nextInt(1000, 9999)
        return "SMS-$p1-$p2"
    }

    private suspend fun seedStarterWorkspaceIfNeeded(profile: UserProfileEntity) {
        val prefs = settingsDataStore.preferencesFlow.first()
        if (prefs.hasSeededData) return

        val now = System.currentTimeMillis()
        val contacts = listOf(
            ContactEntity(
                id = "c_maya",
                uniqueUserId = "SMS-4821-9034",
                name = "Maya Lin",
                phoneNumber = "+1 415 555 0142",
                bio = "Product Designer · San Francisco",
                avatarColorHex = 0xFF00C853,
                isOnline = true,
                lastSeenMillis = now
            ),
            ContactEntity(
                id = "c_liam",
                uniqueUserId = "SMS-7319-2240",
                name = "Liam Carter",
                phoneNumber = "+1 312 555 0198",
                bio = "Always building something cool",
                avatarColorHex = 0xFF00BFA5,
                isOnline = true,
                lastSeenMillis = now
            ),
            ContactEntity(
                id = "c_sofia",
                uniqueUserId = "SMS-5904-6612",
                name = "Sofia Alvarez",
                phoneNumber = "+1 646 555 0175",
                bio = "Music, coffee & late-night chats",
                avatarColorHex = 0xFF2979FF,
                isOnline = false,
                lastSeenMillis = now - 18 * 60_000L
            ),
            ContactEntity(
                id = "c_noah",
                uniqueUserId = "SMS-8812-3049",
                name = "Noah Patel",
                phoneNumber = "+1 206 555 0114",
                bio = "Photography & weekend hikes",
                avatarColorHex = 0xFFFF6D00,
                isOnline = false,
                lastSeenMillis = now - 2 * 3_600_000L
            ),
            ContactEntity(
                id = "c_zara",
                uniqueUserId = "SMS-3190-7751",
                name = "Zara Kim",
                phoneNumber = "+1 512 555 0163",
                bio = "Available on SMS CHAT",
                avatarColorHex = 0xFFAA00FF,
                isOnline = true,
                lastSeenMillis = now
            )
        )
        dao.insertContacts(contacts)

        val conversations = listOf(
            ConversationEntity(
                id = "conv_c_maya",
                title = "Maya Lin",
                isGroup = false,
                peerContactId = "c_maya",
                memberIdsCsv = "me,c_maya",
                avatarColorHex = 0xFF00C853,
                lastMessagePreview = "Welcome to SMS CHAT! Try sending a voice note or sticker.",
                lastMessageTimeMillis = now - 5 * 60_000L,
                unreadCount = 1,
                isPinned = true
            ),
            ConversationEntity(
                id = "conv_c_liam",
                title = "Liam Carter",
                isGroup = false,
                peerContactId = "c_liam",
                memberIdsCsv = "me,c_liam",
                avatarColorHex = 0xFF00BFA5,
                lastMessagePreview = "Let's test a quick video call later today!",
                lastMessageTimeMillis = now - 42 * 60_000L,
                unreadCount = 0
            ),
            ConversationEntity(
                id = "grp_launch",
                title = "SMS CHAT Launch Squad",
                isGroup = true,
                peerContactId = null,
                memberIdsCsv = "me,c_maya,c_liam,c_sofia,c_zara",
                avatarColorHex = 0xFF00E676,
                lastMessagePreview = "Liam: Group voice & video calls are live!",
                lastMessageTimeMillis = now - 95 * 60_000L,
                unreadCount = 2
            ),
            ConversationEntity(
                id = "conv_c_sofia",
                title = "Sofia Alvarez",
                isGroup = false,
                peerContactId = "c_sofia",
                memberIdsCsv = "me,c_sofia",
                avatarColorHex = 0xFF2979FF,
                lastMessagePreview = "Sent you the weekend playlist",
                lastMessageTimeMillis = now - 4 * 3_600_000L,
                unreadCount = 0
            )
        )
        dao.insertConversations(conversations)

        val messages = listOf(
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_c_maya",
                senderId = "c_maya",
                senderName = "Maya Lin",
                type = MessageType.TEXT.name,
                text = "Hey ${profile.displayName}! Your unique ID is ${profile.uniqueUserId}.",
                timestampMillis = now - 8 * 60_000L,
                status = MessageStatus.READ.name
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_c_maya",
                senderId = "c_maya",
                senderName = "Maya Lin",
                type = MessageType.TEXT.name,
                text = "Welcome to SMS CHAT! Try sending a voice note or sticker.",
                timestampMillis = now - 5 * 60_000L,
                status = MessageStatus.READ.name
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_c_liam",
                senderId = "me",
                senderName = profile.displayName,
                type = MessageType.TEXT.name,
                text = "Hey Liam, just set up my SMS CHAT profile!",
                timestampMillis = now - 50 * 60_000L,
                status = MessageStatus.READ.name
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_c_liam",
                senderId = "c_liam",
                senderName = "Liam Carter",
                type = MessageType.TEXT.name,
                text = "Let's test a quick video call later today!",
                timestampMillis = now - 42 * 60_000L,
                status = MessageStatus.READ.name
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "grp_launch",
                senderId = "c_maya",
                senderName = "Maya Lin",
                type = MessageType.TEXT.name,
                text = "Morning team! Check out the new emerald dark theme.",
                timestampMillis = now - 110 * 60_000L,
                status = MessageStatus.READ.name
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "grp_launch",
                senderId = "c_liam",
                senderName = "Liam Carter",
                type = MessageType.TEXT.name,
                text = "Liam: Group voice & video calls are live!",
                timestampMillis = now - 95 * 60_000L,
                status = MessageStatus.READ.name
            ),
            MessageEntity(
                id = UUID.randomUUID().toString(),
                conversationId = "conv_c_sofia",
                senderId = "c_sofia",
                senderName = "Sofia Alvarez",
                type = MessageType.TEXT.name,
                text = "Sent you the weekend playlist",
                timestampMillis = now - 4 * 3_600_000L,
                status = MessageStatus.READ.name
            )
        )
        dao.insertMessages(messages)

        val statuses = listOf(
            StatusUpdateEntity(
                id = "st_maya",
                ownerId = "c_maya",
                ownerName = "Maya Lin",
                ownerAvatarColorHex = 0xFF00C853,
                type = StatusMediaType.TEXT.name,
                text = "Shipping fresh UI polish on SMS CHAT today!",
                backgroundHex = 0xFF0B3B24,
                createdAtMillis = now - 2 * 3_600_000L,
                expiresAtMillis = now + 22 * 3_600_000L,
                isViewed = false,
                viewCount = 14
            ),
            StatusUpdateEntity(
                id = "st_liam",
                ownerId = "c_liam",
                ownerName = "Liam Carter",
                ownerAvatarColorHex = 0xFF00BFA5,
                type = StatusMediaType.TEXT.name,
                text = "Crystal clear HD calls & dark mode vibes.",
                backgroundHex = 0xFF0F2933,
                createdAtMillis = now - 5 * 3_600_000L,
                expiresAtMillis = now + 19 * 3_600_000L,
                isViewed = false,
                viewCount = 9
            ),
            StatusUpdateEntity(
                id = "st_zara",
                ownerId = "c_zara",
                ownerName = "Zara Kim",
                ownerAvatarColorHex = 0xFFAA00FF,
                type = StatusMediaType.TEXT.name,
                text = "Coffee break before our group sync.",
                backgroundHex = 0xFF28163A,
                createdAtMillis = now - 9 * 3_600_000L,
                expiresAtMillis = now + 15 * 3_600_000L,
                isViewed = true,
                viewCount = 22
            )
        )
        dao.insertStatuses(statuses)

        val calls = listOf(
            CallLogEntity(
                id = "call_1",
                peerId = "c_maya",
                peerName = "Maya Lin",
                peerAvatarColorHex = 0xFF00C853,
                isGroup = false,
                callType = CallType.VIDEO.name,
                direction = CallDirection.INCOMING.name,
                timestampMillis = now - 3 * 3_600_000L,
                durationSec = 245
            ),
            CallLogEntity(
                id = "call_2",
                peerId = "grp_launch",
                peerName = "SMS CHAT Launch Squad",
                peerAvatarColorHex = 0xFF00E676,
                isGroup = true,
                participantNamesCsv = "Maya Lin, Liam Carter, Sofia Alvarez",
                callType = CallType.VOICE.name,
                direction = CallDirection.OUTGOING.name,
                timestampMillis = now - 7 * 3_600_000L,
                durationSec = 612
            ),
            CallLogEntity(
                id = "call_3",
                peerId = "c_noah",
                peerName = "Noah Patel",
                peerAvatarColorHex = 0xFFFF6D00,
                isGroup = false,
                callType = CallType.VOICE.name,
                direction = CallDirection.MISSED.name,
                timestampMillis = now - 26 * 3_600_000L,
                durationSec = 0
            )
        )
        dao.insertCallLogs(calls)

        settingsDataStore.markSeeded()
    }
}
