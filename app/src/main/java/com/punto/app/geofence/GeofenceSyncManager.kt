package com.punto.app.geofence

import android.util.Log
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceSyncManager @Inject constructor(
    private val locationRepository: LocationRepository,
    private val reminderRepository: ReminderRepository,
    private val geofenceManager: ReminderGeofenceManager,
    private val notificationScheduler: ReminderNotificationScheduler,
) {
    suspend fun syncFromDatabase() {
        val locations = locationRepository.getAll()
        val reminders = reminderRepository.getAll()
        val result = geofenceManager.syncAll(locations, reminders)
        result.onFailure { Log.w("PuntoGeofence", "Geofence sync failed", it) }
        notificationScheduler.reschedulePending()
    }
}
