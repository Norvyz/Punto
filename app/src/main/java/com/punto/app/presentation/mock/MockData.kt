package com.punto.app.presentation.mock

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.punto.app.domain.model.PlaceCategory
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType

data class MockLocation(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val address: String,
    val radiusMeters: Int,
    val lat: Double,
    val lng: Double,
)

data class MockReminder(
    val id: String,
    val title: String,
    val subject: String,
    val description: String,
    val locationName: String,
    val locationId: String,
    val triggerType: TriggerType,
    val dwellMinutes: Int?,
    val repeatMinutes: Int?,
    val status: ReminderStatus,
    val timeLabel: String,
)

object MockData {
    val locations = listOf(
        MockLocation("1", "Casa", PlaceCategory.HOME, "Calle 45 #12-30, Bogotá", 150, 4.7110, -74.0721),
        MockLocation("2", "Trabajo", PlaceCategory.WORK, "Cra 7 #71-52, Bogotá", 200, 4.6560, -74.0570),
        MockLocation("3", "Universidad", PlaceCategory.SCHOOL, "Ciudad Universitaria", 120, 4.6382, -74.0840),
        MockLocation("4", "Gimnasio", PlaceCategory.GYM, "Av. 68 #80-10", 100, 4.7010, -74.0900),
    )

    val reminders = listOf(
        MockReminder("r1", "Enviar correo al jefe", "Trabajo", "Enviar informe semanal de avances.", "Casa", "1", TriggerType.ON_DWELL, 30, 30, ReminderStatus.ACTIVE, "Activa hace 10 min"),
        MockReminder("r2", "Entregar documento", "Trabajo", "Llevar documento firmado a recepción.", "Trabajo", "2", TriggerType.ON_ENTER, null, 15, ReminderStatus.WAITING_ARRIVAL, "Esperando llegada"),
        MockReminder("r3", "Comprar proteína", "Salud", "Recordar comprar suplemento después de entrenar.", "Gimnasio", "4", TriggerType.ON_EXIT, null, null, ReminderStatus.PENDING, "Pendiente"),
        MockReminder("r4", "Revisar apuntes", "Estudio", "Repasar notas antes de clase.", "Universidad", "3", TriggerType.ON_DWELL, 10, 20, ReminderStatus.SNOOZED, "Pospuesto 15 min"),
        MockReminder("r5", "Sacar la basura", "Hogar", "Sacar bolsas antes de salir.", "Casa", "1", TriggerType.ON_EXIT, null, null, ReminderStatus.COMPLETED, "Completado hoy"),
        MockReminder("r6", "Llamar a mamá", "Personal", "Llamada rápida al llegar a casa.", "Casa", "1", TriggerType.ON_ENTER, null, 60, ReminderStatus.COMPLETED, "Completado ayer"),
    )

    val activeReminders = reminders.filter { it.status == ReminderStatus.ACTIVE || it.status == ReminderStatus.WAITING_ARRIVAL || it.status == ReminderStatus.WAITING_DWELL || it.status == ReminderStatus.SNOOZED }
    val pendingReminders = reminders.filter { it.status == ReminderStatus.PENDING }
    val historyReminders = reminders.filter { it.status == ReminderStatus.COMPLETED }
}

fun PlaceCategory.label(): String = when (this) {
    PlaceCategory.HOME -> "Casa"
    PlaceCategory.WORK -> "Trabajo"
    PlaceCategory.SCHOOL -> "Estudio"
    PlaceCategory.GYM -> "Gimnasio"
    PlaceCategory.CUSTOM -> "Personalizado"
}

fun TriggerType.label(): String = when (this) {
    TriggerType.ON_ENTER -> "Al llegar"
    TriggerType.ON_DWELL -> "Al permanecer"
    TriggerType.ON_EXIT -> "Al salir"
    TriggerType.ALREADY_INSIDE -> "Si ya estoy dentro"
}

fun ReminderStatus.label(): String = when (this) {
    ReminderStatus.PENDING -> "Pendiente"
    ReminderStatus.WAITING_ARRIVAL -> "Esperando llegada"
    ReminderStatus.WAITING_DWELL -> "Esperando permanencia"
    ReminderStatus.ACTIVE -> "Activo"
    ReminderStatus.SNOOZED -> "Pospuesto"
    ReminderStatus.COMPLETED -> "Completado"
    ReminderStatus.PAUSED -> "Pausado"
}

@Composable
fun ReminderStatus.containerColor(): Color = when (this) {
    ReminderStatus.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
    ReminderStatus.WAITING_ARRIVAL, ReminderStatus.WAITING_DWELL, ReminderStatus.SNOOZED -> MaterialTheme.colorScheme.tertiaryContainer
    ReminderStatus.COMPLETED -> MaterialTheme.colorScheme.secondaryContainer
    ReminderStatus.PAUSED, ReminderStatus.PENDING -> MaterialTheme.colorScheme.surfaceContainerHighest
}

@Composable
fun ReminderStatus.contentColor(): Color = when (this) {
    ReminderStatus.ACTIVE -> MaterialTheme.colorScheme.onPrimaryContainer
    ReminderStatus.WAITING_ARRIVAL, ReminderStatus.WAITING_DWELL, ReminderStatus.SNOOZED -> MaterialTheme.colorScheme.onTertiaryContainer
    ReminderStatus.COMPLETED -> MaterialTheme.colorScheme.onSecondaryContainer
    ReminderStatus.PAUSED, ReminderStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
}
