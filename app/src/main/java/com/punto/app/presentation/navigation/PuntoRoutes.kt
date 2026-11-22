package com.punto.app.presentation.navigation

object PuntoRoutes {
    /** Configuración inicial: solo la primera vez que se abre la app. */
    const val SETUP = "setup"
    const val MAIN = "main"
    const val CREATE_REMINDER = "create_reminder"
    const val EDIT_REMINDER = "create_reminder?reminderId={reminderId}"

    fun editReminder(reminderId: String) = "create_reminder?reminderId=$reminderId"
}
