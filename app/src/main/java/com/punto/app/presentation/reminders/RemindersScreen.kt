package com.punto.app.presentation.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punto.app.R
import com.punto.app.domain.model.Reminder
import com.punto.app.domain.model.ReminderStatus
import com.punto.app.presentation.components.EmptyState
import com.punto.app.presentation.components.ReminderCard
import com.punto.app.presentation.mock.MockReminder
import com.punto.app.presentation.mock.label
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun RemindersScreen(
    onCreateReminder: () -> Unit = {},
    onEditReminder: (String) -> Unit = {},
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.dismissError() } }

    // El texto del buscador vive aquí (instantáneo al teclear); el filtro se aplica con un pequeño
    // debounce para no recalcular y recomponer la lista completa en cada letra.
    val searchState = rememberSaveable { mutableStateOf(state.query) }
    LaunchedEffect(Unit) {
        snapshotFlow { searchState.value }
            .debounce(250)
            .collect { viewModel.onQueryChange(it) }
    }

    val filtered = remember(state.reminders, state.filter, state.query, state.locationsById) {
        state.reminders.filter { it.matchesFilter(state.filter) }.filter { r ->
            val loc = state.locationsById[r.locationId]?.name.orEmpty()
            state.query.isBlank() || r.title.contains(state.query, true) || (r.subject ?: "").contains(state.query, true) || loc.contains(state.query, true)
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.reminders_title), fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = onCreateReminder) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.reminders_create)) } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            SearchField(searchState, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReminderFilter.entries) { filter ->
                    FilterChip(selected = state.filter == filter, onClick = { viewModel.onFilterChange(filter) }, label = { Text(filter.label) })
                }
            }
            Spacer(Modifier.height(10.dp))
            if (filtered.isEmpty()) {
                EmptyState(Icons.Outlined.NotificationsNone, stringResource(R.string.reminders_empty_title), stringResource(R.string.reminders_empty_desc), Modifier.weight(1f))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { reminder ->
                        ReminderManageCard(
                            modifier = Modifier.animateItem(),
                            reminder = reminder,
                            locationName = state.locationsById[reminder.locationId]?.name ?: "Ubicación desconocida",
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { viewModel.delete(reminder.id) },
                            onComplete = { viewModel.setStatus(reminder, ReminderStatus.COMPLETED) },
                            onPause = { viewModel.setStatus(reminder, if (reminder.status == ReminderStatus.PAUSED) ReminderStatus.PENDING else ReminderStatus.PAUSED) },
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

/** Campo de búsqueda con estado local: solo este campo se recompone al teclear. */
@Composable
private fun SearchField(state: MutableState<String>, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.reminders_search)) },
        leadingIcon = { Icon(Icons.Outlined.Search, null) },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
    )
}

@Composable
private fun ReminderManageCard(modifier: Modifier = Modifier, reminder: Reminder, locationName: String, onEdit: () -> Unit, onDelete: () -> Unit, onComplete: () -> Unit, onPause: () -> Unit) {
    val mock = MockReminder(reminder.id, reminder.title, reminder.subject ?: "General", reminder.description ?: "", locationName, reminder.locationId, reminder.triggerType, reminder.dwellMinutes, reminder.repeatIntervalMinutes, reminder.status, reminder.status.label())
    ReminderCard(
        reminder = mock,
        modifier = modifier,
        footer = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                ActionIcon(Icons.Outlined.TaskAlt, "Completar", onComplete)
                ActionIcon(if (reminder.status == ReminderStatus.PAUSED) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, "Pausar/Reactivar", onPause)
                ActionIcon(Icons.Outlined.Edit, "Editar", onEdit)
                ActionIcon(Icons.Outlined.Delete, "Eliminar", onDelete)
            }
        },
    )
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(40.dp)) { Icon(icon, contentDescription = description, modifier = Modifier.size(22.dp)) }
}
