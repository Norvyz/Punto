package com.punto.app.geofence

object GeofenceConstants {
    const val ACTION_GEOFENCE_EVENT = "com.punto.app.action.GEOFENCE_EVENT"
    const val ACTION_PROCESS_DWELL = "com.punto.app.action.PROCESS_DWELL"
    const val ACTION_REMINDER_DONE = "com.punto.app.action.REMINDER_DONE"
    const val ACTION_REMINDER_SNOOZE = "com.punto.app.action.REMINDER_SNOOZE"
    const val EXTRA_REQUEST_ID = "extra_request_id"
    const val EXTRA_TRANSITION = "extra_transition"
    const val EXTRA_REMINDER_ID = "extra_reminder_id"
    const val EXTRA_SNOOZE_MINUTES = "extra_snooze_minutes"
    const val CHANNEL_BASE_ID = "punto_reminders"
    const val DWELL_WORK_PREFIX = "dwell_"
    const val REPEAT_WORK_PREFIX = "repeat_"
    const val MIN_RADIUS_METERS = 50f
    const val MAX_RADIUS_METERS = 500f
    const val LOITERING_DELAY_MS_MIN = 30_000
    const val DEFAULT_SNOOZE_MINUTES = 15
}