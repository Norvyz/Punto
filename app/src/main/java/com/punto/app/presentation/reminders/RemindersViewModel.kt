package com.punto.app.presentation.reminders

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import com.punto.app.core.common.AppResult
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import com.punto.app.geofence.GeofenceSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemindersViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val locationRepository: LocationRepository,
    private val geofenceSyncManager: GeofenceSyncManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemindersUiState())
    val uiState: StateFlow<RemindersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(reminderRepository.observeReminders(), locationRepository.observeLocations()) { reminders, locations ->
                reminders to locations.associateBy { it.id }
            }.collect { (reminders, locationsById) ->
                _uiState.update { it.copy(reminders = reminders, locationsById = locationsById, isLoading = false) }
            }
        }
    }

    fun onQueryChange(query: String) = _uiState.update { it.copy(query = query) }
    fun onFilterChange(filter: ReminderFilter) = _uiState.update { it.copy(filter = filter) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun delete(id: String) = mutate {
        com.punto.app.geofence.ReminderNotificationHelper.cancel(context, id)
        reminderRepository.delete(id)
    }

    fun setStatus(reminder: Reminder, status: ReminderStatus) = mutate {
        val now = System.currentTimeMillis()
        val updated = when (status) {
            ReminderStatus.COMPLETED, ReminderStatus.PAUSED, ReminderStatus.PENDING -> reminder.copy(status = status, updatedAtEpochMillis = now, snoozeUntilEpochMillis = null)
            else -> reminder.copy(status = status, updatedAtEpochMillis = now)
        }
        val result = reminderRepository.upsert(updated)
        if (status == ReminderStatus.COMPLETED || status == ReminderStatus.PAUSED) {
            com.punto.app.geofence.ReminderNotificationHelper.cancel(context, reminder.id)
        }
        result
    }

    private fun mutate(block: suspend () -> AppResult<Unit>) {
        viewModelScope.launch {
            when (val result = block()) {
                is AppResult.Error -> _uiState.update { it.copy(errorMessage = result.error.message ?: "Ocurrió un error.") }
                is AppResult.Success -> geofenceSyncManager.syncFromDatabase()
                else -> Unit
            }
        }
    }
}