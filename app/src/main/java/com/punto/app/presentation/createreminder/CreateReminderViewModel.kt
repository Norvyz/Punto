package com.punto.app.presentation.createreminder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punto.app.core.common.AppResult
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import com.punto.app.domain.repository.UserPreferencesRepository
import com.punto.app.geofence.AlreadyInsideChecker
import com.punto.app.geofence.GeofenceSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CreateReminderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reminderRepository: ReminderRepository,
    private val locationRepository: LocationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val geofenceSyncManager: GeofenceSyncManager,
    private val alreadyInsideChecker: AlreadyInsideChecker,
) : ViewModel() {

    private val reminderId: String? = savedStateHandle["reminderId"]
    // En "crear" no hay nada que cargar, así que los textos ya están listos desde el inicio.
    private val _uiState = MutableStateFlow(CreateReminderUiState(reminderId = reminderId, initialLoaded = reminderId == null))
    val uiState: StateFlow<CreateReminderUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.observeLocations().collectLatest { locations ->
                _uiState.update { state ->
                    val selected = state.selectedLocationId?.takeIf { id -> locations.any { it.id == id } }
                        ?: locations.firstOrNull()?.id
                    state.copy(locations = locations, selectedLocationId = selected)
                }
            }
        }
        if (reminderId != null) {
            viewModelScope.launch {
                reminderRepository.observeReminder(reminderId).collectLatest { reminder ->
                    reminder ?: return@collectLatest
                    _uiState.update {
                        it.copy(
                            name = reminder.title,
                            subject = reminder.subject.orEmpty(),
                            description = reminder.description.orEmpty(),
                            initialLoaded = true,
                            selectedLocationId = reminder.locationId,
                            triggerType = reminder.triggerType,
                            dwellMinutes = reminder.dwellMinutes ?: 30,
                            repeatIntervalMinutes = reminder.repeatIntervalMinutes,
                            maxNotifications = reminder.maxNotifications,
                            status = reminder.status,
                            createdAtEpochMillis = reminder.createdAtEpochMillis,
                        )
                    }
                }
            }
        } else {
            viewModelScope.launch {
                val p = userPreferencesRepository.preferences.first()
                _uiState.update { it.copy(dwellMinutes = p.defaultDwellMinutes, repeatIntervalMinutes = p.defaultRepeatIntervalMinutes, maxNotifications = p.defaultMaxNotifications) }
            }
        }
    }

    // Los textos (nombre, asunto, descripción) ya no pasan por aquí en cada tecla: llegan al guardar.
    fun onLocation(id: String) = _uiState.update { it.copy(selectedLocationId = id) }
    fun onTrigger(type: TriggerType) = _uiState.update { it.copy(triggerType = type) }
    fun onDwell(minutes: Int) = _uiState.update { it.copy(dwellMinutes = minutes) }
    fun onRepeat(minutes: Int?) = _uiState.update { it.copy(repeatIntervalMinutes = minutes) }
    fun onMaxNotifications(max: Int?) = _uiState.update { it.copy(maxNotifications = max) }
    fun onStatus(status: ReminderStatus) = _uiState.update { it.copy(status = status) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun save(name: String, subject: String, description: String) {
        val state = _uiState.value
        if (state.isSaving) return
        val locationId = state.selectedLocationId ?: run {
            _uiState.update { it.copy(errorMessage = "Selecciona una ubicación.") }
            return
        }
        if (name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre es obligatorio.") }
            return
        }
        val now = System.currentTimeMillis()
        val reminder = Reminder(
            id = state.reminderId ?: UUID.randomUUID().toString(),
            locationId = locationId,
            title = name.trim(),
            subject = subject.trim().ifBlank { null },
            description = description.trim().ifBlank { null },
            triggerType = state.triggerType,
            dwellMinutes = if (state.triggerType == TriggerType.ON_DWELL) state.dwellMinutes else null,
            repeatIntervalMinutes = state.repeatIntervalMinutes,
            maxNotifications = state.maxNotifications,
            status = state.status,
            createdAtEpochMillis = state.createdAtEpochMillis,
            updatedAtEpochMillis = now,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = reminderRepository.upsert(reminder)) {
                is AppResult.Success -> {
                    geofenceSyncManager.syncFromDatabase()
                    alreadyInsideChecker.checkOnCreate(reminder)
                    _uiState.update { it.copy(isSaving = false, saved = true) }
                }
                is AppResult.Error -> _uiState.update { it.copy(isSaving = false, errorMessage = result.error.message ?: "No se pudo guardar.") }
                AppResult.Loading -> Unit
            }
        }
    }
}
