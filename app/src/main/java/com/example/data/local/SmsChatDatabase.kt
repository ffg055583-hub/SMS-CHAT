package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CallLogEntity
import com.example.data.model.ContactEntity
import com.example.data.model.ConversationEntity
import com.example.data.model.MessageEntity
import com.example.data.model.StatusUpdateEntity
import com.example.data.model.UserProfileEntity

@Database(
    entities = [
        UserProfileEntity::class,
        ContactEntity::class,
        ConversationEntity::class,
        MessageEntity::class,
        StatusUpdateEntity::class,
        CallLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SmsChatDatabase : RoomDatabase() {
    abstract fun smsChatDao(): SmsChatDao

    companion object {
        @Volatile
        private var INSTANCE: SmsChatDatabase? = null

        fun getInstance(context: Context): SmsChatDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SmsChatDatabase::class.java,
                    "sms_chat_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
