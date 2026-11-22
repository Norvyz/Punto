package com.punto.app.presentation.home

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

data class HomeUiState(
    val activeCount: Int = 0,
    val pendingCount: Int = 0,
    val placesCount: Int = 0,
    val upcoming: List<com.punto.app.domain.model.Reminder> = emptyList(),
    val locationsById: Map<String, com.punto.app.domain.model.Location> = emptyMap(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    reminderRepository: ReminderRepository,
    locationRepository: LocationRepository,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = combine(
        reminderRepository.observeReminders(),
        locationRepository.observeLocations(),
    ) { reminders, locations ->
        val locationsById = locations.associateBy { it.id }
        val upcoming = reminders.filter { it.status in setOf(ReminderStatus.ACTIVE, ReminderStatus.WAITING_ARRIVAL, ReminderStatus.WAITING_DWELL, ReminderStatus.SNOOZED, ReminderStatus.PENDING) }
        HomeUiState(
            activeCount = reminders.count { it.status == ReminderStatus.ACTIVE || it.status == ReminderStatus.WAITING_ARRIVAL || it.status == ReminderStatus.WAITING_DWELL },
            pendingCount = reminders.count { it.status == ReminderStatus.PENDING || it.status == ReminderStatus.SNOOZED },
            placesCount = locations.size,
            upcoming = upcoming.take(5),
            locationsById = locationsById,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
