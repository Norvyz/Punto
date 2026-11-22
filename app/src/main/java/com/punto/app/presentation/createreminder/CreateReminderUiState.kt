package com.punto.app.presentation.createreminder

import com.punto.app.domain.model.Location
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType

data class CreateReminderUiState(
    val reminderId: String? = null,
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    val locations: List<Location> = emptyList(),
    val selectedLocationId: String? = null,
    val triggerType: TriggerType = TriggerType.ON_ENTER,
    val dwellMinutes: Int = 30,
    val repeatIntervalMinutes: Int? = 30,
    val maxNotifications: Int? = null,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val errorMessage: String? = null,
)

val CreateReminderUiState.isEdit: Boolean get() = reminderId != null
val CreateReminderUiState.canSave: Boolean get() = name.isNotBlank() && selectedLocationId != null && !isSaving
