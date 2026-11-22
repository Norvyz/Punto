package com.punto.app.presentation.map

import com.punto.app.domain.model.Location
import com.punto.app.domain.model.PlaceCategory

data class MapUiState(
    val locations: List<Location> = emptyList(),
    val selectedLocationId: String? = null,
    val draft: LocationDraft = LocationDraft(),
    val isEditing: Boolean = false,
    val showEditor: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

data class LocationDraft(
    val id: String? = null,
    val name: String = "",
    val category: PlaceCategory = PlaceCategory.HOME,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val address: String = "",
    val radiusMeters: Int = 150,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)