package com.punto.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.punto.app.data.local.db.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY updatedAtEpochMillis DESC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE locationId = :locationId ORDER BY updatedAtEpochMillis DESC")
    fun observeByLocation(locationId: String): Flow<List<ReminderEntity>>

    @Upsert
    suspend fun upsert(entity: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM reminders")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE locationId = :locationId")
    suspend fun getByLocation(locationId: String): List<ReminderEntity>

    @Query("UPDATE reminders SET status = :status, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, updatedAt: Long)

    @Query("UPDATE reminders SET status = :status, notificationCount = :count, lastNotifiedAtEpochMillis = :notifiedAt, snoozeUntilEpochMillis = :snoozeUntil, updatedAtEpochMillis = :updatedAt WHERE id = :id")
    suspend fun updateNotificationState(id: String, status: String, count: Int, notifiedAt: Long?, snoozeUntil: Long?, updatedAt: Long)
}
