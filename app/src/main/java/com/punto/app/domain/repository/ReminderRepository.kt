package com.punto.app.domain.repository

import com.punto.app.core.common.AppResult
import com.punto.app.domain.model.Reminder
import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminders(): Flow<List<Reminder>>
    fun observeReminder(id: String): Flow<Reminder?>
    fun observeRemindersByLocation(locationId: String): Flow<List<Reminder>>
    suspend fun getAll(): List<Reminder>
    suspend fun getById(id: String): Reminder?
    suspend fun getByLocation(locationId: String): List<Reminder>
    suspend fun upsert(reminder: Reminder): AppResult<Unit>
    suspend fun updateStatus(id: String, status: com.punto.app.domain.model.ReminderStatus): AppResult<Unit>
    suspend fun updateNotificationState(id: String, status: com.punto.app.domain.model.ReminderStatus, count: Int, notifiedAt: Long?, snoozeUntil: Long?): AppResult<Unit>
    suspend fun delete(id: String): AppResult<Unit>
}
