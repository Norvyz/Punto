package com.punto.app.presentation.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.punto.app.core.common.AppResult
import com.punto.app.domain.model.Location
import com.punto.app.domain.model.PlaceCategory
import com.punto.app.domain.repository.LocationRepository
import com.punto.app.geofence.GeofenceSyncManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationRepository: LocationRepository,
    private val geofenceSyncManager: GeofenceSyncManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            locationRepository.observeLocations().collectLatest { locations ->
                _uiState.update { it.copy(locations = locations) }
            }
        }
    }



    fun selectLocation(id: String?) = _uiState.update { it.copy(selectedLocationId = id) }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }

    fun startCreate(lat: Double, lng: Double, address: String?) {
        _uiState.update {
            it.copy(
                showEditor = true,
                isEditing = false,
                draft = LocationDraft(
                    latitude = lat,
                    longitude = lng,
                    address = address.orEmpty(),
                    radiusMeters = 150,
                    createdAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun startEdit(location: Location) {
        _uiState.update {
            it.copy(
                showEditor = true,
                isEditing = true,
                selectedLocationId = location.id,
                draft = LocationDraft(
                    id = location.id,
                    name = location.name,
                    category = location.category,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    address = location.address.orEmpty(),
                    radiusMeters = location.radiusMeters,
                    createdAtEpochMillis = location.createdAtEpochMillis,
                ),
            )
        }
    }

    fun closeEditor() = _uiState.update { it.copy(showEditor = false) }

    fun updateDraft(transform: (LocationDraft) -> LocationDraft) =
        _uiState.update { it.copy(draft = transform(it.draft)) }

    /** Recibe los textos escritos en la hoja (viven en estado local mientras se teclea). */
    fun saveDraft(name: String, address: String) {
        updateDraft { it.copy(name = name, address = address) }
        persistDraft()
    }

    private fun persistDraft() {
        val draft = _uiState.value.draft
        val lat = draft.latitude ?: return
        val lng = draft.longitude ?: return
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "El nombre del lugar es obligatorio.") }
            return
        }
        val location = Location(
            id = draft.id ?: UUID.randomUUID().toString(),
            name = draft.name.trim(),
            category = draft.category,
            latitude = lat,
            longitude = lng,
            address = draft.address.trim().ifBlank { null },
            radiusMeters = draft.radiusMeters,
            createdAtEpochMillis = draft.createdAtEpochMillis,
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            when (val result = locationRepository.upsert(location)) {
                is AppResult.Success -> {
                    geofenceSyncManager.syncFromDatabase()
                    _uiState.update {
                        it.copy(isSaving = false, showEditor = false, selectedLocationId = location.id)
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(isSaving = false, errorMessage = result.error.message ?: "No se pudo guardar el lugar.")
                }
                AppResult.Loading -> Unit
            }
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            when (val result = locationRepository.delete(id)) {
                is AppResult.Success -> {
                    geofenceSyncManager.syncFromDatabase()
                    _uiState.update {
                        if (it.selectedLocationId == id) it.copy(selectedLocationId = null) else it
                    }
                }
                is AppResult.Error -> _uiState.update {
                    it.copy(errorMessage = result.error.message ?: "No se pudo eliminar el lugar.")
                }
                AppResult.Loading -> Unit
            }
        }
    }
}