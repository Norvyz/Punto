package com.punto.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.punto.app.domain.repository.UserPreferencesRepository
import com.punto.app.geofence.GeofenceSyncManager
import com.punto.app.geofence.ReminderNotificationHelper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PuntoApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var geofenceSyncManager: GeofenceSyncManager
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            val config = ReminderNotificationHelper.resolveChannelConfig(userPreferencesRepository)
            ReminderNotificationHelper.ensureChannel(this@PuntoApplication, config)
            geofenceSyncManager.syncFromDatabase()
        }
    }
}
