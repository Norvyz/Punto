package com.punto.app.data.repository

import com.punto.app.data.local.datastore.UserPreferencesDataSource
import com.punto.app.domain.model.ThemePreference
import com.punto.app.domain.repository.UserPreferences
import com.punto.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreUserPreferencesRepository @Inject constructor(
    private val dataSource: UserPreferencesDataSource,
) : UserPreferencesRepository {

    override val preferences: Flow<UserPreferences> = dataSource.preferences

    override suspend fun setTheme(theme: ThemePreference) = dataSource.setTheme(theme)
    override suspend fun setDefaultRadiusMeters(value: Int) = dataSource.setDefaultRadiusMeters(value)
    override suspend fun setDefaultDwellMinutes(value: Int) = dataSource.setDefaultDwellMinutes(value)
    override suspend fun setDefaultRepeatIntervalMinutes(value: Int) = dataSource.setDefaultRepeatIntervalMinutes(value)
    override suspend fun setDefaultMaxNotifications(value: Int?) = dataSource.setDefaultMaxNotifications(value)
    override suspend fun setDefaultSnoozeMinutes(value: Int) = dataSource.setDefaultSnoozeMinutes(value)
    override suspend fun setVibrationEnabled(enabled: Boolean) = dataSource.setVibrationEnabled(enabled)
    override suspend fun setNotificationSoundUri(uri: String?) = dataSource.setNotificationSoundUri(uri)
}
