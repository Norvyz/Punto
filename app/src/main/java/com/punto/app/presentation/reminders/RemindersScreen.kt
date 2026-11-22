package com.punto.app.presentation.reminders

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersScreen(
    onCreateReminder: () -> Unit = {},
    onEditReminder: (String) -> Unit = {},
    viewModel: RemindersViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.errorMessage) { state.errorMessage?.let { snackbar.showSnackbar(it); viewModel.dismissError() } }

    val filtered = state.reminders.filter { it.matchesFilter(state.filter) }.filter { r ->
        val loc = state.locationsById[r.locationId]?.name.orEmpty()
        state.query.isBlank() || r.title.contains(state.query, true) || (r.subject ?: "").contains(state.query, true) || loc.contains(state.query, true)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.reminders_title), fontWeight = FontWeight.Bold) }) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = { FloatingActionButton(onClick = onCreateReminder) { Icon(Icons.Outlined.Add, contentDescription = stringResource(R.string.reminders_create)) } }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
            OutlinedTextField(
                value = state.query, onValueChange = viewModel::onQueryChange, modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.reminders_search)) },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReminderFilter.entries) { filter ->
                    FilterChip(selected = state.filter == filter, onClick = { viewModel.onFilterChange(filter) }, label = { Text(filter.label) })
                }
            }
            Spacer(Modifier.height(14.dp))
            if (filtered.isEmpty()) {
                EmptyState(Icons.Outlined.NotificationsNone, stringResource(R.string.reminders_empty_title), stringResource(R.string.reminders_empty_desc), Modifier.weight(1f))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
                    items(filtered, key = { it.id }) { reminder ->
                        ReminderManageCard(
                            reminder = reminder,
                            locationName = state.locationsById[reminder.locationId]?.name ?: "Ubicación desconocida",
                            onEdit = { onEditReminder(reminder.id) },
                            onDelete = { viewModel.delete(reminder.id) },
                            onComplete = { viewModel.setStatus(reminder, ReminderStatus.COMPLETED) },
                            onPause = { viewModel.setStatus(reminder, if (reminder.status == ReminderStatus.PAUSED) ReminderStatus.PENDING else ReminderStatus.PAUSED) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ReminderManageCard(reminder: Reminder, locationName: String, onEdit: () -> Unit, onDelete: () -> Unit, onComplete: () -> Unit, onPause: () -> Unit) {
    val mock = MockReminder(reminder.id, reminder.title, reminder.subject ?: "General", reminder.description ?: "", locationName, reminder.locationId, reminder.triggerType, reminder.dwellMinutes, reminder.repeatIntervalMinutes, reminder.status, reminder.status.label())
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        ReminderCard(mock)
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = onComplete) { Icon(Icons.Outlined.TaskAlt, contentDescription = "Completar") }
            IconButton(onClick = onPause) { Icon(if (reminder.status == ReminderStatus.PAUSED) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, contentDescription = "Pausar/Reactivar") }
            IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "Editar") }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "Eliminar") }
        }
    }
}
