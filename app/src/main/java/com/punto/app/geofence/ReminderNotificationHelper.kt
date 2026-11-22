package com.punto.app.geofence

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.punto.app.MainActivity
import com.punto.app.R
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.first

object ReminderNotificationHelper {

    data class ChannelConfig(val id: String, val soundUri: Uri, val vibration: Boolean)

    suspend fun resolveChannelConfig(prefsRepo: UserPreferencesRepository?): ChannelConfig {
        val prefs = prefsRepo?.let { runCatching { it.preferences.first() }.getOrNull() }
        val soundUri = prefs?.notificationSoundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val vibration = prefs?.vibrationEnabled ?: true
        return ChannelConfig(channelIdFor(prefs?.notificationSoundUri, vibration), soundUri, vibration)
    }

    fun ensureChannel(context: Context, config: ChannelConfig): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            // Remove old Punto channels to avoid stale settings accumulation
            nm.notificationChannels.filter { it.id.startsWith(GeofenceConstants.CHANNEL_BASE_ID) && it.id != config.id }.forEach { nm.deleteNotificationChannel(it.id) }
            if (nm.getNotificationChannel(config.id) == null) {
                val channel = NotificationChannel(config.id, "Recordatorios de lugar", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Avisos cuando llegas o permaneces en tus lugares"
                    setSound(config.soundUri, android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build())
                    enableVibration(config.vibration)
                }
                nm.createNotificationChannel(channel)
            }
        }
        return config.id
    }

    suspend fun showReminder(context: Context, reminder: Reminder, locationName: String, prefsRepo: UserPreferencesRepository? = null) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val prefs = prefsRepo?.let { runCatching { it.preferences.first() }.getOrNull() }
        val config = ChannelConfig(
            id = channelIdFor(prefs?.notificationSoundUri, prefs?.vibrationEnabled ?: true),
            soundUri = prefs?.notificationSoundUri?.let(Uri::parse) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            vibration = prefs?.vibrationEnabled ?: true,
        )
        val channelId = ensureChannel(context, config)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(GeofenceConstants.EXTRA_REMINDER_ID, reminder.id)
        }
        val pending = PendingIntent.getActivity(context, reminder.id.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val snoozeMinutes = prefs?.defaultSnoozeMinutes ?: GeofenceConstants.DEFAULT_SNOOZE_MINUTES
        val doneIntent = actionIntent(context, GeofenceConstants.ACTION_REMINDER_DONE, reminder.id, 1)
        val snoozeIntent = actionIntent(context, GeofenceConstants.ACTION_REMINDER_SNOOZE, reminder.id, 2, snoozeMinutes)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(reminder.title)
            .setContentText("$locationName · ${reminder.description ?: reminder.subject ?: "Recordatorio pendiente"}")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Qué: ${reminder.title}\nDónde: $locationName\n${reminder.description ?: ""}"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setSound(config.soundUri)
            .setDefaults(if (config.vibration) NotificationCompat.DEFAULT_VIBRATE else 0)
            .setContentIntent(pending)
            .addAction(0, "Realizado", doneIntent)
            .addAction(0, "Posponer ${snoozeMinutes} min", snoozeIntent)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId(reminder.id), notification)
    }

    private fun channelIdFor(soundUri: String?, vibration: Boolean): String {
        val raw = (soundUri ?: "default") + "|" + vibration
        return "${GeofenceConstants.CHANNEL_BASE_ID}_${raw.hashCode().toUInt().toString(16)}"
    }

    fun cancel(context: Context, reminderId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(reminderId))
    }

    private fun actionIntent(context: Context, action: String, reminderId: String, reqOffset: Int, snoozeMinutes: Int = GeofenceConstants.DEFAULT_SNOOZE_MINUTES): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(GeofenceConstants.EXTRA_REMINDER_ID, reminderId)
            if (action == GeofenceConstants.ACTION_REMINDER_SNOOZE) {
                putExtra(GeofenceConstants.EXTRA_SNOOZE_MINUTES, snoozeMinutes)
            }
        }
        val req = (reminderId.hashCode() * 31) + reqOffset
        return PendingIntent.getBroadcast(context, req, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun notificationId(reminderId: String): Int = reminderId.hashCode()
}
