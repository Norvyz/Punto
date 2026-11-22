package com.punto.app.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HistoryUiState(
    val completed: List<com.punto.app.domain.model.Reminder> = emptyList(),
    val locationsById: Map<String, com.punto.app.domain.model.Location> = emptyMap(),
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    locationRepository: LocationRepository,
) : ViewModel() {
    val uiState: StateFlow<HistoryUiState> = combine(
        reminderRepository.observeReminders(),
        locationRepository.observeLocations(),
    ) { reminders, locations ->
        HistoryUiState(
            completed = reminders.filter { it.status == ReminderStatus.COMPLETED },
            locationsById = locations.associateBy { it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HistoryUiState())
}
