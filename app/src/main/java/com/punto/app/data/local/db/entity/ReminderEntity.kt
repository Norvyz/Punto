package com.punto.app.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = LocationEntity::class,
            parentColumns = ["id"],
            childColumns = ["locationId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["locationId"]),
        Index(value = ["status"]),
    ],
)
data class ReminderEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val title: String,
    val subject: String?,
    val description: String?,
    val triggerType: String,
    val dwellMinutes: Int?,
    val repeatIntervalMinutes: Int?,
    val maxNotifications: Int?,
    val status: String,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val notificationCount: Int = 0,
    val lastNotifiedAtEpochMillis: Long? = null,
    val snoozeUntilEpochMillis: Long? = null,
)
