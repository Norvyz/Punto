package com.punto.app.data.repository

import com.punto.app.core.common.AppResult
import com.punto.app.core.error.AppError
import com.punto.app.core.error.runAppCatching
import com.punto.app.data.local.db.dao.ReminderDao
import com.punto.app.data.mapper.toDomain
import com.punto.app.data.mapper.toEntity
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.repository.ReminderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomReminderRepository @Inject constructor(
    private val dao: ReminderDao,
) : ReminderRepository {

    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeReminder(id: String): Flow<Reminder?> =
        dao.observeById(id).map { it?.toDomain() }

    override fun observeRemindersByLocation(locationId: String): Flow<List<Reminder>> =
        dao.observeByLocation(locationId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getAll(): List<Reminder> = dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Reminder? = dao.getById(id)?.toDomain()

    override suspend fun getByLocation(locationId: String): List<Reminder> = dao.getByLocation(locationId).map { it.toDomain() }

    override suspend fun upsert(reminder: Reminder): AppResult<Unit> = runAppCatching(
        mapError = { AppError.Database(it.message, it) },
    ) {
        dao.upsert(reminder.toEntity())
    }

    override suspend fun updateStatus(id: String, status: com.punto.app.domain.model.ReminderStatus): AppResult<Unit> = runAppCatching(
        mapError = { AppError.Database(it.message, it) },
    ) {
        dao.updateStatus(id, status.name, System.currentTimeMillis())
    }

    override suspend fun updateNotificationState(id: String, status: com.punto.app.domain.model.ReminderStatus, count: Int, notifiedAt: Long?, snoozeUntil: Long?): AppResult<Unit> = runAppCatching(
        mapError = { AppError.Database(it.message, it) },
    ) {
        dao.updateNotificationState(id, status.name, count, notifiedAt, snoozeUntil, System.currentTimeMillis())
    }

    override suspend fun delete(id: String): AppResult<Unit> = runAppCatching(
        mapError = { AppError.Database(it.message, it) },
    ) {
        dao.deleteById(id)
    }
}
