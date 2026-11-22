package com.punto.app.domain.repository

import com.punto.app.core.common.AppResult
import com.punto.app.domain.model.Location
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun observeLocations(): Flow<List<Location>>
    fun observeLocation(id: String): Flow<Location?>
    suspend fun getAll(): List<Location>
    suspend fun getById(id: String): Location?
    suspend fun upsert(location: Location): AppResult<Unit>
    suspend fun delete(id: String): AppResult<Unit>
}
