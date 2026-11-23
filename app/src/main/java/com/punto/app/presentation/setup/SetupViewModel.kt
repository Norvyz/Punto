package com.punto.app.presentation.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punto.app.core.common.AppResult
import com.punto.app.domain.model.AccentColor
import com.punto.app.domain.model.Location
import com.punto.app.domain.model.PlaceCategory
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.ThemePreference
import com.punto.app.domain.model.TriggerType
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import com.punto.app.domain.repository.UserPreferences
import com.punto.app.domain.repository.UserPreferencesRepository
import com.punto.app.geofence.GeofenceSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

data class SetupUiState(
    val sampleLocationId: String? = null,
    val sampleLocationName: String? = null,
    val sampleReminderId: String? = null,
    val sampleReminderName: String? = null,
    val isSavingLocation: Boolean = false,
    val isSavingReminder: Boolean = false,
    val locationSaved: Boolean = false,
    val reminderSaved: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val prefs: UserPreferencesRepository,
    private val locationRepository: LocationRepository,
    private val reminderRepository: ReminderRepository,
    private val geofenceSyncManager: GeofenceSyncManager,
) : ViewModel() {

    val uiState: StateFlow<UserPreferences> = prefs.preferences.stateIn(
        viewModelScope, SharingStarted.Eagerly, UserPreferences(),
    )

    private val _flowState = MutableStateFlow(SetupUiState())
    val flowState: StateFlow<SetupUiState> = _flowState.asStateFlow()

    fun setTheme(theme: ThemePreference) { viewModelScope.launch { prefs.setTheme(theme) } }
    fun setAccent(accent: AccentColor) { viewModelScope.launch { prefs.setAccent(accent) } }
    fun clearError() { _flowState.update { it.copy(error = null) } }

    fun saveSampleLocation(name: String, latitude: Double, longitude: Double, address: String?) {
        val trimmed = name.trim().ifBlank { "Mi lugar" }
        viewModelScope.launch {
            _flowState.update { it.copy(isSavingLocation = true, error = null) }
            val location = Location(
                id = _flowState.value.sampleLocationId ?: UUID.randomUUID().toString(),
                name = trimmed,
                category = PlaceCategory.HOME,
                latitude = latitude,
                longitude = longitude,
                address = address?.trim()?.ifBlank { null },
                radiusMeters = 150,
                createdAtEpochMillis = System.currentTimeMillis(),
            )
            when (val result = locationRepository.upsert(location)) {
                is AppResult.Success -> {
                    geofenceSyncManager.syncFromDatabase()
                    _flowState.update {
                        it.copy(
                            isSavingLocation = false,
                            locationSaved = true,
                            sampleLocationId = location.id,
                            sampleLocationName = location.name,
                        )
                    }
                }
                is AppResult.Error -> _flowState.update {
                    it.copy(isSavingLocation = false, error = result.error.message ?: "No se pudo guardar el lugar.")
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun saveSampleReminder(name: String) {
        val locationId = _flowState.value.sampleLocationId ?: run {
            _flowState.update { it.copy(error = "Primero guarda un lugar.") }
            return
        }
        val trimmed = name.trim().ifBlank { "Mi recordatorio" }
        viewModelScope.launch {
            _flowState.update { it.copy(isSavingReminder = true, error = null) }
            val now = System.currentTimeMillis()
            val reminder = Reminder(
                id = _flowState.value.sampleReminderId ?: UUID.randomUUID().toString(),
                locationId = locationId,
                title = trimmed,
                subject = "Personal",
                description = "Creado durante la guía de inicio.",
                triggerType = TriggerType.ON_ENTER,
                dwellMinutes = null,
                repeatIntervalMinutes = 15,
                maxNotifications = null,
                status = ReminderStatus.PENDING,
                createdAtEpochMillis = now,
                updatedAtEpochMillis = now,
            )
            when (val result = reminderRepository.upsert(reminder)) {
                is AppResult.Success -> {
                    geofenceSyncManager.syncFromDatabase()
                    _flowState.update {
                        it.copy(
                            isSavingReminder = false,
                            reminderSaved = true,
                            sampleReminderId = reminder.id,
                            sampleReminderName = reminder.title,
                        )
                    }
                }
                is AppResult.Error -> _flowState.update {
                    it.copy(isSavingReminder = false, error = result.error.message ?: "No se pudo guardar el recordatorio.")
                }
                AppResult.Loading -> Unit
            }
        }
    }

    /** Marca la configuración inicial como hecha; así la bienvenida no vuelve a salir. */
    fun complete() {
        viewModelScope.launch { withContext(NonCancellable) { prefs.setOnboardingCompleted(true) } }
    }
}
