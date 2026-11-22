package com.punto.app.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {

    @Inject lateinit var scheduler: ReminderNotificationScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(GeofenceConstants.EXTRA_REMINDER_ID) ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    GeofenceConstants.ACTION_REMINDER_DONE -> scheduler.markDone(reminderId)
                    GeofenceConstants.ACTION_REMINDER_SNOOZE -> {
                        val minutes = intent.getIntExtra(GeofenceConstants.EXTRA_SNOOZE_MINUTES, GeofenceConstants.DEFAULT_SNOOZE_MINUTES)
                        scheduler.snooze(reminderId, minutes)
                    }
                }
            } catch (t: Throwable) {
                Log.e("PuntoNotif", "Action failed", t)
            } finally {
                pending.finish()
            }
        }
    }
}
