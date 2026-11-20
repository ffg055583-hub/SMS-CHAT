package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val uniqueUserId: String,
    val phoneNumber: String,
    val displayName: String,
    val bio: String = "Hey there! I am using SMS CHAT.",
    val avatarUri: String? = null,
    val avatarColorHex: Long = 0xFF00C853,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val uniqueUserId: String,
    val name: String,
    val phoneNumber: String,
    val bio: String = "Available",
    val avatarColorHex: Long = 0xFF00C853,
    val avatarUri: String? = null,
    val isOnline: Boolean = false,
    val lastSeenMillis: Long = System.currentTimeMillis(),
    val isBlocked: Boolean = false,
    val isReported: Boolean = false
)

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isGroup: Boolean = false,
    val peerContactId: String? = null,
    val memberIdsCsv: String = "",
    val avatarColorHex: Long = 0xFF00C853,
    val avatarUri: String? = null,
    val lastMessagePreview: String = "",
    val lastMessageTimeMillis: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val isPinned: Boolean = false
)

enum class MessageType {
    TEXT,
    IMAGE,
    VIDEO,
    VOICE,
    STICKER,
    SYSTEM
}

enum class MessageStatus {
    SENT,
    DELIVERED,
    READ
}

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val type: String = MessageType.TEXT.name,
    val text: String = "",
    val mediaUri: String? = null,
    val voiceDurationSec: Int = 0,
    val stickerCode: String? = null,
    val replyToText: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = MessageStatus.SENT.name,
    val isDeletedForAll: Boolean = false
)

enum class StatusMediaType {
    TEXT,
    PHOTO,
    VIDEO
}

@Entity(tableName = "status_updates")
data class StatusUpdateEntity(
    @PrimaryKey val id: String,
    val ownerId: String,
    val ownerName: String,
    val ownerAvatarColorHex: Long = 0xFF00C853,
    val ownerAvatarUri: String? = null,
    val type: String = StatusMediaType.TEXT.name,
    val text: String = "",
    val mediaUri: String? = null,
    val backgroundHex: Long = 0xFF0B2B1C,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val expiresAtMillis: Long = System.currentTimeMillis() + 24L * 60L * 60L * 1000L,
    val isViewed: Boolean = false,
    val viewCount: Int = 0
)

enum class CallType {
    VOICE,
    VIDEO
}

enum class CallDirection {
    INCOMING,
    OUTGOING,
    MISSED
}

@Entity(tableName = "call_logs")
data class CallLogEntity(
    @PrimaryKey val id: String,
    val peerId: String,
    val peerName: String,
    val peerAvatarColorHex: Long = 0xFF00C853,
    val isGroup: Boolean = false,
    val participantNamesCsv: String = "",
    val callType: String = CallType.VOICE.name,
    val direction: String = CallDirection.OUTGOING.name,
    val timestampMillis: Long = System.currentTimeMillis(),
    val durationSec: Int = 0
)
