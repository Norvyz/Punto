package com.punto.app.presentation.navigation

object PuntoRoutes {
    const val WELCOME = "welcome"
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val MAIN = "main"
    const val CREATE_REMINDER = "create_reminder"
    const val EDIT_REMINDER = "create_reminder?reminderId={reminderId}"

    fun editReminder(reminderId: String) = "create_reminder?reminderId=$reminderId"
}
