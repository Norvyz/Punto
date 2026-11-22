package com.punto.app.data.mapper

import com.punto.app.data.local.db.entity.LocationEntity
import com.punto.app.domain.model.Location
import com.punto.app.domain.model.PlaceCategory

fun LocationEntity.toDomain(): Location = Location(
    id = id,
    name = name,
    category = runCatching { PlaceCategory.valueOf(category) }.getOrDefault(PlaceCategory.CUSTOM),
    latitude = latitude,
    longitude = longitude,
    address = address,
    radiusMeters = radiusMeters,
    createdAtEpochMillis = createdAtEpochMillis,
)

fun Location.toEntity(): LocationEntity = LocationEntity(
    id = id,
    name = name,
    category = category.name,
    latitude = latitude,
    longitude = longitude,
    address = address,
    radiusMeters = radiusMeters,
    createdAtEpochMillis = createdAtEpochMillis,
)
