package com.punto.app.presentation.createreminder

import com.punto.app.domain.model.Location
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType

/**
 * Nota: nombre, asunto y descripción viven en estado LOCAL de la pantalla (ver
 * CreateReminderScreen) para que escribir no pase por el ViewModel en cada tecla.
 * Aquí solo se guardan como valores iniciales al editar un recordatorio existente.
 */
data class CreateReminderUiState(
    val reminderId: String? = null,
    val name: String = "",
    val subject: String = "",
    val description: String = "",
    /** true cuando ya están cargados los textos iniciales (en "crear" es true desde el inicio). */
    val initialLoaded: Boolean = false,
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
