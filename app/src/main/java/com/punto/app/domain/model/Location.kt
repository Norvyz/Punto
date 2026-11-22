package com.punto.app.domain.model

data class Location(
    val id: String,
    val name: String,
    val category: PlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val address: String?,
    val radiusMeters: Int,
    val createdAtEpochMillis: Long,
)
