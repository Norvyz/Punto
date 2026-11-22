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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.punto.app.R
import com.punto.app.domain.model.Location
import com.punto.app.domain.model.PlaceCategory
import com.punto.app.presentation.components.EmptyState
import com.punto.app.presentation.components.SectionTitle
import com.punto.app.presentation.mock.label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

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
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) hasLocationPermission = hasFineOrCoarseLocation(context) }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        hasLocationPermission = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    val defaultLatLng = LatLng(4.7110, -74.0721)
    val cameraPositionState = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(defaultLatLng, 12f) }
    var centeredOnUser by remember { mutableStateOf(false) }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && !centeredOnUser) {
            val loc = getCurrentLatLng(context)
            if (loc != null) {
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 16f))
                centeredOnUser = true
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
                onClick = {
                    val target = cameraPositionState.position.target
                    viewModel.startCreate(target.latitude, target.longitude, null)
                },
                icon = { Icon(Icons.Outlined.AddLocationAlt, null) },
                text = { Text(stringResource(R.string.map_add_place)) },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                val mapsKeyMissing = remember { isMapsApiKeyMissing(context) }
                if (mapsKeyMissing) {
                    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) {
                        Text(
                            "Google Maps no tiene una API key válida. Configura MAPS_API_KEY en local.properties o como variable de entorno y recompila la app.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                }
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.map_search)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                val bias = cameraPositionState.position.target
                                val result = geocode(context, state.searchQuery, bias)
                                if (result != null) {
                                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(result, 15f))
                                    viewModel.startCreate(result.latitude, result.longitude, state.searchQuery)
                                } else {
                                    snackbar.showSnackbar("No se encontró la dirección. Intenta con otra búsqueda.")
                                }
                            }
                        }) { Icon(Icons.Outlined.Search, contentDescription = "Buscar") }
                    },
                    singleLine = true, shape = MaterialTheme.shapes.extraLarge,
                )
            }
            item {
                Card(Modifier.fillMaxWidth().height(320.dp), shape = RoundedCornerShape(24.dp)) {
                    Box(Modifier.fillMaxSize()) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
                            uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
                            onMapLongClick = { latLng ->
                                scope.launch {
                                    val address = reverseGeocode(context, latLng)
                                    viewModel.startCreate(latLng.latitude, latLng.longitude, address)
                                }
                            },
                        ) {
                            state.locations.forEach { location ->
                                val pos = LatLng(location.latitude, location.longitude)
                                Marker(state = MarkerState(pos), title = location.name, snippet = location.address, onClick = { viewModel.selectLocation(location.id); false })
                                Circle(center = pos, radius = location.radiusMeters.toDouble(), fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), strokeColor = MaterialTheme.colorScheme.primary, strokeWidth = 2f)
                            }
                        }
                        Icon(
                            Icons.Outlined.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.align(Alignment.Center).size(34.dp).padding(bottom = 17.dp),
                        )
                        Column(Modifier.align(Alignment.TopEnd).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            SmallMapAction(Icons.Outlined.MyLocation, "Mi ubicación") {
                                if (!hasLocationPermission) { showPermissionDialog = true; return@SmallMapAction }
                                scope.launch {
                                    val loc = getCurrentLatLng(context)
                                    if (loc != null) cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(loc, 16f))
                                    else snackbar.showSnackbar("No se pudo obtener tu ubicación actual. Activa GPS y espera señal.")
                                }
                            }
                        }
                        Button(
                            onClick = {
                                val target = cameraPositionState.position.target
                                scope.launch {
                                    val address = reverseGeocode(context, target)
                                    viewModel.startCreate(target.latitude, target.longitude, address)
                                }
                            },
                            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = if (!hasLocationPermission) 72.dp else 12.dp),
                        ) {
                            Icon(Icons.Outlined.AddLocationAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Elegir este punto")
                        }
                        if (!hasLocationPermission) {
                            PermissionBanner(Modifier.align(Alignment.BottomCenter), onRequest = { showPermissionDialog = true })
                        }
                    }
                }
            }
            item {
                Text("Mantén presionado el mapa, usa el botón +, o toca el pin del centro para guardar justo donde está el mapa.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item { SectionTitle(stringResource(R.string.map_saved_places)) }
            if (state.locations.isEmpty()) {
                item { EmptyState(Icons.Outlined.LocationOn, stringResource(R.string.map_empty_title), stringResource(R.string.map_empty_desc)) }
            } else {
                items(state.locations, key = { it.id }) { location ->
                    LocationRow(location = location, onEdit = { viewModel.startEdit(location) }, onDelete = { pendingDeleteId = location.id }, onSelect = {
                        viewModel.selectLocation(location.id)
                        scope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude, location.longitude), 16f)) }
                    })
                }
            }
            item { Spacer(Modifier.height(90.dp)) }
        }
    }

    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permiso de ubicación") },
            text = { Text("Punto necesita tu ubicación para mostrarte en el mapa y activar recordatorios en tus lugares. La ubicación se procesa en el dispositivo; no se envían coordenadas a servidores propios.") },
            confirmButton = { TextButton(onClick = { showPermissionDialog = false; permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) }) { Text("Permitir") } },
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
            LocationEditor(state = state, onName = { viewModel.updateDraft { d -> d.copy(name = it) } }, onCategory = { viewModel.updateDraft { d -> d.copy(category = it) } }, onAddress = { viewModel.updateDraft { d -> d.copy(address = it) } }, onRadius = { viewModel.updateDraft { d -> d.copy(radiusMeters = it) } }, onSave = viewModel::saveDraft, onCancel = viewModel::closeEditor)
        }
    }
}

@Composable
private fun SmallMapAction(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Card(shape = CircleShape, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        IconButton(onClick = onClick) { Icon(icon, contentDescription = desc) }
    }
}

@Composable
private fun PermissionBanner(modifier: Modifier, onRequest: () -> Unit) {
    Card(modifier = modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Activa la ubicación para ver tu posición en el mapa.", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRequest) { Text("Permitir") }
        }
    }
}

@Composable
private fun LocationRow(location: Location, onEdit: () -> Unit, onDelete: () -> Unit, onSelect: () -> Unit) {
    Card(onClick = onSelect, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(location.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(location.address ?: "Sin dirección", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${location.category.label()} · ${location.radiusMeters} m", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationEditor(state: MapUiState, onName: (String) -> Unit, onCategory: (PlaceCategory) -> Unit, onAddress: (String) -> Unit, onRadius: (Int) -> Unit, onSave: () -> Unit, onCancel: () -> Unit) {
    val draft = state.draft
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (state.isEditing) "Editar lugar" else "Guardar lugar", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("La precisión real depende del dispositivo, señales disponibles y restricciones de Android.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(draft.name, onName, label = { Text("Nombre") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(draft.address, onAddress, label = { Text("Dirección") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Text("Categoría", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            PlaceCategory.entries.forEach { cat ->
                FilterChip(selected = draft.category == cat, onClick = { onCategory(cat) }, label = { Text(cat.label()) })
            }
        }
        Text("Radio de detección: ${draft.radiusMeters} m", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(50, 100, 150, 300).forEach { r -> FilterChip(selected = draft.radiusMeters == r, onClick = { onRadius(r) }, label = { Text("$r m") }) }
        }
        Slider(value = draft.radiusMeters.toFloat(), onValueChange = { onRadius(it.toInt()) }, valueRange = 50f..500f, steps = 8)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = onSave, enabled = !state.isSaving, modifier = Modifier.weight(1f)) { Text(if (state.isSaving) "Guardando..." else "Guardar") }
        }
        Spacer(Modifier.height(24.dp))
    }
}

private fun hasFineOrCoarseLocation(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
    return fine || coarse
}

private suspend fun geocode(context: Context, query: String, bias: LatLng? = null): LatLng? = withContext(Dispatchers.IO) {
    if (query.isBlank()) return@withContext null
    runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                if (bias != null) {
                    geocoder.getFromLocationName(query, 5, bias.latitude - 0.5, bias.longitude - 0.5, bias.latitude + 0.5, bias.longitude + 0.5) { cont.resume(it) }
                } else {
                    geocoder.getFromLocationName(query, 5) { cont.resume(it) }
                }
            }
        } else {
            @Suppress("DEPRECATION")
            if (bias != null) geocoder.getFromLocationName(query, 5, bias.latitude - 0.5, bias.longitude - 0.5, bias.latitude + 0.5, bias.longitude + 0.5)
            else geocoder.getFromLocationName(query, 5)
        }
        pickBestGeocodeResult(results, bias)?.let { LatLng(it.latitude, it.longitude) }
    }.getOrNull()
}

private fun pickBestGeocodeResult(results: List<android.location.Address>?, bias: LatLng?): android.location.Address? {
    val list = results.orEmpty()
    if (list.isEmpty()) return null
    if (bias == null) return list.first()
    return list.minByOrNull { addr ->
        val dLat = addr.latitude - bias.latitude
        val dLng = addr.longitude - bias.longitude
        dLat * dLat + dLng * dLng
    }
}

private fun isMapsApiKeyMissing(context: Context): Boolean = runCatching {
    val appInfo = context.packageManager.getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
    val key = appInfo.metaData?.getString("com.google.android.geo.API_KEY").orEmpty()
    key.isBlank() || key == "YOUR_MAPS_API_KEY_HERE"
}.getOrDefault(false)

private suspend fun reverseGeocode(context: Context, latLng: LatLng): String? = withContext(Dispatchers.IO) {
    runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val results = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1) { cont.resume(it) }
            }
        } else {
            @Suppress("DEPRECATION") geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        }
        results?.firstOrNull()?.getAddressLine(0)
    }.getOrNull()
}

private suspend fun getCurrentLatLng(context: Context): LatLng? {
    val client = LocationServices.getFusedLocationProviderClient(context)
    return try {
        val current = client.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            com.google.android.gms.tasks.CancellationTokenSource().token,
        ).await()
        val loc = current ?: client.lastLocation.await()
        loc?.let { LatLng(it.latitude, it.longitude) }
    } catch (_: Exception) {
        null
    }
}
