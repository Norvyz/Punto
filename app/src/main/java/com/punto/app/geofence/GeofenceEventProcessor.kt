package com.punto.app.geofence

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceEventProcessor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reminderRepository: ReminderRepository,
    private val locationRepository: LocationRepository,
    private val notificationScheduler: ReminderNotificationScheduler,
) {
    suspend fun handleTransition(locationId: String, transition: Int) {
        val reminders = reminderRepository.getByLocation(locationId)
        val location = locationRepository.getById(locationId) ?: return
        for (reminder in reminders) {
            if (reminder.status == ReminderStatus.COMPLETED || reminder.status == ReminderStatus.PAUSED) continue
            when (transition) {
                com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER -> onEnter(reminder, location.name)
                com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_DWELL -> onDwell(reminder, location.name)
                com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT -> onExit(reminder)
            }
        }
    }

    private suspend fun onEnter(reminder: Reminder, locationName: String) {
        when (reminder.triggerType) {
            TriggerType.ON_ENTER, TriggerType.ALREADY_INSIDE -> activate(reminder, locationName)
            TriggerType.ON_DWELL -> {
                reminderRepository.updateStatus(reminder.id, ReminderStatus.WAITING_DWELL)
                scheduleDwell(reminder)
            }
            TriggerType.ON_EXIT -> reminderRepository.updateStatus(reminder.id, ReminderStatus.WAITING_ARRIVAL)
        }
    }

    private suspend fun onDwell(reminder: Reminder, locationName: String) {
        if (reminder.triggerType == TriggerType.ON_DWELL) {
            activate(reminder, locationName)
        }
    }

    private suspend fun onExit(reminder: Reminder) {
        WorkManager.getInstance(context).cancelUniqueWork(GeofenceConstants.DWELL_WORK_PREFIX + reminder.id)
        when (reminder.triggerType) {
            TriggerType.ON_EXIT -> {
                val locationName = locationRepository.getById(reminder.locationId)?.name ?: "tu lugar"
                activate(reminder, locationName)
            }
            TriggerType.ON_DWELL -> reminderRepository.updateStatus(reminder.id, ReminderStatus.WAITING_ARRIVAL)
            else -> if (reminder.status == ReminderStatus.WAITING_DWELL) reminderRepository.updateStatus(reminder.id, ReminderStatus.WAITING_ARRIVAL)
        }
    }

    private fun scheduleDwell(reminder: Reminder) {
        val minutes = reminder.dwellMinutes ?: return
        val req = OneTimeWorkRequestBuilder<DwellCheckWorker>()
            .setInitialDelay(minutes.toLong(), TimeUnit.MINUTES)
            .setInputData(workDataOf(GeofenceConstants.EXTRA_REMINDER_ID to reminder.id))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            GeofenceConstants.DWELL_WORK_PREFIX + reminder.id,
            ExistingWorkPolicy.REPLACE,
            req,
        )
        Log.d("PuntoGeofence", "Scheduled dwell for ${reminder.id} in $minutes min")
    }

    suspend fun activate(reminder: Reminder, locationName: String) {
        val current = reminderRepository.getById(reminder.id) ?: return
        if (current.status == ReminderStatus.COMPLETED || current.status == ReminderStatus.PAUSED) return
        // Idempotency guard: avoid duplicate activation bursts from geofence re-registration/events.
        val now = System.currentTimeMillis()
        val last = current.lastNotifiedAtEpochMillis ?: 0L
        if (current.status == ReminderStatus.ACTIVE && (now - last) < 60_000L) return
        notificationScheduler.trigger(current.id)
    }
}
