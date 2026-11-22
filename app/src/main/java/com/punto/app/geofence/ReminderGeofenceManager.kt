package com.punto.app.geofence

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.punto.app.domain.model.Location
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderGeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client = LocationServices.getGeofencingClient(context)

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasBackgroundPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true

    fun pendingIntent(): PendingIntent {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            action = GeofenceConstants.ACTION_GEOFENCE_EVENT
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getBroadcast(context, 1001, intent, flags)
    }

    @Suppress("MissingPermission")
    suspend fun syncAll(locations: List<Location>, reminders: List<Reminder>): Result<Unit> = runCatching {
        val activeReminders = reminders.filter { it.status != ReminderStatus.COMPLETED && it.status != ReminderStatus.PAUSED }
        val locationIds = activeReminders.map { it.locationId }.toSet()
        val targets = locations.filter { it.id in locationIds }
        if (targets.isEmpty()) {
            runCatching { client.removeGeofences(pendingIntent()).await() }
            return@runCatching
        }
        if (!hasLocationPermission()) return@runCatching
        val geofences = targets.map { loc -> buildGeofence(loc, activeReminders.filter { it.locationId == loc.id }) }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER or GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofences(geofences)
            .build()
        client.addGeofences(request, pendingIntent()).await()
    }

    suspend fun removeLocation(locationId: String): Result<Unit> = runCatching {
        client.removeGeofences(listOf(locationId)).await()
    }

    private fun buildGeofence(location: Location, reminders: List<Reminder>): Geofence {
        val needsDwell = reminders.any { it.triggerType == TriggerType.ON_DWELL }
        val needsExit = reminders.any { it.triggerType == TriggerType.ON_EXIT }
        var transitionTypes = Geofence.GEOFENCE_TRANSITION_ENTER
        if (needsDwell) transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_DWELL
        if (needsExit) transitionTypes = transitionTypes or Geofence.GEOFENCE_TRANSITION_EXIT
        val loiterMs = reminders.filter { it.triggerType == TriggerType.ON_DWELL }
            .mapNotNull { it.dwellMinutes }
            .minOrNull()
            ?.let { (it * 60_000).coerceAtLeast(GeofenceConstants.LOITERING_DELAY_MS_MIN) }
            ?: GeofenceConstants.LOITERING_DELAY_MS_MIN
        return Geofence.Builder()
            .setRequestId(location.id)
            .setCircularRegion(location.latitude, location.longitude, location.radiusMeters.toFloat().coerceIn(GeofenceConstants.MIN_RADIUS_METERS, GeofenceConstants.MAX_RADIUS_METERS))
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transitionTypes)
            .setLoiteringDelay(loiterMs)
            .build()
    }
}
