package com.punto.app.di

import android.content.Context
import androidx.room.Room
import com.punto.app.data.local.db.PuntoDatabase
import com.punto.app.data.local.db.dao.LocationDao
import com.punto.app.data.local.db.dao.ReminderDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PuntoDatabase =
        Room.databaseBuilder(context, PuntoDatabase::class.java, PuntoDatabase.NAME)
            .addMigrations(PuntoDatabase.MIGRATION_1_2, PuntoDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideLocationDao(database: PuntoDatabase): LocationDao = database.locationDao()

    @Provides
    fun provideReminderDao(database: PuntoDatabase): ReminderDao = database.reminderDao()
}
