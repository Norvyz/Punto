package com.punto.app.presentation.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.punto.app.R
import com.punto.app.presentation.components.EmptyState
import com.punto.app.presentation.components.ReminderCard
import com.punto.app.presentation.mock.MockReminder
import com.punto.app.presentation.mock.label

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.history_title), fontWeight = FontWeight.Bold) }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (state.completed.isEmpty()) {
                item { EmptyState(Icons.Outlined.History, stringResource(R.string.history_empty_title), stringResource(R.string.history_empty_desc)) }
            } else {
                items(state.completed, key = { it.id }) { reminder ->
                    val loc = state.locationsById[reminder.locationId]?.name ?: "Ubicación"
                    ReminderCard(modifier = Modifier.animateItem(), reminder = MockReminder(reminder.id, reminder.title, reminder.subject ?: "General", reminder.description ?: "", loc, reminder.locationId, reminder.triggerType, reminder.dwellMinutes, reminder.repeatIntervalMinutes, reminder.status, reminder.status.label()))
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}
