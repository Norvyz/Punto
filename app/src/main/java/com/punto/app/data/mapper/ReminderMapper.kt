package com.punto.app.data.mapper

import com.punto.app.data.local.db.entity.ReminderEntity
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType

fun ReminderEntity.toDomain(): Reminder = Reminder(
    id = id,
    locationId = locationId,
    title = title,
    subject = subject,
    description = description,
    triggerType = runCatching { TriggerType.valueOf(triggerType) }.getOrDefault(TriggerType.ON_ENTER),
    dwellMinutes = dwellMinutes,
    repeatIntervalMinutes = repeatIntervalMinutes,
    maxNotifications = maxNotifications,
    status = runCatching { ReminderStatus.valueOf(status) }.getOrDefault(ReminderStatus.PENDING),
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    notificationCount = notificationCount,
    lastNotifiedAtEpochMillis = lastNotifiedAtEpochMillis,
    snoozeUntilEpochMillis = snoozeUntilEpochMillis,
)

fun Reminder.toEntity(): ReminderEntity = ReminderEntity(
    id = id,
    locationId = locationId,
    title = title,
    subject = subject,
    description = description,
    triggerType = triggerType.name,
    dwellMinutes = dwellMinutes,
    repeatIntervalMinutes = repeatIntervalMinutes,
    maxNotifications = maxNotifications,
    status = status.name,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    notificationCount = notificationCount,
    lastNotifiedAtEpochMillis = lastNotifiedAtEpochMillis,
    snoozeUntilEpochMillis = snoozeUntilEpochMillis,
)
