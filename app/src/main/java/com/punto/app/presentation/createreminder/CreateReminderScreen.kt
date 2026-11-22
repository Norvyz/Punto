package com.punto.app.presentation.createreminder

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punto.app.R
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.domain.model.TriggerType
import com.punto.app.presentation.mock.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderScreen(onBack: () -> Unit, onSaved: () -> Unit = onBack, viewModel: CreateReminderViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.dismissError() } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (state.isEdit) R.string.create_edit_title else R.string.create_title), fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null) } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(state.name, viewModel::onName, label = { Text(stringResource(R.string.create_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.subject, viewModel::onSubject, label = { Text(stringResource(R.string.create_subject)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(state.description, viewModel::onDescription, label = { Text(stringResource(R.string.create_description)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)

            if (state.locations.isEmpty()) {
                Text("No hay ubicaciones guardadas. Crea un lugar en el mapa primero.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            } else {
                val selectedName = state.locations.firstOrNull { it.id == state.selectedLocationId }?.name ?: ""
                DropdownField(stringResource(R.string.create_location), selectedName, state.locations.map { it.name to it.id }) { viewModel.onLocation(it) }
            }

            Text("Tipo de activación", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf(TriggerType.ON_ENTER, TriggerType.ON_DWELL, TriggerType.ALREADY_INSIDE).forEach { t ->
                    FilterChip(selected = state.triggerType == t, onClick = { viewModel.onTrigger(t) }, label = { Text(t.label()) })
                }
            }
            // ON_EXIT prepared but not primary in this phase; keep available
            FilterChip(selected = state.triggerType == TriggerType.ON_EXIT, onClick = { viewModel.onTrigger(TriggerType.ON_EXIT) }, label = { Text("${TriggerType.ON_EXIT.label()} (preparado)") })

            if (state.triggerType == TriggerType.ON_DWELL) {
                DropdownField(stringResource(R.string.create_dwell), "${state.dwellMinutes} min", listOf(5, 10, 15, 30, 60).map { "$it min" to it }) { viewModel.onDwell(it) }
            }

            val repeatLabel = state.repeatIntervalMinutes?.let { "$it min" } ?: "Sin repetir"
            DropdownField(stringResource(R.string.create_repeat), repeatLabel, listOf(15, 30, 60, 120).map { "$it min" to (it as Int?) } + ("Sin repetir" to null)) { viewModel.onRepeat(it) }

            val limitLabel = state.maxNotifications?.let { "$it veces" } ?: "Sin límite"
            DropdownField(stringResource(R.string.create_limit), limitLabel, listOf(3, 5, 10).map { "$it veces" to (it as Int?) } + ("Sin límite" to null)) { viewModel.onMaxNotifications(it) }

            if (state.isEdit) {
                DropdownField("Estado", state.status.label(), ReminderStatus.entries.map { it.label() to it }) { viewModel.onStatus(it) }
            }

            ReminderSummary(state)

            Spacer(Modifier.height(8.dp))
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().height(52.dp), enabled = state.canSave) {
                Text(if (state.isSaving) "Guardando..." else stringResource(R.string.create_save))
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ReminderSummary(state: CreateReminderUiState) {
    val location = state.locations.firstOrNull { it.id == state.selectedLocationId }?.name ?: "—"
    val whenText = when (state.triggerType) {
        TriggerType.ON_ENTER -> "Al llegar a $location"
        TriggerType.ON_DWELL -> "Al permanecer ${state.dwellMinutes} min en $location"
        TriggerType.ON_EXIT -> "Al salir de $location"
        TriggerType.ALREADY_INSIDE -> "Si ya estás dentro de $location al crear"
    }
    val repeatText = state.repeatIntervalMinutes?.let { "Cada $it min" } ?: "Sin repetición"
    Column(Modifier.fillMaxWidth().padding(12.dp)) {
        Text("Resumen", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        Text("Qué: ${state.name.ifBlank { "—" }}", style = MaterialTheme.typography.bodySmall)
        Text("Dónde: $location", style = MaterialTheme.typography.bodySmall)
        Text("Cuándo: $whenText", style = MaterialTheme.typography.bodySmall)
        Text("Repetición: $repeatText", style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> DropdownField(label: String, value: String, options: List<Pair<String, T>>, modifier: Modifier = Modifier, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, label = { Text(label) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (labelOpt, optValue) -> DropdownMenuItem(text = { Text(labelOpt) }, onClick = { onSelect(optValue); expanded = false }) }
        }
    }
}
