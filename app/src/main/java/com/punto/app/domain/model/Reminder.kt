package com.punto.app.domain.model

data class Reminder(
    val id: String,
    val locationId: String,
    val title: String,
    val subject: String?,
    val description: String?,
    val triggerType: TriggerType,
    val dwellMinutes: Int?,
    val repeatIntervalMinutes: Int?,
    val maxNotifications: Int?,
    val status: ReminderStatus,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val notificationCount: Int = 0,
    val lastNotifiedAtEpochMillis: Long? = null,
    val snoozeUntilEpochMillis: Long? = null,
)
