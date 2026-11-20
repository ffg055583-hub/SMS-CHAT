package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "sms_chat_settings")

enum class ThemeMode {
    DARK,
    LIGHT,
    SYSTEM
}

data class PrivacyPreferences(
    val showLastSeen: Boolean = true,
    val showProfilePhoto: Boolean = true,
    val readReceipts: Boolean = true
)

data class NotificationPreferences(
    val messageNotifications: Boolean = true,
    val groupNotifications: Boolean = true,
    val callRingtone: Boolean = true,
    val inAppSounds: Boolean = true
)

data class AppPreferencesState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val privacy: PrivacyPreferences = PrivacyPreferences(),
    val notifications: NotificationPreferences = NotificationPreferences(),
    val hasSeededData: Boolean = false
)

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SHOW_LAST_SEEN = booleanPreferencesKey("show_last_seen")
        val SHOW_PROFILE_PHOTO = booleanPreferencesKey("show_profile_photo")
        val READ_RECEIPTS = booleanPreferencesKey("read_receipts")
        val MESSAGE_NOTIFICATIONS = booleanPreferencesKey("message_notifications")
        val GROUP_NOTIFICATIONS = booleanPreferencesKey("group_notifications")
        val CALL_RINGTONE = booleanPreferencesKey("call_ringtone")
        val IN_APP_SOUNDS = booleanPreferencesKey("in_app_sounds")
        val HAS_SEEDED_DATA = booleanPreferencesKey("has_seeded_data")
    }

    val preferencesFlow: Flow<AppPreferencesState> = context.settingsDataStore.data.map { prefs ->
        val themeStr = prefs[Keys.THEME_MODE] ?: ThemeMode.DARK.name
        val theme = runCatching { ThemeMode.valueOf(themeStr) }.getOrDefault(ThemeMode.DARK)
        AppPreferencesState(
            themeMode = theme,
            privacy = PrivacyPreferences(
                showLastSeen = prefs[Keys.SHOW_LAST_SEEN] ?: true,
                showProfilePhoto = prefs[Keys.SHOW_PROFILE_PHOTO] ?: true,
                readReceipts = prefs[Keys.READ_RECEIPTS] ?: true
            ),
            notifications = NotificationPreferences(
                messageNotifications = prefs[Keys.MESSAGE_NOTIFICATIONS] ?: true,
                groupNotifications = prefs[Keys.GROUP_NOTIFICATIONS] ?: true,
                callRingtone = prefs[Keys.CALL_RINGTONE] ?: true,
                inAppSounds = prefs[Keys.IN_APP_SOUNDS] ?: true
            ),
            hasSeededData = prefs[Keys.HAS_SEEDED_DATA] ?: false
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun updatePrivacy(transform: (PrivacyPreferences) -> PrivacyPreferences) {
        context.settingsDataStore.edit { prefs ->
            val current = PrivacyPreferences(
                showLastSeen = prefs[Keys.SHOW_LAST_SEEN] ?: true,
                showProfilePhoto = prefs[Keys.SHOW_PROFILE_PHOTO] ?: true,
                readReceipts = prefs[Keys.READ_RECEIPTS] ?: true
            )
            val updated = transform(current)
            prefs[Keys.SHOW_LAST_SEEN] = updated.showLastSeen
            prefs[Keys.SHOW_PROFILE_PHOTO] = updated.showProfilePhoto
            prefs[Keys.READ_RECEIPTS] = updated.readReceipts
        }
    }

    suspend fun updateNotifications(transform: (NotificationPreferences) -> NotificationPreferences) {
        context.settingsDataStore.edit { prefs ->
            val current = NotificationPreferences(
                messageNotifications = prefs[Keys.MESSAGE_NOTIFICATIONS] ?: true,
                groupNotifications = prefs[Keys.GROUP_NOTIFICATIONS] ?: true,
                callRingtone = prefs[Keys.CALL_RINGTONE] ?: true,
                inAppSounds = prefs[Keys.IN_APP_SOUNDS] ?: true
            )
            val updated = transform(current)
            prefs[Keys.MESSAGE_NOTIFICATIONS] = updated.messageNotifications
            prefs[Keys.GROUP_NOTIFICATIONS] = updated.groupNotifications
            prefs[Keys.CALL_RINGTONE] = updated.callRingtone
            prefs[Keys.IN_APP_SOUNDS] = updated.inAppSounds
        }
    }

    suspend fun markSeeded() {
        context.settingsDataStore.edit { it[Keys.HAS_SEEDED_DATA] = true }
    }
}
