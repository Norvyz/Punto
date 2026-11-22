package com.punto.app.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.tasks.await

@HiltWorker
class DwellCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val reminderRepository: ReminderRepository,
    private val locationRepository: LocationRepository,
    private val processor: GeofenceEventProcessor,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(GeofenceConstants.EXTRA_REMINDER_ID) ?: return Result.failure()
        val reminder = reminderRepository.getById(reminderId) ?: return Result.success()
        if (reminder.status == ReminderStatus.COMPLETED || reminder.status == ReminderStatus.PAUSED) return Result.success()
        if (reminder.triggerType != TriggerType.ON_DWELL) return Result.success()

        val location = locationRepository.getById(reminder.locationId) ?: return Result.success()
        val hasPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            // Can't verify; keep waiting state, no activation
            return Result.success()
        }

        val fused = LocationServices.getFusedLocationProviderClient(applicationContext)
        val current = runCatching { fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await() }.getOrNull()
            ?: runCatching { fused.lastLocation.await() }.getOrNull()

        if (current == null) {
            // Conservative: do not activate without confirmation
            return Result.retry()
        }

        val results = FloatArray(1)
        android.location.Location.distanceBetween(current.latitude, current.longitude, location.latitude, location.longitude, results)
        val distance = results[0]
        if (distance <= location.radiusMeters + current.accuracy.coerceAtLeast(0f)) {
            processor.activate(reminder, location.name)
        } else {
            reminderRepository.updateStatus(reminder.id, ReminderStatus.WAITING_ARRIVAL)
        }
        return Result.success()
    }
}
