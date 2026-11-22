package com.punto.app.di

import com.punto.app.data.repository.DataStoreUserPreferencesRepository
import com.punto.app.data.repository.RoomLocationRepository
import com.punto.app.data.repository.RoomReminderRepository
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.domain.repository.ReminderRepository
import com.punto.app.domain.repository.UserPreferencesRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindLocationRepository(impl: RoomLocationRepository): LocationRepository

    @Binds
    @Singleton
    abstract fun bindReminderRepository(impl: RoomReminderRepository): ReminderRepository

    @Binds
    @Singleton
    abstract fun bindUserPreferencesRepository(
        impl: DataStoreUserPreferencesRepository,
    ): UserPreferencesRepository
}
