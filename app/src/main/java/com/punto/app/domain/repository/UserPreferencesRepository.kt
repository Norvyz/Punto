package com.punto.app.domain.repository

import com.punto.app.domain.model.ThemePreference
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setTheme(theme: ThemePreference)
    suspend fun setDefaultRadiusMeters(value: Int)
    suspend fun setDefaultDwellMinutes(value: Int)
    suspend fun setDefaultRepeatIntervalMinutes(value: Int)
    suspend fun setDefaultMaxNotifications(value: Int?)
    suspend fun setDefaultSnoozeMinutes(value: Int)
    suspend fun setVibrationEnabled(enabled: Boolean)
    suspend fun setNotificationSoundUri(uri: String?)
}

data class UserPreferences(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val defaultRadiusMeters: Int = 150,
    val defaultDwellMinutes: Int = 30,
    val defaultRepeatIntervalMinutes: Int = 30,
    val defaultMaxNotifications: Int? = null,
    val defaultSnoozeMinutes: Int = 15,
    val vibrationEnabled: Boolean = true,
    val notificationSoundUri: String? = null,
)
