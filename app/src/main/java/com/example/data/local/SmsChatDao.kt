package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CallLogEntity
import com.example.data.model.ContactEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StatusUpdateEntity
import com.example.data.model.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsChatDao {

    // User Profile
    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observeUserProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun getUserProfileOnce(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // Contacts
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun observeContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContact(contact: ContactEntity)

    @Query("UPDATE contacts SET isBlocked = :blocked WHERE id = :contactId")
    suspend fun setContactBlocked(contactId: String, blocked: Boolean)

    @Query("UPDATE contacts SET isReported = :reported, isBlocked = :blocked WHERE id = :contactId")
    suspend fun reportAndBlockContact(contactId: String, reported: Boolean, blocked: Boolean)

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY isPinned DESC, lastMessageTimeMillis DESC")
    fun observeConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    fun observeConversation(conversationId: String): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE id = :conversationId LIMIT 1")
    suspend fun getConversationById(conversationId: String): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE peerContactId = :contactId AND isGroup = 0 LIMIT 1")
    suspend fun getDirectConversationForContact(contactId: String): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConversation(conversation: ConversationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversations(conversations: List<ConversationEntity>)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun clearUnreadCount(conversationId: String)

    @Query("UPDATE conversations SET lastMessagePreview = :preview, lastMessageTimeMillis = :timeMillis, unreadCount = unreadCount + :unreadDelta WHERE id = :conversationId")
    suspend fun updateConversationPreview(
        conversationId: String,
        preview: String,
        timeMillis: Long,
        unreadDelta: Int
    )

    @Query("UPDATE conversations SET memberIdsCsv = :memberIdsCsv, title = :title WHERE id = :conversationId")
    suspend fun updateGroupDetails(conversationId: String, title: String, memberIdsCsv: String)

    @Query("UPDATE conversations SET isPinned = :pinned WHERE id = :conversationId")
    suspend fun setConversationPinned(conversationId: String, pinned: Boolean)

    // Messages
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestampMillis ASC")
    fun observeMessages(conversationId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<MessageEntity>)

    @Query("UPDATE messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET status = :status WHERE conversationId = :conversationId AND senderId != :currentUserId AND status != :status")
    suspend fun markIncomingMessagesRead(conversationId: String, currentUserId: String, status: String)

    @Query("UPDATE messages SET isDeletedForAll = 1, text = 'This message was deleted', mediaUri = NULL, stickerCode = NULL WHERE id = :messageId")
    suspend fun markMessageDeletedForAll(messageId: String)

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageLocally(messageId: String)

    // Status & Stories (24-hour expiration)
    @Query("SELECT * FROM status_updates WHERE expiresAtMillis > :nowMillis ORDER BY createdAtMillis DESC")
    fun observeActiveStatuses(nowMillis: Long): Flow<List<StatusUpdateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusUpdateEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatuses(statuses: List<StatusUpdateEntity>)

    @Query("UPDATE status_updates SET isViewed = 1, viewCount = viewCount + 1 WHERE id = :statusId AND isViewed = 0")
    suspend fun markStatusViewed(statusId: String)

    @Query("DELETE FROM status_updates WHERE id = :statusId")
    suspend fun deleteStatus(statusId: String)

    @Query("DELETE FROM status_updates WHERE expiresAtMillis <= :nowMillis")
    suspend fun purgeExpiredStatuses(nowMillis: Long)

    // Call History
    @Query("SELECT * FROM call_logs ORDER BY timestampMillis DESC")
    fun observeCallLogs(): Flow<List<CallLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(callLogs: List<CallLogEntity>)

    @Query("DELETE FROM call_logs WHERE id = :callLogId")
    suspend fun deleteCallLog(callLogId: String)
}
