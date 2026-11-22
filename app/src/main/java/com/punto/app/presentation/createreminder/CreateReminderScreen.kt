package com.punto.app.presentation.createreminder

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.Surface
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
        // El botón de guardar queda fijo abajo: ya no hay que bajar para confirmar.
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                Button(
                    onClick = viewModel::save,
                    enabled = state.canSave,
                    modifier = Modifier
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text(if (state.isSaving) "Guardando..." else stringResource(R.string.create_save))
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(state.name, viewModel::onName, label = { Text(stringResource(R.string.create_name)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(state.subject, viewModel::onSubject, label = { Text(stringResource(R.string.create_subject)) }, modifier = Modifier.weight(1f), singleLine = true)
                if (state.locations.isEmpty()) {
                    Text(
                        "Primero crea un lugar en el mapa.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f).align(Alignment.CenterVertically),
                    )
                } else {
                    val selectedName = state.locations.firstOrNull { it.id == state.selectedLocationId }?.name ?: ""
                    DropdownField(stringResource(R.string.create_location), selectedName, state.locations.map { it.name to it.id }, Modifier.weight(1f)) { viewModel.onLocation(it) }
                }
            }

            OutlinedTextField(state.description, viewModel::onDescription, label = { Text(stringResource(R.string.create_description)) }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 4)

            Text(stringResource(R.string.create_trigger), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
                listOf(TriggerType.ON_ENTER, TriggerType.ON_DWELL, TriggerType.ON_EXIT, TriggerType.ALREADY_INSIDE).forEach { t ->
                    FilterChip(selected = state.triggerType == t, onClick = { viewModel.onTrigger(t) }, label = { Text(t.label()) })
                }
            }

            // Campos cortos de dos en dos para no alargar el formulario.
            val fields = buildList<@Composable (Modifier) -> Unit> {
                if (state.triggerType == TriggerType.ON_DWELL) {
                    add { m -> DropdownField(stringResource(R.string.create_dwell), "${state.dwellMinutes} min", listOf(5, 10, 15, 30, 60).map { "$it min" to it }, m) { viewModel.onDwell(it) } }
                }
                val repeatLabel = state.repeatIntervalMinutes?.let { "$it min" } ?: "Sin repetir"
                add { m -> DropdownField(stringResource(R.string.create_repeat), repeatLabel, listOf(15, 30, 60, 120).map { "$it min" to (it as Int?) } + ("Sin repetir" to null), m) { viewModel.onRepeat(it) } }
                val limitLabel = state.maxNotifications?.let { "$it veces" } ?: "Sin límite"
                add { m -> DropdownField(stringResource(R.string.create_limit), limitLabel, listOf(3, 5, 10).map { "$it veces" to (it as Int?) } + ("Sin límite" to null), m) { viewModel.onMaxNotifications(it) } }
                if (state.isEdit) {
                    add { m -> DropdownField("Estado", state.status.label(), ReminderStatus.entries.map { it.label() to it }, m) { viewModel.onStatus(it) } }
                }
            }
            Column(Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                fields.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        pair.forEach { field -> field(Modifier.weight(1f)) }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            ReminderSummary(state)
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
    val repeatText = state.repeatIntervalMinutes?.let { "repite cada $it min" } ?: "sin repetición"
    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainerLow, modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Text(
            "$whenText · $repeatText",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        )
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
