package com.punto.app.geofence

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import com.punto.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderNotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val locationRepository: LocationRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
) {
    suspend fun trigger(reminderId: String) {
        val reminder = reminderRepository.getById(reminderId) ?: return
        if (reminder.status == ReminderStatus.COMPLETED || reminder.status == ReminderStatus.PAUSED) return
        val max = reminder.maxNotifications
        if (max != null && reminder.notificationCount >= max) return
        // Dedup: geofence may re-deliver; avoid double notification in short window.
        val now = System.currentTimeMillis()
        val last = reminder.lastNotifiedAtEpochMillis ?: 0L
        if ((now - last) < 45_000L) return

        val locationName = locationRepository.getById(reminder.locationId)?.name ?: "tu lugar"
        ReminderNotificationHelper.showReminder(context, reminder, locationName, userPreferencesRepository)

        val nextCount = reminder.notificationCount + 1
        val hasMoreRepeats = reminder.repeatIntervalMinutes != null && (max == null || nextCount < max)
        reminderRepository.updateNotificationState(reminder.id, ReminderStatus.ACTIVE, nextCount, now, null)

        cancelRepeat(reminder.id)
        if (hasMoreRepeats) {
            scheduleRepeat(reminder.id, reminder.repeatIntervalMinutes!!.toLong(), TimeUnit.MINUTES)
        }
    }

    suspend fun markDone(reminderId: String) {
        cancelRepeat(reminderId)
        ReminderNotificationHelper.cancel(context, reminderId)
        reminderRepository.updateNotificationState(reminderId, ReminderStatus.COMPLETED, reminderRepository.getById(reminderId)?.notificationCount ?: 0, System.currentTimeMillis(), null)
    }

    suspend fun snooze(reminderId: String, minutes: Int) {
        val reminder = reminderRepository.getById(reminderId) ?: return
        cancelRepeat(reminderId)
        ReminderNotificationHelper.cancel(context, reminderId)
        val until = System.currentTimeMillis() + minutes.toLong() * 60_000L
        reminderRepository.updateNotificationState(reminderId, ReminderStatus.SNOOZED, reminder.notificationCount, reminder.lastNotifiedAtEpochMillis, until)
        scheduleRepeat(reminderId, minutes.toLong(), TimeUnit.MINUTES)
    }

    fun cancelRepeat(reminderId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(GeofenceConstants.REPEAT_WORK_PREFIX + reminderId)
    }

    private fun scheduleRepeat(reminderId: String, delay: Long, unit: TimeUnit) {
        val req = OneTimeWorkRequestBuilder<ReminderRepeatWorker>()
            .setInitialDelay(delay, unit)
            .setInputData(workDataOf(GeofenceConstants.EXTRA_REMINDER_ID to reminderId))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            GeofenceConstants.REPEAT_WORK_PREFIX + reminderId,
            ExistingWorkPolicy.REPLACE,
            req,
        )
    }

    suspend fun reschedulePending() {
        val reminders = reminderRepository.getAll()
        val now = System.currentTimeMillis()
        for (r in reminders) {
            when {
                r.status == ReminderStatus.COMPLETED || r.status == ReminderStatus.PAUSED -> cancelRepeat(r.id)
                r.status == ReminderStatus.SNOOZED && r.snoozeUntilEpochMillis != null -> {
                    val delay = (r.snoozeUntilEpochMillis - now).coerceAtLeast(0L)
                    scheduleRepeat(r.id, delay, TimeUnit.MILLISECONDS)
                }
                r.status == ReminderStatus.ACTIVE && r.repeatIntervalMinutes != null -> {
                    val max = r.maxNotifications
                    if (max != null && r.notificationCount >= max) continue
                    val last = r.lastNotifiedAtEpochMillis ?: now
                    val delay = (last + r.repeatIntervalMinutes * 60_000L - now).coerceAtLeast(0L)
                    scheduleRepeat(r.id, delay, TimeUnit.MILLISECONDS)
                }
            }
        }
    }
}
