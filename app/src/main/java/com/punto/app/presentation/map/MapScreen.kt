package com.punto.app.presentation.map

import android.Manifest
import android.content.Context
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.punto.app.R
import com.punto.app.domain.model.Location
import com.punto.app.domain.model.PlaceCategory
import com.punto.app.presentation.components.EmptyState
import com.punto.app.presentation.components.SectionTitle
import com.punto.app.presentation.mock.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

private data class CapturedLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float?,
    val address: String?,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(viewModel: MapViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var hasLocationPermission by remember { mutableStateOf(hasFineOrCoarseLocation(context)) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var pendingDeleteId by remember { mutableStateOf<String?>(null) }
    var isLocating by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf<CapturedLocation?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) hasLocationPermission = hasFineOrCoarseLocation(context)
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    fun captureCurrentLocation() {
        if (!hasLocationPermission) {
            showPermissionDialog = true
            return
        }
        scope.launch {
            isLocating = true
            val result = getCurrentLocation(context)
            isLocating = false
            if (result != null) {
                captured = result
            } else {
                snackbar.showSnackbar("No se pudo obtener tu ubicación. Activa el GPS, verifica permisos y espera señal.")
            }
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.dismissError() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.map_title), fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { captureCurrentLocation() },
                icon = { Icon(Icons.Outlined.MyLocation, null) },
                text = { Text(if (isLocating) "Ubicando..." else "Mi ubicación") },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                CaptureCard(
                    isLocating = isLocating,
                    captured = captured,
                    hasLocationPermission = hasLocationPermission,
                    onCapture = { captureCurrentLocation() },
                    onSave = {
                        val c = captured ?: return@CaptureCard
                        viewModel.startCreate(c.latitude, c.longitude, c.address)
                    },
                    onRequestPermission = { showPermissionDialog = true },
                )
            }

            item { SectionTitle(stringResource(R.string.map_saved_places)) }

            if (state.locations.isEmpty()) {
                item { EmptyState(Icons.Outlined.LocationOn, stringResource(R.string.map_empty_title), stringResource(R.string.map_empty_desc)) }
            } else {
                items(state.locations, key = { it.id }) { location ->
                    LocationRow(
                        location = location,
                        onEdit = { viewModel.startEdit(location) },
                        onDelete = { pendingDeleteId = location.id },
                        onSelect = { viewModel.selectLocation(location.id) },
                    )
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permiso de ubicación") },
            text = { Text("Punto necesita tu ubicación para capturar coordenadas exactas y activar recordatorios en tus lugares. La ubicación se procesa en el dispositivo; no se envían coordenadas a servidores propios.") },
            confirmButton = {
                TextButton(onClick = {
                    showPermissionDialog = false
                    permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Permitir") }
            },
            dismissButton = { TextButton(onClick = { showPermissionDialog = false }) { Text("Ahora no") } },
        )
    }

    if (pendingDeleteId != null) {
        val id = pendingDeleteId!!
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Eliminar lugar") },
            text = { Text("También se eliminarán los recordatorios asociados a este lugar por integridad de datos. ¿Deseas continuar?") },
            confirmButton = { TextButton(onClick = { viewModel.deleteLocation(id); pendingDeleteId = null }) { Text("Eliminar") } },
            dismissButton = { TextButton(onClick = { pendingDeleteId = null }) { Text("Cancelar") } },
        )
    }

    if (state.showEditor) {
        ModalBottomSheet(onDismissRequest = viewModel::closeEditor, sheetState = sheetState) {
            LocationEditor(
                state = state,
                onName = { viewModel.updateDraft { d -> d.copy(name = it) } },
                onCategory = { viewModel.updateDraft { d -> d.copy(category = it) } },
                onAddress = { viewModel.updateDraft { d -> d.copy(address = it) } },
                onRadius = { viewModel.updateDraft { d -> d.copy(radiusMeters = it) } },
                onSave = viewModel::saveDraft,
                onCancel = viewModel::closeEditor,
            )
        }
    }
}

@Composable
private fun CaptureCard(
    isLocating: Boolean,
    captured: CapturedLocation?,
    hasLocationPermission: Boolean,
    onCapture: () -> Unit,
    onSave: () -> Unit,
    onRequestPermission: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.GpsFixed, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Capturar ubicación actual", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Usa el GPS del teléfono para obtener coordenadas exactas sin depender de un mapa visual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!hasLocationPermission) {
                Text(
                    "Necesitas conceder permiso de ubicación para capturar tu posición.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(onClick = onRequestPermission) { Text("Conceder permiso") }
            } else {
                Button(onClick = onCapture, enabled = !isLocating, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.MyLocation, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isLocating) "Obteniendo ubicación..." else "Obtener mi ubicación actual")
                }
            }

            if (captured != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Ubicación capturada", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text("Lat: ${"%.6f".format(captured.latitude)}", style = MaterialTheme.typography.bodyMedium)
                        Text("Lng: ${"%.6f".format(captured.longitude)}", style = MaterialTheme.typography.bodyMedium)
                        captured.accuracyMeters?.let {
                            Text("Precisión: ±${it.toInt()} m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            captured.address?.takeIf { it.isNotBlank() } ?: "Dirección no disponible (puedes escribirla al guardar).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(4.dp))
                        Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.AddLocationAlt, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar este lugar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(location: Location, onEdit: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    location.address ?: "Lat ${"%.5f".format(location.latitude)}, Lng ${"%.5f".format(location.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${location.category.label()} · ${location.radiusMeters} m",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationEditor(
    state: MapUiState,
    onName: (String) -> Unit,
    onCategory: (PlaceCategory) -> Unit,
    onAddress: (String) -> Unit,
    onRadius: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val draft = state.draft
    Column(
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(if (state.isEditing) "Editar lugar" else "Guardar lugar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Las coordenadas se capturan desde tu dispositivo. La precisión real depende del GPS, señales disponibles y Android.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (draft.latitude != null && draft.longitude != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow), shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("Coordenadas", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Lat: ${"%.6f".format(draft.latitude)}", style = MaterialTheme.typography.bodySmall)
                    Text("Lng: ${"%.6f".format(draft.longitude)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        OutlinedTextField(draft.name, onName, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.address, onAddress, label = { Text("Dirección (opcional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PlaceCategory.entries.forEach { cat ->
                FilterChip(selected = draft.category == cat, onClick = { onCategory(cat) }, label = { Text(cat.label()) })
            }
        }

        Text("Radio de detección: ${draft.radiusMeters} m", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(50, 100, 150, 300).forEach { r ->
                FilterChip(selected = draft.radiusMeters == r, onClick = { onRadius(r) }, label = { Text("$r m") })
            }
        }
        Slider(value = draft.radiusMeters.toFloat(), onValueChange = { onRadius(it.toInt()) }, valueRange = 50f..500f, steps = 8)

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                Text(if (state.isSaving) "Guardando..." else "Guardar lugar")
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

private fun hasFineOrCoarseLocation(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private suspend fun getCurrentLocation(context: Context): CapturedLocation? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
        val current = client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token).await()
        val loc = current ?: client.lastLocation.await() ?: return null
        val latLng = Pair(loc.latitude, loc.longitude)
        val address = reverseGeocode(context, latLng.first, latLng.second)
        CapturedLocation(
            latitude = latLng.first,
            longitude = latLng.second,
            accuracyMeters = if (loc.hasAccuracy()) loc.accuracy else null,
            address = address,
        )
    } catch (_: Exception) {
        null
    }
}

private suspend fun reverseGeocode(context: Context, lat: Double, lng: Double): String? = withContext(Dispatchers.IO) {
    runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1) { cont.resume(it) }
            }
        } else {
            @Suppress("DEPRECATION") geocoder.getFromLocation(lat, lng, 1)
        }
        results?.firstOrNull()?.getAddressLine(0)
    }.getOrNull()
}
