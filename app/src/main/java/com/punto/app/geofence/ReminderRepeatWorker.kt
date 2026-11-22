package com.punto.app.geofence

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderRepeatWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val scheduler: ReminderNotificationScheduler,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(GeofenceConstants.EXTRA_REMINDER_ID) ?: return Result.failure()
        val reminder = reminderRepository.getById(reminderId) ?: return Result.success()
        if (reminder.status == ReminderStatus.COMPLETED || reminder.status == ReminderStatus.PAUSED) return Result.success()
        scheduler.trigger(reminderId)
        return Result.success()
    }
}
