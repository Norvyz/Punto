package com.punto.app.geofence

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.TriggerType
import com.punto.app.domain.repository.LocationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlreadyInsideChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val locationRepository: LocationRepository,
    private val processor: GeofenceEventProcessor,
) {
    suspend fun checkOnCreate(reminder: Reminder) {
        if (reminder.triggerType != TriggerType.ALREADY_INSIDE) return
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return
        val location = locationRepository.getById(reminder.locationId) ?: return
        val fused = LocationServices.getFusedLocationProviderClient(context)
        val current = runCatching { fused.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await() }.getOrNull()
            ?: runCatching { fused.lastLocation.await() }.getOrNull()
            ?: return
        val results = FloatArray(1)
        android.location.Location.distanceBetween(current.latitude, current.longitude, location.latitude, location.longitude, results)
        if (results[0] <= location.radiusMeters + current.accuracy.coerceAtLeast(0f)) {
            processor.activate(reminder, location.name)
        }
    }
}
