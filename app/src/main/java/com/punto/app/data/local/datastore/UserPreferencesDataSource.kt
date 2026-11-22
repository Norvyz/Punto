package com.punto.app.data.local.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.punto.app.domain.model.ThemePreference
import com.punto.app.domain.repository.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = UserPreferencesDataSource.DATASTORE_NAME,
)

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val preferences: Flow<UserPreferences> = context.userPreferencesDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            UserPreferences(
                theme = prefs[Keys.THEME]
                    ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                    ?: ThemePreference.SYSTEM,
                defaultRadiusMeters = prefs[Keys.DEFAULT_RADIUS_METERS] ?: 150,
                defaultDwellMinutes = prefs[Keys.DEFAULT_DWELL_MINUTES] ?: 30,
                defaultRepeatIntervalMinutes = prefs[Keys.DEFAULT_REPEAT_INTERVAL_MINUTES] ?: 30,
                defaultMaxNotifications = prefs[Keys.DEFAULT_MAX_NOTIFICATIONS]?.takeIf { it > 0 },
                defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MINUTES] ?: 15,
                vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
                notificationSoundUri = prefs[Keys.NOTIFICATION_SOUND_URI],
            )
        }

    suspend fun setTheme(theme: ThemePreference) { context.userPreferencesDataStore.edit { it[Keys.THEME] = theme.name } }
    suspend fun setDefaultRadiusMeters(value: Int) { context.userPreferencesDataStore.edit { it[Keys.DEFAULT_RADIUS_METERS] = value } }
    suspend fun setDefaultDwellMinutes(value: Int) { context.userPreferencesDataStore.edit { it[Keys.DEFAULT_DWELL_MINUTES] = value } }
    suspend fun setDefaultRepeatIntervalMinutes(value: Int) { context.userPreferencesDataStore.edit { it[Keys.DEFAULT_REPEAT_INTERVAL_MINUTES] = value } }
    suspend fun setDefaultMaxNotifications(value: Int?) { context.userPreferencesDataStore.edit { if (value == null) it.remove(Keys.DEFAULT_MAX_NOTIFICATIONS) else it[Keys.DEFAULT_MAX_NOTIFICATIONS] = value } }
    suspend fun setDefaultSnoozeMinutes(value: Int) { context.userPreferencesDataStore.edit { it[Keys.DEFAULT_SNOOZE_MINUTES] = value } }
    suspend fun setVibrationEnabled(enabled: Boolean) { context.userPreferencesDataStore.edit { it[Keys.VIBRATION_ENABLED] = enabled } }
    suspend fun setNotificationSoundUri(uri: String?) { context.userPreferencesDataStore.edit { if (uri == null) it.remove(Keys.NOTIFICATION_SOUND_URI) else it[Keys.NOTIFICATION_SOUND_URI] = uri } }

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val DEFAULT_RADIUS_METERS = intPreferencesKey("default_radius_meters")
        val DEFAULT_DWELL_MINUTES = intPreferencesKey("default_dwell_minutes")
        val DEFAULT_REPEAT_INTERVAL_MINUTES = intPreferencesKey("default_repeat_interval_minutes")
        val DEFAULT_MAX_NOTIFICATIONS = intPreferencesKey("default_max_notifications")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val NOTIFICATION_SOUND_URI = stringPreferencesKey("notification_sound_uri")
    }

    companion object {
        const val DATASTORE_NAME = "user_preferences"
    }
}
