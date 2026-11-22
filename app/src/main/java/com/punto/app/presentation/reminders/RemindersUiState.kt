package com.punto.app.presentation.reminders

import com.punto.app.domain.model.Location
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus

data class RemindersUiState(
    val reminders: List<Reminder> = emptyList(),
    val locationsById: Map<String, Location> = emptyMap(),
    val query: String = "",
    val filter: ReminderFilter = ReminderFilter.ALL,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

enum class ReminderFilter(val label: String) {
    ALL("Todos"),
    PENDING("Pendientes"),
    ACTIVE("Activos"),
    COMPLETED("Completados"),
    PAUSED("Pausados"),
}

fun Reminder.matchesFilter(filter: ReminderFilter): Boolean = when (filter) {
    ReminderFilter.ALL -> true
    ReminderFilter.PENDING -> status == ReminderStatus.PENDING || status == ReminderStatus.WAITING_ARRIVAL || status == ReminderStatus.WAITING_DWELL || status == ReminderStatus.SNOOZED
    ReminderFilter.ACTIVE -> status == ReminderStatus.ACTIVE
    ReminderFilter.COMPLETED -> status == ReminderStatus.COMPLETED
    ReminderFilter.PAUSED -> status == ReminderStatus.PAUSED
}
