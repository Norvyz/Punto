package com.punto.app.data.repository

import com.punto.app.core.common.AppResult
import com.punto.app.core.error.AppError
import com.punto.app.core.error.runAppCatching
import com.punto.app.data.local.db.dao.LocationDao
import com.punto.app.data.mapper.toDomain
import com.punto.app.data.mapper.toEntity
import com.punto.app.domain.model.Location
import com.punto.app.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomLocationRepository @Inject constructor(
    private val dao: LocationDao,
) : LocationRepository {

    override fun observeLocations(): Flow<List<Location>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override fun observeLocation(id: String): Flow<Location?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun getAll(): List<Location> = dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Location? = dao.getById(id)?.toDomain()

    override suspend fun upsert(location: Location): AppResult<Unit> = runAppCatching(
        mapError = { AppError.Database(it.message, it) },
    ) {
        dao.upsert(location.toEntity())
    }

    override suspend fun delete(id: String): AppResult<Unit> = runAppCatching(
        mapError = { AppError.Database(it.message, it) },
    ) {
        dao.deleteById(id)
    }
}
