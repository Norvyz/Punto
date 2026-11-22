package com.punto.app.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punto.app.domain.model.AccentColor
import com.punto.app.domain.model.ThemePreference
import com.punto.app.domain.repository.UserPreferences
import com.punto.app.domain.repository.UserPreferencesRepository
import com.punto.app.geofence.ReminderNotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<UserPreferences> = prefs.preferences.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences()
    )

    fun setTheme(v: ThemePreference) = launch { prefs.setTheme(v) }
    fun setAccent(v: AccentColor) = launch { prefs.setAccent(v) }
    fun setDefaultRadius(v: Int) = launch { prefs.setDefaultRadiusMeters(v) }
    fun setDefaultDwell(v: Int) = launch { prefs.setDefaultDwellMinutes(v) }
    fun setDefaultRepeat(v: Int) = launch { prefs.setDefaultRepeatIntervalMinutes(v) }
    fun setDefaultMax(v: Int?) = launch { prefs.setDefaultMaxNotifications(v) }
    fun setDefaultSnooze(v: Int) = launch { prefs.setDefaultSnoozeMinutes(v) }
    fun setVibration(v: Boolean) = launch { prefs.setVibrationEnabled(v); refreshChannel() }
    fun setSound(uri: String?) = launch { prefs.setNotificationSoundUri(uri); refreshChannel() }

    private suspend fun refreshChannel() {
        val config = ReminderNotificationHelper.resolveChannelConfig(prefs)
        ReminderNotificationHelper.ensureChannel(context, config)
    }

    private fun launch(block: suspend () -> Unit) { viewModelScope.launch { block() } }
}
